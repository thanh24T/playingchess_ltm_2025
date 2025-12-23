package com.chess_client.network;

import com.chess_client.models.Move;
import javafx.application.Platform;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Xử lý giao tiếp mạng P2P (Peer-to-Peer) giữa hai client trong trận đấu.
 * Chịu trách nhiệm gửi/nhận nước đi, tin nhắn chat và các game actions
 * qua socket TCP trực tiếp giữa hai client (không qua server).
 */
public class PeerNetworkHandler {

    // ===================== FIELDS =====================
    
    private Socket peerSocket;        // Socket kết nối P2P với đối thủ
    private BufferedReader peerIn;    // Stream đọc dữ liệu từ đối thủ
    private PrintWriter peerOut;      // Stream ghi dữ liệu đến đối thủ

    // ===================== CALLBACKS =====================
    
    private OnMoveReceived onMoveReceived;           // Callback khi nhận nước đi
    private OnChatReceived onChatReceived;           // Callback khi nhận tin nhắn chat
    private OnGameActionReceived onGameActionReceived; // Callback khi nhận game action
    private OnFileReceived onFileReceived;           // Callback khi nhận file

    /**
     * Callback interface khi nhận được nước đi từ đối thủ.
     * GameController sẽ lấy Piece từ board và tạo Move.
     */
    public interface OnMoveReceived {
        void onMove(int fromRow, int fromCol, int toRow, int toCol);
    }

    /**
     * Callback interface khi nhận được tin nhắn chat từ đối thủ.
     */
    public interface OnChatReceived {
        void onChat(String message);
    }

    /**
     * Callback interface khi nhận được game action từ đối thủ.
     * Các action: "resign", "offer_draw", "accept_draw", "reject_draw"
     */
    public interface OnGameActionReceived {
        void onGameAction(String action);
    }

    /**
     * Callback interface khi nhận được file từ đối thủ.
     */
    public interface OnFileReceived {
        void onFile(String filename, long fileSize, byte[] fileData);
    }

    // ===================== CONSTRUCTOR =====================
    
    public PeerNetworkHandler() {
        // Constructor rỗng, socket sẽ được thiết lập sau
    }

    // ===================== SOCKET SETUP =====================
    
