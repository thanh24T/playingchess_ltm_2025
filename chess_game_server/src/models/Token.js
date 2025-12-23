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
   * Tìm kiếm thông tin của một Refresh Token cụ thể.
   * Dùng để kiểm tra xem token client gửi lên có hợp lệ (tồn tại trong DB) hay không
   * khi client yêu cầu cấp lại Access Token mới (API /refresh-token).
   */
  static async findByToken(refresh_token) {
    const [rows] = await db
      .promise()
      .query("SELECT * FROM tokens WHERE refresh_token = ?", [refresh_token]);
    return rows[0]; // Trả về undefined nếu không tìm thấy (token giả hoặc đã bị xóa)
  }

  /**
   * Xóa một Refresh Token cụ thể.
   * Dùng cho chức năng Đăng Xuất (Logout).
   * Khi user bấm đăng xuất, ta xóa token này đi để kẻ gian không thể dùng lại nó nữa.
   */
  static async delete(refresh_token) {
    await db
      .promise()
      .query("DELETE FROM tokens WHERE refresh_token = ?", [refresh_token]);
  }

  /**
   * Xóa TẤT CẢ token của một user cụ thể.
   * Dùng cho tính năng "Đăng xuất khỏi tất cả các thiết bị" (Force Logout) 
   * hoặc khi user đổi mật khẩu (để bảo mật).
   */
  static async deleteByUser(user_id) {
    await db.promise().query("DELETE FROM tokens WHERE user_id = ?", [user_id]);
  }
}