---
module: bridge-talk
file: usecase
type: spec
aspect: usecase
version: 0.1.0
status: draft
owner: Duy, Thui
last_updated: 2026-08-25
related:
  - business-rules.md
  - dataflow.md
  - userflow.md
  - wireframe.md
depends_on:
  - business-rules.md
  - userflow.md
bug_refs: []
---

# Use case - BridgeTalk MVP phát hành hỗ trợ cuộc gọi

## UC-BT-01: Kết bạn bằng Bridge ID

- **Actor:** Người dùng đã đăng nhập.
- **Precondition:** Actor có `UserAccount` hợp lệ.
- **Trigger:** Actor gửi lời mời từ Bridge ID của người khác.

**Luồng chính:**
1. Actor nhập Bridge ID của người muốn kết bạn.
2. Hệ thống tạo `ContactRequest` ở State `Pending`.
3. Người nhận mở lời mời và chọn Chấp nhận.
4. Hệ thống cập nhật State thành `Accepted`.
5. Hai người xuất hiện trong danh bạ của nhau.

**Luồng thay thế:**
- **1a.** Actor chưa đăng nhập: hệ thống trả `BT_E_AUTH_REQUIRED` và mở luồng đăng nhập.
- **3a.** Người nhận từ chối: hệ thống cập nhật State thành `Declined`; hai người không xuất hiện trong danh bạ gọi được.

- **Postcondition:** Quan hệ bạn bè là `Accepted` hoặc `Declined`.
- **BR liên quan:** BR-BT-13, BR-BT-14

## UC-BT-02: Tạo và nhận cuộc gọi 1-1

- **Actor:** `Caller_A`, `Receiver_B`.
- **Precondition:** Cả hai có `UserAccount` hợp lệ và là bạn bè `Accepted`.
- **Trigger:** A chọn Gọi trong danh bạ.

**Luồng chính:**
1. A chọn B và yêu cầu tạo cuộc gọi.
2. Hệ thống tạo `CallSession` ở State `Pending`.
3. Hệ thống thông báo cuộc gọi đến cho B.
4. B chấp nhận cuộc gọi.
5. Hệ thống chuyển `CallSession` thành `Connected`; A đã được ghi nhận chấp nhận khi tạo phiên.
6. Hệ thống mở màn Cuộc gọi trợ năng cho A và B.

**Luồng thay thế:**
- **1a.** A hoặc B không có phiên đăng nhập: hệ thống trả `BT_E_AUTH_REQUIRED`.
- **1b.** A và B chưa là bạn bè `Accepted`: hệ thống trả `BT_E_CONTACT_NOT_ACCEPTED` và không tạo `CallSession`.
- **4a.** B từ chối hoặc không chấp nhận: hệ thống kết thúc `CallSession`; caption và Morse không hoạt động.

- **Postcondition:** `CallSession` là `Connected` hoặc `Ended`.
- **BR liên quan:** BR-BT-01, BR-BT-13, BR-BT-14

## UC-BT-03: Tạo caption và bản dịch trong cuộc gọi

- **Actor:** `Caller_A`, `Receiver_B`.
- **Precondition:** `CallSession` là `Connected`.
- **Trigger:** A nói trong cuộc gọi.

**Luồng chính:**
1. Ứng dụng gửi âm thanh của A tới adapter speech-to-text.
2. Adapter trả về `Caption` tạm thời.
3. Ứng dụng thay bằng `Caption` cuối khi có kết quả cuối.
4. Nếu B bật ngôn ngữ đích, ứng dụng yêu cầu dịch cho `Caption` cuối.
5. Ứng dụng hiển thị caption nguồn cùng bản dịch.

**Luồng thay thế:**
- **1a.** `CallSession` không còn kết nối: hệ thống trả `BT_E_SESSION_NOT_CONNECTED`.
- **2a.** Speech-to-text không có kết quả dùng được: hệ thống trả `BT_E_CAPTION_UNAVAILABLE`, giữ caption cuối gần nhất và cho phép thử lại.
- **4a.** Dịch lỗi: hệ thống trả `BT_E_TRANSLATION_UNAVAILABLE`, vẫn hiển thị caption nguồn.

- **Postcondition:** B thấy caption nguồn; bản dịch chỉ có khi xử lý thành công.
- **BR liên quan:** BR-BT-01, BR-BT-02, BR-BT-03, BR-BT-08

## UC-BT-04: Nhập Morse và phát lời nói đã xác nhận

- **Actor:** `Receiver_B`.
- **Precondition:** `CallSession` là `Connected`; B đang ở màn Cuộc gọi trợ năng.
- **Trigger:** B nhấn và thả nút Morse.

