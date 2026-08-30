# Nghiên cứu redesign Call UI cho BridgeTalk

**Ngày:** 27/08/2026  
**Mục tiêu:** làm màn hình cuộc gọi dễ hiểu hơn khi quay demo và giảm tải thao tác cho người khó nói, khó nghe, mù/thị lực thấp hoặc khó thao tác tay.

## Phân biệt evidence và giả thuyết

- **FACT:** Android khuyến nghị interactive target tối thiểu 48dp; Compose có semantics để accessibility service hiểu vai trò và hành động của UI.
- **FACT:** Android khuyến nghị không phụ thuộc vào gesture duy nhất; người dùng có thể dùng TalkBack, Voice Access hoặc Switch Access.
- **FACT:** Một sản phẩm AAC có pattern Text Pad làm trung tâm, word/sentence prediction, phrase library, history và nút Play khi người dùng muốn phát.
- **HYPOTHESIS:** Hai nút Morse trực tiếp `·` và `—` dễ học và dễ thao tác hơn một vùng nhấn-giữ phải nhớ ngưỡng thời gian.
- **HYPOTHESIS:** Một màn hình cuộc gọi có bốn vùng ổn định sẽ dễ quay demo và dễ quét bằng mắt/TalkBack hơn layout nhiều card có cùng trọng lượng.
- **OPEN ISSUE:** Chưa có usability test trực tiếp với người dùng BridgeTalk; không được coi các giả thuyết này là kết luận về mọi người khuyết tật.

## Pattern tham chiếu

### Android accessibility

1. Component tương tác cần vùng chạm tối thiểu 48dp; BridgeTalk dùng nút Morse lớn hơn mức này.
2. Button/semantics nên mô tả rõ vai trò và hành động cho TalkBack.
3. Các thao tác phức tạp không được chỉ có một đường gesture; cần có nút thay thế, nhãn và trạng thái.
4. Voice Access và Switch Access là các đường tương tác quan trọng với người khó thao tác tay hoặc không muốn chạm chính xác.

