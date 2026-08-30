---
module: bridge-talk
file: business-rules
type: spec
aspect: business-rules
version: 0.1.0
status: draft
owner: Duy, Thui
last_updated: 2026-08-25
related:
  - ../../../PRODUCT-CONCEPT-BRIDGETALK.md
depends_on:
  - Đề bài BridgeTalk do nhóm cung cấp, 2026-08-25
bug_refs: []
---

# Quy tắc nghiệp vụ - BridgeTalk MVP phát hành hỗ trợ cuộc gọi

> Logic cho bản Android chính thức của BridgeTalk: A gọi và B nhận trong cùng ứng dụng. Bản nộp phải có đường chạy production, không phụ thuộc mock adapter cho luồng chính. MVP không thay thế ứng dụng Điện thoại, cuộc gọi GSM, FaceTime hay dịch vụ khẩn cấp. Bản phát hành đánh giá sẽ được đưa lên Google Play.

## Phạm vi và thuật ngữ

| Thuật ngữ | Diễn giải |
|---|---|
| `Caller_A` | Người gọi, người nói trong cuộc gọi trên ứng dụng. |
| `Receiver_B` | Người nhận, dùng caption, Tap-to-Speech hoặc cả hai. |
| `CallSession` | Phiên gọi trong ứng dụng đã kết nối, có mã phiên duy nhất. |
| `Caption` | Văn bản từ lời nói của A để B đọc. |
| `TapSequence` | Chuỗi sự kiện nhấn và thả do B tạo. |
| `MorseToken` | Một dấu chấm hoặc gạch được suy ra từ một lần nhấn hợp lệ. |
| `MessageDraft` | Văn bản được giải mã từ `TapSequence`, trước khi B gửi. |
| `VoiceOutput` | Giọng nói được tổng hợp từ `MessageDraft` sau khi B gửi. |

## Quyết định cho MVP phát hành

- MVP hỗ trợ tiếng Việt và tiếng Anh. Dịch ngôn ngữ là tùy chọn theo `CallSession`; `Caption` gốc luôn phải hiển thị.
- Morse hỗ trợ trong MVP: chuẩn Quốc tế cho `A-Z`, `0-9`, khoảng trắng và các câu nhanh do cấu hình sản phẩm quy định. MVP không tuyên bố nhập trực tiếp dấu tiếng Việt bằng Morse.
- Ngưỡng nhấn có thể điều chỉnh. Giá trị thử nghiệm ban đầu là 250 ms: nhấn không vượt ngưỡng là chấm, nhấn dài hơn là gạch. Đây là giá trị để thử nghiệm, không phải khẳng định tối ưu cho mọi người dùng.
- Nhận diện ngôn ngữ ký hiệu bằng video nằm ngoài MVP. Giai đoạn sau chỉ được triển khai khi đã chốt loại ngôn ngữ ký hiệu, có dữ liệu được đồng ý sử dụng và đánh giá cùng người ký hiệu.

## Danh sách quy tắc nghiệp vụ

### BR-BT-01: Chỉ kích hoạt tính năng trong phiên gọi của ứng dụng
- **Mô tả:** BridgeTalk SHALL chỉ cung cấp tính năng trợ năng sau khi A và B tham gia cùng một `CallSession`.
- **Điều kiện:** **When** A tạo cuộc gọi, hệ thống SHALL tạo `CallSession` ở trạng thái `Pending` và ghi nhận A đã chấp nhận. Phiên chỉ chuyển `Connected` khi B chấp nhận lời mời còn hiệu lực.
- **Hành vi:** **If** một trong hai người chưa chấp nhận phiên gọi, **then** hệ thống MUST giữ caption, Tap-to-Speech và `VoiceOutput` ở trạng thái không hoạt động, đồng thời trả `BT_E_SESSION_NOT_CONNECTED` cho mọi thao tác trợ năng.
- **Mã lỗi:** `BT_E_SESSION_NOT_CONNECTED`
- **Lý do nghiệp vụ:** Cần nêu rõ ranh giới kỹ thuật để không tạo cảm giác ứng dụng kiểm soát cuộc gọi ở cấp thiết bị.

