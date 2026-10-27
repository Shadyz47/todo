# Đăng nhập và xử lý xung đột

API dùng session cookie `JSESSIONID`. Sau khi login, giữ cookie này cho các request
task/category. Danh tính người dùng được lấy từ phiên đăng nhập.

## Gọi bằng Postman

Đặt biến `baseUrl` thành `http://localhost:9001`. Bật cookie jar của Postman.

1. Gọi `GET {{baseUrl}}/api/users/csrf`. Response có `headerName` và `token`.
   Lưu token bằng post-response script:

   ```javascript
   const csrf = pm.response.json();
   pm.environment.set("csrfHeader", csrf.headerName);
   pm.environment.set("csrfToken", csrf.token);
   ```

2. Thêm header `{{csrfHeader}}: {{csrfToken}}` và `Content-Type: application/json`
   cho các request POST, PUT, DELETE.

3. Gọi `POST {{baseUrl}}/api/users/register` với body:

   ```json
   { "username": "alice", "password": "example-password" }
   ```

   Thành công trả `201`. Username đã tồn tại trả `409` và giữ nguyên tài khoản cũ.
   Đăng ký không tự động đăng nhập.

4. Gọi `POST {{baseUrl}}/api/users/login` với cùng body và CSRF header.
   Thành công trả `200` và username, đồng thời lưu đăng nhập trong session.
   Sai thông tin đăng nhập trả `401`. Session ID được đổi sau khi đăng nhập thành công.

5. Gọi lại `GET {{baseUrl}}/api/users/csrf` sau khi login để lấy token mới.
   Token cũ bị hủy khi đăng nhập; dùng token mới cho request thay đổi dữ liệu.

6. Gọi `GET {{baseUrl}}/api/tasks` hoặc `GET {{baseUrl}}/api/categories`.
   Postman tự gửi cookie đã lưu. Các request GET này không cần CSRF header.

7. `POST {{baseUrl}}/api/users/logout` với CSRF header trả `204` và hủy session.
   Lấy lại CSRF token trước khi đăng nhập lần tiếp theo.

Request POST/PUT/DELETE thiếu hoặc sai CSRF token trả `403`.
Request vào API cần đăng nhập mà không có phiên hợp lệ trả `401` khi qua được kiểm tra CSRF.

## Category

- Tạo category trùng tên trong cùng tài khoản: `409`.
- Đổi tên category thành tên của category khác cùng tài khoản: `409`.
- Giữ nguyên tên category khi update: được phép.
- Các tài khoản khác nhau được dùng cùng tên category.
- Xóa category đang có task tham chiếu: `409`; task và category được giữ lại.
  Chuyển task sang category khác hoặc gỡ category khỏi task trước khi xóa.
- Xóa category không có task: `200`.
- Sửa/xóa category của tài khoản khác hoặc không tồn tại: `404`.
- Xung đột ràng buộc database phát sinh do request đồng thời được trả thành `409`.

## Phạm vi thay đổi

Lần sửa này chỉ xử lý các mục 2, 3, 5, 6 trong kết quả review.
Cơ chế lưu password, cấu hình tạo schema và các trường response category không thay đổi.

## Kiểm thử

Chạy `mvnw.cmd test` bằng JDK 25. Test dùng H2 trong bộ nhớ.
`UserSessionTests` kiểm tra đăng nhập qua controller, duy trì phiên,
đổi session ID, CSRF và logout. `CategoryConflictTests` kiểm tra xung đột
tên category, category đang được dùng và quyền sở hữu.
`SessionCookieHttpTests` chạy Tomcat trên cổng ngẫu nhiên và dùng HTTP client thật
để kiểm tra cookie giữa các request đăng ký, đăng nhập và tạo task.
