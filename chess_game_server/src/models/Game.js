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
   * - Cập nhật status thàn