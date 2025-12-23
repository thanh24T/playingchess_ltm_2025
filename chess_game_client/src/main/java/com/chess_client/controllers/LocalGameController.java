package com.chess_client.controllers;

import com.chess_client.models.Board;
import com.chess_client.models.Move;
import com.chess_client.models.Piece;
import com.chess_client.services.GameLogic;
import com.chess_client.services.GameStateChecker;
import com.chess_client.ui.BoardView;
import com.chess_client.ui.NavigationHelper;
import javafx.animation.RotateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller cho chế độ chơi 2 người trên 1 máy (Local Multiplayer).
 * Bàn cờ sẽ tự động xoay 180 độ sau mỗi lượt để người chơi tiếp theo
 * có góc nhìn thuận lợi.
 */
public class LocalGameController {

    @FXML private Pane boardPane;
    @FXML private Label turnLabel;
    @FXML private Label statusLabel;
    @FXML private Label whitePlayerLabel;
    @FXML private Label blackPlayerLabel;
    @FXML private Label lastMoveLabel;
    @FXML private VBox moveHistoryBox;
    @FXML private Button undoButton;
    @FXML private Button newGameButton;
    @FXML private Button exitButton;

    private Board board;
    private GameLogic gameLogic;
    private GameStateChecker gameStateChecker;
    private BoardView boardView;
    private Piece.Color currentPlayer;
    private List<Move> moveHistory;
    private boolean gameOver;
    private Stage stage;
    
    // Rotation state
    private boolean isBoardRotated = false;
    private static final double ROTATION_DURATION = 0.8; // seconds

    @FXML
    public void initialize() {
        moveHistory = new ArrayList<>();
        currentPlayer = Piece.Color.WHITE;
        gameOver = false;
        
        initializeGame();
        setupBoardView();
        setupEventHandlers();
        updateUI();
    }

    private void initializeGame() {
        board = new Board();
        gameLogic = new GameLogic(board);
        gameStateChecker = new GameStateChecker(board, gameLogic);
    }

    private void setupBoardView() {
        boardView = new BoardView(board, boardPane);
        boardView.setOnSquareClicked((coords) -> handleSquareClick(coords[0], coords[1]));
        
        // Căn giữa bàn cờ trong pane (8x8 ô, mỗi ô 70px = 560px)
        double boardSize = 560;
        double centerX = (boardPane.getPrefWidth() - boardSize) / 2;
        double centerY = (boardPane.getPrefHeight() - boardSize) / 2;
        
        boardView.getChessBoard().setLayoutX(centerX);
        boardView.getChessBoard().setLayoutY(centerY);
        
        boardView.render();
    }

    private void setupEventHandlers() {
        undoButton.setOnAction(e -> undoLastMove());
        newGameButton.setOnAction(e -> startNewGame());
        exitButton.setOnAction(e -> exitToHome());
    }

    private void handleSquareClick(int row, int col) {
        if (gameOver) {
            return;
        }

        Piece clickedPiece = board.getPiece(row, col);

        // Nếu chưa chọn quân nào
        if (boardView.getSelectedRow() == -1) {
            // Chỉ cho phép chọn quân của người chơi hiện tại
            if (clickedPiece != null && clickedPiece.getColor() == currentPlayer) {
                boardView.setSelectedSquare(row, col);
                boardView.render();
            }
        } else {
            // Đã chọn quân, thử di chuyển
            int fromRow = boardView.getSelectedRow();
            int fromCol = boardView.getSelectedCol();
            Piece selectedPiece = board.getPiece(fromRow, fromCol);

            // Nếu click vào quân cùng màu khác -> đổi selection
            if (clickedPiece != null && clickedPiece.getColor() == currentPlayer) {
                boardView.setSelectedSquare(row, col);
                boardView.render();
                return;
            }

            // Thử thực hiện nước đi
            Move move = new Move(fromRow, fromCol, row, col, selectedPiece);
            
            if (gameLogic.isValidMove(move, currentPlayer)) {
                executeMove(move);
            } else {
                showAlert("Nước đi không hợp lệ", "Vui lòng chọn nước đi khác.");
            }

            // Clear selection
            boardView.clearSelection();
            boardView.render();
        }
    }

    private void executeMove(Move move) {
        // Lưu move vào history
        moveHistory.add(move);
        
        // Thực hiện move trên board
        board.movePiece(move);
        
        // Cập nhật UI
        updateLastMove(move);
        updateMoveHistory(move);
        
        // Kiểm tra trạng thái game
        checkGameState();
        
        if (!gameOver) {
            // Đổi lượt
            switchPlayer();
            
            // Xoay bàn cờ
            rotateBoardWithAnimation();
        }
        
        // Render lại board
        boardView.render();
    }

    private void switchPlayer() {
        currentPlayer = (currentPlayer == Piece.Color.WHITE) 
            ? Piece.Color.BLACK 
            : Piece.Color.WHITE;
        updateUI();
    }

    private void rotateBoardWithAnimation() {
        RotateTransition rotate = new RotateTransition(
            Duration.seconds(ROTATION_DURATION), 
            boardPane
        );
        
        // Xoay 180 độ
        double targetAngle = isBoardRotated ? 0 : 180;
        rotate.setToAngle(targetAngle);
        
        rotate.setOnFinished(e -> {
            isBoardRotated = !isBoardRotated;
            // Cập nhật rotation state của BoardView để render đúng
            boardView.setRotated(isBoardRotated);
            boardView.render();
        });
        
        rotate.play();
    }

