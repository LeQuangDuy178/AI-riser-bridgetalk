---
module: bridge-talk
file: userflow
type: spec
aspect: userflow
version: 0.1.0
status: draft
owner: Duy, Thui
last_updated: 2026-08-25
related:
  - business-rules.md
  - dataflow.md
depends_on:
  - business-rules.md
  - dataflow.md
bug_refs: []
---

# Luồng người dùng - BridgeTalk MVP phát hành hỗ trợ cuộc gọi

> Hành trình của người dùng trong ứng dụng Android. Tên màn hình `WF-*` sẽ được đặc tả trong `wireframe.md`.

## Luồng chính: Đăng nhập và tạo hồ sơ (Persona: người dùng mới)

| Bước | Người dùng làm gì | Hệ thống phản hồi | Màn hình | BR liên quan |
|---:|---|---|---|---|
| 1 | Mở BridgeTalk. | Hiển thị lựa chọn đăng nhập. | WF-01 | BR-BT-13 |
| 2 | Đăng nhập bằng phương thức Firebase Authentication được hỗ trợ. | Xác thực phiên người dùng. | WF-01 | BR-BT-13 |
| 3 | Nhập tên hiển thị và `bridgeId`. | Kiểm tra `bridgeId` khả dụng, tạo `UserAccount`. | WF-02 | BR-BT-13 |
| 4 | Xác nhận hoàn tất. | Mở danh bạ rỗng hoặc danh bạ hiện có. | WF-03 | BR-BT-13 |

## Luồng chính: Kết bạn bằng Bridge ID (Persona: người dùng đã đăng nhập)

| Bước | Người dùng làm gì | Hệ thống phản hồi | Màn hình | BR liên quan |
|---:|---|---|---|---|
| 1 | Chọn Thêm bạn. | Hiển thị ô nhập `bridgeId`. | WF-04 | BR-BT-13 |
| 2 | Nhập `bridgeId` của người cần kết bạn và gửi lời mời. | Tạo `ContactRequest` ở State `Pending`. | WF-04 | BR-BT-13 |
| 3 | Người nhận mở lời mời và chọn Chấp nhận. | Chuyển `ContactRequest` sang `Accepted`. | WF-05 | BR-BT-14 |
| 4 | Một trong hai người mở danh bạ. | Hiển thị người kia là bạn có thể gọi. | WF-03 | BR-BT-14 |

## Luồng chính: A tạo cuộc gọi 1-1 cho B (Persona: Caller_A)

| Bước | Người dùng làm gì | Hệ thống phản hồi | Màn hình | BR liên quan |
|---:|---|---|---|---|
| 1 | A chọn B trong danh bạ. | Kiểm tra A và B có quan hệ `Accepted`. | WF-03 | BR-BT-14 |
| 2 | A chọn Gọi. | Tạo `CallSession` ở State `Pending` và gửi thông báo cuộc gọi cho B. | WF-06 | BR-BT-01, BR-BT-14 |
| 3 | B chấp nhận cuộc gọi. | Đánh dấu B đã chấp nhận; phiên thành `Connected` vì A đã chấp nhận khi tạo cuộc gọi. | WF-07 | BR-BT-01 |
| 4 | A nói. | Hiển thị giao diện cuộc gọi, trạng thái caption và Tap-to-Speech của B. | WF-08 | BR-BT-01 |

## Luồng chính: B đọc caption và trả lời bằng Tap-to-Speech (Persona: Receiver_B)

| Bước | Người dùng làm gì | Hệ thống phản hồi | Màn hình | BR liên quan |
|---:|---|---|---|---|
| 1 | B chọn ngôn ngữ nguồn và ngôn ngữ dịch nếu cần. | Lưu cấu hình ngôn ngữ cho `CallSession`. | WF-08 | BR-BT-02, BR-BT-03 |
| 2 | A nói trong cuộc gọi. | Hiển thị caption tạm thời, sau đó caption cuối; hiển thị bản dịch dưới caption nguồn khi B bật dịch. | WF-08 | BR-BT-02, BR-BT-03 |
| 3 | B nhấn và thả nút Tap-to-Speech để nhập Morse. | Hiển thị chuỗi chấm-gạch và giải mã thành `MessageDraft`. | WF-08 | BR-BT-04, BR-BT-05 |
| 4 | B kiểm tra `MessageDraft`. | Hiển thị nội dung gốc, các nút từ/câu gợi ý, Sửa và Gửi. | WF-08 | BR-BT-06, BR-BT-11 |
| 5 | B chạm một trong tối đa năm gợi ý hoặc bỏ qua. | Điền gợi ý đã chọn vào bản nháp để B kiểm tra; không tự gửi. | WF-10 | BR-BT-11 |
| 6 | B chọn một nội dung và chọn Gửi. | Tạo `VoiceOutput`, phát câu đã được B xác nhận cho A. | WF-08 | BR-BT-06 |
| 7 | Một trong hai người kết thúc cuộc gọi. | Kết thúc `CallSession`, gửi metric hợp lệ và yêu cầu từng người quyết định lưu transcript. | WF-08 | BR-BT-09, BR-BT-12, BR-BT-16 |
| 8 | Mỗi người chọn Đồng ý lưu hoặc Không lưu. | Chỉ lưu transcript khi cả A và B đồng ý; nếu không, xóa nội dung giao tiếp theo chính sách phiên. | WF-11 | BR-BT-09 |

## Luồng phụ: B chưa chấp nhận lời mời kết bạn

| Bước | Người dùng làm gì | Hệ thống phản hồi |
|---:|---|---|
| 1 | A chọn Gọi một người chưa là bạn bè. | Không tạo `CallSession`; hiển thị trạng thái chưa thể gọi. |
| 2 | A chọn Gửi lời mời kết bạn. | Điều hướng đến luồng Kết bạn bằng Bridge ID. |

**BR liên quan:** BR-BT-14, `BT_E_CONTACT_NOT_ACCEPTED`.

## Luồng phụ: Không giải mã được Morse hoặc provider lỗi

| Bước | Người dùng làm gì | Hệ thống phản hồi |
|---:|---|---|
| 1 | B hoàn tất một ký tự Morse không có trong từ điển. | Giữ chuỗi Morse thô, không phát âm và cho B xóa hoặc nhập lại. |
| 2 | Provider caption, dịch, TTS hoặc Gemini lỗi. | Giữ phần chức năng không lỗi, hiển thị trạng thái lỗi và cho phép thử lại. |
| 3 | Không thiết lập hoặc mất kênh âm thanh thời gian thực. | Hiển thị lỗi media; không giả vờ cuộc gọi còn hoạt động và cho phép kết nối lại hoặc kết thúc. |

**BR liên quan:** BR-BT-05, BR-BT-08, BR-BT-11, BR-BT-17.

## Sơ đồ hành trình sản phẩm

```text
[WF-01 Đăng nhập] -> [WF-03 Danh bạ] -> [WF-06 A gọi B]
                                      -> [WF-07 B chấp nhận]
                                      -> [WF-08 Caption trực tiếp + Morse]
                                      -> [WF-10 Gợi ý nhanh]
                                      -> [B gửi -> VoiceOutput]
                                      -> [Kết thúc phiên]
                                      -> [WF-11 Quyết định lưu transcript]
```

<!-- Append CR sections below when logic changes -->
