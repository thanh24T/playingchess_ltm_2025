package com.chess_client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import com.chess_client.controllers.LoginController;
import com.chess_client.services.AuthService;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        // --- 1. Thiết lập Icon cho Stage chính ---
        // Mặc dù code bên dưới dùng WindowFactory để tạo Stage mới, 
        // dòng này thêm icon cho Stage mặc định được JavaFX tạo ra.
        stage.getIcons().add(new Image(
                getClass().getResourceAsStream("/com/chess_client/images/logo.png")));

        // --- 2. Xử lý tham số dòng lệnh (Command Line Arguments) ---
        // Biến toOpen xác định số lượng cửa sổ sẽ mở (mặc định là 1).
        int toOpen = 1;
        try {
            // Lấy danh sách tham số truyền vào khi chạy chương trình
            String[] args = getParameters().getRaw().toArray(new String[0]);
            for (int i = 0; i < args.length; i++) {
                // Kiểm tra nếu có cờ "-n" hoặc "--multi" để mở nhiều cửa sổ
                // Ví dụ: java -jar app.jar -n 2
                if (("-n".equals(args[i]) || "--multi".equals(args[i])) && i + 1 < args.length) {
                    toOpen = Math.max(1, Integer.parseInt(args[i + 1]));
                    break;
                }
            }
        } catch (Exception ignored) {
            // Bỏ qua lỗi nếu tham số không hợp lệ, giữ mặc định là 1
        }

        // --- 3. Khởi tạo và hiển thị cửa sổ ---
        
        // Trường hợp 1: Chỉ mở 1 cửa sổ (Chế độ bình thường)
        if (toOpen <= 1) {
            // Sử dụng Factory Pattern để tạo cửa sổ Đăng nhập
            Stage s = com.chess_client.ui.WindowFactory.createLoginStage();
            
            // Đặt icon cho cửa sổ này
            s.getIcons().add(new Image(getClass().getResourceAsStream("/com/chess_client/images/logo.png")));
            
            // Xử lý sự kiện khi người dùng bấm nút X (đóng cửa sổ)
            s.setOnCloseRequest(event -> {
                // Gọi API hoặc dịch vụ để đăng xuất đồng bộ (đảm bả