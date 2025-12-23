package com.chess_client.services;

import com.chess_client.models.Board;
import com.chess_client.models.Move;
import com.chess_client.models.Piece;

import java.util.ArrayList;
import java.util.List;

/**
 * Xử lý logic nghiệp vụ của cờ vua.
 * Kiểm tra tính hợp lệ của nước đi, các luật đặc biệt,
 * và các trạng thái game như chiếu, chiếu hết, hòa cờ.
 */
public class GameLogic {
    
    // ===================== FIELDS =====================
    
    private Board board;

    // ===================== CONSTRUCTOR =====================
    
    /**
     * Tạo GameLogic với bàn cờ được chỉ định.
     * 
     * @param board Bàn cờ cần xử lý logic
     */
    public GameLogic(Board board) {
        this.board = board;
    }

    // ===================== MOVE VALIDATION =====================
    
    /**
     * Kiểm tra nước đi có hợp lệ không.
     * 
     * @param move          Nước đi cần kiểm tra
     * @param currentPlayer Người chơi hiện tại
     * @return true nếu nước đi hợp lệ, false nếu không
     */
    public boolean isValidMove(Move move, Piece.Color currentPlayer) {
        Piece piece = move.getPieceMoved();

        // Kiểm tra quân cờ có tồn tại và thuộc về người chơi hiện tại không
        if (piece == null || piece.getColor() != currentPlayer) {
            return false;
        }

        // Kiểm tra theo luật di chuyển của từng loại quân cờ
        if (!isPieceMoveLegal(move)) {
            return false;
        }

        // Lưu ý: Theo yêu cầu, KHÔNG kiểm tra việc vua có còn bị chiếu sau khi đi hay không.
        // Nghĩa là nước đi vẫn được chấp nhận kể cả khi không thoát khỏi chiếu tướng.
        return true;
    }

    /**
     * Kiểm tra luật di chuyển của quân cờ có hợp lệ không.
     * 
     * @param move Nước đi cần kiểm tra
     * @return true nếu hợp lệ theo luật di chuyển của quân cờ
     */
    private boolean isPieceMoveLegal(Move move) {
        Piece piece = move.getPieceMoved();
        int fromRow = move.getFromRow();
        int fromCol = move.getFromCol();
        int toRow = move.getToRow();
        int toCol = move.getToCol();

        // Không được đi tại chỗ
        if (fromRow == toRow && fromCol == toCol) {
            return false;
        }

        Piece targetPiece = board.getPiece(toRow, toCol);

        // Không được ăn quân cùng màu
        if (targetPiece != null && targetPiece.getColor() == piece.getColor()) {
            return false;
        }

        // Kiểm tra theo từng loại quân cờ
        return switch (piece.getType()) {
            case PAWN -> isValidPawnMove(move);
            case ROOK -> isValidRookMove(move);
            case KNIGHT -> isValidKnightMove(move);
            case BISHOP -> isValidBishopMove(move);
            case QUEEN -> isValidQueenMove(move);
            case KING -> isValidKingMove(move);
        };
    }

    // ===================== PIECE-SPECIFIC MOVE VALIDATION =====================
    