    private void checkGameState() {
        Piece.Color opponent = (currentPlayer == Piece.Color.WHITE) 
            ? Piece.Color.BLACK 
            : Piece.Color.WHITE;

        GameStateChecker.GameStateResult result = gameStateChecker.checkGameState(opponent);
        
        switch (result.getState()) {
            case CHECKMATE:
            case KING_CAPTURED:
                gameOver = true;
                String winner = currentPlayer == Piece.Color.WHITE ? "Trắng" : "Đen";
                statusLabel.setText("Chiếu hết! " + winner + " thắng!");
                showGameOverDialog(winner + " thắng bằng chiếu hết!");
                break;
            case STALEMATE:
                gameOver = true;
                statusLabel.setText("Hòa cờ!");
                showGameOverDialog("Trận đấu hòa!");
                break;
            case CHECK:
                statusLabel.setText("Chiếu tướng!");
                break;
            default:
                statusLabel.setText("");
                break;
        }
    }

    private void updateUI() {
        String currentPlayerName = currentPlayer == Piece.Color.WHITE ? "Trắng" : "Đen";
        turnLabel.setText("Lượt: " + currentPlayerName);
        
        // Highlight người chơi hiện tại
        if (currentPlayer == Piece.Color.WHITE) {
            whitePlayerLabel.setStyle("-fx-background-color: #4a9eff; -fx-text-fill: white;");
            blackPlayerLabel.setStyle("-fx-background-color: transparent; -fx-text-fill: white;");
        } else {
            blackPlayerLabel.setStyle("-fx-background-color: #4a9eff; -fx-text-fill: white;");
            whitePlayerLabel.setStyle("-fx-background-color: transparent; -fx-text-fill: white;");
        }
    }

    private void updateLastMove(Move move) {
        String from = positionToChessNotation(move.getFromRow(), move.getFromCol());
        String to = positionToChessNotation(move.getToRow(), move.getToCol());
        String pieceType = move.getPieceMoved().getType().toString();
        
        lastMoveLabel.setText(String.format("Nước đi: %s %s → %s", 
            pieceType, from, to));
    }

    private void updateMoveHistory(Move move) {
        String from = positionToChessNotation(move.getFromRow(), move.getFromCol());
        String to = positionToChessNotation(move.getToRow(), move.getToCol());
        String color = move.getPieceMoved().getColor() == Piece.Color.WHITE ? "Trắng" : "Đen";
        
        Label moveLabel = new Label(String.format("%d. %s: %s → %s", 
            moveHistory.size(), color, from, to));
        moveLabel.setStyle("-fx-text-fill: white; -fx-padding: 5;");
        
        moveHistoryBox.getChildren().add(moveLabel);
        
        // Auto scroll to bottom
        Platform.runLater(() -> {
            if (moveHistoryBox.getParent() instanceof javafx.scene.control.ScrollPane) {
                javafx.scene.control.ScrollPane scrollPane = 
                    (javafx.scene.control.ScrollPane) moveHistoryBox.getParent();
                scrollPane.setVvalue(1.0);
            }
        });
    }

    private void undoLastMove() {
        if (moveHistory.isEmpty() || gameOver) {
            return;
        }

        // Reset game và replay tất cả moves trừ move cuối
        initializeGame();
        List<Move> tempHistory = new ArrayList<>(moveHistory);
        moveHistory.clear();
        moveHistoryBox.getChildren().clear();
        
        currentPlayer = Piece.Color.WHITE;
        
        // Replay moves
        for (int i = 0; i < tempHistory.size() - 1; i++) {
            Move move = tempHistory.get(i);
            board.movePiece(move);
            moveHistory.add(move);
            updateMoveHistory(move);
            currentPlayer = (currentPlayer == Piece.Color.WHITE) 
                ? Piece.Color.BLACK 
                : Piece.Color.WHITE;
        }
        
        // Cập nhật UI
        if (!moveHistory.isEmpty()) {
            updateLastMove(moveHistory.get(moveHistory.size() - 1));
        } else {
            lastMoveLabel.setText("Nước đi: ---");
        }
        
        // Reset rotation nếu cần
        if (isBoardRotated != (currentPlayer == Piece.Color.BLACK)) {
            rotateBoardWithAnimation();
        }
        
        updateUI();
        boardView.render();
    }

    private void startNewGame() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Ván mới");
        confirm.setHeaderText("Bắt đầu ván mới?");
        confirm.setContentText("Ván đấu hiện tại sẽ bị hủy.");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                // Reset everything
                initializeGame();
                moveHistory.clear();
                moveHistoryBox.getChildren().clear();
                currentPlayer = Piece.Color.WHITE;
                gameOver = false;
                
                // Reset rotation
                if (isBoardRotated) {
                    boardPane.setRotate(0);
                    isBoardRotated = false;
                    boardView.setRotated(false);
                }
                
                lastMoveLabel.setText("Nước đi: ---");
                updateUI();
                boardView.render();
            }
        });
    }

    private void exitToHome() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Thoát");
        confirm.setHeaderText("Quay về trang chủ?");
        confirm.setContentText("Ván đấu hiện tại sẽ bị hủy.");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                try {
                    NavigationHelper.navigateTo(stage, "/com/chess_client/fxml/home.fxml", 930, 740);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void showGameOverDialog(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Kết thúc");
            alert.setHeaderText("Trận đấu đã kết thúc!");
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String positionToChessNotation(int row, int col) {
        char file = (char) ('a' + col);
        int rank = 8 - row;
        return "" + file + rank;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }
}
