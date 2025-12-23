import net from 'net';
import tls from 'tls';

/**
 * SMTP Client thuần túy - Không dùng thư viện
 * Implement SMTP protocol từ đầu bằng raw socket
 */
export class SMTPClient {
  constructor(host, port, secure = true) {
    this.host = host;
    this.port = port;
    this.secure = secure;
    this.socket = null;
    this.buffer = '';
  }

  /**
   * Kết nối tới SMTP server
   */
  async connect() {
    return new Promise((resolve, reject) => {
      if (this.secure) {
        // TLS Socket cho SMTP secure (port 465)
        this.socket = tls.connect({
          host: this.host,
          port: this.port,
          rejectUnauthorized: false
        }, () => {
          console.log('TLS connection established');
        });
      } else {
        // TCP Socket thường (port 25, 587)
        this.socket = net.createConnection({
          host: this.host,
          port: this.port
        });
      }

      this.socket.on('data', (data) => {
        this.buffer += data.toString();
        console.log('SMTP <<', data.toString().trim());
      });

      this.socket.on('error', (err) => {
        reject(err);
      });

      // Đợi server greeting (220)
      this.socket.once('data', (data) => {
        if (data.toString().startsWith('220')) {
          resolve();
        } else {
          reject(new Error('SMTP greeting failed'));
        }
      });
    });
  }

  /**
   * Gửi lệnh SMTP và đợi response
   */
  async sendCommand(command, expectedCode) {
    return new Promise((resolve, reject) => {
      console.log('SMTP >>', command);
      this.socket.write(command + '\r\n');

      const handler = (data) => {
        const response = data.toString();
        if (response.startsWith(expectedCode)) {
          this.socket.removeListener('data', handler);
          resolve(response);
        } else if (response.startsWith('5')) {
          this.socket.removeListener('data', handler);
          reject(new Error(`SMTP Error: ${response}`));
        }
      };

      this.socket.on('data', handler);

      setTimeout(() => {
        this.socket.removeListener('data', handler);
        reject(new Error('SMTP timeout'));
      }, 10000);
    });
  }

  /**
   * Xác thực SMTP (AUTH LOGIN)
   */
  async authenticate(username, password) {
    await this.sendCommand('EHLO localhost', '250');
    await this.sendCommand('AUTH LOGIN', '334');
    
    // Base64 encode username
    const encodedUser = Buffer.from(username).toString('base64');
    await this.sendCommand(encodedUser, '334');
    
    // Base64 encode password
    const encodedPass = Buffer.from(password).toString('base64');
    await this.sendCommand(encodedPass, '235');
  }

  /**
   * Gửi email
   */
  async sendMail(from, to, subject, body) {
    try {
      // MAIL FROM
      await this.sendCommand(`MAIL FROM:<${from}>`, '250');
      
      // RCPT TO
      await this.sendCommand(`RCPT TO:<${to}>`, '250');
      
      // DATA
      await this.sendCommand('DATA', '354');
      
      // Email headers và body
      const emailContent = [
        `From: ${from}`,
        `To: ${to}`,
        `Subject: ${subject}`,
        `Content-Type: text/html; charset=UTF-8`,
        '',
        body,
        '.'
      ].join('\r\n');
      
      await this.sendCommand(emailContent, '250');
      
      return true;
    } catch (error) {
      console.error('Send mail error:', error);
      throw error;
    }
  }

  /**
   * Đóng kết nối
   */
  async quit() {
    if (this.socket) {
      await this.sendCommand('QUIT', '221');
      this.socket.end();
    }
  }
}

/**
 * Helper function để gửi email nhanh
 */
export async function sendEmail(to, subject, htmlBody) {
  const smtp = new SMTPClient(
    process.env.SMTP_HOST || 'smtp.gmail.com',
    parseInt(process.env.SMTP_PORT || '465'),
    true
  );

  try {
    await smtp.connect();
    await smtp.authenticate(
      process.env.EMAIL_USER,
      process.env.EMAIL_PASSWORD
    );
    await smtp.sendMail(
      process.env.EMAIL_USER,
      to,
      subject,
      htmlBody
    );
    await smtp.quit();
    return true;
  } catch (error) {
    console.error('Email send failed:', error);
    throw error;
  }
}
