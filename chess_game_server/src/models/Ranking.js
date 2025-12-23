import db from "../config/db.js"; // Import kết nối Database

export default class Ranking {
  
  /**
   * Lấy thông tin xếp hạng của một người dùng cụ thể.
   * Dùng để hiển thị profile cá nhân (VD: Bạn đã thắng bao nhiêu trận).
   */
  static async findByUserId(user_id) {
    const [rows] = await db.promise().query("SELECT * FROM rankings WHERE user_id=?", [user_id]);
    // Trả về object chứa stats của user đó (hoặc undefined nếu chưa có)
    return rows[0];
  }

  /**
   * Tạo bảng thành tích khởi tạo cho user mới.
   * Hàm này thường được gọi ngay sau khi User đăng ký tài khoản thành công.
   * Các chỉ số games_played, wins, losses, draws, score đều được set bằng 0.
   */
  static async create(user_id) {
    const [result] = await db
      .promise()
      .query("INSERT INTO rankings (user_id, games_played, wins, losses, draws, score) VALUES (?, 0, 0, 0, 0, 0)", [user_id]);
    return { id: result.insertId, user_id };
  }

  /**
   * Cập nhật chỉ số sau khi một ván cờ kết thúc.
   * Logic tính điểm:
   * - Thắng (win): +3 điểm
   * - Hòa (draw):  +1 điểm
   * - Thua (loss): +0 điểm (nhưng vẫn tăng số trận đã chơi và số trận thua)
   */
  static async updateAfterGame(user_id, result) {
    let query;
    switch (result) {
      case