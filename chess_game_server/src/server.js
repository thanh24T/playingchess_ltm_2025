import express from "express";
import dotenv from "dotenv";
import path from "path";
import { fileURLToPath } from "url";
import cookieParser from "cookie-parser";
import authRoute from "./routes/authRoute.js";
import gameRoute from "./routes/gameRoute.js";
import userRoute from "./routes/userRoute.js";
import friendRoute from "./routes/friendRoute.js";
import leaderboardRoute from "./routes/leaderboardRoute.js";
import adminRoute from "./routes/adminRoute.js";
import { protectedRoute } from "./middlewares/authMiddleware.js";

// Load .env relative to this file so running from a different CWD still finds it
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
dotenv.config({ path: path.join(__dirname, "..", ".env") });

const app = express();
const PORT = process.env.PORT || 3000;

// Debug: show which DB user/host are being used (don't print password in logs)
console.error(
  "DB config:",
  { host: process.env.DB_HOST, user: process.env.DB_USER, database: process.env.DB_NAME }
);

app.use(express.json());
app.use(cookieParser());

// Logging middleware: method + path
app.use((req, res, next) => {
  console.error(`>>> ${req.method} ${req.path}`);
  next();
});

// Detailed request logger for debugging (prints body for POST/PUT/PATCH)
app.use((req, res, next) => {
  try {
    if (req.method === 'POST' || req.method === 'PUT' || req.method === 'PATCH') {
      console.error('>>> REQ BODY:', JSON.stringify(req.body));
    }
  } catch (e) {
    console.error('>>> REQ BODY: <unserializable>');
  }
  next();
});

app.use("/api/auth", authRoute);


app.use(protectedRoute);
app.use("/api/matchmaking", gameRoute);
app.use("/api/games", gameRoute);
app.use("/api/users", userRoute);
app.use("/api/friends", friendRoute);
app.use("/api/leaderboard", leaderboardRoute);
app.use("/api/admin", adminRoute);

// Error handler
app.use((err, req, res, next) => {
  console.error("Error caught:", err);
  res.status(500).json({ message: "Lỗi hệ thống" });
});

app.listen(PORT, "0.0.0.0", () => {
  console.log(`Server is running on port ${PORT}`);
});
