package com.chess_client.controllers;

import com.chess_client.models.Board;
import com.chess_client.models.Move;
import com.chess_client.models.Piece;
import com.chess_client.network.PeerNetworkHandler;
import com.chess_client.services.AIPlayer;
import com.chess_client.services.GameLogic;
import com.chess_client.services.GameService;
import com.chess_client.services.GameStateChecker;
import com.chess_client.ui.BoardView;
import com.chess_client.ui.ChatManager;
import com.chess_client.ui.UIGameInfoUpdater;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;


public class GameController {

    // ===================== UI COMPONENTS - BOARD =====================
    
    @FXML
    private GridPane chessBoard;

    // ===================== UI COMPONENTS - PLAYER INFO =====================
    
    @FXML
    private Label opponentPlayerLabel;  // Hiển thị màu quân đối thủ
    @FXML
    private Label opponentNameLabel;   // Hiển thị tên đối thủ
    @FXML
    private Label playerLabel;         // Hiển thị màu quân người chơi
    @FXML
    private Label playerNameLabel;     // Hiển thị tên người chơi

    // ===================== UI COMPONENTS - GAME INFO =====================
    
    @FXML
    private Label turnLabel;      // Hiển thị lượt đi hiện tại
    @FXML
    private Label statusLabel;    // Hiển thị trạng thái game (chiếu, chiếu hết, hòa...)
    @FXML
    private Label lastMoveLabel;  // Hiển thị nước đi cuối cùng

    // ===================== UI COMPONENTS - BUTTONS =====================
    
    @FXML
    private Button drawButton;    // Nút đề nghị hòa
    @FXML
    private Button resignButton;  // Nút đầu hàng

    // ===================== UI COMPONENTS - CHAT =====================
    
    @FXML
    private ScrollPane chatScrollPane;  // ScrollPane chứa danh sách tin nhắn
    @FXML
    private VBox chatMessagesBox;       // VBox chứa các tin nhắn
    @FXML
    private TextField chatInput;        // Ô nhập tin nhắn
    @FXML
    private Button sendMessageButton;   // Nút gửi tin nhắn
    @FXML
    private Button sendFileButton;      // Nút gửi file

    // ===================== UI MANAGERS =====================
    
    private ChatManager chatManager;    // Quản lý chat UI và logic
    private BoardView boardView;        // Quản lý hiển thị và tương tác bàn cờ

    // ===================== GAME STATE =====================
    
    private Board board;                    // Bàn cờ hiện tại
    private GameLogic gameLogic;           // Logic nghiệp vụ cờ vua
    private Piece.Color currentPlayer;     // Người chơi hiện tại (WHITE/BLACK)
    private Piece.Color playerColor;       // Màu quân của người chơi này
    private List<Move> moveHistory;        // Lịch sử các nước đi
    
    // Thông tin trận đấu
    private String gameId;          // ID trận đấu từ server
    private String opponentName;    // Tên đối thủ
    private String playerName;      // Tên người chơi

    // ===================== HANDLERS & SERVICES =====================
    
    private PeerNetworkHandler peerNetworkHandler;  // Xử lý giao tiếp P2P với đối thủ
    private AIPlayer aiPlayer;                      // AI player (nếu chơi với máy)
    private GameStateChecker gameStateChecker;      // Kiểm tra trạng thái game
    private GameActionHandler gameActionHandler;    // Xử lý các hành động game (hòa, đầu hàng)
    private UIGameInfoUpdater uiUpdater;            // Cập nhật thông tin UI
    
    // Service gọi API game server
    private final GameService gameService = new GameService();

    // ===================== INITIALIZATION =====================
    
