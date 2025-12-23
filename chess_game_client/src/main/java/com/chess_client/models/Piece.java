package com.chess_client.models;

/**
 * Đại diện cho một quân cờ trên bàn cờ.
 * Mỗi quân cờ có loại (Type) và màu (Color).
 */
public class Piece {
    
    // ===================== ENUMS =====================
    
    /**
     * Các loại quân cờ trong cờ vua.
     */
    public enum Type {
        KING,    // Vua
        QUEEN,   // Hậu
        ROOK,    // Xe
        BISHOP,  // Tượng
        KNIGHT,  // Mã
        PAWN     // Tốt
    }

    /**
     * Màu của quân cờ.
     */
    public enum Color {
        WHITE,   // Trắng
        BLACK    // Đen
    }

    // ===================== FIELDS =====================
    
    private Type type;              // Loại quân cờ
    private Color color;            // Màu quân cờ
    private boolean hasMoved;       // Đánh dấu đã di chuyển (dùng cho nhập thành và en passant)

    // ===================== CONSTRUCTOR =====================
    
    /**
     * Tạo một quân cờ mới.
     * 
     * @param type  Loại quân cờ
     * @param color Màu quân cờ
     */
    public Piece(Type type, Color color) {
        this.type = type;
        this.color = color;
        this.hasMoved = false;
    }

    // ===================== GETTERS & SETTERS =====================
    
    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Color getColor() {
        return color;
    }

    public boolean hasMoved() {
        return hasMoved;
    }

    public void setMoved(boolean moved) {
        this.hasMoved = moved;
    }

    // ===================== UTILITY METHODS =====================
    
    /**
     * Lấy ký hiệu Unicode để hiển thị quân cờ.
     * 
     * @return Ký hiệu Unicode của quân cờ
     */
    public String getUnicode() {
        switch (type) {
            case KING:
                return color == Color.WHITE ? "♔" : "♚";
            case QUEEN:
                return color == Color.WHITE ? "♕" : "♛";
            case ROOK:
                return color == Color.WHITE ? "♖" : "♜";
            case BISHOP:
                return color == Color.WHITE ? "♗" : "♝";
            case KNIGHT:
                return color == Color.WHITE ? "♘" : "♞";
            case PAWN:
                return color == Color.WHITE ? "♙" : "♟";
            default:
                return "";
        }
    }

    @Override
    public String toString() {
        return color + " " + type;
    }
}
