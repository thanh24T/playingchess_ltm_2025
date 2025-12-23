package com.chess_client.models;

/**
 * Đại diện cho một nước đi trong cờ vua.
 * Lưu trữ thông tin về vị trí bắt đầu, vị trí kết thúc,
 * và các thông tin đặc biệt như nhập thành, en passant, phong cấp.
 */
public class Move {
    
    // ===================== FIELDS =====================
    
    private int fromRow;            // Hàng bắt đầu (0-7)
    private int fromCol;            // Cột bắt đầu (0-7)
    private int toRow;              // Hàng kết thúc (0-7)
    private int toCol;              // Cột kết thúc (0-7)
    private Piece pieceMoved;       // Quân cờ được di chuyển
    private Piece pieceCaptured;    // Quân cờ bị ăn (nếu có)
    
    // Các cờ đặc biệt
    private boolean isEnPassant;   // Có phải nước đi en passant không
    private boolean isCastling;    // Có phải nước đi nhập thành không
    private boolean isPromotion;    // Có phải nước đi phong cấp không

    // ===================== CONSTRUCTOR =====================
    
    /**
     * Tạo một nước đi mới.
     * 
     * @param fromRow    Hàng bắt đầu
     * @param fromCol    Cột bắt đầu
     * @param toRow      Hàng kết thúc
     * @param toCol      Cột kết thúc
     * @param pieceMoved Quân cờ được di chuyển
     */
    public Move(int fromRow, int fromCol, int toRow, int toCol, Piece pieceMoved) {
        this.fromRow = fromRow;
        this.fromCol = fromCol;
        this.toRow = toRow;
        this.toCol = toCol;
        this.pieceMoved = pieceMoved;
        this.pieceCaptured = null;
        this.isEnPassant = false;
        this.isCastling = false;
        this.isPromotion = false;
    }

    // ===================== GETTERS & SETTERS =====================
    
    public int getFromRow() {
        return fromRow;
    }

    public int getFromCol() {
        return fromCol;
    }

    public int getToRow() {
        return toRow;
    }

    public int getToCol() {
        return toCol;
    }

    public Piece getPieceMoved() {
        return pieceMoved;
    }

    public Piece getPieceCaptured() {
        return pieceCaptured;
    }

    public void setPieceCaptured(Piece piece) {
        this.pieceCaptured = piece;
    }

    public boolean isEnPassant() {
        return isEnPassant;
    }

    public void setEnPassant(boolean enPassant) {
        this.isEnPassant = enPassant;
    }

    public boolean isCastling() {
        return isCastling;
    }

    public void setCastling(boolean castling) {
        this.isCastling = castling;
    }

    public boolean isPromotion() {
        return isPromotion;
    }

    public void setPromotion(boolean promotion) {
        this.isPromotion = promotion;
    }

    // ===================== UTILITY METHODS =====================
    
    @Override
    public String toString() {
        return pieceMoved + " từ (" + fromRow + "," + fromCol + ") đến (" + toRow + "," + toCol + ")";
    }
}
