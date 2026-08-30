---
module: bridge-talk
file: userstory
type: spec
aspect: userstory
version: 0.1.0
status: draft
owner: Duy, Thui
last_updated: 2026-08-25
related:
  - business-rules.md
  - usecase.md
depends_on:
  - business-rules.md
  - usecase.md
bug_refs: []
---

# User story - BridgeTalk MVP phát hành hỗ trợ cuộc gọi

## US-BT-01: Kết bạn bằng Bridge ID

**Là** người dùng đã đăng nhập, **tôi muốn** gửi và chấp nhận lời mời bằng Bridge ID, **để** chỉ những người tôi biết mới gọi cho tôi.

> INVEST: Độc lập, có giá trị, ước lượng được, nhỏ và kiểm thử được.

```gherkin
Scenario: Hai người trở thành bạn bè
  Given người gửi và người nhận đều có UserAccount hợp lệ
  When người nhận chấp nhận ContactRequest đang Pending
  Then ContactRequest có State Accepted
  And hai người có thể thấy nhau trong danh bạ gọi được

Scenario: Người dùng chưa đăng nhập gửi lời mời
  Given người dùng không có phiên đăng nhập hợp lệ
  When người dùng yêu cầu gửi ContactRequest
  Then hệ thống trả BT_E_AUTH_REQUIRED
  And không tạo ContactRequest
```

**BR liên quan:** BR-BT-13, BR-BT-14
**UC liên quan:** UC-BT-01

## US-BT-02: Gọi một người bạn

**Là** người dùng BridgeTalk, **tôi muốn** gọi một người bạn đã chấp nhận, **để** bắt đầu cuộc hội thoại có trợ năng.

> INVEST: Độc lập, có giá trị, ước lượng được, nhỏ và kiểm thử được.

```gherkin
Scenario: Tạo cuộc gọi thành công
  Given Caller_A và Receiver_B là bạn bè Accepted
  And cả hai có phiên đăng nhập hợp lệ
  When Caller_A tạo CallSession với Receiver_B
  Then CallSession có State Pending
  And Receiver_B nhận được lời mời gọi

Scenario: Gọi người chưa là bạn bè
  Given Caller_A và Receiver_B không có quan hệ Accepted
  When Caller_A tạo CallSession với Receiver_B
  Then hệ thống trả BT_E_CONTACT_NOT_ACCEPTED
  And không tạo CallSession
```

**BR liên quan:** BR-BT-01, BR-BT-13, BR-BT-14
**UC liên quan:** UC-BT-02

## US-BT-03: Đọc caption và bản dịch trong cuộc gọi

**Là** Receiver_B không nghe được hoặc khó nghe, **tôi muốn** đọc caption và bản dịch ngay trong cuộc gọi, **để** hiểu nội dung A nói.

> INVEST: Độc lập, có giá trị, ước lượng được, nhỏ và kiểm thử được.

```gherkin
Scenario: Hiển thị caption nguồn và bản dịch
  Given CallSession có State Connected
  And Receiver_B đã chọn ngôn ngữ đích khác ngôn ngữ nguồn
  When Caller_A tạo một Caption cuối
  Then Receiver_B thấy Caption nguồn
  And Receiver_B thấy CaptionTranslation tương ứng

Scenario: Dịch thất bại
  Given CallSession có State Connected
  And Caption nguồn đã có kết quả cuối
  When dịch vụ dịch không tạo được CaptionTranslation
  Then hệ thống trả BT_E_TRANSLATION_UNAVAILABLE
  And Caption nguồn vẫn hiển thị cho Receiver_B
```

**BR liên quan:** BR-BT-02, BR-BT-03, BR-BT-08
**UC liên quan:** UC-BT-03

## US-BT-04: Trả lời bằng Morse-to-voice

**Là** Receiver_B không thể hoặc khó nói, **tôi muốn** nhập Morse bằng một nút lớn và gửi nội dung thành giọng nói, **để** Caller_A nghe được câu trả lời của tôi.

> INVEST: Độc lập, có giá trị, ước lượng được, nhỏ và kiểm thử được.

```gherkin
Scenario: Gửi MessageDraft đã xác nhận
  Given CallSession có State Connected
  And Receiver_B có MessageDraft hợp lệ
  When Receiver_B chọn Gửi
  Then hệ thống tạo VoiceOutput từ MessageDraft
  And Caller_A nhận được VoiceOutput

Scenario: Chuỗi Morse không có trong từ điển
  Given CallSession có State Connected
  And Receiver_B đã hoàn tất một TapSequence không có ánh xạ
  When hệ thống giải mã TapSequence
  Then hệ thống trả BT_E_UNKNOWN_MORSE_SEQUENCE
  And không tạo VoiceOutput
```

**BR liên quan:** BR-BT-04, BR-BT-05, BR-BT-06
**UC liên quan:** UC-BT-04

## US-BT-05: Chọn gợi ý nhanh từ Gemini

**Là** Receiver_B, **tôi muốn** chọn từ hoặc câu gợi ý sau khi nhập Morse, **để** hoàn thành thông điệp với ít thao tác hơn.

> INVEST: Độc lập, có giá trị, ước lượng được, nhỏ và kiểm thử được.

```gherkin
Scenario: Chọn gợi ý nhanh
  Given Receiver_B có MessageDraft hợp lệ
  And Gemini đã trả về tối đa năm MessageSuggestion Available
  When Receiver_B chọn một MessageSuggestion
  Then MessageDraft được cập nhật bằng gợi ý đã chọn
  And hệ thống chưa tạo VoiceOutput

Scenario: Gemini không trả được gợi ý
  Given Receiver_B có MessageDraft hợp lệ
  When Gemini không hoàn tất yêu cầu gợi ý
  Then hệ thống trả BT_E_AI_SUGGESTION_UNAVAILABLE
  And MessageDraft gốc không thay đổi
```

**BR liên quan:** BR-BT-06, BR-BT-11
**UC liên quan:** UC-BT-05

## US-BT-06: Quyết định lưu transcript có đồng ý của cả hai bên

**Là** người tham gia cuộc gọi, **tôi muốn** tự quyết định có lưu transcript hay không sau khi cuộc gọi kết thúc, **để** nội dung giao tiếp của tôi không bị lưu khi tôi chưa đồng ý.

> INVEST: Độc lập, có giá trị, ước lượng được, nhỏ và kiểm thử được.

```gherkin
Scenario: Cả hai người đồng ý lưu transcript
  Given CallSession có State Ended
  And Caller_A đã chọn decision Granted cho SaveTranscript
  When Receiver_B chọn decision Granted cho SaveTranscript
  Then hệ thống lưu transcript theo nơi lưu và thời hạn đã công bố

Scenario: Một người không đồng ý lưu transcript
  Given CallSession có State Ended
  And Caller_A đã chọn decision Declined cho SaveTranscript
  When Receiver_B hoàn tất quyết định lưu transcript
  Then hệ thống không lưu transcript dài hạn
  And hệ thống xóa nội dung giao tiếp theo chính sách phiên

Scenario: Người ngoài phiên cố ghi quyết định lưu
  Given người dùng không phải là CallParticipant của CallSession
  When người dùng gửi quyết định SaveTranscript
  Then hệ thống trả BT_E_FORBIDDEN
  And không tạo hoặc sửa ConsentRecord
```

**BR liên quan:** BR-BT-09, BR-BT-15
**UC liên quan:** UC-BT-06

<!-- Append CR sections below when logic changes -->
