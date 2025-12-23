import db from "../config/db.js";

// --- DANH SÁCH CỘT ĐƯỢC PHÉP SỬA (WHITELIST) ---
// Đây là kỹ thuật bảo mật quan trọng (chống lỗi Mass Assignment).
// Nó đảm bảo user chỉ có thể sửa các trường này, không thể sửa các trường
// nhạy cảm khác (ví dụ: id, created_at) nếu không được định nghĩa ở đây.
const MUTABLE_COLUMNS = [
  "display_name",
  "email",
  "phone",
  "avatar",
  "role",       // Lưu ý: Cần cẩn thận khi cho phép update role trực tiếp
  "status",     // online/offline/playing
  "is_active",  // true/false (dùng để ban user)
  "password",
];

export default class User {
  // --- 1. CÁC HÀM TÌM KIẾM (READ) ---
  
  static async findAll() {
    const [rows] = await db.promise().query("SELECT * FROM users");
    return rows;
  }

  static async findById(id) {
    const [rows] = await db
      .promise()
      .query("SELECT * FROM users WHERE id = ?", [id]);
    return rows[0]; // Trả về object user hoặc undefined
  }

  static async findByUsername(username) {
    const [rows] = await db
      .promise()
      .query("SELECT * FROM users WHERE username = ?", [username]);
    return rows[0];
  }

  static async findByEmail(email) {
    const [rows] = await db
      .promise()
      .query("SELECT * FROM users WHERE email = ?", [email]);
    return rows[0];
  }

  // --- 2. TẠO USER MỚI (CREATE) ---
  
  static async create({
    username,
    password, // Lưu ý: Password truyền vào đây phải là đã Hash (mã hóa) rồi
    display_name,
    email,
    phone = null,
    avatar = null,
    role = "user",        // Mặc định là user thường
    status = "offline",   // Mặc định vừa tạo xong là offline
    is_active = true,     // Mặc định tài khoản được kích hoạt
  }) {
    const [result] = await db.promise().query(
      `INSERT INTO users
        (username, password, display_name, email, phone, avatar, role, status, is_active)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        username,
        password,
        display_name,
        email,
        phone,
        avatar,
        role,
        status,
        is_active,
      ]
    );
    // Sau khi insert, trả về thông tin đầy đủ của user vừa tạo
    return this.findById(result.insertId);
  }

  // --- 3. CẬP NHẬT THÔNG TIN (DYNAMIC UPDATE) ---
  
  /**
   * Hàm update tổng quát.
   * - id: ID của user cần sửa.
   * - fields: Object chứa các trường cần sửa (VD: { display_name: "ABC", phone: "123" }).
   */
  static async update(id, fields = {}) {
    // Lọc ra các trường hợp lệ (nằm trong MUTABLE_COLUMNS) và có giá trị khác undefined
    const entries = Object.entries(fields).filter(
      ([column, value]) =>
        MUTABLE_COLUMNS.includes(column) && value !== undefined
    );

    // Nếu không có trường nào hợp lệ để sửa thì trả về user hiện tại luôn
    if (entries.length === 0) {
      return this.findById(id);
    }

    // Tạo chuỗi SQL động: "display_name=?, phone=?"
    const assignments = entries.map(([column]) => `${column}=?`).join(", ");
    // Lấy danh sách giá trị tương ứng
    const values = entries.map(([, value]) => value);
    // Thêm ID vào cuối mảng tham số cho mệnh đề WHERE id=?
    values.push(id);

    await db
      .promise()
      .query(`UPDATE users SET ${assignments} WHERE id=?`, values);
    
    // Trả về dữ liệu mới nhất sau khi update
    return this.findById(id);
  }

  // Các hàm wrapper tiện ích, gọi lại hàm update ở trên
  static async updatePassword(id, hashedPassword) {
    await this.update(id, { password: hashedPassword });
    return { message: "Password updated successfully" };
  }

  static async updateStatus(id, status) {
    return this.update(id, { status });
  }

  static async updateActiveState(id, is_active) {
    return this.update(id, { is_active });
  }

  // --- 4. XÓA USER (DELETE) ---
  static async delete(id) {
    await db.promise().query("DELETE FROM users WHERE id=?", [id]);
    return { message: "User deleted successfully" };
  }

  // --- 5. TÌM KIẾM NÂNG CAO ---
  
  /**
   * Tìm user theo tên hiển thị hoặc username.
   * - searchTerm: Từ khóa tìm kiếm.
   * - excludeUserId: ID của người đang tìm (để không tự tìm thấy chính mình).
   */
  static async searchByUsernameOrDisplayName(searchTerm, excludeUserId = null) {
    const searchPattern = `%${searchTerm}%`; // Thêm % cho phép tìm gần đúng (LIKE)
    
    // Câu query tìm user có username HOẶC display_name chứa từ khóa
    // COALESCE(?, -1): Nếu excludeUserId là null, coi như là -1 (ID không tồn tại)
    let query = `SELECT id, username, display_name, avatar, status 
                 FROM users 
                 WHERE (username LIKE ? OR display_name LIKE ?) 
                 AND id != COALESCE(?, -1) 
                 AND is_active = TRUE
                 LIMIT 20`; // Chỉ lấy 20 kết quả để tối ưu hiệu năng
                 
    const params = excludeUserId 
      ? [searchPattern, searchPattern, excludeUserId]
      : [searchPattern, searchPattern, null];
    
    const [rows] = await db.promise().query(query, params);
    return rows;
  }
}