    /**
     * Khởi tạo controller khi FXML được load.
     * Thiết lập tất cả các component, handlers và callbacks.
     */
    @FXML
    public void initialize() {
        // Khởi tạo game state
        moveHistory = new ArrayList<>();
        playerColor = Piece.Color.WHITE; // Mặc định người chơi là TRẮNG
        initializeGame();

        // Khởi tạo BoardView để vẽ và điều khiển bàn cờ
        boardView = new BoardView(chessBoard, board, gameLogic, playerColor, this::executeMove);
        boardView.setCurrentPlayer(currentPlayer);
        boardView.refreshBoard();

        // Khởi tạo quản lý chat
        chatManager = new ChatManager(chatScrollPane, chatMessagesBox, chatInput, sendMessageButton, sendFileButton);
        chatManager.initialize();

        // Khởi tạo network handler và thiết lập callbacks
        peerNetworkHandler = new PeerNetworkHandler();
        setupNetworkCallbacks();

        // Kết nối callback gửi tin nhắn chat qua network
        chatManager.setOnSendMessage(message -> {
            if (peerNetworkHandler != null) {
                peerNetworkHandler.sendChatMessage(message);
            }
        });

        // Kết nối callback gửi file qua network
        chatManager.setOnSendFile(this::handleSendFile);

        // Khởi tạo game state checker
        gameStateChecker = new GameStateChecker(board, gameLogic);

        // Khởi tạo UI updater (sẽ update aiPlayer sau khi setupVsComputer được gọi)
        uiUpdater = new UIGameInfoUpdater(turnLabel, statusLabel, lastMoveLabel,
                playerLabel, opponentPlayerLabel, playerNameLabel, opponentNameLabel,
                playerColor, aiPlayer);

        // Khởi tạo game action handler
        gameActionHandler = new GameActionHandler(statusLabel, chatManager, peerNetworkHandler,
                playerColor, this::endGame, () -> disableGameButtons(false));

        // Thiết lập event handlers cho các nút
        setupEventHandlers();

        // Reset UI về trạng thái ban đầu
        if (uiUpdater != null) {
            uiUpdater.reset();
            uiUpdater.updateTurnLabel(currentPlayer);
            uiUpdater.updatePlayerLabels();
        }
    }

    // ===================== GAME SETUP =====================
    
    /**
     * Được gọi từ HomeController sau khi ghép trận thành công.
     * Thiết lập thông tin trận đấu và cập nhật UI.
     * 
     * @param gameId       ID trận đấu từ server
     * @param opponentName Tên đối thủ
     * @param playerName   Tên người chơi
     */
    public void setGameInfo(String gameId, String opponentName, String playerName) {
        this.gameId = gameId;
        this.opponentName = opponentName;
        this.playerName = playerName;
        if (uiUpdater != null) {
            uiUpdater.updatePlayerInfo(playerName, opponentName);
        }
    }

    /**
     * Thiết lập chế độ chơi với máy (AI).
     * Vô hiệu hóa các tính năng không cần thiết như cầu hòa và chat.
     *
     * @param difficulty  Độ khó AI: 1 = dễ, 2 = trung bình, 3 = khó
     * @param humanColor  Màu quân của người chơi (thường là WHITE)
     */
    public void setupVsComputer(int difficulty, Piece.Color humanColor) {
        this.playerColor = humanColor;
        Piece.Color computerColor = (humanColor == Piece.Color.WHITE) 
            ? Piece.Color.BLACK 
            : Piece.Color.WHITE;
        this.aiPlayer = new AIPlayer(board, gameLogic, computerColor, difficulty);

        // Chơi với máy thì không cho cầu hòa
        if (drawButton != null) {
            drawButton.setDisable(true);
            drawButton.setVisible(false);
        }

        // Chơi với máy thì vô hiệu hóa chat
        if (chatInput != null) {
            chatInput.setDisable(true);
            chatInput.setPromptText("Không thể chat với máy");
        }
        if (sendMessageButton != null) {
            sendMessageButton.setDisable(true);
        }
        if (sendFileButton != null) {
            sendFileButton.setDisable(true);
        }

        // Cập nhật lại UI với thông tin AI
        if (uiUpdater != null) {
            uiUpdater.setPlayerColor(playerColor);
            uiUpdater.setAiPlayer(aiPlayer);
            uiUpdater.updatePlayerLabels();
            uiUpdater.updatePlayerInfo(playerName, opponentName);
        }
        if (boardView != null) {
            boardView.setPlayerColor(playerColor);
            boardView.setCurrentPlayer(currentPlayer);
            boardView.refreshBoard();
        }
    }

    /**
     * Được gọi từ HomeController sau khi ghép trận để thiết lập màu quân.
     * 
     * @param color Màu quân của người chơi (WHITE/BLACK)
     */
    public void setPlayerColor(Piece.Color color) {
        this.playerColor = color;
        if (uiUpdater != null) {
            uiUpdater.setPlayerColor(color);
            uiUpdater.updatePlayerLabels();
        }
        if (boardView != null) {
            boardView.setPlayerColor(color);
            boardView.refreshBoard();
        }
    }

