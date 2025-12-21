import db from "../src/config/db.js";
import bcrypt from "bcrypt";

async function check() {
  try {
    const username = process.argv[2] || 'nguyenvana';
    const password = process.argv[3] || '123456';
    const [rows] = await db.promise().query('SELECT password FROM users WHERE username = ?', [username]);
    if (rows.length === 0) {
      console.log('NOT_FOUND');
      process.exit(0);
    }
    const hash = rows[0].password;
    const ok = await bcrypt.compare(password, hash);
    console.log(ok ? 'MATCH' : 'NO_MATCH');
    console.log('CHECK_COMPLETE');
  } catch (e) {
    console.error('ERR', e.message || e.toString());
  }
}

check();