    /**
     * Thiết lập socket P2P và bắt đầu lắng nghe tin nhắn từ đối thủ.
     * Tạo một thread riêng để lắng nghe không block UI thread.
     * 
     * @param socket Socket P2P đã được kết nối
     */
    public void setPeerSocket(Socket socket) {
        this.peerSocket = socket;
        try {
            // Khởi tạo input/output streams
            this.peerIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.peerOut = new PrintWriter(socket.getOutputStream(), true);

            // Bắt đầu thread lắng nghe tin nhắn (daemon thread để tự động dừng khi app đóng)
            Thread listener = new Thread(this::listenForPeerMessages);
            listener.setDaemon(true);
            listener.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ===================== CALLBACK SETTERS =====================
    
    /**
     * Thiết lập callback khi nhận nước đi từ đối thủ.
     */
    public void setOnMoveReceived(OnMoveReceived callback) {
        this.onMoveReceived = callback;
    }

    /**
     * Thiết lập callback khi nhận tin nhắn chat từ đối thủ.
     */
    public void setOnChatReceived(OnChatReceived callback) {
        this.onChatReceived = callback;
    }

    /**
     * Thiết lập callback khi nhận game action từ đối thủ.
     */
    public void setOnGameActionReceived(OnGameActionReceived callback) {
        this.onGameActionReceived = callback;
    }

    /**
     * Thiết lập callback khi nhận file từ đối thủ.
     */
    public void setOnFileReceived(OnFileReceived callback) {
        this.onFileReceived = callback;
    }

    // ===================== SEND METHODS =====================
    
    /**
     * Gửi nước đi đến đối thủ qua P2P socket.
     * Format JSON: {"type": "move", "fromRow": x, "fromCol": y, "toRow": a, "toCol": b}
     * 
     * @param move Nước đi cần gửi
     */
    public void sendMove(Move move) {
        if (peerOut == null) {
            return;
        }

        try {
            JSONObject json = new JSONObject();
            json.put("type", "move");
            json.put("fromRow", move.getFromRow());
            json.put("fromCol", move.getFromCol());
            json.put("toRow", move.getToRow());
            json.put("toCol", move.getToCol());
            peerOut.println(json.toString());
            peerOut.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Gửi tin nhắn chat đến đối thủ qua P2P socket.
     * Format JSON: {"type": "chat", "message": "..."}
     * 
     * @param message Nội dung tin nhắn
     */
    public void sendChatMessage(String message) {
        if (peerOut == null) {
            return;
        }

        try {
            JSONObject json = new JSONObject();
            json.put("type", "chat");
            json.put("message", message);
            peerOut.println(json.toString());
            peerOut.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Gửi game action đến đối thủ qua P2P socket.
     * Format JSON: {"type": "game_action", "action": "..."}
     * 
     * Các action có thể:
     * - "resign": Đầu hàng
     * - "offer_draw": Đề nghị hòa
     * - "accept_draw": Chấp nhận hòa
     * - "reject_draw": Từ chối hòa
     * 
     * @param action Tên action cần gửi
     */
    public void sendGameAction(String action) {
        if (peerOut == null) {
            return;
        }

        try {
            JSONObject json = new JSONObject();
            json.put("type", "game_action");
            json.put("action", action);
            peerOut.println(json.toString());
            peerOut.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Gửi file đến đối thủ qua P2P socket.
     * Format JSON: {"type": "file", "filename": "...", "fileSize": 123, "fileData": "base64..."}
     * 
     * @param filename Tên file
     * @param fileData Dữ liệu file dạng byte array
     */
    public void sendFile(String filename, byte[] fileData) {
        if (peerOut == null) {
            return;
        }

        try {
            JSONObject json = new JSONObject();
            json.put("type", "file");
            json.put("filename", filename);
            json.put("fileSize", fileData.length);
            // Encode file data thành Base64 để gửi qua JSON
            json.put("fileData", java.util.Base64.getEncoder().encodeToString(fileData));
            peerOut.println(json.toString());
            peerOut.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===================== RECEIVE METHODS =====================
    
    /**
     * Lắng nghe tin nhắn từ đối thủ ở thread riêng.
     * Phân tích JSON và gọi callback tương ứng.
     * Chạy trong daemon thread để không block UI.
     */
    private void listenForPeerMessages() {
        if (peerIn == null) {
            return;
        }

        try {
            String line;
            // Đọc từng dòng JSON từ socket
            while ((line = peerIn.readLine()) != null) {
                try {
                    JSONObject json = new JSONObject(line);
                    String type = json.optString("type");

                    // Phân loại và xử lý tin nhắn theo type
                    switch (type) {
                        case "move" -> handleReceivedMove(json);
                        case "chat" -> handleReceivedChat(json);
                        case "game_action" -> handleReceivedGameAction(json);
                        case "file" -> handleReceivedFile(json);
                    }
                } catch (Exception parseEx) {
                    // Bỏ qua tin nhắn không hợp lệ
                    parseEx.printStackTrace();
                }
            }
        } catch (IOException e) {
            // Socket đã đóng hoặc có lỗi kết nối
            e.printStackTrace();
        }
    }

    /**
     * Xử lý nước đi nhận được từ đối thủ.
     * Gọi callback trên JavaFX Application Thread để cập nhật UI an toàn.
     */
    private void handleReceivedMove(JSONObject json) {
        if (onMoveReceived == null) {
            return;
        }

        int fromRow = json.getInt("fromRow");
        int fromCol = json.getInt("fromCol");
        int toRow = json.getInt("toRow");
        int toCol = json.getInt("toCol");

        // Chuyển về JavaFX Application Thread để cập nhật UI
        Platform.runLater(() -> {
            if (onMoveReceived != null) {
                onMoveReceived.onMove(fromRow, fromCol, toRow, toCol);
            }
        });
    }

    /**
     * Xử lý tin nhắn chat nhận được từ đối thủ.
     * Gọi callback trên JavaFX Application Thread để cập nhật UI an toàn.
     */
    private void handleReceivedChat(JSONObject json) {
        if (onChatReceived == null) {
            return;
        }

        String message = json.getString("message");
        Platform.runLater(() -> onChatReceived.onChat(message));
    }

    /**
     * Xử lý game action nhận được từ đối thủ.
     * Gọi callback trên JavaFX Application Thread để cập nhật UI an toàn.
     */
    private void handleReceivedGameAction(JSONObject json) {
        if (onGameActionReceived == null) {
            return;
        }

        String action = json.getString("action");
        Platform.runLater(() -> onGameActionReceived.onGameAction(action));
    }

    /**
     * Xử lý file nhận được từ đối thủ.
     * Decode Base64 và gọi callback trên JavaFX Application Thread.
     */
    private void handleReceivedFile(JSONObject json) {
        if (onFileReceived == null) {
            return;
        }

        String filename = json.getString("filename");
        long fileSize = json.getLong("fileSize");
        String fileDataBase64 = json.getString("fileData");
        
        // Decode Base64 thành byte array
        byte[] fileData = java.util.Base64.getDecoder().decode(fileDataBase64);
        
        Platform.runLater(() -> onFileReceived.onFile(filename, fileSize, fileData));
    }

    // ===================== CLEANUP =====================
    
    /**
     * Đóng kết nối network và giải phóng tài nguyên.
     * Nên gọi khi game kết thúc hoặc đóng ứng dụng.
     */
    public void close() {
        try {
            if (peerIn != null) {
                peerIn.close();
            }
            if (peerOut != null) {
                peerOut.close();
            }
            if (peerSocket != null && !peerSocket.isClosed()) {
                peerSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