    /**
     * Thiết lập socket P2P đã được kết nối giữa hai client (LAN).
     * Được gọi sau khi PeerService thiết lập kết nối thành công.
     * 
     * @param socket Socket P2P đã kết nối
     */
    public void setPeerSocket(Socket socket) {
        if (peerNetworkHandler != null) {
            peerNetworkHandler.setPeerSocket(socket);
        }
    }

    // ===================== NETWORK SETUP =====================
    
    /**
     * Thiết lập các callbacks cho network handler.
     * Xử lý các sự kiện: nhận nước đi, nhận tin nhắn chat, nhận game action, nhận file.
     */
    private void setupNetworkCallbacks() {
        // Callback khi nhận nước đi từ đối thủ
        peerNetworkHandler.setOnMoveReceived((fromRow, fromCol, toRow, toCol) -> {
            Piece piece = board.getPiece(fromRow, fromCol);
            if (piece == null) {
                return;
            }

            Move move = new Move(fromRow, fromCol, toRow, toCol, piece);
            executeMove(move, true); // true = từ network (không gửi lại)
        });

        // Callback khi nhận tin nhắn chat từ đối thủ
        peerNetworkHandler.setOnChatReceived(message -> {
            if (chatManager != null) {
                chatManager.addChatMessage(
                        opponentName != null ? opponentName : "Đối thủ",
                        message,
                        false);
            }
        });

        // Callback khi nhận game action từ đối thủ (hòa, đầu hàng...)
        peerNetworkHandler.setOnGameActionReceived(action -> {
            if (gameActionHandler != null) {
                gameActionHandler.handleGameAction(action);
            }
        });

        // Callback khi nhận file từ đối thủ
        peerNetworkHandler.setOnFileReceived((filename, fileSize, fileData) -> {
            if (chatManager != null) {
                // Hiển thị file trong chat với nút tải xuống (truyền fileData để hiển thị ảnh)
                chatManager.addFileMessage(
                        opponentName != null ? opponentName : "Đối thủ",
                        filename,
                        fileSize,
                        false,
                        () -> handleDownloadFile(filename, fileData),
                        fileData);
            }
        });
    }

    // ===================== EVENT HANDLERS =====================
    
    /**
     * Thiết lập event handlers cho các nút điều khiển.
     */
    private void setupEventHandlers() {
        drawButton.setOnAction(e -> offerDraw());
        resignButton.setOnAction(e -> resign());
    }

    // ===================== GAME INITIALIZATION =====================
    
    /**
     * Khởi tạo game mới: tạo bàn cờ, logic, và reset trạng thái.
     */
    private void initializeGame() {
        board = new Board();
        gameLogic = new GameLogic(board);
        currentPlayer = Piece.Color.WHITE; // Trắng đi trước
        moveHistory.clear();
    }

    // ===================== MOVE EXECUTION =====================
    
    /**
     * Thực hiện một nước đi (public method, được gọi từ BoardView).
     * 
     * @param move Nước đi cần thực hiện
     */
    private void executeMove(Move move) {
        executeMove(move, false);
    }

    /**
     * Thực hiện một nước đi và cập nhật game state.
     * 
     * @param move        Nước đi cần thực hiện
     * @param fromNetwork true nếu nước đi đến từ socket P2P (không gửi lại để tránh vòng lặp)
     */
    private void executeMove(Move move, boolean fromNetwork) {
        // Thực hiện nước đi trên bàn cờ
        board.movePiece(move);
        moveHistory.add(move);

        // Gửi nước đi đến đối thủ qua P2P (nếu không phải từ network)
        if (!fromNetwork && peerNetworkHandler != null) {
            peerNetworkHandler.sendMove(move);
        }

        // Đổi lượt chơi
        currentPlayer = currentPlayer == Piece.Color.WHITE 
            ? Piece.Color.BLACK 
            : Piece.Color.WHITE;

        // Cập nhật UI nước đi cuối cùng
        if (uiUpdater != null) {
            uiUpdater.updateLastMoveLabel(move);
        }

        // Kiểm tra trạng thái game (chiếu, chiếu hết, hòa...)
        GameStateChecker.GameStateResult stateResult = gameStateChecker.checkGameState(currentPlayer);
        handleGameStateResult(stateResult);

        // Cập nhật UI lượt đi và bàn cờ
        if (uiUpdater != null) {
            uiUpdater.updateTurnLabel(currentPlayer);
        }
        if (boardView != null) {
            boardView.setCurrentPlayer(currentPlayer);
            boardView.setLastMove(move);
            boardView.refreshBoard();
        }

        // Nếu đang chơi với máy và đến lượt AI -> AI tự động đi sau một khoảng trễ
        if (aiPlayer != null && currentPlayer == aiPlayer.getAiColor()) {
            new Thread(() -> {
                try {
                    Thread.sleep(600); // Delay ~0.6s cho tự nhiên
                } catch (InterruptedException ignored) {
                    // Ignore
                }
                Platform.runLater(this::makeComputerMove);
            }).start();
        }
    }

