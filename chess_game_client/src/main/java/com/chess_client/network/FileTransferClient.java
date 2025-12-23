package com.chess_client.network;

import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.function.Consumer;

/**
 * File Transfer Client - TCP Socket thuần túy
 * Gửi file (ảnh/video/document) qua TCP socket
 * 
 * Kỹ thuật:
 * - TCP Socket programming
 * - Binary file transfer
 * - Checksum verification (MD5)
 * - Chunked transfer
 * - Progress callback
 */
public class FileTransferClient {
    
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 9000;
    private static final int CHUNK_SIZE = 8192; // 8KB chunks
    
    private Socket socket;
    private DataOutputStream out;
    private BufferedReader in;
    private int userId;
    
    /**
     * Kết nối tới File Transfer Server
     */
    public void connect(int userId) throws IOException {
        this.userId = userId;
        this.socket = new Socket(SERVER_HOST, SERVER_PORT);
        this.out = new DataOutputStream(socket.getOutputStream());
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        
        // Xác thực
        JSONObject auth = new JSONObject();
        auth.put("type", "AUTH");
        auth.put("userId", userId);
        
        out.write((auth.toString() + "\n").getBytes());
        out.flush();
        
        // Đợi ACK
        String response = in.readLine();
        JSONObject ack = new JSONObject(response);
        
        if (!"OK".equals(ack.getString("status"))) {
            throw new IOException("Authentication failed");
        }
        
        System.out.println("[FileTransfer] Connected and authenticated");
    }
    
    /**
     * Gửi file tới người nhận
     * 
     * @param file File cần gửi
     * @param receiverId ID người nhận
     * @param senderName Tên người gửi
     * @param progressCallback Callback để update progress (0.0 - 1.0)
     * @return URL của file trên server
     */
    public String sendFile(File file, int receiverId, String senderName, 
                          Consumer<Double> progressCallback) throws Exception {
        
        // Đọc file thành bytes
        byte[] fileData = Files.readAllBytes(file.toPath());
        
        // Tính checksum (MD5)
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(fileData);
        String checksum = bytesToHex(digest);
        
        // Xác định loại file
        String fileName = file.getName();
        String fileType = getFileType(fileName);
        
        // Tạo header
        JSONObject header = new JSONObject();
        header.put("type", "FILE");
        header.put("fileName", fileName);
        header.put("fileSize", fileData.length);
        header.put("fileType", fileType);
        header.put("senderId", userId);
        header.put("senderName", senderName);
        header.put("receiverId", receiverId);
        header.put("checksum", checksum);
        
        // Gửi header
        out.write((header.toString() + "\n").getBytes());
        out.flush();
        
        // Đợi server READY
        String readyResponse = in.readLine();
        JSONObject ready = new JSONObject(readyResponse);
        
        if (!"READY".equals(ready.getString("status"))) {
            throw new IOException("Server not ready");
        }
        
        // Gửi file data theo chunks
        int totalSent = 0;
        int chunkCount = (int) Math.ceil((double) fileData.length / CHUNK_SIZE);
        
        for (int i = 0; i < chunkCount; i++) {
            int start = i * CHUNK_SIZE;
            int end = Math.min(start + CHUNK_SIZE, fileData.length);
            int chunkLength = end - start;
            
            out.write(fileData, start, chunkLength);
            out.flush();
            
            totalSent += chunkLength;
            
            // Update progress
            if (progressCallback != null) {
                double progress = (double) totalSent / fileData.length;
                progressCallback.accept(progress);
            }
            
            System.out.printf("[FileTransfer] Sent chunk %d/%d (%.1f%%)\n", 
                i + 1, chunkCount, (double) totalSent / fileData.length * 100);
        }
        
        // Đợi server ACK
        String ackResponse = in.readLine();
        JSONObject ack = new JSONObject(ackResponse);
        
        if ("SUCCESS".equals(ack.getString("status"))) {
            String fileUrl = ack.getString("fileUrl");
            System.out.println("[FileTransfer] File sent successfully: " + fileUrl);
            return fileUrl;
        } else {
            throw new IOException("File transfer failed: " + ack.optString("message"));
        }
    }
    
    /**
     * Nhận file từ server (listener)
     */
    public void startReceiveListener(Consumer<FileNotification> onFileReceived) {
        new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    JSONObject notification = new JSONObject(line);
                    
                    if ("FILE_RECEIVED".equals(notification.getString("type"))) {
                        FileNotification fileNotif = new FileNotification(
                            notification.getInt("senderId"),
                            notification.getString("senderName"),
                            notification.getString("fileName"),
                            notification.getLong("fileSize"),
                            notification.getString("fileType"),
                            notification.getString("fileUrl")
                        );
                        
                        if (onFileReceived != null) {
                            onFileReceived.accept(fileNotif);
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("[FileTransfer] Receive listener error: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * Download file từ server
     */
    public void downloadFile(String fileUrl, File destination, 
                            Consumer<Double> progressCallback) throws IOException {
        // Tạo socket mới để download
        try (Socket downloadSocket = new Socket(SERVER_HOST, SERVER_PORT);
             DataOutputStream downloadOut = new DataOutputStream(downloadSocket.getOutputStream());
             DataInputStream downloadIn = new DataInputStream(downloadSocket.getInputStream())) {
            
            // Gửi request download
            JSONObject request = new JSONObject();
            request.put("type", "DOWNLOAD");
            request.put("fileUrl", fileUrl);
            request.put("userId", userId);
            
            downloadOut.write((request.toString() + "\n").getBytes());
            downloadOut.flush();
            
            // Nhận file size
            String sizeResponse = new BufferedReader(
                new InputStreamReader(downloadSocket.getInputStream())).readLine();
            JSONObject sizeInfo = new JSONObject(sizeResponse);
            long fileSize = sizeInfo.getLong("fileSize");
            
            // Nhận file data
            try (FileOutputStream fos = new FileOutputStream(destination)) {
                byte[] buffer = new byte[CHUNK_SIZE];
                long totalReceived = 0;
                int bytesRead;
                
                while (totalReceived < fileSize && 
                       (bytesRead = downloadIn.read(buffer, 0, 
                           (int) Math.min(buffer.length, fileSize - totalReceived))) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    totalReceived += bytesRead;
                    
                    if (progressCallback != null) {
                        progressCallback.accept((double) totalReceived / fileSize);
                    }
                }
                
                System.out.println("[FileTransfer] File downloaded: " + destination.getName());
            }
        }
    }
    
    /**
     * Đóng kết nối
     */
    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    // ===================== HELPER METHODS =====================
    
    private String getFileType(String fileName) {
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        
        return switch (ext) {
            case "jpg", "jpeg", "png", "gif", "bmp", "webp" -> "image";
            case "mp4", "avi", "mov", "mkv", "webm", "flv" -> "video";
            case "mp3", "wav", "ogg", "flac" -> "audio";
            case "pdf", "doc", "docx", "txt" -> "document";
            default -> "file";
        };
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    
    // ===================== INNER CLASS =====================
    
    public static class FileNotification {
        public final int senderId;
        public final String senderName;
        public final String fileName;
        public final long fileSize;
        public final String fileType;
        public final String fileUrl;
        
        public FileNotification(int senderId, String senderName, String fileName,
                               long fileSize, String fileType, String fileUrl) {
            this.senderId = senderId;
            this.senderName = senderName;
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.fileType = fileType;
            this.fileUrl = fileUrl;
        }
    }
}