    /**
     * Kiểm tra nước đi của tốt có hợp lệ không.
     * Xử lý: di chuyển thẳng, ăn chéo, đi 2 ô đầu tiên, en passant, phong cấp.
     */
    private boolean isValidPawnMove(Move move) {
        Piece pawn = move.getPieceMoved();
        int fromRow = move.getFromRow();
        int fromCol = move.getFromCol();
        int toRow = move.getToRow();
        int toCol = move.getToCol();

        // Hướng di chuyển: trắng đi lên (-1), đen đi xuống (+1)
        int direction = pawn.getColor() == Piece.Color.WHITE ? -1 : 1;
        // Hàng xuất phát: trắng ở hàng 6, đen ở hàng 1
        int startRow = pawn.getColor() == Piece.Color.WHITE ? 6 : 1;

        Piece targetPiece = board.getPiece(toRow, toCol);

        // === DI CHUYỂN THẲNG 1 Ô ===
        if (fromCol == toCol && toRow == fromRow + direction && targetPiece == null) {
            // Kiểm tra phong cấp nếu đến cuối bàn cờ
            if ((pawn.getColor() == Piece.Color.WHITE && toRow == 0) ||
                    (pawn.getColor() == Piece.Color.BLACK && toRow == 7)) {
                move.setPromotion(true);
            }
            return true;
        }

        // === DI CHUYỂN THẲNG 2 Ô TỪ VỊ TRÍ BAN ĐẦU ===
        if (fromCol == toCol && fromRow == startRow && toRow == fromRow + 2 * direction &&
                targetPiece == null && board.getPiece(fromRow + direction, fromCol) == null) {
            return true;
        }

        // === ĂN CHÉO ===
        if (Math.abs(fromCol - toCol) == 1 && toRow == fromRow + direction) {
            // Ăn quân địch
            if (targetPiece != null && targetPiece.getColor() != pawn.getColor()) {
                // Kiểm tra phong cấp
                if ((pawn.getColor() == Piece.Color.WHITE && toRow == 0) ||
                        (pawn.getColor() == Piece.Color.BLACK && toRow == 7)) {
                    move.setPromotion(true);
                }
                return true;
            }

            // === EN PASSANT: Ăn tốt qua đường ===
            Move lastMove = board.getLastMove();
            if (lastMove != null) {
                Piece lastPiece = board.getPiece(lastMove.getToRow(), lastMove.getToCol());
                // Kiểm tra: nước trước là tốt đi 2 ô, cùng cột, cùng hàng
                if (lastPiece != null && 
                    lastPiece.getType() == Piece.Type.PAWN &&
                    Math.abs(lastMove.getToRow() - lastMove.getFromRow()) == 2 &&
                    lastMove.getToCol() == toCol && 
                    lastMove.getToRow() == fromRow) {
                    move.setEnPassant(true);
                    move.setPieceCaptured(lastPiece);
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Kiểm tra nước đi của xe có hợp lệ không.
     * Xe chỉ di chuyển theo hàng hoặc cột.
     */
    private boolean isValidRookMove(Move move) {
        int fromRow = move.getFromRow();
        int fromCol = move.getFromCol();
        int toRow = move.getToRow();
        int toCol = move.getToCol();

        // Phải đi theo hàng hoặc cột
        if (fromRow != toRow && fromCol != toCol) {
            return false;
        }

        // Kiểm tra đường đi không bị chặn
        return !isPathBlocked(fromRow, fromCol, toRow, toCol);
    }

    /**
     * Kiểm tra nước đi của mã có hợp lệ không.
     * Mã di chuyển theo hình chữ L (2 ô một hướng, 1 ô hướng vuông góc).
     */
    private boolean isValidKnightMove(Move move) {
        int rowDiff = Math.abs(move.getToRow() - move.getFromRow());
        int colDiff = Math.abs(move.getToCol() - move.getFromCol());

        // Hình chữ L: (2,1) hoặc (1,2)
        return (rowDiff == 2 && colDiff == 1) || (rowDiff == 1 && colDiff == 2);
    }

    /**
     * Kiểm tra nước đi của tượng có hợp lệ không.
     * Tượng chỉ di chuyển theo đường chéo.
     */
    private boolean isValidBishopMove(Move move) {
        int rowDiff = Math.abs(move.getToRow() - move.getFromRow());
        int colDiff = Math.abs(move.getToCol() - move.getFromCol());

        // Phải đi theo đường chéo (chênh lệch hàng = chênh lệch cột)
        if (rowDiff != colDiff) {
            return false;
        }

        // Kiểm tra đường đi không bị chặn
        return !isPathBlocked(move.getFromRow(), move.getFromCol(), move.getToRow(), move.getToCol());
    }

    /**
     * Kiểm tra nước đi của hậu có hợp lệ không.
     * Hậu kết hợp khả năng của xe và tượng (đi theo hàng, cột, hoặc đường chéo).
     */
    private boolean isValidQueenMove(Move move) {
        return isValidRookMove(move) || isValidBishopMove(move);
    }

    /**
     * Kiểm tra nước đi của vua có hợp lệ không.
     * Vua di chuyển 1 ô mọi hướng, hoặc nhập thành (2 ô).
     */
    private boolean isValidKingMove(Move move) {
        int rowDiff = Math.abs(move.getToRow() - move.getFromRow());
        int colDiff = Math.abs(move.getToCol() - move.getFromCol());

        // Di chuyển 1 ô mọi hướng
        if (rowDiff <= 1 && colDiff <= 1) {
            return true;
        }

        // Nhập thành: di chuyển 2 ô theo hàng
        if (rowDiff == 0 && colDiff == 2) {
            return isValidCastling(move);
        }

        return false;
    }

    /**
     * Kiểm tra nhập thành có hợp lệ không.
     * Điều kiện: vua và xe chưa di chuyển, đường đi không bị chặn,
     * vua không bị chiếu và không đi qua ô bị chiếu.
     */
    private boolean isValidCastling(Move move) {
        Piece king = move.getPieceMoved();
        int row = move.getFromRow();
        int kingCol = move.getFromCol();
        int toCol = move.getToCol();

        // Vua chưa di chuyển
        if (king.hasMoved()) {
            return false;
        }

        // Xác định vị trí xe (bên phải hoặc bên trái)
        int rookCol = toCol > kingCol ? 7 : 0;
        Piece rook = board.getPiece(row, rookCol);

        // Xe phải tồn tại, chưa di chuyển, và cùng màu với vua
        if (rook == null || rook.getType() != Piece.Type.ROOK || rook.hasMoved()) {
            return false;
        }

        // Kiểm tra đường đi giữa vua và xe không bị chặn
        int start = Math.min(kingCol, rookCol) + 1;
        int end = Math.max(kingCol, rookCol);

        for (int col = start; col < end; col++) {
            if (board.getPiece(row, col) != null) {
                return false;
            }
        }

        // Vua không được đang bị chiếu
        if (isKingInCheck(board, king.getColor())) {
            return false;
        }

        // Vua không được đi qua ô bị chiếu
        int direction = toCol > kingCol ? 1 : -1;
        for (int col = kingCol; col != toCol + direction; col += direction) {
            Board tempBoard = board.copy();
            tempBoard.setPiece(row, col, king);
            tempBoard.setPiece(row, kingCol, null);

            if (isKingInCheck(tempBoard, king.getColor())) {
                return false;
            }
        }

        // Đánh dấu đây là nước nhập thành
        move.setCastling(true);
        return true;
    }

    // ===================== PATH CHECKING =====================
    
    /**
     * Kiểm tra đường đi có bị chặn bởi quân cờ khác không.
     * Dùng cho xe, tượng, hậu (các quân di chuyển theo đường thẳng).
     * 
     * @param fromRow Hàng bắt đầu
     * @param fromCol Cột bắt đầu
     * @param toRow   Hàng kết thúc
     * @param toCol   Cột kết thúc
     * @return true nếu đường đi bị chặn, false nếu không
     */
    private boolean isPathBlocked(int fromRow, int fromCol, int toRow, int toCol) {
        int rowStep = Integer.compare(toRow, fromRow);
        int colStep = Integer.compare(toCol, fromCol);

        int currentRow = fromRow + rowStep;
        int currentCol = fromCol + colStep;

        // Kiểm tra từng ô trên đường đi (không bao gồm ô đích)
        while (currentRow != toRow || currentCol != toCol) {
            if (board.getPiece(currentRow, currentCol) != null) {
                return true; // Bị chặn
            }
            currentRow += rowStep;
            currentCol += colStep;
        }

        return false; // Không bị chặn
    }

    // ===================== CHECK & CHECKMATE =====================
    
    /**
     * Kiểm tra vua có đang bị chiếu không.
     * 
     * @param checkBoard Bàn cờ cần kiểm tra
     * @param kingColor  Màu của vua cần kiểm tra
     * @return true nếu vua bị chiếu, false nếu không
     */
    public boolean isKingInCheck(Board checkBoard, Piece.Color kingColor) {
        // Tìm vị trí vua trên bàn cờ
        int kingRow = -1, kingCol = -1;

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = checkBoard.getPiece(row, col);
                if (piece != null && 
                    piece.getType() == Piece.Type.KING && 
                    piece.getColor() == kingColor) {
                    kingRow = row;
                    kingCol = col;
                    break;
                }
            }
            if (kingRow != -1) {
                break;
            }
        }

        // Kiểm tra xem có quân địch nào có thể tấn công vua không
        Piece.Color enemyColor = kingColor == Piece.Color.WHITE 
            ? Piece.Color.BLACK 
            : Piece.Color.WHITE;

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = checkBoard.getPiece(row, col);
                if (piece != null && piece.getColor() == enemyColor) {
                    Move attackMove = new Move(row, col, kingRow, kingCol, piece);
                    GameLogic tempLogic = new GameLogic(checkBoard);
                    if (tempLogic.isPieceMoveLegal(attackMove)) {
                        return true; // Vua bị chiếu
                    }
                }
            }
        }

        return false; // Vua không bị chiếu
    }

