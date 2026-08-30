---
module: bridge-talk
file: wireframe
type: spec
aspect: wireframe
version: 0.1.0
status: draft
owner: Minh
last_updated: 2026-08-25
related:
  - business-rules.md
  - dataflow.md
  - userflow.md
depends_on:
  - business-rules.md
  - userflow.md
bug_refs: []
---

# Wireframe - BridgeTalk MVP phát hành hỗ trợ cuộc gọi

> Giao diện Android dọc. Màn gọi dùng full-page; thêm bạn, chấp nhận bạn và gợi ý AI dùng dialog/bottom sheet vì là thao tác ngắn, có xác nhận.

## WF-01: Đăng nhập

**Ngữ cảnh:** Người dùng chưa có phiên Firebase Authentication.

```text
+----------------------------------+
|           BridgeTalk             |
|                                  |
|       Kết nối theo cách của bạn  |
|                                  |
|      [ Đăng nhập để tiếp tục ]   |
|                                  |
|  Chính sách riêng tư             |
+----------------------------------+
```

| Component | Loại | Validation | BR liên quan |
|---|---|---|---|
| Đăng nhập để tiếp tục | Button | Chỉ mở luồng xác thực | BR-BT-13 |
| Chính sách riêng tư | Link | Không cản luồng đăng nhập | BR-BT-09 |

## WF-02: Tạo hồ sơ

**Ngữ cảnh:** Người dùng đã xác thực lần đầu; full-page vì phải tạo danh tính trước khi vào danh bạ.

```text
+----------------------------------+
| <- Tạo hồ sơ                     |
|                                  |
| Tên hiển thị *                   |
| [ Minh________________________ ] |
|                                  |
| Bridge ID *                      |
| [ minh-1234___________________ ] |
| Dùng mã này để bạn bè tìm bạn.   |
|                                  |
|        [ Hoàn tất ]              |
+----------------------------------+
```

| Component | Loại | Validation | BR liên quan |
|---|---|---|---|
| Tên hiển thị | Input text | Bắt buộc | BR-BT-13 |
| Bridge ID | Input text | Bắt buộc, unique | BR-BT-13 |
| Hoàn tất | Button | Disabled khi form chưa hợp lệ | BR-BT-13 |

## WF-03: Danh bạ

**Ngữ cảnh:** Người dùng đã đăng nhập. Đây là màn khởi đầu để chọn bạn và gọi.

```text
+----------------------------------+
| BridgeTalk                  (+)  |
| [ Tìm bạn theo tên hoặc Bridge ID]|
|                                  |
| Bạn bè                           |
| +------------------------------+ |
| | An Nam                  [Gọi]| |
| +------------------------------+ |
| | Mẹ                      [Gọi]| |
| +------------------------------+ |
|                                  |
| Danh bạ   Cuộc gọi   Hồ sơ      |
+----------------------------------+
```

| Component | Loại | Validation | BR liên quan |
|---|---|---|---|
| Tìm bạn | Search input | Tìm theo danh bạ hoặc Bridge ID | BR-BT-13 |
| (+) | Icon button | Mở màn Thêm bạn | BR-BT-13 |
| Gọi | Icon button | Chỉ active với bạn `Accepted` | BR-BT-14 |

## WF-04: Thêm bạn

**Ngữ cảnh:** Dialog chặn, một trường nhập, mở từ màn Danh bạ.

```text
+----------------------------------+
| Thêm bạn                      X  |
|                                  |
| Bridge ID                        |
| [ _____________________________] |
|                                  |
|       [ Hủy ]  [ Gửi lời mời ]   |
+----------------------------------+
```

| Component | Loại | Validation | BR liên quan |
|---|---|---|---|
| Bridge ID | Input text | Bắt buộc; không cho nhập Bridge ID của chính mình | BR-BT-13 |
| Gửi lời mời | Button | Disabled khi Bridge ID rỗng | BR-BT-13 |

## WF-05: Lời mời kết bạn

**Ngữ cảnh:** Bottom sheet dành cho người nhận lời mời.

```text
+----------------------------------+
| Lời mời kết bạn                  |
| Minh muốn kết bạn với bạn.       |
|                                  |
| [ Từ chối ]     [ Chấp nhận ]    |
+----------------------------------+
```

| Component | Loại | Validation | BR liên quan |
|---|---|---|---|
| Từ chối | Button | Chuyển lời mời thành `Declined` | BR-BT-14 |
| Chấp nhận | Button | Chuyển lời mời thành `Accepted` | BR-BT-14 |

## WF-06: Gọi đi

**Ngữ cảnh:** A chọn Gọi trên một bạn đã có State `Accepted`.

```text
+----------------------------------+
| <-                               |
|                                  |
|              An Nam              |
|            Đang gọi...           |
|                                  |
|          [ Hủy cuộc gọi ]        |
+----------------------------------+
```

| Component | Loại | Validation | BR liên quan |
|---|---|---|---|
| Hủy cuộc gọi | Button | Kết thúc `CallSession` đang chờ | BR-BT-01 |

## WF-07: Cuộc gọi đến

**Ngữ cảnh:** B nhận lời mời gọi trong app từ một bạn đã chấp nhận.

```text
+----------------------------------+
|          Cuộc gọi BridgeTalk     |
|                                  |
|              An Nam              |
|          đang gọi cho bạn        |
|                                  |
| [ Từ chối ]       [ Chấp nhận ]  |
+----------------------------------+
```

| Component | Loại | Validation | BR liên quan |
|---|---|---|---|
| Từ chối | Button | Không mở chức năng trợ năng | BR-BT-01 |
| Chấp nhận | Button | Mở màn Cuộc gọi trợ năng khi phiên thành `Connected` | BR-BT-01 |

