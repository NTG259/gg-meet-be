# Task: API Admin – Danh sách cuộc họp (`GET /api/admin/meetings`)

> Trạng thái: **CHƯA IMPLEMENT** – tài liệu mô tả việc cần làm. Không sửa code cho tới khi thống nhất.

## 1. Mục tiêu

Bổ sung một endpoint admin để trang quản trị (FE) lấy ra **danh sách toàn bộ cuộc họp** trong hệ thống, có phân trang, tìm kiếm và lọc theo trạng thái. Đây là phần mở rộng của bộ API admin (đã có `/api/admin/users`).

Khác với `GET /api/meetings` (chỉ trả các cuộc họp sắp tới của riêng user đang đăng nhập – xem `MeetingRepository.findUpcomingMeetingsForUser`), endpoint admin này:
- Trả **tất cả** cuộc họp của mọi host, mọi trạng thái (`SCHEDULED`, `ACTIVE`, `ENDED`).
- Chỉ dành cho role `ADMIN`.

---

## 2. Phân quyền

- Endpoint nằm dưới `/api/admin/**` → đã được `SecurityConfig` chặn `hasRole("ADMIN")`, nên **không cần thêm rule security mới** (cần kiểm tra lại để chắc chắn).
- `401 Unauthorized` nếu chưa đăng nhập; `403 Forbidden` nếu role không phải `ADMIN`.

---

## 3. Thiết kế API

### Request

```
GET /api/admin/meetings
```

| Query param | Bắt buộc | Mặc định | Mô tả |
|-------------|----------|----------|-------|
| `page`   | Không | `0`  | Số trang |
| `size`   | Không | `10` | Số record mỗi trang |
| `search` | Không | —    | Tìm theo `title`, `meetingCode`, **tên host** hoặc **email host** |
| `status` | Không | —    | Lọc theo trạng thái: `SCHEDULED` \| `ACTIVE` \| `ENDED` |
| `from`   | Không | —    | Lọc `startTime >= from` (ISO-8601, vd `2026-05-01T00:00:00`) |
| `to`     | Không | —    | Lọc `startTime <= to` (ISO-8601) |

> Sắp xếp mặc định: `createdAt desc` (mới nhất trước).

**Headers:** `Authorization: Bearer <admin_token>`

### Response (200 OK)

Dùng lại `ApiResponse<PagedResponse<AdminMeetingResponseDTO>>` cho đồng nhất với API admin user.

```json
{
    "data": {
        "content": [
            {
                "id": 12,
                "meetingCode": "abc-defg-hij",
                "title": "Daily standup",
                "status": "ACTIVE",
                "isScheduled": true,
                "hostId": 3,
                "hostName": "Nguyen Van A",
                "hostEmail": "host@example.com",
                "participantCount": 5,
                "startTime": "2026-05-29T09:00:00",
                "endTime": null,
                "createdAt": "2026-05-28T15:30:00"
            }
        ],
        "page": 0,
        "size": 10,
        "totalElements": 120,
        "totalPages": 12,
        "last": false
    },
    "message": "Fetched meetings successfully",
    "error": null,
    "statusCode": 200
}
```

> `participantCount`: số người tham gia có `participantStatus = ACCEPT` (đã/đang trong phòng).

---

## 4. Danh sách việc cần làm (checklist)

### 4.1. DTO response mới
- [ ] Tạo `domain/response/AdminMeetingResponseDTO.java` với các field: `id`, `meetingCode`, `title`, `status`, `isScheduled`, `hostId`, `hostName`, `hostEmail`, `participantCount`, `startTime`, `endTime`, `createdAt`.

