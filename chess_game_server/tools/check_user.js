import db from "../src/config/db.js";

async function check() {
  try {
    const username = process.argv[2] || 'nguyenvana';
    const [rows] = await db.promise().query('SELECT id, username, display_name, password, is_active FROM users WHERE username = ?', [username]);
    if (rows.length === 0) {
      console.log('NOT_FOUND');
      process.exit(0);
    }
    console.log(JSON.stringify(rows[0], null, 2));
    process.exit(0);
  } catch (e) {
    console.error('ERR', e.message || e.toString());
    process.exit(2);
  }
}

check();