### BR-BT-02: Tạo caption có nguồn rõ ràng
- **Mô tả:** Lời nói từ `Caller_A` SHALL trở thành `Caption` cho `Receiver_B` bằng ngôn ngữ nguồn đã chọn.
- **Điều kiện:** **When** A gửi một đoạn âm thanh trong `CallSession` đã kết nối, dịch vụ nhận dạng giọng nói SHALL trả về `Caption` hoặc trạng thái lỗi.
- **Hành vi:** Client MUST gắn nhãn đây là bản chép lời trực tiếp và MUST hiển thị kết quả tạm thời tách biệt với kết quả cuối. **If** dịch vụ không trả được kết quả cuối có thể dùng, **then** client MUST giữ caption cuối gần nhất, hiển thị trạng thái lỗi và trả `BT_E_CAPTION_UNAVAILABLE`.
- **Mã lỗi:** `BT_E_CAPTION_UNAVAILABLE`
- **Lý do nghiệp vụ:** B cần đọc nội dung theo từng lượt nói nhưng không được coi nhận dạng chưa chắc chắn là lời nói đã xác nhận.

### BR-BT-03: Giữ văn bản nguồn khi dịch
- **Mô tả:** Bản dịch SHALL là biểu diễn bổ sung của `Caption` cuối, không thay thế văn bản nguồn.
- **Điều kiện:** **When** B bật một ngôn ngữ đích khác ngôn ngữ nguồn, dịch vụ dịch SHALL xử lý từng `Caption` cuối.
- **Hành vi:** Client MUST hiển thị đồng thời `Caption` nguồn và bản dịch. **If** dịch thất bại, **then** `Caption` nguồn MUST vẫn đọc được, client MUST trả `BT_E_TRANSLATION_UNAVAILABLE` và cuộc gọi MUST vẫn sử dụng được.
- **Mã lỗi:** `BT_E_TRANSLATION_UNAVAILABLE`
- **Lý do nghiệp vụ:** Văn bản nguồn giúp hai bên làm rõ khi bản dịch thiếu hoặc sai.

### BR-BT-04: Giải mã từng lần nhập chạm
- **Mô tả:** BridgeTalk SHALL chuyển `TapSequence` đã nhận thành các `MorseToken` theo ngưỡng nhấn do B cấu hình.
- **Điều kiện:** **When** B thả nút Tap-to-Speech trong `CallSession` đã kết nối, client SHALL tính thời lượng nhấn.
- **Hành vi:** **If** thời lượng ngắn hơn mức tối thiểu hoặc dài hơn mức tối đa đã cấu hình, **then** client MUST từ chối lần nhấn đó, MUST NOT thêm `MorseToken` và trả `BT_E_INVALID_TAP_DURATION`. **When** B dừng đủ thời gian phân cách ký tự, client SHALL kết thúc ký tự Morse hiện tại; **when** B dừng đủ thời gian phân cách từ, client SHALL kết thúc từ hiện tại.
- **Mã lỗi:** `BT_E_INVALID_TAP_DURATION`
- **Lý do nghiệp vụ:** Một nút lớn hỗ trợ người dùng chỉ cần thực hiện một cử động đơn giản; cấu hình thời gian đáp ứng khác biệt về khả năng vận động.

### BR-BT-05: Không đoán chuỗi Morse không hợp lệ
- **Mô tả:** Bộ giải mã SHALL chỉ ánh xạ ký tự Morse hoàn chỉnh khi chuỗi đó tồn tại trong từ điển Morse được hỗ trợ.
- **Điều kiện:** **When** bộ giải mã kết thúc một ký tự Morse, hệ thống SHALL tra cứu chuỗi chấm-gạch trong từ điển đã hỗ trợ.
- **Hành vi:** **If** không có kết quả khớp, **then** client MUST giữ lại chuỗi thô để B sửa, MUST NOT tự thay bằng chữ cái hoặc câu khác và trả `BT_E_UNKNOWN_MORSE_SEQUENCE`.
- **Mã lỗi:** `BT_E_UNKNOWN_MORSE_SEQUENCE`
- **Lý do nghiệp vụ:** Phát âm một thông điệp bị tự suy đoán có thể gây hại trong hội thoại; tốc độ không biện minh cho việc thay thế không truy vết được.

