# Tìm kiếm và sắp xếp task

`GET /api/tasks` trả về danh sách task của người dùng đang đăng nhập bằng session.
Không truyền bộ lọc sẽ lấy tất cả task của người dùng, mặc định theo độ ưu tiên.

Các query parameter đều không bắt buộc:

- `query`: tìm trong title hoặc description, không phân biệt chữ hoa/thường;
  bỏ khoảng trắng đầu/cuối. Chuỗi rỗng không giới hạn từ khóa.
  `%` và `_` có ý nghĩa wildcard của `LIKE`.
- `fromDate`: ngày bắt đầu theo định dạng `yyyy-MM-dd`, tính từ 00:00.
- `toDate`: ngày kết thúc theo định dạng `yyyy-MM-dd`, bao gồm cả ngày này.
  Khoảng ngày lọc theo `createdAt`; có thể truyền riêng một trong hai mốc.
- `completed`: `true` lấy task đã xong, `false` lấy task chưa xong.
  Không truyền sẽ lấy cả hai trạng thái.
- `sortBy`: `priority` (mặc định, HIGH → MEDIUM → LOW), `createdAt`
  hoặc `updatedAt` (mới nhất trước), `category` (tên tăng dần).
  Khi bằng nhau, sắp xếp tiếp theo `id` tăng dần.
  Task chưa có category vẫn được trả về; vị trí của category null tùy database.
- `order`: truyền `asc` để tăng dần hoặc `desc` để giảm dần
  (không phân biệt hoa/thường), áp dụng cho tất cả giá trị `sortBy`.
  Với `priority`, `asc` là LOW → MEDIUM → HIGH; `desc` là HIGH → MEDIUM → LOW.
  Không truyền sẽ giữ chiều mặc định ở trên; giá trị không hợp lệ trả `400`.

Ví dụ:

```http
GET /api/tasks?query=spring&fromDate=2026-10-01&toDate=2026-10-20&completed=false&sortBy=createdAt
```

Thành công trả `200` với mảng `TaskResponse`; không có kết quả trả `[]`.
Không đăng nhập trả `401`. `sortBy` không hợp lệ, tham số ngày/boolean không
hợp lệ hoặc `fromDate` sau `toDate` trả `400`.

Kiểm tra bằng `mvnw.cmd test` với JDK 25. `TaskSearchTests` kiểm tra luồng
controller → service → repository bằng MockMvc và H2 trong bộ nhớ.