**Luồng chính:**
1. Ứng dụng đo thời lượng nhấn của B.
2. Ứng dụng thêm chấm hoặc gạch vào `TapSequence`.
3. Ứng dụng hoàn tất ký tự hoặc từ khi B dừng theo ngưỡng đã cấu hình.
4. Ứng dụng giải mã và hiển thị `MessageDraft`.
5. B có thể sửa bản nháp hoặc chọn gợi ý.
6. B chọn Gửi.
7. Ứng dụng tạo `VoiceOutput` và phát nội dung đã xác nhận cho A.

**Luồng thay thế:**
- **1a.** Thời lượng nhấn ngoài khoảng cấu hình: hệ thống trả `BT_E_INVALID_TAP_DURATION` và bỏ lần nhấn đó.
- **4a.** Chuỗi Morse không có trong từ điển: hệ thống trả `BT_E_UNKNOWN_MORSE_SEQUENCE`, giữ chuỗi thô và không phát giọng nói.
- **6a.** Bản nháp rỗng hoặc còn chuỗi không hợp lệ: hệ thống trả `BT_E_MESSAGE_NOT_SENDABLE`.
- **7a.** Provider TTS lỗi: hệ thống trả `BT_E_PROVIDER_UNAVAILABLE`, giữ bản nháp và cho phép thử lại.

- **Postcondition:** A nghe `VoiceOutput` chỉ khi B đã chọn Gửi; nếu lỗi, B vẫn còn bản nháp.
- **BR liên quan:** BR-BT-04, BR-BT-05, BR-BT-06, BR-BT-08

## UC-BT-05: Chọn từ hoặc câu gợi ý AI

- **Actor:** `Receiver_B`.
- **Precondition:** `MessageDraft` có nội dung hợp lệ trong `CallSession` đang kết nối.
- **Trigger:** Hệ thống hiển thị gợi ý nhanh hoặc B yêu cầu thêm gợi ý.

**Luồng chính:**
1. Ứng dụng gửi `MessageDraft` và ngôn ngữ đầu ra tới Gemini qua adapter phía server.
2. Gemini trả về tối đa năm `MessageSuggestion` có State `Available`.
3. Ứng dụng hiển thị các gợi ý dưới dạng nút chọn nhanh.
4. B chạm một gợi ý.
5. Ứng dụng điền gợi ý vào `MessageDraft` để B kiểm tra.
6. B chọn Gửi theo luồng Morse-to-voice hoặc giữ bản nháp gốc.

**Luồng thay thế:**
- **1a.** Gemini lỗi hoặc hết thời gian chờ: hệ thống trả `BT_E_AI_SUGGESTION_UNAVAILABLE` và giữ bản nháp gốc.
- **4a.** B không chọn gợi ý: ứng dụng không thay đổi bản nháp.

- **Postcondition:** `MessageDraft` chỉ thay đổi sau thao tác chọn của B.
- **BR liên quan:** BR-BT-06, BR-BT-11

## UC-BT-06: Quyết định lưu transcript sau cuộc gọi

- **Actor:** `Caller_A`, `Receiver_B`.
- **Precondition:** `CallSession` có State `Ended`.
- **Trigger:** Một người tham gia chọn Đồng ý lưu hoặc Không lưu tại WF-11.

**Luồng chính:**
1. Ứng dụng hiển thị loại nội dung, nơi lưu và thời hạn lưu transcript cho từng người tham gia.
2. A chọn Đồng ý lưu; hệ thống tạo `ConsentRecord` với `decision = Granted`.
3. B chọn Đồng ý lưu; hệ thống tạo `ConsentRecord` với `decision = Granted`.
4. Hệ thống kiểm tra cả hai `CallParticipant` đều có quyết định `Granted` cho `SaveTranscript`.
5. Hệ thống lưu transcript theo chính sách đã công bố.

**Luồng thay thế:**
- **2a.** A hoặc B chọn Không lưu: hệ thống ghi `decision = Declined`, không lưu transcript và xóa nội dung giao tiếp theo chính sách phiên.
- **4a.** Chưa có quyết định của người còn lại: hệ thống không lưu transcript; chỉ giữ trạng thái chờ quyết định trong thời hạn đã công bố.
- **4b.** Người gửi token không thuộc `CallSession`: hệ thống trả `BT_E_FORBIDDEN` và không tạo hoặc sửa `ConsentRecord`.

- **Postcondition:** Transcript chỉ tồn tại dài hạn khi mọi người tham gia đã đồng ý rõ ràng.
- **BR liên quan:** BR-BT-09, BR-BT-15

<!-- Append CR sections below when logic changes -->