### BR-BT-06: B phải gửi trước khi hệ thống đọc cho A
- **Mô tả:** `VoiceOutput` SHALL chỉ được tạo từ `MessageDraft` mà B chủ động gửi.
- **Điều kiện:** **When** B chọn Gửi với `MessageDraft` không rỗng, dịch vụ text-to-speech SHALL tổng hợp nội dung đang hiển thị theo ngôn ngữ và giọng B đã chọn.
- **Hành vi:** **If** `MessageDraft` rỗng, có ký tự Morse chưa được xử lý hoặc B chưa chọn Gửi, **then** hệ thống MUST NOT tổng hợp hay truyền giọng nói và trả `BT_E_MESSAGE_NOT_SENDABLE` khi có yêu cầu tổng hợp.
- **Mã lỗi:** `BT_E_MESSAGE_NOT_SENDABLE`
- **Lý do nghiệp vụ:** B là người quyết định nội dung A nghe; bộ giải mã hoặc mô hình ngôn ngữ không được nói thay B khi chưa xác nhận.

### BR-BT-07: Gửi câu nhanh như thông điệp có xác nhận
- **Mô tả:** BridgeTalk MAY cung cấp một danh sách đóng các câu nhanh có thể cấu hình cho nhu cầu gọi phổ biến.
- **Điều kiện:** **When** B chọn một câu nhanh có sẵn và chọn Gửi, hệ thống SHALL tạo `MessageDraft` chứa đúng câu đó và áp dụng logic xác nhận gửi.
- **Hành vi:** **If** câu được chọn không thuộc danh sách đã cấu hình, **then** client MUST NOT tổng hợp câu đó và trả `BT_E_UNAVAILABLE_QUICK_PHRASE`.
- **Mã lỗi:** `BT_E_UNAVAILABLE_QUICK_PHRASE`
- **Lý do nghiệp vụ:** Câu nhanh giảm thời gian cho nhu cầu lặp lại, nhưng không tuyên bố Morse luôn nhanh hơn gõ phím.

### BR-BT-08: Thoái lui an toàn khi dịch vụ giọng nói lỗi
- **Mô tả:** Lỗi provider SHALL không được kết thúc `CallSession` đang kết nối.
- **Điều kiện:** **If** speech-to-text, dịch hoặc text-to-speech trả lỗi hay hết thời gian chờ, **then** client SHALL giữ các chức năng không lỗi tiếp tục hoạt động.
- **Hành vi:** Client MUST nêu rõ chức năng lỗi, MUST cho phép thử lại và MUST NOT gắn nhãn đầu ra chưa có là đã hoàn thành. `MessageDraft` dạng văn bản vẫn dùng được khi text-to-speech lỗi; `CallSession` vẫn dùng được khi caption lỗi.
- **Mã lỗi:** `BT_E_PROVIDER_UNAVAILABLE`
- **Lý do nghiệp vụ:** Tính năng trợ năng phải lỗi một cách rõ ràng và cục bộ, không được kết thúc phiên gọi hoặc bịa đầu ra.

### BR-BT-09: Giảm thiểu lưu giữ nội dung cuộc gọi
- **Mô tả:** BridgeTalk SHALL xử lý âm thanh, caption, bản dịch và `MessageDraft` như dữ liệu của phiên; chỉ lưu transcript khi mọi `CallParticipant` đồng ý rõ ràng.
- **Điều kiện:** **When** `CallSession` kết thúc, client SHALL yêu cầu từng người tham gia quyết định riêng về `SaveTranscript`. Client chỉ giữ dữ liệu nghiệp vụ cần cho lịch sử, danh bạ và thống kê đã được phép.
- **Hành vi:** **If** thiếu một đồng ý hoặc có một người từ chối, **then** hệ thống MUST NOT lưu âm thanh thô, caption, bản dịch, thời lượng nhấn hay nội dung `VoiceOutput` sau phiên đang hoạt động. **If** mọi người tham gia đồng ý, **then** client MUST hiển thị loại nội dung, nơi lưu và thời hạn lưu trước khi xác nhận lưu.
- **Mã lỗi:** `BT_E_SAVE_CONSENT_REQUIRED`
- **Lý do nghiệp vụ:** Nội dung cuộc gọi liên quan trực tiếp đến cả hai bên; mặc định giảm thiểu lưu trữ và cần đồng ý của mọi người tham gia.