    /**
     * Kiểm tra có phải chiếu hết không.
     * Chiếu hết = vua bị chiếu + không còn nước đi hợp lệ nào.
     * 
     * @param kingColor Màu của vua cần kiểm tra
     * @return true nếu chiếu hết, false nếu không
     */
    public boolean isCheckmate(Piece.Color kingColor) {
        // Phải đang bị chiếu
        if (!isKingInCheck(board, kingColor)) {
            return false;
        }

        // Không còn nước đi hợp lệ nào
        return getAllValidMoves(kingColor).isEmpty();
    }

    /**
     * Kiểm tra có phải hòa cờ (stalemate) không.
     * Hòa cờ = không bị chiếu + không còn nước đi hợp lệ nào.
     * 
     * @param currentPlayer Người chơi hiện tại
     * @return true nếu hòa cờ, false nếu không
     */
    public boolean isStalemate(Piece.Color currentPlayer) {
        // Không được đang bị chiếu
        if (isKingInCheck(board, currentPlayer)) {
            return false;
        }

        // Không còn nước đi hợp lệ nào
        return getAllValidMoves(currentPlayer).isEmpty();
    }

    // ===================== MOVE GENERATION =====================
    
    /**
     * Lấy tất cả các nước đi hợp lệ của một màu quân cờ.
     * 
     * @param playerColor Màu quân cờ cần lấy nước đi
     * @return Danh sách tất cả nước đi hợp lệ
     */
    public List<Move> getAllValidMoves(Piece.Color playerColor) {
        List<Move> validMoves = new ArrayList<>();

        // Duyệt qua tất cả các ô trên bàn cờ
        for (int fromRow = 0; fromRow < 8; fromRow++) {
            for (int fromCol = 0; fromCol < 8; fromCol++) {
                Piece piece = board.getPiece(fromRow, fromCol);
                
                // Nếu có quân cờ cùng màu
                if (piece != null && piece.getColor() == playerColor) {
                    // Thử tất cả các ô đích có thể
                    for (int toRow = 0; toRow < 8; toRow++) {
                        for (int toCol = 0; toCol < 8; toCol++) {
                            Move move = new Move(fromRow, fromCol, toRow, toCol, piece);
                            if (isValidMove(move, playerColor)) {
                                validMoves.add(move);
                            }
                        }
                    }
                }
            }
        }

        return validMoves;
    }

    // ===================== UTILITY METHODS =====================
    
    /**
     * Kiểm tra một màu quân cờ có còn vua trên bàn cờ không.
     * 
     * @param color Màu quân cờ cần kiểm tra
     * @return true nếu còn vua, false nếu không
     */
    public boolean hasKing(Piece.Color color) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = board.getPiece(row, col);
                if (piece != null && 
                    piece.getType() == Piece.Type.KING && 
                    piece.getColor() == color) {
                    return true;
                }
            }
        }
        return false;
    }
}
