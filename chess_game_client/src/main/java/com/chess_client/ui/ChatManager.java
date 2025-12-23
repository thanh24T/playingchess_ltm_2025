package com.chess_client.ui;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Quản lý phần chat trong màn hình game.
 * Xử lý UI chat phía client: hiển thị tin nhắn, nhận input từ người dùng,
 * và gửi tin nhắn qua callback để truyền qua network.
 */
public class ChatManager {

    // ===================== FIELDS =====================
    
    private final ScrollPane chatScrollPane;  // ScrollPane chứa danh sách tin nhắn
    private final VBox chatMessagesBox;       // VBox chứa các tin nhắn
    private final TextField chatInput;        // Ô nhập tin nhắn
    private final Button sendMessageButton;   // Nút gửi tin nhắn
    private final Button sendFileButton;      // Nút gửi file
    
    private java.util.function.Consumer<String> onSendMessage; // Callback để gửi tin nhắn qua network
    private Runnable onSendFile;              // Callback để gửi file

    // ===================== CONSTRUCTOR =====================
    
    /**
     * Tạo ChatManager với các UI components từ FXML.
     * 
     * @param chatScrollPane   ScrollPane chứa tin nhắn
     * @param chatMessagesBox  VBox chứa các tin nhắn
     * @param chatInput        TextField để nhập tin nhắn
     * @param sendMessageButton Button để gửi tin nhắn
     * @param sendFileButton   Button để gửi file
     */
    public ChatManager(ScrollPane chatScrollPane,
            VBox chatMessagesBox,
            TextField chatInput,
            Button sendMessageButton,
            Button sendFileButton) {
        this.chatScrollPane = chatScrollPane;
        this.chatMessagesBox = chatMessagesBox;
        this.chatInput = chatInput;
        this.sendMessageButton = sendMessageButton;
        this.sendFileButton = sendFileButton;
    }

    // ===================== INITIALIZATION =====================
    
    /**
     * Thiết lập callback để gửi tin nhắn qua network.
     * Callback này sẽ được gọi khi người chơi gửi tin nhắn.
     * 
     * @param onSendMessage Callback nhận message String và gửi qua network
     */
    public void setOnSendMessage(java.util.function.Consumer<String> onSendMessage) {
        this.onSendMessage = onSendMessage;
    }

    /**
     * Thiết lập callback để gửi file qua network.
     * Callback này sẽ được gọi khi người chơi chọn file để gửi.
     * 
     * @param onSendFile Callback để xử lý gửi file
     */
    public void setOnSendFile(Runnable onSendFile) {
        this.onSendFile = onSendFile;
    }

    /**
     * Khởi tạo ChatManager: thiết lập event handlers.
     */
    public void initialize() {
        setupEventHandlers();
        setupChatEnterKey();
    }

    /**
     * Thiết lập event handler cho nút gửi tin nhắn và gửi file.
     */
    private void setupEventHandlers() {
        sendMessageButton.setOnAction(e -> sendMessage());
        sendFileButton.setOnAction(e -> sendFile());
    }

    /**
     * Thiết lập phím Enter để gửi tin nhắn (UX tốt hơn).
     */
    private void setupChatEnterKey() {
        chatInput.setOnAction(e -> sendMessage());
    }

    // ===================== MESSAGE SENDING =====================
    
    /**
     * Gửi tin nhắn từ người chơi.
     * Hiển thị tin nhắn trong chat box và gọi callback để gửi qua network.
     */
    public void sendMessage() {
        String message = chatInput.getText().trim();
        
        // Chỉ gửi nếu tin nhắn không rỗng
        if (!message.isEmpty()) {
            // Hiển thị tin nhắn của người chơi trong chat box
            addChatMessage("Bạn", message, true);
            
            // Xóa ô nhập
            chatInput.clear();
            
            // Gửi tin nhắn qua network nếu có callback
            if (onSendMessage != null) {
                onSendMessage.accept(message);
            }
        }
    }

    /**
     * Xử lý gửi file từ người chơi.
     * Gọi callback để mở FileChooser và gửi file qua network.
     */
    public void sendFile() {
        if (onSendFile != null) {
            onSendFile.run();
        }
    }

    // ===================== MESSAGE DISPLAY =====================
    