### BR-BT-10: Không tự nhận là dịch vụ khẩn cấp hay phiên dịch ngôn ngữ ký hiệu
- **Mô tả:** BridgeTalk SHALL nêu ranh giới dịch vụ tại màn hình bắt đầu gọi và trợ giúp.
- **Điều kiện:** **When** một người bắt đầu hoặc tham gia `CallSession`, client SHALL hiển thị rằng BridgeTalk là công cụ hỗ trợ giao tiếp, không phải dịch vụ khẩn cấp.
- **Hành vi:** Client MUST NOT tuyên bố Morse, caption, dịch hay phân tích cử chỉ trong tương lai đủ chính xác cho điều phối khẩn cấp, chỉ dẫn y tế, phiên dịch pháp lý hoặc phiên dịch ngôn ngữ ký hiệu. **If** giai đoạn sau phát hiện một câu khẩn cấp, **then** nhóm sản phẩm MUST xác định riêng luồng chuyển tiếp đã được kiểm chứng trước khi phát hành.
- **Mã lỗi:** `BT_E_UNSUPPORTED_SAFETY_USE`
- **Lý do nghiệp vụ:** Ranh giới rõ ràng tránh tạo cảm giác an toàn sai trong tình huống hậu quả cao.

### BR-BT-11: Gemini chỉ được gợi ý, không được nói thay B
- **Mô tả:** Gemini MAY tạo tối đa năm từ hoặc câu gợi ý từ `MessageDraft` khi B chủ động yêu cầu hoặc khi bản nháp có nội dung đã giải mã.
- **Điều kiện:** **When** hệ thống tạo gợi ý cho một `MessageDraft` hợp lệ, hệ thống SHALL gửi nội dung bản nháp và ngôn ngữ đầu ra đã chọn tới Gemini qua adapter phía server.
- **Hành vi:** Client MUST giữ nguyên `MessageDraft` gốc. Client MAY hiển thị mỗi gợi ý như một nút chọn nhanh. **If** B chưa chọn một gợi ý và chọn Gửi, **then** hệ thống MUST NOT thay thế bản nháp hoặc tạo `VoiceOutput` từ gợi ý đó. **If** B chọn một gợi ý, **then** client SHALL điền gợi ý vào `MessageDraft` để B kiểm tra trước khi gửi. **If** Gemini không trả kết quả, **then** client MUST giữ bản nháp gốc và trả `BT_E_AI_SUGGESTION_UNAVAILABLE`.
- **Mã lỗi:** `BT_E_AI_SUGGESTION_UNAVAILABLE`
- **Lý do nghiệp vụ:** Gemini tạo giá trị cho người dùng khó nhập câu đầy đủ, nhưng quyền phát ngôn luôn thuộc về B.

### BR-BT-12: Chỉ ghi metric ẩn danh phục vụ đánh giá sản phẩm
- **Mô tả:** Firebase Analytics SHALL chỉ ghi metric sự kiện không chứa âm thanh, caption, Morse, `MessageDraft` hoặc định danh trực tiếp của người dùng.
- **Điều kiện:** **When** một sự kiện MVP hoàn tất, hệ thống SHALL gửi một `InteractionMetric` có tên sự kiện thuộc danh sách đóng.
- **Hành vi:** **If** người dùng chưa đồng ý analytics hoặc sự kiện chứa nội dung giao tiếp, **then** client MUST NOT gửi metric và trả `BT_E_ANALYTICS_CONSENT_REQUIRED` khi có yêu cầu ghi nhận. Danh sách sự kiện MVP gồm `CallConnected`, `CaptionFinalized`, `MorseMessageSent`, `QuickPhraseSent` và `CallEnded`.
- **Mã lỗi:** `BT_E_ANALYTICS_CONSENT_REQUIRED`
- **Lý do nghiệp vụ:** Đội cần bằng chứng sử dụng và tỷ lệ hoàn thành để đánh giá sản phẩm, nhưng không đánh đổi riêng tư của người dùng.

### BR-BT-13: Chỉ người dùng đã đăng nhập mới dùng lớp social
- **Mô tả:** BridgeTalk SHALL yêu cầu `UserAccount` hợp lệ trước khi người dùng xem danh bạ, gửi lời mời kết bạn hoặc tạo `CallSession`.
- **Điều kiện:** **When** người dùng truy cập một chức năng social, hệ thống SHALL xác thực phiên đăng nhập qua Firebase Authentication.
- **Hành vi:** **If** không có phiên đăng nhập hợp lệ, **then** client MUST chặn thao tác, chuyển người dùng tới màn hình đăng nhập và trả `BT_E_AUTH_REQUIRED`.
- **Mã lỗi:** `BT_E_AUTH_REQUIRED`
- **Lý do nghiệp vụ:** Danh bạ và quyền gọi 1-1 cần một danh tính ổn định để tránh gửi nhầm người.

