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
    
    private java.util.function.Consumer<String> onSendMessage; // Callback để gửi tin nhắn qua network

    // ===================== CONSTRUCTOR =====================
    
    /**
     * Tạo ChatManager với các UI components từ FXML.
     * 
     * @param chatScrollPane   ScrollPane chứa tin nhắn
     * @param chatMessagesBox  VBox chứa các tin nhắn
     * @param chatInput        TextField để nhập tin nhắn
     * @param sendMessageButton Button để gửi tin nhắn
     */
    public ChatManager(ScrollPane chatScrollPane,
            VBox chatMessagesBox,
            TextField chatInput,
            Button sendMessageButton) {
        this.chatScrollPane = chatScrollPane;
        this.chatMessagesBox = chatMessagesBox;
        this.chatInput = chatInput;
        this.sendMessageButton = sendMessageButton;
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
     * Khởi tạo ChatManager: thiết lập event handlers.
     */
    public void initialize() {
        setupEventHandlers();
        setupChatEnterKey();
    }

    /**
     * Thiết lập event handler cho nút gửi tin nhắn.
     */
    private void setupEventHandlers() {
        sendMessageButton.setOnAction(e -> sendMessage());
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
}