    /**
     * Xử lý kết quả kiểm tra trạng thái game.
     * 
     * @param result Kết quả kiểm tra trạng thái
     */
    private void handleGameStateResult(GameStateChecker.GameStateResult result) {
        // Cập nhật label trạng thái
        if (uiUpdater != null) {
            uiUpdater.updateStatusLabel(gameStateChecker.getStatusText(result));
        }

        // Nếu game vẫn tiếp tục (NORMAL hoặc CHECK)
        if (result.getState() == GameStateChecker.GameStateResult.State.NORMAL ||
                result.getState() == GameStateChecker.GameStateResult.State.CHECK) {
            return;
        }

        // Game kết thúc: vô hiệu hóa các nút và kết thúc game
        disableGameButtons(false);
        endGame(result.getWinner());
    }

    // ===================== GAME ACTIONS =====================
    
    /**
     * Xử lý đề nghị hòa từ người chơi.
     */
    private void offerDraw() {
        if (gameActionHandler != null) {
            gameActionHandler.offerDraw();
        }
    }

    /**
     * Xử lý đầu hàng từ người chơi.
     */
    private void resign() {
        if (gameActionHandler != null) {
            gameActionHandler.resign();
        }
    }

    /**
     * Kết thúc game và hiển thị kết quả.
     * 
     * @param winner Người thắng (null nếu hòa)
     */
    private void endGame(Piece.Color winner) {
        // Vô hiệu hóa tương tác với bàn cờ
        if (boardView != null) {
            boardView.setCurrentPlayer(null);
            boardView.refreshBoard();
        }

        // Gọi API để cập nhật game và ranking trên server
        new Thread(() -> gameService.endGame(gameId, winner)).start();

        // Hiển thị dialog thông báo kết thúc
        Platform.runLater(() -> {
            String message;
            if (winner == null) {
                message = "Trận đấu kết thúc HÒA!";
            } else if (winner == playerColor) {
                message = "CHÚC MỪNG! Bạn đã THẮNG!";
            } else {
                message = "Bạn đã THUA trận đấu này.";
            }

            Alert endAlert = new Alert(Alert.AlertType.INFORMATION);
            endAlert.setTitle("Kết thúc trận đấu");
            endAlert.setHeaderText(null);
            endAlert.setContentText(message);
            endAlert.showAndWait();

            // Quay về trang chủ
            returnToHome();
        });
    }