### BR-BT-14: Chỉ bạn bè đã chấp nhận mới gọi 1-1 được
- **Mô tả:** BridgeTalk SHALL cho phép tạo `CallSession` chỉ khi quan hệ `ContactRequest` giữa A và B có State `Accepted`.
- **Điều kiện:** **When** A chọn gọi một người trong danh bạ, hệ thống SHALL kiểm tra quan hệ bạn bè của cặp người dùng đó.
- **Hành vi:** **If** quan hệ không tồn tại hoặc có State khác `Accepted`, **then** hệ thống MUST NOT tạo `CallSession` và trả `BT_E_CONTACT_NOT_ACCEPTED`.
- **Mã lỗi:** `BT_E_CONTACT_NOT_ACCEPTED`
- **Lý do nghiệp vụ:** Cần một ranh giới xã hội rõ ràng để hạn chế cuộc gọi không mong muốn.

### BR-BT-15: Server xác thực người thực hiện từ phiên đăng nhập
- **Mô tả:** Mọi API thay đổi dữ liệu SHALL xác định người thực hiện từ Firebase Authentication token đã xác minh ở server, không tin các ID người dùng do client tự khai trong body request.
- **Điều kiện:** **When** client tạo cuộc gọi, chấp nhận cuộc gọi, nhập Morse, gửi bản nháp hoặc ghi nhận đồng ý lưu, server SHALL suy ra `UserAccount` và `CallParticipant` từ token cùng quyền trên `CallSession`.
- **Hành vi:** **If** token không thuộc người tham gia hợp lệ hoặc client cố thực hiện thay người khác, **then** server MUST từ chối thao tác, không thay đổi dữ liệu và trả `BT_E_FORBIDDEN`.
- **Mã lỗi:** `BT_E_FORBIDDEN`
- **Lý do nghiệp vụ:** ID trong request có thể bị sửa; quyền gọi, nội dung gửi và quyết định lưu phải gắn với đúng tài khoản đã đăng nhập.

### BR-BT-16: Kết thúc phiên có nguyên nhân xác định
- **Mô tả:** `CallSession` SHALL ghi nhận nguyên nhân kết thúc để phân biệt hủy khi đang chờ, từ chối, hết thời gian chờ và kết thúc sau khi đã kết nối.
- **Điều kiện:** **When** một phiên `Pending` hoặc `Connected` bị đóng, server SHALL chuyển phiên sang `Ended` và lưu `endReason`.
- **Hành vi:** **If** B từ chối, A hủy hoặc lời mời hết hạn, **then** `endReason` MUST lần lượt là `Declined`, `Cancelled` hoặc `TimedOut`; **if** một người kết thúc phiên đã kết nối, `endReason` MUST là `ParticipantEnded`. Tính năng trợ năng MUST dừng ngay khi phiên không còn `Connected`.
- **Mã lỗi:** `BT_E_SESSION_NOT_CONNECTED`
- **Lý do nghiệp vụ:** Đội vận hành và client cần xử lý đúng trạng thái gọi thay vì coi mọi kết thúc như một trường hợp.

### BR-BT-17: Âm thanh cuộc gọi đi qua kênh thời gian thực đã xác thực
- **Mô tả:** BridgeTalk SHALL truyền âm thanh cuộc gọi qua kênh thời gian thực được xác thực; Firebase chỉ phục vụ xác thực, dữ liệu phiên, thông báo và phân tích, không được coi là kênh media.
- **Điều kiện:** **When** `CallSession` chuyển `Connected`, client SHALL thiết lập media session cho đúng hai `CallParticipant` bằng tầng RTC đã chọn và dùng server để ủy quyền/signaling.
- **Hành vi:** **If** không thiết lập hoặc duy trì được media session, **then** client MUST hiển thị lỗi media, không phát caption hay `VoiceOutput` như thể cuộc gọi vẫn đang hoạt động và cho phép kết thúc hoặc thử kết nối lại.
- **Mã lỗi:** `BT_E_MEDIA_UNAVAILABLE`
- **Lý do nghiệp vụ:** Bản phát hành cần một đường truyền âm thanh thực, có kiểm soát quyền truy cập và không phụ thuộc adapter giả lập.

## Danh mục mã lỗi