Nguồn: [Compose accessibility](https://developer.android.com/develop/ui/compose/accessibility), [minimum touch targets](https://developer.android.com/develop/ui/compose/accessibility/api-defaults), [Android accessibility foundations](https://developer.android.com/design/ui/mobile/guides/foundations/accessibility).

### Sản phẩm và pattern gần bài toán

- **Live Transcribe:** đặt lời nói và âm thanh thành chữ theo thời gian thực, cho phép người dùng giữ nội dung trên màn hình để đọc dễ hơn. Pattern áp dụng cho BridgeTalk: caption là vùng đọc chính, có trạng thái đang nghe rõ ràng và không biến caption thành tin nhắn gửi đi.
- **Project Relate / speech resources:** Google xem các công cụ speech là hỗ trợ cho người có giọng nói không chuẩn. Pattern áp dụng: Speech-to-Text là một đường nhập tùy chọn; không được coi một persona có khó nói là không thể nói.
- **Proloquo4Text:** Text Pad, dự đoán từ/câu, phrase library, History và nút Play tạo thành một chuỗi “soạn → kiểm tra → phát”. Pattern áp dụng: BridgeTalk có draft và gợi ý, nhưng vẫn giữ quyền chủ động phát cho người đang trả lời.

Đây là benchmark/pattern từ sản phẩm và tài liệu chính thức, chưa phải usability evidence của người dùng BridgeTalk. Cần test riêng với tối thiểu một người khó nghe, một người khó nói và một người mù/thị lực thấp trước khi chốt production UI.

Nguồn: [Live Transcribe](https://support.google.com/accessibility/android/answer/9158064?hl=en), [Google speech resources](https://support.google.com/accessibility/answer/15559617?hl=en), [Proloquo4Text Quick Start Guide](https://download.assistiveware.com/proloquo4text/files/Proloquo4Text_Quickstart_Guide_English.pdf).

### AAC và giao tiếp hỗ trợ

Quickstart guide của Proloquo4Text mô tả Text Pad là trung tâm giao tiếp, kết hợp dự đoán từ/câu, phrase library, Quick Talk, History và thao tác Play khi người dùng hoàn tất. Đây là **pattern sản phẩm tham khảo**, không phải nghiên cứu người dùng BridgeTalk.

Nguồn: [Proloquo4Text Quick Start Guide](https://download.assistiveware.com/proloquo4text/files/Proloquo4Text_Quickstart_Guide_English.pdf), đặc biệt phần Text Pad, Quick Blocks và Prediction.

### Người mù hoặc thị lực thấp

TalkBack cung cấp điều khiển không cần nhìn màn hình; Google cũng mô tả các trải nghiệm accessibility kết hợp âm thanh, tương phản và rung. Vì vậy BridgeTalk phải nói rõ trạng thái bằng semantics/âm thanh, nhưng không được suy ra người mù không thể nói.

Nguồn: [TalkBack testing](https://developer.android.com/guide/topics/ui/accessibility/testing), [Google accessibility resources](https://support.google.com/accessibility/answer/15566541?hl=en).

## Quyết định thiết kế cho bản quay demo

### Hierarchy màn hình

1. **Header cuộc gọi:** tên người gọi, trạng thái kết nối, loại media.
2. **Caption:** câu gốc lớn; bản dịch ngay bên dưới; trạng thái final/interim ngắn.
3. **Trả lời:** hai nút Morse trực tiếp `·` và `—`, không yêu cầu ước lượng thời gian giữ.
4. **MessageDraft:** nội dung tự cập nhật từ Morse hoặc Speech-to-Text; người dùng vẫn có thể sửa.
5. **Gợi ý:** tối đa năm câu ngắn, cùng một kiểu chip, không tạo cảm giác là form kỹ thuật.
6. **Phát ngay:** một nút xanh lớn, có chủ đích; không tự phát khi draft thay đổi.

### Vì sao đổi từ gesture sang hai nút

Vùng nhấn-giữ giúp tiết kiệm diện tích nhưng tạo hai gánh nặng: người dùng phải học ngưỡng thời gian và phải giữ tay ổn định. Hai nút trực tiếp tăng thêm diện tích nhưng làm hành động rõ ràng, dễ kiểm thử với TalkBack/Switch Access và phù hợp với video demo. Âm báo sau mỗi lần chạm giúp xác nhận token đã được nhận; âm báo này không thay thế audio cuộc gọi hoặc TTS.

### Tiêu chí kiểm chứng

| Giả thuyết | Cách test nhanh | Tín hiệu đạt |
|---|---|---|
| Hai nút Morse dễ học hơn gesture | Cho người mới nhập một từ ngắn bằng hai cách | Không cần giải thích lại sau lượt đầu |
| Hierarchy mới dễ quét | Yêu cầu chỉ ra caption, draft và phát ngay | Chỉ ra đúng vùng trong vài giây |
| Âm báo hữu ích | Nhập khi không nhìn màn hình | Biết mỗi token đã được nhận |
| Draft tự cập nhật giảm thao tác | Nhập bằng Morse/STT rồi sửa một từ | Không phải mở màn hình phụ |
| Nút Phát ngay đủ rõ | Sau khi có draft, hỏi hành động tiếp theo | Người dùng chọn đúng mà không đọc hướng dẫn dài |

## Ranh giới vẫn giữ

- Tự cập nhật `MessageDraft` không đồng nghĩa tự gửi.
- TTS chỉ được tạo sau hành động `Phát ngay`/Gửi của B theo BR-BT-06.
- Âm thanh cuộc gọi production vẫn phải qua WebRTC; âm báo Morse là local feedback.
- Speech-to-Text prototype dùng Android `SpeechRecognizer`; production vẫn cần adapter backend theo đặc tả.
- Video call và nhận diện ngôn ngữ ký hiệu không được suy diễn là đã có chỉ vì UI được redesign.