## WF-08: Cuộc gọi trợ năng

**Ngữ cảnh:** `CallSession` đã `Connected`. Caption và Morse luôn hiển thị đồng thời.

```text
+----------------------------------+
| <-  An Nam        00:18     [..] |
| Nguồn: VI  Dich: EN               |
|----------------------------------|
| CAPTION TRUC TIEP                 |
|                                  |
| Toi can them thoi gian de tra loi |
| I need more time to reply.        |
|                                  |
|----------------------------------|
| MORSE INPUT                       |
| .... ..                           |
| [HI____________________________] |
|                                  |
| GOI Y NHANH                       |
| [PLEASE] [I DO] [THANK YOU]       |
| [HELP]   [YES]  [WAIT]            |
|                                  |
|      [ NHAN GIU - THA ]           |
|                                  |
| [Sua]                     [Gui]   |
|                                  |
| [Tat mic]  [Loa]  [Ket thuc]      |
+----------------------------------+
```

| Component | Loại | Validation | BR liên quan |
|---|---|---|---|
| Nguồn / Dịch | Segmented control | Dịch là tùy chọn; luôn giữ caption nguồn | BR-BT-02, BR-BT-03 |
| Caption trực tiếp | Text region | Phân biệt caption tạm thời và cuối | BR-BT-02 |
| Nhấn giữ - thả | Large press button | Ghi thời lượng theo cấu hình B | BR-BT-04 |
| Morse / MessageDraft | Read-only text + edit action | Không tự thay thế chuỗi không hợp lệ | BR-BT-05, BR-BT-06 |
| Sửa | Button | Cho B chỉnh `MessageDraft` trước gửi | BR-BT-06 |
| Gợi ý nhanh | Choice chip | Điền vào `MessageDraft`; không tự gửi | BR-BT-11 |
| Gửi | Button | Disabled khi bản nháp rỗng hoặc chưa giải mã xong | BR-BT-06 |
| Kết thúc | Icon button | Kết thúc phiên, sau đó mở bước quyết định lưu transcript cho từng người tham gia | BR-BT-09, BR-BT-16 |

## WF-10: Gợi ý AI

**Ngữ cảnh:** Bottom sheet mở khi B muốn xem thêm gợi ý ngoài các nút chọn nhanh trên màn Cuộc gọi trợ năng.

```text
+----------------------------------+
| Gợi ý câu                         |
| Bản nháp gốc: HI                  |
|                                  |
| ( ) Tôi cần thêm thời gian.       |
| ( ) Xin hãy chờ tôi một chút.     |
| ( ) Tôi sẽ trả lời ngay.          |
|                                  |
| [ Giữ bản nháp ] [ Dùng câu chọn ]|
+----------------------------------+
```

| Component | Loại | Validation | BR liên quan |
|---|---|---|---|
| Danh sách gợi ý | Radio group | Tối đa năm câu; không có câu nào được chọn sẵn | BR-BT-11 |
| Giữ bản nháp | Button | Đóng sheet, không sửa bản nháp | BR-BT-11 |
| Dùng câu chọn | Button | Disabled khi chưa chọn một câu | BR-BT-11 |

## WF-11: Quyết định lưu transcript

**Ngữ cảnh:** `CallSession` vừa kết thúc. Mỗi người tham gia tự quyết định; chỉ lưu khi cả A và B đồng ý.

```text
+----------------------------------+
| Lưu transcript?                  |
|                                  |
| Có thể lưu caption và bản dịch   |
| của cuộc gọi này. Không lưu audio|
| thô hoặc nội dung Morse.          |
|                                  |
| Lưu tại: [nơi lưu đã công bố]    |
| Thời hạn: [thời hạn đã công bố]  |
|                                  |
| [ Không lưu ]       [ Đồng ý lưu ]|
+----------------------------------+
```

| Component | Loại | Validation | BR liên quan |
|---|---|---|---|
| Không lưu | Button | Ghi `decision = Declined`; nội dung giao tiếp không được lưu dài hạn. | BR-BT-09 |
| Đồng ý lưu | Button | Ghi `decision = Granted`; chỉ hoàn tất lưu khi người còn lại cũng đã đồng ý. | BR-BT-09 |

## Biến thể trạng thái

| Điều kiện | Thay đổi trên màn hình |
|---|---|
| Caption lỗi | Vùng caption hiển thị trạng thái lỗi và nút Thử lại; vùng Morse vẫn hoạt động. |
| Morse không hợp lệ | Giữ chuỗi chấm-gạch, đánh dấu lỗi cạnh `MessageDraft`; nút Gửi disabled. |
| TTS lỗi | Giữ `MessageDraft`, hiển thị nút Thử phát lại. |
| Media lỗi | Hiển thị lỗi kết nối âm thanh, dừng caption/VoiceOutput và cho phép kết nối lại hoặc kết thúc. |
| Không phải bạn bè | Ở màn Danh bạ, nút Gọi đổi thành Gửi lời mời. |

## Ghi chú tương tác

| ID | Tương tác | Hành vi |
|---|---|---|
| I-01 | Nhấn và thả nút Morse | Tạo một token chấm/gạch; không phát âm ngay. |
| I-02 | Dừng nhập | Hoàn tất ký tự hoặc từ theo ngưỡng cấu hình. |
| I-03 | Chọn Gửi | Chỉ phát `VoiceOutput` từ nội dung B đã xác nhận. |
| I-04 | Kết thúc cuộc gọi | Đóng phiên, sau đó xử lý lưu/xóa theo quyết định của cả hai người tham gia. |

<!-- Append CR sections below when logic changes -->