### 4.2. Repository
- [ ] Thêm vào `MeetingRepository`:
  - Query phân trang lấy tất cả meeting, hỗ trợ các điều kiện optional (null = bỏ qua):
    - lọc theo `status`: `(:status is null or m.status = :status)`;
    - search theo `title`, `meetingCode`, **host name**, **host email** — cần `left join m.host h` để search được host, vd `(:search is null or lower(m.title) like :kw or lower(m.meetingCode) like :kw or lower(h.fullName) like :kw or lower(h.email) like :kw)`;
    - lọc khoảng thời gian: `(:from is null or m.startTime >= :from)` và `(:to is null or m.startTime <= :to)`.
  - Dùng `@EntityGraph(attributePaths = "host")` để tránh N+1 khi đọc host.
  - Gợi ý chữ ký: `Page<Meeting> findForAdmin(@Param("status") MeetingStatus status, @Param("search") String search, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable)`.
- [ ] Thêm cách đếm participant `ACCEPT`: query riêng đếm theo danh sách meetingId trong trang hiện tại (vd `countAcceptByMeetingIds(List<Long> ids)` trả về `(meetingId, count)`), map vào DTO. **Tránh** gọi count trong vòng lặp từng meeting (N+1).

### 4.3. Service
- [ ] Tạo `service/admin/AdminMeetingService.java`:
  - Method `getMeetings(int page, int size, String search, String status, LocalDateTime from, LocalDateTime to)`.
  - Validate/parse `status` string → enum `MeetingStatus`; nếu sai → `BusinessException(ErrorCode.INVALID_STATUS)` (tái dùng error code đang có).
  - Sort mặc định `createdAt desc` (truyền vào `PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))`).
  - Lấy `participantCount` (ACCEPT) cho các meeting trong trang qua một query gom (xem 4.2), map vào DTO.
  - Map `Meeting` → `AdminMeetingResponseDTO` (lấy host từ quan hệ `host`, xử lý null-safe nếu host bị xóa).
  - Trả `ApiResponse<PagedResponse<...>>` đúng format như `AdminUserService.getUsers`.

### 4.4. Controller
- [ ] Tạo `controller/AdminMeetingController.java` `@RequestMapping("/api/admin/meetings")`:
  - `@GetMapping getMeetings(page, size, search, status, from, to)` → gọi service.
  - `from`/`to` nhận kiểu `LocalDateTime` với `@DateTimeFormat(iso = ISO.DATE_TIME)`.

### 4.5. Security
- [ ] Kiểm tra `SecurityConfig`: rule `/api/admin/**` đã `hasRole("ADMIN")` bao trùm endpoint mới. Không cần thêm gì nếu đã đúng.

### 4.6. Test
- [ ] Test controller/service: phân trang, lọc `status`, search, và chặn quyền (non-admin → 403).

---

## 5. Error Codes

| HTTP | Khi nào |
|------|---------|
| 401  | Chưa đăng nhập |
| 403  | Role không phải `ADMIN` |
| 400  | `status` truyền sai (không thuộc `SCHEDULED`/`ACTIVE`/`ENDED`), hoặc `from`/`to` sai định dạng ISO-8601 |

---

## 6. Các file dự kiến thêm / sửa

| File | Thay đổi |
|------|----------|
| `domain/response/AdminMeetingResponseDTO.java` | **Thêm mới** – DTO response |
| `repository/MeetingRepository.java` | Thêm query `findForAdmin` (+ đếm participant nếu cần) |
| `service/admin/AdminMeetingService.java` | **Thêm mới** – service xử lý list/lọc |
| `controller/AdminMeetingController.java` | **Thêm mới** – controller `/api/admin/meetings` |
| `config/SecurityConfig.java` | Kiểm tra (thường không đổi – đã có rule `/api/admin/**`) |

---

## 7. Quyết định đã chốt

1. `participantCount`: **có**, đếm participant có `participantStatus = ACCEPT`.
2. `search`: tìm theo `title`, `meetingCode`, **tên host** và **email host**.
3. **Có** filter theo khoảng thời gian `from`/`to` (áp lên `startTime`).
4. Sort mặc định: `createdAt desc` (mới nhất trước).