    /**
     * Thêm tin nhắn vào chat box.
     * 
     * @param sender   Tên người gửi
     * @param message  Nội dung tin nhắn
     * @param isPlayer true nếu là tin nhắn của người chơi này, false nếu của đối thủ
     */
    public void addChatMessage(String sender, String message, boolean isPlayer) {
        // Tạo HBox chứa tin nhắn (căn trái hoặc phải tùy người gửi)
        HBox messageBox = new HBox(5);
        messageBox.setAlignment(isPlayer ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        // Tạo bubble chat với màu khác nhau cho người chơi và đối thủ
        VBox bubble = new VBox(3);
        String bgColor = isPlayer ? "#4a9eff" : "#4a4541"; // Xanh cho người chơi, nâu cho đối thủ
        bubble.setStyle("-fx-background-color: " + bgColor + ";" +
                "-fx-background-radius: 8; -fx-padding: 8 12 8 12;");

        // Label hiển thị tên người gửi
        Label senderLabel = new Label(sender);
        senderLabel.setStyle("-fx-text-fill: #f0d9b5; -fx-font-size: 11px; -fx-font-weight: bold;");

        // Label hiển thị nội dung tin nhắn
        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(220);
        messageLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");

        // Thêm vào bubble và messageBox
        bubble.getChildren().addAll(senderLabel, messageLabel);
        messageBox.getChildren().add(bubble);
        chatMessagesBox.getChildren().add(messageBox);

        // Tự động scroll xuống tin nhắn mới nhất
        Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
    }

    /**
     * Thêm tin nhắn hệ thống vào chat box (ví dụ: "Bạn đã đề nghị hòa").
     * Tin nhắn hệ thống được hiển thị ở giữa với style khác.
     * 
     * @param message Nội dung tin nhắn hệ thống
     */
    public void addSystemMessage(String message) {
        Label systemMsg = new Label(message);
        systemMsg.setStyle("-fx-text-fill: #999; -fx-font-size: 11px; " +
                "-fx-font-style: italic; -fx-padding: 5 0 5 0;");
        systemMsg.setAlignment(Pos.CENTER);
        systemMsg.setMaxWidth(Double.MAX_VALUE);
        chatMessagesBox.getChildren().add(systemMsg);
        
        // Tự động scroll xuống tin nhắn mới nhất
        Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
    }

    /**
     * Thêm thông báo file vào chat box.
     * Hiển thị tên file và kích thước, với nút tải xuống nếu là file nhận được.
     * Nếu là file ảnh, hiển thị preview ảnh.
     * 
     * @param sender     Tên người gửi
     * @param filename   Tên file
     * @param fileSize   Kích thước file (bytes)
     * @param isPlayer   true nếu là file của người chơi này gửi
     * @param onDownload Callback khi bấm nút tải xuống (null nếu là file đã gửi)
     * @param fileData   Dữ liệu file (để hiển thị ảnh preview)
     */
    public void addFileMessage(String sender, String filename, long fileSize, boolean isPlayer, Runnable onDownload, byte[] fileData) {
        // Tạo HBox chứa tin nhắn file
        HBox messageBox = new HBox(5);
        messageBox.setAlignment(isPlayer ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        // Tạo bubble chat với màu khác nhau
        VBox bubble = new VBox(5);
        String bgColor = isPlayer ? "#4a9eff" : "#4a4541";
        bubble.setStyle("-fx-background-color: " + bgColor + ";" +
                "-fx-background-radius: 8; -fx-padding: 8 12 8 12;");

        // Label hiển thị tên người gửi
        Label senderLabel = new Label(sender);
        senderLabel.setStyle("-fx-text-fill: #f0d9b5; -fx-font-size: 11px; -fx-font-weight: bold;");

        bubble.getChildren().add(senderLabel);

        // Kiểm tra xem có phải file ảnh không
        if (isImageFile(filename) && fileData != null) {
            try {
                // Tạo ImageView để hiển thị ảnh
                javafx.scene.image.Image image = new javafx.scene.image.Image(
                    new java.io.ByteArrayInputStream(fileData)
                );
                javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(image);
                
                // Giới hạn kích thước ảnh
                imageView.setFitWidth(200);
                imageView.setPreserveRatio(true);
                imageView.setSmooth(true);
                
                // Thêm border cho ảnh
                imageView.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 2);");
                
                bubble.getChildren().add(imageView);
                
                // Label tên file (nhỏ hơn)
                Label fileNameLabel = new Label("📷 " + filename);
                fileNameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 10px;");
                bubble.getChildren().add(fileNameLabel);
                
            } catch (Exception e) {
                // Nếu không load được ảnh, hiển thị như file thông thường
                addFileIconAndName(bubble, filename);
            }
        } else {
            // File không phải ảnh, hiển thị icon file
            addFileIconAndName(bubble, filename);
        }

        // Kích thước file
        String sizeStr = formatFileSize(fileSize);
        Label sizeLabel = new Label(sizeStr);
        sizeLabel.setStyle("-fx-text-fill: #ddd; -fx-font-size: 10px;");
        bubble.getChildren().add(sizeLabel);

        // Nếu là file nhận được, thêm nút tải xuống
        if (!isPlayer && onDownload != null) {
            Button downloadBtn = new Button("Tải xuống");
            downloadBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; " +
                    "-fx-font-size: 10px; -fx-padding: 4 8 4 8; -fx-background-radius: 3; -fx-cursor: hand;");
            downloadBtn.setOnAction(e -> onDownload.run());
            bubble.getChildren().add(downloadBtn);
        }

        messageBox.getChildren().add(bubble);
        chatMessagesBox.getChildren().add(messageBox);

        // Tự động scroll xuống
        Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
    }

    /**
     * Thêm icon và tên file vào bubble (cho file không phải ảnh).
     */
    private void addFileIconAndName(VBox bubble, String filename) {
        Label fileIcon = new Label("📄 " + filename);
        fileIcon.setStyle("-fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;");
        bubble.getChildren().add(fileIcon);
    }

    /**
     * Kiểm tra xem file có phải là ảnh không dựa trên extension.
     */
    private boolean isImageFile(String filename) {
        String lowerName = filename.toLowerCase();
        return lowerName.endsWith(".jpg") || 
               lowerName.endsWith(".jpeg") || 
               lowerName.endsWith(".png") || 
               lowerName.endsWith(".gif") || 
               lowerName.endsWith(".bmp") ||
               lowerName.endsWith(".webp");
    }

    /**
     * Format kích thước file thành chuỗi dễ đọc (KB, MB, GB).
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
