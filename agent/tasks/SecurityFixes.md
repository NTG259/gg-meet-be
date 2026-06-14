# Danh sách lỗ hổng cần sửa (Security Fixes)

---

## 1. Admin tự ban chính mình → mất quyền truy cập vĩnh viễn

**File:** `src/main/java/com/meet/BE/service/admin/AdminUserService.java`  
**Method:** `banUser(Long id)`  
**Mức độ:** Cao

**Vấn đề:**  
Không có kiểm tra nào ngăn admin gọi `POST /api/admin/users/{id}/ban` với chính `id` của mình. Khi bị ban, `accessTokenVersion` tăng lên và `refreshToken` bị xóa → token hiện tại bị từ chối ngay lập tức, không thể đăng nhập lại. Nếu chỉ có một admin duy nhất, toàn bộ trang admin bị khóa vĩnh viễn.

**Cần sửa:**  
Trong `banUser()`, so sánh `id` mục tiêu với ID của admin đang đăng nhập, nếu trùng thì từ chối:

```java
String currentEmail = SecurityUtils.getCurrentUserLogin().orElseThrow(...);
User currentUser = userRepository.findByEmail(currentEmail).orElseThrow(...);
if (currentUser.getId().equals(id)) {
    throw new BusinessException(ErrorCode.ACCESS_DENIED);
}
```

---

## 2. Không bảo vệ admin cuối cùng khỏi bị xóa/hạ cấp/ban

**File:** `src/main/java/com/meet/BE/service/admin/AdminUserService.java`  
**Methods:** `updateUser()`, `deleteUser()`, `banUser()`  
**Mức độ:** Cao

**Vấn đề:**  
Không có kiểm tra nào đảm bảo hệ thống luôn còn ít nhất một admin đang hoạt động. Ba thao tác dưới đây đều có thể xóa trắng toàn bộ admin:
- `PUT /api/admin/users/{id}` với `{"role":"USER"}` → hạ cấp admin xuống user
- `DELETE /api/admin/users/{id}` → soft delete admin (set `isDeleted=true`, `enabled=false`)
- `POST /api/admin/users/{id}/ban` → ban admin (set `isBanned=true`)

**Cần sửa:**  
Thêm hàm kiểm tra trước khi thực hiện các thao tác trên tài khoản admin:

```java
private void assertNotLastAdmin(User target) {
    if (!"ADMIN".equals(target.getRole())) return;
    long activeAdminCount = userRepository.countActiveAdmins(); // đếm admin chưa bị xóa/ban/disabled
    if (activeAdminCount <= 1) {
        throw new BusinessException(ErrorCode.ACCESS_DENIED); // không được xóa/hạ cấp/ban admin cuối
    }
}
```

Gọi hàm này ở đầu `updateUser()` (khi `role` thay đổi từ ADMIN sang USER), `deleteUser()`, và `banUser()`.

Thêm query vào `UserRepository`:
```java
@Query("SELECT COUNT(u) FROM User u WHERE u.role = 'ADMIN' AND u.isDeleted = false AND u.enabled = true AND u.isBanned = false")
long countActiveAdmins();
```

---

## 3. `cookieTokenResolver()` được định nghĩa nhưng không được đăng ký

**File:** `src/main/java/com/meet/BE/config/SecurityConfig.java`  
**Method:** `cookieTokenResolver()` (private, không dùng)  
**Mức độ:** Trung bình

**Vấn đề:**  
Hàm `cookieTokenResolver()` đọc access token từ cookie `access_token` nhưng **không bao giờ được truyền vào filter chain**. Spring Security mặc định chỉ đọc header `Authorization: Bearer`, hoàn toàn bỏ qua cookie. Client nào lưu token trong cookie sẽ bị coi là chưa đăng nhập.

**Cần sửa:**  
Đăng ký resolver vào cấu hình OAuth2 resource server:

```java
.oauth2ResourceServer(oauth2 -> oauth2
    .bearerTokenResolver(cookieTokenResolver())   // thêm dòng này
    .jwt(jwt -> jwt
        .jwtAuthenticationConverter(jwtAuthenticationConverter())
    )
    .authenticationEntryPoint(customAuthenticationEntryPoint)
    .accessDeniedHandler(customAccessDeniedHandler)
)
```

---

## Tóm tắt

| # | File | Method | Độ ưu tiên |
|---|------|--------|------------|
| 1 | `AdminUserService.java` | `banUser()` | Cao — sửa ngay |
| 2 | `AdminUserService.java` | `updateUser()`, `deleteUser()`, `banUser()` | Cao — sửa ngay |
| 3 | `SecurityConfig.java` | `cookieTokenResolver()` | Trung bình |
