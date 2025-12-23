import net from 'net';
import fs from 'fs';
import path from 'path';
import crypto from 'crypto';

/**
 * File Transfer Server - TCP Socket thuần túy
 * Protocol tự định nghĩa để transfer file giữa clients
 * 
 * Protocol Format:
 * 1. Header (JSON): {type, fileName, fileSize, senderId, receiverId, checksum}
 * 2. Body: Binary file data
 * 3. Footer: "EOF"
 */
export class FileTransferServer {
  constructor(port = 9000) {
    this.port = port;
    this.server = null;
    this.clients = new Map(); // userId -> socket
    this.uploadDir = path.join(process.cwd(), 'uploads');
    
    // Tạo thư mục uploads nếu chưa có
    if (!fs.existsSync(this.uploadDir)) {
      fs.mkdirSync(this.uploadDir, { recursive: true });
    }
  }

  /**
   * Khởi động server
   */
  start() {
    this.server = net.createServer((socket) => {
      console.log(`[FileTransfer] Client connected: ${socket.remoteAddress}:${socket.remotePort}`);
      
      let userId = null;
      let headerReceived = false;
      let fileHeader = null;
      let fileBuffer = Buffer.alloc(0);
      let expectedSize = 0;

      // Xử lý dữ liệu nhận được
      socket.on('data', (data) => {
        try {
          if (!headerReceived) {
            // Đọc header (JSON)
            const headerEnd = data.indexOf('\n');
            if (headerEnd !== -1) {
              const headerStr = data.slice(0, headerEnd).toString();
              fileHeader = JSON.parse(headerStr);
              
              console.log('[FileTransfer] Header received:', fileHeader);
              
              // Xác thực user
              if (fileHeader.type === 'AUTH') {
                userId = fileHeader.userId;
                this.clients.set(userId, socket);
                socket.write(JSON.stringify({ status: 'OK', message: 'Authenticated' }) + '\n');
                return;
              }
              
              if (fileHeader.type === 'FILE') {
                headerReceived = true;
                expectedSize = fileHeader.fileSize;
                fileBuffer = data.slice(headerEnd + 1);
                
                // Gửi ACK
                socket.write(JSON.stringify({ status: 'READY' }) + '\n');
              }
            }
          } else {
            // Nhận file data
            fileBuffer = Buffer.concat([fileBuffer, data]);
            
            console.log(`[FileTransfer] Received ${fileBuffer.length}/${expectedSize} bytes`);
            
            // Kiểm tra đã nhận đủ chưa
            if (fileBuffer.length >= expectedSize) {
              this.handleFileReceived(socket, fileHeader, fileBuffer.slice(0, expectedSize));
              
              // Reset state
              headerReceived = false;
              fileHeader = null;
              fileBuffer = Buffer.alloc(0);
              expectedSize = 0;
            }
          }
        } catch (error) {
          console.error('[FileTransfer] Error:', error);
          socket.write(JSON.stringify({ status: 'ERROR', message: error.message }) + '\n');
        }
      });

      socket.on('end', () => {
        console.log('[FileTransfer] Client disconnected');
        if (userId) {
          this.clients.delete(userId);
        }
      });

      socket.on('error', (err) => {
        console.error('[FileTransfer] Socket error:', err);
      });
    });

    this.server.listen(this.port, () => {
      console.log(`[FileTransfer] Server listening on port ${this.port}`);
    });
  }

  /**
   * Xử lý file đã nhận đủ
   */
  handleFileReceived(socket, header, fileData) {
    try {
      // Verify checksum
      const receivedChecksum = crypto.createHash('md5').update(fileData).digest('hex');
      
      if (receivedChecksum !== header.checksum) {
        socket.write(JSON.stringify({ 
          status: 'ERROR', 
          message: 'Checksum mismatch' 
        }) + '\n');
        return;
      }

      // Lưu file
      const fileName = `${Date.now()}_${header.fileName}`;
      const filePath = path.join(this.uploadDir, fileName);
      
      fs.writeFileSync(filePath, fileData);
      
      console.log(`[FileTransfer] File saved: ${fileName} (${fileData.length} bytes)`);
      
      // Gửi ACK
      socket.write(JSON.stringify({ 
        status: 'SUCCESS', 
        fileName: fileName,
        fileUrl: `/uploads/${fileName}`
      }) + '\n');
      
      // Forward file tới receiver nếu online
      if (header.receiverId) {
        this.forwardFileToReceiver(header, fileName, fileData);
      }
      
    } catch (error) {
      console.error('[FileTransfer] Save error:', error);
      socket.write(JSON.stringify({ 
        status: 'ERROR', 
        message: error.message 
      }) + '\n');
    }
  }

  /**
   * Chuyển tiếp file tới người nhận
   */
  forwardFileToReceiver(header, fileName, fileData) {
    const receiverSocket = this.clients.get(header.receiverId);
    
    if (receiverSocket) {
      // Gửi notification
      const notification = {
        type: 'FILE_RECEIVED',
        senderId: header.senderId,
        senderName: header.senderName,
        fileName: header.fileName,
        fileSize: header.fileSize,
        fileType: header.fileType,
        fileUrl: `/uploads/${fileName}`
      };
      
      receiverSocket.write(JSON.stringify(notification) + '\n');
      console.log(`[FileTransfer] Forwarded to user ${header.receiverId}`);
    } else {
      console.log(`[FileTransfer] Receiver ${header.receiverId} offline, file saved for later`);
    }
  }

  /**
   * Dừng server
   */
  stop() {
    if (this.server) {
      this.server.close();
      this.clients.clear();
    }
  }
}
