import db from "../config/db.js"; // Import kết nối Database

export default class Token {
  
  /**
   * Lưu một Refresh Token mới vào Database.
   * Hàm này được gọi ngay sau khi User đăng nhập thành công.
   * - user_id: ID của người dùng sở hữu token.
   * - refresh_token: Chuỗi token ngẫu nhiên hoặc JWT dài hạn.
   * - expires_at: Thời điểm token này sẽ hết hạn.
   */
  static async create({ user_id, refresh_token, expires_at }) {
    const [result] = await db
      .promise()
      .query(
        "INSERT INTO tokens (user_id, refresh_token, expires_at) VALUES (?, ?, ?)",
        [user_id, refresh_token, expires_at]
      );
    // Trả về object token vừa tạo
    return { id: result.insertId, user_id, refresh_token, expires_at };
  }

  /**
   * Tìm kiếm thông tin của một Refresh Token 