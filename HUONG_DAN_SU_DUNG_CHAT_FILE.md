# HƯỚNG DẪN SỬ DỤNG TÍNH NĂNG CHAT VÀ GỬI FILE

## 📋 TỔNG QUAN

Hệ thống chat trong Chess Game hỗ trợ:
- ✅ Gửi/nhận tin nhắn văn bản
- ✅ Gửi/nhận file (tối đa 10MB)
- ✅ **Hiển thị ảnh trực tiếp trong chat** (JPG, PNG, GIF, BMP, WEBP)
- ✅ Hiển thị file với nút tải xuống
- ✅ Tự động format kích thước file
- ✅ Xử lý file trùng tên

---

## 🎨 GIAO DIỆN CHAT

### Vị trí:
- Nằm ở sidebar bên phải màn hình game
- Bao gồm:
  - Khu vực hiển thị tin nhắn (ScrollPane)
  - Ô nhập tin nhắn (TextField)
  - Nút gửi file 📎 (màu xanh lá)
  - Nút gửi tin nhắn "Gửi" (màu xanh dương)

### Màu sắc:
- **Tin nhắn của bạn**: Bubble màu xanh dương (#4a9eff), căn phải
- **Tin nhắn đối thủ**: Bubble màu nâu (#4a4541), căn trái
- **Tin nhắn hệ thống**: Màu xám, in nghiêng, căn giữa

---

## 💬 CÁCH SỬ DỤNG CHAT

### Gửi tin nhắn văn bản:
1. Nhập tin nhắn vào ô "Nhập tin nhắn..."
2. Nhấn Enter hoặc click nút "Gửi"
3. Tin nhắn sẽ hiển thị ngay lập tức

### Nhận tin nhắn:
- Tin nhắn từ đối thủ tự động hiển thị
- Chat tự động scroll xuống tin nhắn mới nhất

---

## 📎 CÁCH GỬI FILE

### Bước 1: Chọn file
1. Click nút 📎 (màu xanh lá)
2. Cửa sổ FileChooser sẽ mở ra
3. Chọn file muốn gửi (tối đa 10MB)

### Bước 2: Gửi file
- File sẽ được đọc và gửi qua P2P
- Hiển thị trong chat với:
  - Icon 📄 (file thông thường) hoặc 📷 (ảnh)
  - Tên file
  - Kích thước file (KB/MB/GB)
  - **Preview ảnh** (nếu là file ảnh)

### File ảnh được hỗ trợ:
- ✅ JPG / JPEG
- ✅ PNG
- ✅ GIF
- ✅ BMP
- ✅ WEBP

### Hiển thị ảnh:
- Ảnh được hiển thị trực tiếp trong chat
- Kích thước tối đa: 200px width (giữ tỷ lệ)
- Có shadow effect để nổi bật
- Vẫn có nút "Tải xuống" để lưu ảnh gốc

### Giới hạn:
- **Kích thước tối đa**: 10MB
- **Định dạng**: Tất cả các loại file
- **Phương thức**: P2P qua TCP Socket

### Lưu ý:
- File được encode Base64 để gửi qua JSON
- Không lưu file trên server
- File chỉ tồn tại trong memory cho đến khi tải xuống

---

## 📥 CÁCH NHẬN FILE

### Khi nhận được file:
1. File hiển thị trong chat với:
   - Tên người gửi
   - **Preview ảnh** (nếu là file ảnh - JPG, PNG, GIF, BMP, WEBP)
   - Icon 📷 (ảnh) hoặc 📄 (file thông thường) và tên file
   - Kích thước file
   - Nút "Tải xuống" (màu xanh lá)

### Tải xuống file:
1. Click nút "Tải xuống"
2. Chọn thư mục lưu file
3. File sẽ được lưu vào thư mục đã chọn

### Xử lý file trùng tên:
- Nếu file đã tồn tại, tự động thêm số vào tên
- Ví dụ: `document.pdf` → `document_1.pdf` → `document_2.pdf`

### Thông báo:
- Sau khi lưu thành công, hiển thị tin nhắn hệ thống:
  ```
  Đã lưu file: C:\Users\...\document.pdf
  ```

---

## 🔧 KỸ THUẬT IMPLEMENTATION

### 1. Gửi file (Client → Đối thủ)

**Flow:**
```
User click 📎
    ↓
FileChooser mở
    ↓
Chọn file
    ↓
Kiểm tra kích thước (< 10MB)
    ↓
Đọc file thành byte[]
    ↓
Encode Base64
    ↓
Gửi qua PeerNetworkHandler
    ↓
JSON: {"type": "file", "filename": "...", "fileSize": 123, "fileData": "base64..."}
    ↓
Hiển thị trong chat (người gửi)
```

**Code:**
```java
// GameController.java
private void handleSendFile() {
    FileChooser fileChooser = new FileChooser();
    File selectedFile = fileChooser.showOpenDialog(window);
    
    if (selectedFile != null) {
        // Kiểm tra kích thước
        if (selectedFile.length() > 10 * 1024 * 1024) {
            showAlert("File quá lớn!");
            return;
        }
        
        // Đọc file
        byte[] fileData = Files.readAllBytes(selectedFile.toPath());
        
        // Gửi qua P2P
        peerNetworkHandler.sendFile(filename, fileData);
        
        // Hiển thị trong chat
        chatManager.addFileMessage(playerName, filename, fileSize, true, null);
    }
}
```

### 2. Nhận file (Đối thủ → Client)

**Flow:**
```
Nhận JSON từ socket
    ↓
Parse JSON
    ↓
Decode Base64 → byte[]
    ↓
Callback onFileReceived
    ↓
Hiển thị trong chat với nút "Tải xuống"
    ↓
User click "Tải xuống"
    ↓
DirectoryChooser mở
    ↓
Chọn thư mục
    ↓
Ghi file vào disk
    ↓
Thông báo thành công
```

**Code:**
```java
// PeerNetworkHandler.java
private void handleReceivedFile(JSONObject json) {
    String filename = json.getString("filename");
    long fileSize = json.getLong("fileSize");
    String fileDataBase64 = json.getString("fileData");
    
    // Decode Base64
    byte[] fileData = Base64.getDecoder().decode(fileDataBase64);
    
    // Callback
    Platform.runLater(() -> 
        onFileReceived.onFile(filename, fileSize, fileData)
    );
}

// GameController.java
peerNetworkHandler.setOnFileReceived((filename, fileSize, fileData) -> {
    chatManager.addFileMessage(
        opponentName,
        filename,
        fileSize,
        false,
        () -> handleDownloadFile(filename, fileData)
    );
});
```

### 3. Hiển thị file trong chat

**Code:**
```java
// ChatManager.java
public void addFileMessage(String sender, String filename, long fileSize, 
                          boolean isPlayer, Runnable onDownload, byte[] fileData) {
    // Tạo bubble chat
    VBox bubble = new VBox(5);
    bubble.setStyle("-fx-background-color: " + bgColor + ";");
    
    // Tên người gửi
    Label senderLabel = new Label(sender);
    bubble.getChildren().add(senderLabel);
    
    // Kiểm tra xem có phải file ảnh không
    if (isImageFile(filename) && fileData != null) {
        try {
            // Tạo ImageView để hiển thị ảnh
            Image image = new Image(new ByteArrayInputStream(fileData));
            ImageView imageView = new ImageView(image);
            
            // Giới hạn kích thước ảnh
            imageView.setFitWidth(200);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
            
            // Thêm shadow effect
            imageView.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 2);");
            
            bubble.getChildren().add(imageView);
            
            // Label tên file (nhỏ hơn)
            Label fileNameLabel = new Label("📷 " + filename);
            bubble.getChildren().add(fileNameLabel);
            
        } catch (Exception e) {
            // Nếu không load được ảnh, hiển thị như file thông thường
            addFileIconAndName(bubble, filename);
        }
    } else {
        // File không phải ảnh, hiển thị icon file
        Label fileIcon = new Label("📄 " + filename);
        bubble.getChildren().add(fileIcon);
    }
    
    // Kích thước file
    Label sizeLabel = new Label(formatFileSize(fileSize));
    bubble.getChildren().add(sizeLabel);
    
    // Nếu là file nhận được, thêm nút tải xuống
    if (!isPlayer && onDownload != null) {
        Button downloadBtn = new Button("Tải xuống");
        downloadBtn.setOnAction(e -> onDownload.run());
        bubble.getChildren().add(downloadBtn);
    }
    
    chatMessagesBox.getChildren().add(bubble);
}

// Kiểm tra file ảnh
private boolean isImageFile(String filename) {
    String lowerName = filename.toLowerCase();
    return lowerName.endsWith(".jpg") || 
           lowerName.endsWith(".jpeg") || 
           lowerName.endsWith(".png") || 
           lowerName.endsWith(".gif") || 
           lowerName.endsWith(".bmp") ||
           lowerName.endsWith(".webp");
}
```

---

## 🔒 BẢO MẬT & GIỚI HẠN

### Giới hạn kích thước:
- **10MB**: Đủ cho hầu hết file văn bản, hình ảnh, tài liệu
- Lý do: Tránh tràn memory và timeout khi encode/decode Base64

### Bảo mật:
- ✅ File chỉ gửi qua P2P (không qua server)
- ✅ Không lưu file trên server
- ✅ File tồn tại trong memory cho đến khi tải xuống
- ⚠️ Không có mã hóa file (có thể thêm AES encryption)
- ⚠️ Không có virus scan (nên thêm)

### Performance:
- **Base64 encoding**: Tăng kích thước file ~33%
- **Memory usage**: File được load toàn bộ vào RAM
- **Network**: Gửi qua TCP socket (reliable)

---

## 🐛 XỬ LÝ LỖI

### Lỗi có thể xảy ra:

#### 1. File quá lớn (> 10MB)
```
Alert: "File không được vượt quá 10MB!"
```

#### 2. Không đọc được file
```
Alert: "Không thể đọc file: [error message]"
```

#### 3. Không lưu được file
```
Alert: "Không thể lưu file: [error message]"
```

#### 4. Mất kết nối P2P
- File không được gửi
- Không có thông báo lỗi (có thể cải thiện)

---

## 🚀 HƯỚNG PHÁT TRIỂN

### Tính năng có thể thêm:

#### 1. Progress Bar
- Hiển thị tiến trình upload/download
- Đặc biệt quan trọng với file lớn

#### 2. File Preview
- Hiển thị thumbnail cho hình ảnh
- Preview PDF, video

#### 3. Drag & Drop
- Kéo thả file vào chat để gửi

#### 4. File History
- Lưu lịch sử file đã gửi/nhận
- Có thể tải lại file cũ

#### 5. Multiple Files
- Gửi nhiều file cùng lúc
- Nén thành ZIP

#### 6. Encryption
- Mã hóa file bằng AES
- Chỉ đối thủ mới giải mã được

#### 7. Virus Scan
- Scan file trước khi tải xuống
- Tích hợp ClamAV hoặc VirusTotal API

#### 8. Compression
- Nén file trước khi gửi (GZIP)
- Giảm băng thông

#### 9. Resume Transfer
- Tiếp tục gửi nếu bị ngắt kết nối
- Chia file thành chunks

#### 10. File Type Restrictions
- Chặn file thực thi (.exe, .bat, .sh)
- Chỉ cho phép file an toàn

---

## 📊 SO SÁNH VỚI CÁC GIẢI PHÁP KHÁC

### 1. Base64 qua JSON (Hiện tại)
**Ưu điểm:**
- ✅ Đơn giản, dễ implement
- ✅ Sử dụng cùng socket với chat
- ✅ Không cần port riêng

**Nhược điểm:**
- ❌ Tăng kích thước file 33%
- ❌ Phải load toàn bộ file vào RAM
- ❌ Không có progress tracking

### 2. Binary Transfer qua Socket riêng
**Ưu điểm:**
- ✅ Không tăng kích thước
- ✅ Có thể stream (không cần load hết vào RAM)
- ✅ Nhanh hơn

**Nhược điểm:**
- ❌ Phức tạp hơn
- ❌ Cần mở port riêng
- ❌ Cần protocol riêng

### 3. HTTP File Upload/Download
**Ưu điểm:**
- ✅ Chuẩn hóa
- ✅ Có progress tracking
- ✅ Có thể resume

**Nhược điểm:**
- ❌ Cần server lưu file
- ❌ Tốn băng thông server
- ❌ Không phải P2P

---

## 🎓 KẾT LUẬN

Tính năng gửi file qua chat đã được implement thành công với:
- ✅ Giao diện thân thiện
- ✅ Xử lý lỗi tốt
- ✅ P2P transfer (không qua server)
- ✅ Hỗ trợ mọi loại file
- ✅ Tự động xử lý file trùng tên

Đây là một tính năng hữu ích cho người chơi chia sẻ:
- Ảnh chụp màn hình
- Tài liệu chiến thuật
- Replay file
- Bất kỳ file nào khác

**Lưu ý**: Đây là implementation cơ bản, phù hợp cho môi trường LAN và file nhỏ. Với production environment, nên cải thiện thêm về bảo mật, performance và user experience.
