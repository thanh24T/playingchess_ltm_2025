# CHANGELOG - Tính năng gửi file qua Chat

## 📅 Ngày: 24/12/2024

## ✅ Đã hoàn thành

### 1. **UI Updates**
- ✅ Thêm nút gửi file 📎 (màu xanh lá) vào `game.fxml`
- ✅ Nút nằm bên trái ô nhập tin nhắn

### 2. **ChatManager.java**
- ✅ Thêm field `sendFileButton`
- ✅ Thêm callback `onSendFile`
- ✅ Thêm method `sendFile()` - xử lý click nút gửi file
- ✅ Thêm method `addFileMessage()` - hiển thị file trong chat
- ✅ **Thêm method `isImageFile()` - kiểm tra file ảnh**
- ✅ **Thêm method `addFileIconAndName()` - hiển thị icon file**
- ✅ **Hiển thị preview ảnh trực tiếp trong chat (JPG, PNG, GIF, BMP, WEBP)**
- ✅ Thêm method `formatFileSize()` - format KB/MB/GB

### 3. **PeerNetworkHandler.java**
- ✅ Thêm interface `OnFileReceived`
- ✅ Thêm method `sendFile()` - gửi file qua P2P (Base64)
- ✅ Thêm method `handleReceivedFile()` - nhận file từ đối thủ
- ✅ Thêm callback `setOnFileReceived()`
- ✅ Xử lý message type "file" trong `listenForPeerMessages()`

### 4. **GameController.java**
- ✅ Thêm field `sendFileButton` (FXML binding)
- ✅ Cập nhật constructor ChatManager với sendFileButton
- ✅ Thêm callback `setOnSendFile()` → `handleSendFile()`
- ✅ Thêm callback `setOnFileReceived()` trong `setupNetworkCallbacks()`
- ✅ **Truyền `fileData` vào `addFileMessage()` để hiển thị ảnh**
- ✅ Thêm method `handleSendFile()`:
  - Mở FileChooser
  - Kiểm tra kích thước (max 10MB)
  - Đọc file thành byte[]
  - Gửi qua PeerNetworkHandler
  - Hiển thị trong chat (kèm preview ảnh nếu là file ảnh)
- ✅ Thêm method `handleDownloadFile()`:
  - Mở DirectoryChooser
  - Xử lý file trùng tên (thêm số)
  - Lưu file vào disk
  - Thông báo thành công
- ✅ Vô hiệu hóa nút gửi file khi chơi với AI

### 5. **Documentation**
- ✅ Tạo `HUONG_DAN_SU_DUNG_CHAT_FILE.md` - Hướng dẫn chi tiết
- ✅ Tạo `CHANGELOG_CHAT_FILE.md` - File này

## 🔧 Kỹ thuật

### Protocol gửi file:
```json
{
  "type": "file",
  "filename": "document.pdf",
  "fileSize": 1234567,
  "fileData": "base64_encoded_data..."
}
```

### Flow:
1. User click 📎
2. Chọn file (max 10MB)
3. Đọc file → byte[]
4. Encode Base64
5. Gửi qua TCP Socket (P2P)
6. Đối thủ nhận → Decode Base64
7. Hiển thị trong chat với nút "Tải xuống"
8. Click tải xuống → Chọn thư mục → Lưu file

## 🎨 UI

### Tin nhắn ảnh của bạn:
```
┌─────────────────────────┐
│ Bạn                     │
│ [Preview ảnh 200px]     │
│ 📷 photo.jpg            │
│ 856 KB                  │
└─────────────────────────┘
```

### Tin nhắn ảnh từ đối thủ:
```
┌─────────────────────────┐
│ Đối thủ                 │
│ [Preview ảnh 200px]     │
│ 📷 screenshot.png       │
│ 1.2 MB                  │
│ [Tải xuống]             │
└─────────────────────────┘
```

### Tin nhắn file thông thường:
```
┌─────────────────────────┐
│ Bạn                     │
│ 📄 document.pdf         │
│ 1.2 MB                  │
└─────────────────────────┘
```

## 🔒 Giới hạn

- **Kích thước**: Max 10MB
- **Phương thức**: P2P qua TCP Socket
- **Encoding**: Base64 (tăng 33% kích thước)
- **Bảo mật**: Không mã hóa (có thể thêm AES)

## 🐛 Bug Fixes

- ✅ Sửa lỗi syntax: Dấu `}` thừa trong GameController
- ✅ Sửa lỗi lambda: Biến `outputFile` không final

## 📝 Testing

### Để test:
1. Chạy server: `npm run dev`
2. Chạy 2 client
3. Đăng nhập 2 tài khoản
4. Tìm trận đấu
5. Trong game, click nút 📎
6. Chọn file (< 10MB)
7. File hiển thị trong chat
8. Đối thủ thấy file với nút "Tải xuống"
9. Click "Tải xuống" → Chọn thư mục → File được lưu

## 🚀 Hướng phát triển

- [x] **Preview ảnh trong chat** ✅ HOÀN THÀNH
- [ ] Progress bar cho upload/download
- [ ] Click ảnh để xem fullscreen
- [ ] Drag & drop file
- [ ] Mã hóa file (AES)
- [ ] Nén file (GZIP)
- [ ] Virus scan
- [ ] Resume transfer
- [ ] Multiple files
- [ ] File history
- [ ] Video preview
- [ ] Audio player

## ✨ Kết luận

Tính năng gửi file qua chat đã hoàn thành và sẵn sàng sử dụng!

**Tính năng mới**: ✨ **Hiển thị ảnh trực tiếp trong chat**
- Hỗ trợ: JPG, JPEG, PNG, GIF, BMP, WEBP
- Kích thước preview: 200px width (giữ tỷ lệ)
- Shadow effect đẹp mắt
- Vẫn có nút tải xuống để lưu ảnh gốc

**Build status**: ✅ SUCCESS
**Compilation**: ✅ PASSED
**Ready to test**: ✅ YES
