package com.chess_client.models;

/**
 * Đại diện cho bàn cờ vua 8x8.
 * Quản lý việc đặt quân, di chuyển quân, và các thao tác đặc biệt
 * như nhập thành, en passant, phong cấp.
 */
public class Board {
    
    // ===================== FIELDS =====================
    
    private Piece[][] board;    // Mảng 2 chiều 8x8 lưu trữ các quân cờ
    private Move lastMove;      // Nước đi cuối cùng (dùng cho en passant)

    // ===================== CONSTRUCTOR =====================
    
    /**
     * Tạo bàn cờ mới với các quân cờ ở vị trí ban đầu.
     */
    public Board() {
        board = new Piece[8][8];
        initializeBoard();
    }

    // ===================== INITIALIZATION =====================
    
    /**
     * Khởi tạo bàn cờ với đội hình ban đầu theo luật cờ vua.
     * - Quân đen ở hàng 0 và 1
     * - Quân trắng ở hàng 6 và 7
     */
    private void initializeBoard() {
        // ===== QUÂN ĐEN (hàng 0, 1) =====
        // Hàng sau (hàng 0): Xe, Mã, Tượng, Hậu, Vua, Tượng, Mã, Xe
        board[0][0] = new Piece(Piece.Type.ROOK, Piece.Color.BLACK);
        board[0][1] = new Piece(Piece.Type.KNIGHT, Piece.Color.BLACK);
        board[0][2] = new Piece(Piece.Type.BISHOP, Piece.Color.BLACK);
        board[0][3] = new Piece(Piece.Type.QUEEN, Piece.Color.BLACK);
        board[0][4] = new Piece(Piece.Type.KING, Piece.Color.BLACK);
        board[0][5] = new Piece(Piece.Type.BISHOP, Piece.Color.BLACK);
        board[0][6] = new Piece(Piece.Type.KNIGHT, Piece.Color.BLACK);
        board[0][7] = new Piece(Piece.Type.ROOK, Piece.Color.BLACK);

        // Hàng tốt (hàng 1): 8 quân tốt đen
        for (int col = 0; col < 8; col++) {
            board[1][col] = new Piece(Piece.Type.PAWN, Piece.Color.BLACK);
        }

        // ===== QUÂN TRẮNG (hàng 6, 7) =====
        // Hàng tốt (hàng 6): 8 quân tốt trắng
        for (int col = 0; col < 8; col++) {
            board[6][col] = new Piece(Piece.Type.PAWN, Piece.Color.WHITE);
        }

        // Hàng sau (hàng 7): Xe, Mã, Tượng, Hậu, Vua, Tượng, Mã, Xe
        board[7][0] = new Piece(Piece.Type.ROOK, Piece.Color.WHITE);
        board[7][1] = new Piece(Piece.Type.KNIGHT, Piece.Color.WHITE);
        board[7][2] = new Piece(Piece.Type.BISHOP, Piece.Color.WHITE);
        board[7][3] = new Piece(Piece.Type.QUEEN, Piece.Color.WHITE);
        board[7][4] = new Piece(Piece.Type.KING, Piece.Color.WHITE);
        board[7][5] = new Piece(Piece.Type.BISHOP, Piece.Color.WHITE);
        board[7][6] = new Piece(Piece.Type.KNIGHT, Piece.Color.WHITE);
        board[7][7] = new Piece(Piece.Type.ROOK, Piece.Color.WHITE);
    }

    // ===================== BASIC OPERATIONS =====================
    
    /**
     * Lấy quân cờ tại vị trí chỉ định.
     * 
     * @param row Hàng (0-7)
     * @param col Cột (0-7)
     * @return Quân cờ tại vị trí đó, hoặc null nếu không có quân hoặc vị trí không hợp lệ
     */
    public Piece getPiece(int row, int col) {
        if (isValidPosition(row, col)) {
            return board[row][col];
        }
        return null;
    }

    /**
     * Đặt quân cờ tại vị trí chỉ định.
     * 
     * @param row   Hàng (0-7)
     * @param col   Cột (0-7)
     * @param piece Quân cờ cần đặt (có thể null để xóa quân)
     */
    public void setPiece(int row, int col, Piece piece) {
        if (isValidPosition(row, col)) {
            board[row][col] = piece;
        }
    }

    /**
     * Kiểm tra vị trí có hợp lệ không (trong phạm vi 8x8).
     * 
     * @param row Hàng cần kiểm tra
     * @param col Cột cần kiểm tra
     * @return true nếu vị trí hợp lệ, false nếu không
     */
    public boolean isValidPosition(int row, int col) {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }

    // ===================== MOVE OPERATIONS =====================
    
    /**
     * Thực hiện một nước đi trên bàn cờ.
     * Xử lý các trường hợp đặc biệt: ăn quân, nhập thành, en passant, phong cấp.
     * 
     * @param move Nước đi cần thực hiện
     */
    public void movePiece(Move move) {
        Piece piece = getPiece(move.getFromRow(), move.getFromCol());

        if (piece != null) {
            // Lưu quân bị ăn (nếu có)
            Piece captured = getPiece(move.getToRow(), move.getToCol());
            move.setPieceCaptured(captured);

            // Di chuyển quân từ vị trí cũ sang vị trí mới
            setPiece(move.getToRow(), move.getToCol(), piece);
            setPiece(move.getFromRow(), move.getFromCol(), null);

            // Đánh dấu quân đã di chuyển (quan trọng cho nhập thành)
            piece.setMoved(true);

            // Xử lý en passant: ăn tốt qua đường
            if (move.isEnPassant()) {
                int captureRow = move.getFromRow(); // Hàng của tốt bị ăn
                setPiece(captureRow, move.getToCol(), null);
            }

            // Xử lý nhập thành: di chuyển cả vua và xe
            if (move.isCastling()) {
                // Xác định vị trí xe ban đầu
                int rookCol = move.getToCol() > move.getFromCol() ? 7 : 0;
                // Vị trí mới của xe (bên cạnh vua)
                int newRookCol = move.getToCol() > move.getFromCol() 
                    ? move.getToCol() - 1 
                    : move.getToCol() + 1;

                Piece rook = getPiece(move.getFromRow(), rookCol);
                setPiece(move.getFromRow(), newRookCol, rook);
                setPiece(move.getFromRow(), rookCol, null);
                if (rook != null) {
                    rook.setMoved(true);
                }
            }

            // Xử lý phong cấp: tốt đến cuối bàn cờ thành hậu
            if (move.isPromotion()) {
                piece.setType(Piece.Type.QUEEN); // Mặc định phong hậu
            }

            // Lưu nước đi cuối cùng (cần cho en passant)
            lastMove = move;
        }
    }

    /**
     * Hoàn tác một nước đi (undo move).
     * Khôi phục lại trạng thái bàn cờ trước nước đi.
     * 
     * @param move Nước đi cần hoàn tác
     */
    public void undoMove(Move move) {
        Piece piece = getPiece(move.getToRow(), move.getToCol());

        if (piece != null) {
            // Khôi phục vị trí quân cờ
            setPiece(move.getFromRow(), move.getFromCol(), piece);
            setPiece(move.getToRow(), move.getToCol(), move.getPieceCaptured());

            // Xử lý hoàn tác en passant
            if (move.isEnPassant()) {
                int captureRow = move.getFromRow();
                setPiece(captureRow, move.getToCol(), move.getPieceCaptured());
                setPiece(move.getToRow(), move.getToCol(), null);
            }

            // Xử lý hoàn tác nhập thành
            if (move.isCastling()) {
                int rookCol = move.getToCol() > move.getFromCol() ? 7 : 0;
                int newRookCol = move.getToCol() > move.getFromCol() 
                    ? move.getToCol() - 1 
                    : move.getToCol() + 1;

                Piece rook = getPiece(move.getFromRow(), newRookCol);
                setPiece(move.getFromRow(), rookCol, rook);
                setPiece(move.getFromRow(), newRookCol, null);
            }
        }
    }

    // ===================== GETTERS =====================
    
    /**
     * Lấy nước đi cuối cùng.
     * 
     * @return Nước đi cuối cùng, hoặc null nếu chưa có nước đi nào
     */
    public Move getLastMove() {
        return lastMove;
    }

    // ===================== UTILITY METHODS =====================
    
    /**
     * Tạo bản sao của bàn cờ hiện tại.
     * Dùng cho việc kiểm tra nước đi mà không ảnh hưởng đến bàn cờ gốc.
     * 
     * @return Bản sao của bàn cờ
     */
    public Board copy() {
        Board newBoard = new Board();
        
        // Sao chép tất cả các quân cờ
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = getPiece(row, col);
                if (piece != null) {
                    Piece newPiece = new Piece(piece.getType(), piece.getColor());
                    newPiece.setMoved(piece.hasMoved());
                    newBoard.setPiece(row, col, newPiece);
                } else {
                    newBoard.setPiece(row, col, null);
                }
            }
        }
        
        // Sao chép nước đi cuối cùng
        newBoard.lastMove = this.lastMove;
        
        return newBoard;
    }
}