    /**
     * Quay về màn hình Home.
     */
    private void returnToHome() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/chess_client/fxml/home.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.scene.Scene scene = new javafx.scene.Scene(root, 930, 740);
            javafx.stage.Stage stage = (javafx.stage.Stage) chessBoard.getScene().getWindow();
            stage.setScene(scene);
            stage.setResizable(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Vô hiệu hóa hoặc kích hoạt các nút điều khiển game.
     * 
     * @param enable true để enable, false để disable
     */
    private void disableGameButtons(boolean enable) {
        drawButton.setDisable(!enable);
        resignButton.setDisable(!enable);
    }

    // ===================== COMPUTER PLAYER =====================
    
    /**
     * AI player thực hiện nước đi.
     * Được gọi tự động khi đến lượt AI.
     */
    private void makeComputerMove() {
        if (aiPlayer == null) {
            return;
        }

        try {
            Move chosen = aiPlayer.makeMove();
            
            // Nếu không còn nước đi hợp lệ -> kiểm tra chiếu hết / hòa
            if (chosen == null) {
                Piece.Color aiColor = aiPlayer.getAiColor();
                GameStateChecker.GameStateResult result = gameStateChecker.checkGameState(aiColor);
                if (result.getState() != GameStateChecker.GameStateResult.State.NORMAL &&
                        result.getState() != GameStateChecker.GameStateResult.State.CHECK) {
                    endGame(result.getWinner());
                }
                return;
            }

            // Thực hiện nước đi của AI
            executeMove(chosen, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===================== FILE TRANSFER =====================

    /**
     * Xử lý gửi file từ người chơi.
     * Mở FileChooser để chọn file, đọc file và gửi qua P2P.
     */
    private void handleSendFile() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Chọn file để gửi");
        
        // Giới hạn kích thước file (10MB)
        fileChooser.setInitialDirectory(new java.io.File(System.getProperty("user.home")));
        
        java.io.File selectedFile = fileChooser.showOpenDialog(chatInput.getScene().getWindow());
        
        if (selectedFile != null) {
            // Kiểm tra kích thước file (giới hạn 10MB)
            long fileSize = selectedFile.length();
            if (fileSize > 10 * 1024 * 1024) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("File quá lớn");
                alert.setHeaderText(null);
                alert.setContentText("File không được vượt quá 10MB!");
                alert.showAndWait();
                return;
            }
            
            // Đọc file và gửi qua network
            new Thread(() -> {
                try {
                    byte[] fileData = java.nio.file.Files.readAllBytes(selectedFile.toPath());
                    String filename = selectedFile.getName();
                    
                    // Gửi file qua P2P
                    if (peerNetworkHandler != null) {
                        peerNetworkHandler.sendFile(filename, fileData);
                    }
                    
                    // Hiển thị file đã gửi trong chat (truyền fileData để hiển thị ảnh)
                    final byte[] finalFileData = fileData;
                    Platform.runLater(() -> {
                        if (chatManager != null) {
                            chatManager.addFileMessage(
                                playerName != null ? playerName : "Bạn",
                                filename,
                                fileSize,
                                true,
                                null,
                                finalFileData);
                        }
                    });
                    
                } catch (java.io.IOException e) {
                    e.printStackTrace();
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Lỗi");
                        alert.setHeaderText(null);
                        alert.setContentText("Không thể đọc file: " + e.getMessage());
                        alert.showAndWait();
                    });
                }
            }).start();
        }
    }

    /**
     * Xử lý tải xuống file nhận được từ đối thủ.
     * Mở DirectoryChooser để chọn thư mục lưu file.
     * 
     * @param filename Tên file
     * @param fileData Dữ liệu file
     */
    private void handleDownloadFile(String filename, byte[] fileData) {
        javafx.stage.DirectoryChooser dirChooser = new javafx.stage.DirectoryChooser();
        dirChooser.setTitle("Chọn thư mục lưu file");
        dirChooser.setInitialDirectory(new java.io.File(System.getProperty("user.home")));
        
        java.io.File selectedDir = dirChooser.showDialog(chatInput.getScene().getWindow());
        
        if (selectedDir != null) {
            new Thread(() -> {
                try {
                    java.io.File outputFile = new java.io.File(selectedDir, filename);
                    
                    // Nếu file đã tồn tại, thêm số vào tên file
                    int counter = 1;
                    while (outputFile.exists()) {
                        String nameWithoutExt = filename.substring(0, filename.lastIndexOf('.'));
                        String ext = filename.substring(filename.lastIndexOf('.'));
                        outputFile = new java.io.File(selectedDir, nameWithoutExt + "_" + counter + ext);
                        counter++;
                    }
                    
                    // Ghi file
                    java.nio.file.Files.write(outputFile.toPath(), fileData);
                    
                    // Thông báo thành công
                    final java.io.File finalOutputFile = outputFile;
                    Platform.runLater(() -> {
                        if (chatManager != null) {
                            chatManager.addSystemMessage("Đã lưu file: " + finalOutputFile.getAbsolutePath());
                        }
                    });
                    
                } catch (java.io.IOException e) {
                    e.printStackTrace();
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Lỗi");
                        alert.setHeaderText(null);
                        alert.setContentText("Không thể lưu file: " + e.getMessage());
                        alert.showAndWait();
                    });
                }
            }).start();
        }
    }
}