| Mã lỗi | Ý nghĩa | Quy tắc liên quan |
|---|---|---|
| `BT_E_SESSION_NOT_CONNECTED` | Thao tác trợ năng yêu cầu phiên gọi trong ứng dụng đã được chấp nhận. | Rule 01 |
| `BT_E_CAPTION_UNAVAILABLE` | Không tạo được caption cuối. | Rule 02 |
| `BT_E_TRANSLATION_UNAVAILABLE` | Không tạo được bản dịch; caption nguồn vẫn có sẵn. | Rule 03 |
| `BT_E_INVALID_TAP_DURATION` | Thời lượng nhấn nằm ngoài khoảng cấu hình của B. | Rule 04 |
| `BT_E_UNKNOWN_MORSE_SEQUENCE` | Chuỗi Morse hoàn chỉnh không có ánh xạ trong từ điển hỗ trợ. | Rule 05 |
| `BT_E_MESSAGE_NOT_SENDABLE` | Bản nháp chưa đủ điều kiện để tổng hợp giọng nói. | Rule 06 |
| `BT_E_UNAVAILABLE_QUICK_PHRASE` | Câu nhanh được chọn không có trong danh sách cấu hình. | Rule 07 |
| `BT_E_PROVIDER_UNAVAILABLE` | Provider speech, dịch hoặc voice không hoàn tất yêu cầu. | Rule 08 |
| `BT_E_SAVE_CONSENT_REQUIRED` | Không được lưu nội dung phiên khi chưa có đồng ý rõ ràng. | Rule 09 |
| `BT_E_UNSUPPORTED_SAFETY_USE` | Yêu cầu nằm ngoài phạm vi an toàn của sản phẩm. | Rule 10 |
| `BT_E_AI_SUGGESTION_UNAVAILABLE` | Gemini không trả được câu gợi ý; bản nháp gốc vẫn có sẵn. | Rule 11 |
| `BT_E_ANALYTICS_CONSENT_REQUIRED` | Chưa có đồng ý hợp lệ để ghi metric ẩn danh. | Rule 12 |
| `BT_E_AUTH_REQUIRED` | Người dùng chưa có phiên đăng nhập hợp lệ. | Rule 13 |
| `BT_E_CONTACT_NOT_ACCEPTED` | Hai người chưa là bạn bè đã chấp nhận. | Rule 14 |
| `BT_E_FORBIDDEN` | Token không có quyền thao tác trên tài nguyên hoặc phiên gọi. | Rule 15 |
| `BT_E_MEDIA_UNAVAILABLE` | Không thiết lập hoặc duy trì được kênh âm thanh thời gian thực. | Rule 17 |

## Quyết định cần chốt trước khi phát hành public

| ID | Cần quyết định | Giả định hiện tại cho MVP phát hành | Phụ trách |
|---|---|---|---|
| OD-BT-01 | Thời lượng tối thiểu, tối đa, ngắt ký tự và ngắt từ cho Tap-to-Speech. | Bắt đầu thử với ngưỡng 250 ms và cho phép chỉnh trong cài đặt. | Product và người test trợ năng |
| OD-BT-02 | Danh sách đóng các câu nhanh và cách viết Việt/Anh. | Chỉ có 6 đến 10 câu sau thử nghiệm ngắn với người dùng. | Product và design |
| OD-BT-03 | Provider speech-to-text, dịch và text-to-speech. | Gemini cho gợi ý có xác nhận; Google speech/translation/TTS cho giọng nói; Firebase Auth cho danh tính, Firestore cho social graph/trạng thái phiên/signaling và FCM cho thông báo; phát hành Android app trên Google Play. Production build dùng adapter thật, mock chỉ để phát triển nội bộ và không commit credential. | Engineering |
| OD-BT-05 | Tầng media thời gian thực, signaling, STUN/TURN và quy tắc reconnect. | Dùng WebRTC trên Android; server xác thực, cấp quyền và signaling. Chốt nhà cung cấp TURN, giới hạn timeout/reconnect và kiểm thử trên mạng di động trước khi build call flow. | Engineering |
| OD-BT-06 | Thông báo và căn cứ xử lý dữ liệu thời gian thực qua speech, translation, TTS và Gemini. | Chưa được giả định là đã có đồng ý pháp lý. Product/Legal phải chốt chính sách, thời điểm hiển thị và hành vi khi người dùng không đồng ý trước khi phát hành public. | Product, Legal |
| OD-BT-04 | Bằng chứng từ người dùng mục tiêu. | Báo cáo hackathon phải tách phản hồi đã kiểm chứng và giả định của nhóm. | Research |

<!-- Append CR sections below when logic changes -->
