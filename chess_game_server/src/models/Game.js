import db from "../config/db.js"; // Import kết nối database MySQL

// Định nghĩa tập hợp các chế độ chơi hợp lệ:
// - friend: Chơi với bạn bè (có ID cụ thể)
// - random: Ghép cặp ngẫu nhiên
// - bot: Chơi với máy
const VALID_GAME_MODES = new Set(["friend", "random", "bot"]);

export default class Game {
  
  /**
   * Chuẩn hóa chế độ chơi (Helper method).
   * Nếu mode đầu vào không hợp lệ hoặc null, mặc định sẽ là 'random'.
   */
  static normalizeMode(mode) {
    if (mode && VALID_GAME_MODES.has(mode)) {
      return mode;
    }
    return "random";
  }

  /**
   * Tạo một ván cờ mới trong Database.
   * - player_white_id: ID người cầm quân trắng (bắt buộc).
   * - player_black_id: ID người cầm quân đen (có thể null nếu đang chờ ghép cặp).
   * - mode: Chế độ chơi.
   * - status mặc định khi tạo là 'waiting'.
   */
  static async create({ player_white_id, player_black_id = null, mode = "random" }) {
    const normalizedMode = this.normalizeMode(mode);
    
    // Thực hiện câu lệnh INSERT
    const [result] = await db
      .promise()
      .query(
        "INSERT INTO games (player_white_id, player_black_id, mode, status) VALUES (?, ?, ?, 'waiting')",
        [player_white_id, player_black_id, normalizedMode]
      );
      
    // Trả về object chứa thông tin game vừa tạo kèm ID mới sinh ra (insertId)
    return {
      id: result.insertId,
      player_white_id,
      player_black_id,
      mode: normalizedMode,
      status: "waiting",
    };
  }

  /**
   * Cập nhật trạng thái của ván cờ (VD: từ 'waiting' -> 'playing').
   */
  static async updateStatus(id, status) {
    await db.promise().query("UPDATE games SET status=? WHERE id=?", [status, id]);
  }

  /**
   * Kết thúc ván cờ.
   * - Cập nhật status thành 'finished'.
   * - Ghi lại ID người thắng (winner_id).
   * - Ghi lại thời điểm kết thúc (ended_at = NOW()).
   */
  static async setWinner(id, winner_id) {
    await db
      .promise()
      .query("UPDATE games SET status='finished', winner_id=?, ended_at=NOW() WHERE id=?", [
        winner_id,
        id,
      ]);
  }

  /**
   * Lấy danh sách TOÀN BỘ các game trong DB (Thường dùng cho Admin dashboard).
   */
  static async findAll() {
    const [rows] = await db.promise().query("SELECT * FROM games");
    return rows;
  }

  /**
   * Tìm thông tin chi tiết một ván cờ dựa trên ID.
   * Trả về dòng dữ liệu đầu tiên (object) hoặc undefined nếu không tìm thấy.
   */
  static async findById(id) {
    const [rows] = await db.promise().query("SELECT * FROM games WHERE id=?", [id]);
    return rows[0];
  }

  /**
   * Lấy danh sách các ván cờ đang diễn ra (status = 'playing').
   * Dùng để hiển thị danh sách phòng chờ hoặc thống kê realtime.
   */
  static async getOngoingGames() {
    const [rows] = await db.promise().query("SELECT * FROM games WHERE status='playing'");
    return rows;
  }

  /**
   * Xóa một ván cờ khỏi Database.
   */
  static async delete(id) {
    await db.promise().query("DELETE FROM games WHERE id=?", [id]);
  }

  /**
   * Tìm ván cờ đang "treo" (waiting) của một người dùng cụ thể.
   * Logic: Người dùng là quân trắng HOẶC quân đen VÀ trạng thái là 'waiting'.
   * Mục đích: Ngăn người dùng tạo nhiều phòng chờ cùng lúc hoặc hỗ trợ tính năng Reconnect.
   */
  static async findPendingGameForUser(userId) {
    const [rows] = await db
      .promise()
      .query(
        "SELECT * FROM games WHERE status='waiting' AND (player_white_id=? OR player_black_id=?) ORDER BY id DESC LIMIT 1",
        [userId, userId]
      );
    return rows[0];
  }
}