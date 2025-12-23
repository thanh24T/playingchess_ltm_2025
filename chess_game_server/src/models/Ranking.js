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
      case "win":
        // Cộng 1 trận đã chơi, cộng 1 trận thắng, cộng 3 điểm
        query = "UPDATE rankings SET games_played=games_played+1, wins=wins+1, score=score+3 WHERE user_id=?";
        break;
      case "draw":
        // Cộng 1 trận đã chơi, cộng 1 trận hòa, cộng 1 điểm
        query = "UPDATE rankings SET games_played=games_played+1, draws=draws+1, score=score+1 WHERE user_id=?";
        break;
      case "loss":
        // Cộng 1 trận đã chơi, cộng 1 trận thua (không cộng điểm)
        query = "UPDATE rankings SET games_played=games_played+1, losses=losses+1 WHERE user_id=?";
        break;
    }
    // Thực thi câu lệnh SQL tương ứng
    if (query) {
        await db.promise().query(query, [user_id]);
    }
  }

  /**
   * Lấy danh sách Top người chơi (Leaderboard).
   * - Mặc định lấy top 10 (limit = 10).
   * - Thực hiện JOIN giữa bảng 'rankings' và bảng 'users' để lấy tên người chơi (username/display_name) thay vì chỉ lấy ID.
   * - Sắp xếp theo điểm số (score) giảm dần (DESC).
   */
  static async getTopPlayers(limit = 10) {
    const [rows] = await db
      .promise()
      .query(
        `SELECT u.id, u.username, u.display_name, r.score, r.wins, r.losses, r.draws
         FROM rankings r
         JOIN users u ON r.user_id = u.id
         ORDER BY r.score DESC
         LIMIT ?`,
        [limit]
      );
    return rows;
  }
}