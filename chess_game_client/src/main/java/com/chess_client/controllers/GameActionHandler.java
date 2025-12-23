package com.chess_client.controllers;

import com.chess_client.models.Piece;
import com.chess_client.network.PeerNetworkHandler;
import com.chess_client.ui.ChatManager;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;

import java.util.function.Consumer;


 
public class GameActionHandler {

    
    
    private final Label statusLabel;              // Label hiển thị trạng thái game
    private final ChatManager chatManager;        // Quản lý chat để hiển thị thông báo
    private final PeerNetworkHandler peerNetworkHandler; // Gửi actions qua P2P
    private final Piece.Color playerColor;        // Màu quân của người chơi này
    private final Consumer<Piece.Color> onGameEnd; // Callback khi game kết thúc
    private final Runnable disableButtons;        // Callback để vô hiệu hóa các nút

   
    /**
     * Tạo GameActionHandler với các dependencies.
     * 
     * @param statusLabel         Label để hiển thị trạng thái
     * @param chatManager         ChatManager để hiển thị thông báo
     * @param peerNetworkHandler  Network handler để gửi actions
     * @param playerColor         Màu quân của người chơi
     * @param onGameEnd           Callback khi game kết thúc (nhận winner)
     * @param disableButtons      Callback để vô hiệu hóa các nút điều khiển
     */
    public GameActionHandler(Label statusLabel, ChatManager chatManager,
            PeerNetworkHandler peerNetworkHandler, Piece.Color playerColor,
            Consumer<Piece.Color> onGameEnd, Runnable disableButtons) {
        this.statusLabel = statusLabel;
        this.chatManager = chatManager;
        this.peerNetworkHandler = peerNetworkHandler;
        this.playerColor = playerColor;
        this.onGameEnd = onGameEnd;
        this.disableButtons = disableButtons;
    }

    // ===================== PLAYER ACTIONS =====================
    
    /**
     * Xử lý đề nghị hòa từ người chơi.
     * Hiển thị dialog xác nhận, gửi action qua network, và thông báo trong chat.
     */
    public void offerDraw() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Cầu hòa");
        alert.setHeaderText("Bạn muốn đề nghị hòa?");
        alert.setContentText("Đối thủ sẽ được hỏi có chấp nhận không.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Hiển thị thông báo trong chat
                if (chatManager != null) {
                    chatManager.addSystemMessage("Bạn đã đề nghị hòa");
                }
                
                // Gửi action đến đối thủ qua P2P
                if (peerNetworkHandler != null) {
                    peerNetworkHandler.sendGameAction("offer_draw");
                }
            }
        });
    }

    /**
     * Xử lý đầu hàng từ người chơi.
     * Hiển thị dialog xác nhận, kết thúc game, và gửi action qua network.
     */
    public void resign() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Nhận thua");
        alert.setHeaderText("Bạn muốn đầu hàng?");
        alert.setContentText("Bạn sẽ thua ván này.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Xác định người thắng (đối thủ)
                Piece.Color winner = playerColor == Piece.Color.WHITE 
                    ? Piece.Color.BLACK 
                    : Piece.Color.WHITE;
                String winnerText = winner == Piece.Color.WHITE ? "TRẮNG" : "ĐEN";
                
                // Cập nhật UI
                statusLabel.setText("BẠN ĐÃ ĐẦU HÀNG! " + winnerText + " THẮNG!");
                disableButtons.run();
                
                // Hiển thị thông báo trong chat
                if (chatManager != null) {
                    chatManager.addSystemMessage("Bạn đã đầu hàng");
                }
                
                // Gửi action đến đối thủ qua P2P
                if (peerNetworkHandler != null) {
                    peerNetworkHandler.sendGameAction("resign");
                }
                
                // Kết thúc game
                onGameEnd.accept(winner);
            }
        });
    }

    // ===================== NETWORK ACTIONS HANDLING =====================
    
    /**
     * Xử lý game action nhận được từ network (từ đối thủ).
     * 
     * @param action Tên action: "resign", "offer_draw", "accept_draw", "reject_draw"
     */
    public void handleGameAction(String action) {
        switch (action) {
            case "resign" -> handleOpponentResign();
            case "offer_draw" -> handleOpponentOfferDraw();
            case "accept_draw" -> handleOpponentAcceptDraw();
            case "reject_draw" -> handleOpponentRejectDraw();
        }
    }

    /**
     * Xử lý khi đối thủ đầu hàng.
     * Người chơi này thắng.
     */
    private void handleOpponentResign() {
        Piece.Color winner = playerColor; // Bạn thắng vì đối thủ đầu hàng
        String winnerText = winner == Piece.Color.WHITE ? "TRẮNG" : "ĐEN";
        
        // Cập nhật UI
        statusLabel.setText("ĐỐI THỦ ĐÃ ĐẦU HÀNG! " + winnerText + " THẮNG!");
        disableButtons.run();
        
        // Hiển thị thông báo trong chat
        if (chatManager != null) {
            chatManager.addSystemMessage("Đối thủ đã đầu hàng");
        }
        
        // Kết thúc game
        onGameEnd.accept(winner);
    }

    /**
     * Xử lý khi đối thủ đề nghị hòa.
     * Hiển thị dialog để người chơi chấp nhận hoặc từ chối.
     */
    private void handleOpponentOfferDraw() {
        // Hiển thị thông báo trong chat
        if (chatManager != null) {
            chatManager.addSystemMessage("Đối thủ đề nghị hòa");
        }

        // Hiển thị dialog xác nhận
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Đề nghị hòa");
        alert.setHeaderText("Đối thủ đề nghị hòa");
        alert.setContentText("Bạn có chấp nhận hòa không?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Chấp nhận hòa
                if (chatManager != null) {
                    chatManager.addSystemMessage("Bạn đã chấp nhận hòa");
                }
                statusLabel.setText("TRẬN ĐẤU HÒA!");
                disableButtons.run();
                
                // Gửi action chấp nhận đến đối thủ
                if (peerNetworkHandler != null) {
                    peerNetworkHandler.sendGameAction("accept_draw");
                }
                
                // Kết thúc game với kết quả hòa (winner = null)
                onGameEnd.accept(null);
            } else {
                // Từ chối hòa
                if (chatManager != null) {
                    chatManager.addSystemMessage("Bạn đã từ chối hòa");
                }
                
                // Gửi action từ chối đến đối thủ
                if (peerNetworkHandler != null) {
                    peerNetworkHandler.sendGameAction("reject_draw");
                }
            }
        });
    }

    /**
     * Xử lý khi đối thủ chấp nhận hòa.
     * Kết thúc game với kết quả hòa.
     */
    private void handleOpponentAcceptDraw() {
        statusLabel.setText("TRẬN ĐẤU HÒA!");
        disableButtons.run();
        
        if (chatManager != null) {
            chatManager.addSystemMessage("Đối thủ đã chấp nhận hòa");
        }
        
        // Kết thúc game với kết quả hòa (winner = null)
        onGameEnd.accept(null);
    }

    /**
     * Xử lý khi đối thủ từ chối hòa.
     * Chỉ hiển thị thông báo, game tiếp tục.
     */
    private void handleOpponentRejectDraw() {
        if (chatManager != null) {
            chatManager.addSystemMessage("Đối thủ đã từ chối hòa");
        }
    }
}
