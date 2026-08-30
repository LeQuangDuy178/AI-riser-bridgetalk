# Nghiên cứu persona và nguyên tắc accessibility cho BridgeTalk

**Ngày:** 27/08/2026  
**Phạm vi:** hỗ trợ thiết kế prototype và chuẩn bị video demo; không thay thế kiểm thử với người dùng thật.

## Kết luận ngắn

BridgeTalk không nên gán cứng “người mù = không nói được”. Người mù hoặc thị lực thấp có thể nói bằng giọng nói; nhu cầu chính của họ trong cuộc gọi có thể là nghe âm thanh trực tiếp, dùng Speech-to-Text khi cần gửi nội dung, và điều hướng bằng TalkBack. Morse là một kênh bổ sung cho người khó nói hoặc vừa khó nói vừa có nhu cầu phản hồi không phụ thuộc vào giọng.

Thiết kế nên coi khả năng giao tiếp là các kênh có thể bật độc lập:

| Kênh | Giá trị | Persona hưởng lợi |
|---|---|---|
| Âm thanh cuộc gọi trực tiếp | Nghe người gọi nói theo thời gian thực | Người mù/thị lực thấp, người đọc âm thanh tốt |
| Caption gốc + bản dịch | Đọc nội dung người gọi nói | Người điếc hoặc khó nghe |
| Speech-to-Text | Nói để tạo bản nháp văn bản | Người mù/thị lực thấp, người khó thao tác tay |
| Morse | Nhập chấm/gạch bằng nút lớn | Người khó nói; người vừa mù vừa khó nói |
| Phản hồi âm thanh khi chạm | Biết thao tác đã được nhận | Người mù/thị lực thấp và người không nhìn màn hình liên tục |
| MessageDraft + xác nhận Gửi | Kiểm soát nội dung trước khi phát | Tất cả persona, đặc biệt người dùng cần tránh gửi nhầm |

## Persona thiết kế

### P01 — Người khó nghe hoặc điếc

- **Mục tiêu:** hiểu người gọi mà không phụ thuộc vào âm thanh.
- **Rào cản:** âm thanh không đủ tin cậy; bản dịch có thể làm mất nghĩa nếu thay thế câu gốc.
- **Thiết kế:** caption gốc luôn hiện; bản dịch nằm ngay bên dưới; trạng thái interim/final rõ ràng; tín hiệu cuộc gọi đến có cả hình ảnh và rung; không dùng màu đơn độc để biểu thị trạng thái.
- **Tiêu chí demo:** người xem thấy được câu gốc và bản dịch cùng lúc, không cần nghe audio.

### P02 — Người khó nói hoặc không thể nói

- **Mục tiêu:** gửi một câu có thể nghe được mà không cần phát âm bằng giọng của mình.
- **Rào cản:** nhập liệu thông thường có thể chậm hoặc khó; gửi nhầm nội dung gây mất quyền kiểm soát.
- **Thiết kế:** nút Morse lớn; chạm nhanh tạo chấm, giữ ngắn tạo gạch; phản hồi âm thanh cho từng token; câu gợi ý chọn một chạm; MessageDraft luôn chỉnh sửa được; chỉ phát TTS sau nút Gửi.
- **Tiêu chí demo:** người dùng tạo được token, chọn/sửa câu, và thấy rõ rằng chưa phát âm trước khi xác nhận.

### P03 — Người mù hoặc thị lực thấp có thể nói

- **Mục tiêu:** nghe được cuộc gọi và tạo nội dung bằng giọng nói hoặc thao tác có phản hồi âm thanh.
- **Rào cản:** phụ thuộc vào thị giác để biết focus, trạng thái kết nối, nội dung draft và kết quả thao tác.
- **Thiết kế:** hỗ trợ TalkBack semantics; thứ tự đọc hợp lý; nút tối thiểu 48dp, nút Morse lớn hơn mức này; live region cho trạng thái ngắn; Speech-to-Text dạng nhấn để nói, không tự ghi liên tục; phản hồi âm thanh cho chạm và gửi.
- **Tiêu chí demo:** bật TalkBack vẫn tìm được nút gọi, nút nói, nút Morse, các câu gợi ý và nút Gửi.

### P04 — Người vừa khó nghe vừa khó nói

- **Mục tiêu:** nhận nội dung bằng caption và phản hồi bằng Morse hoặc văn bản.
- **Rào cản:** nếu chỉ thiết kế audio hoặc chỉ thiết kế caption thì một nửa vòng giao tiếp bị mất.
- **Thiết kế:** giữ caption, bản dịch, Morse, draft và nút Gửi trên cùng một call surface; không ẩn Morse sau một menu khó tìm; cho phép chuyển giữa Speech-to-Text và Morse.
- **Tiêu chí demo:** người dùng không cần nghe để nhận và không cần nói để trả lời.

## Nguyên tắc UI áp dụng vào prototype

1. **Đa kênh, không loại trừ:** mọi trạng thái quan trọng có chữ, màu/tương phản và âm thanh hoặc TalkBack phù hợp.
2. **Nút dễ bấm:** interactive target tối thiểu 48dp theo Android; nút Morse dùng kích thước lớn hơn và nằm ở vùng dễ chạm.
3. **Âm thanh phản hồi không phải nội dung giao tiếp:** beep khi nhận thao tác chỉ xác nhận thao tác; audio cuộc gọi là media của người gọi; TTS chỉ sau khi người dùng bấm Gửi.
4. **Không dùng thao tác giữ quá lâu:** prototype bắt đầu với ngưỡng 250ms cho chấm/gạch; cần kiểm thử lại với người dùng thật trước khi chốt production.
5. **Speech-to-Text có chủ đích:** dùng nhấn để nói và dừng để nhận kết quả; không ghi âm nền liên tục.
6. **TalkBack là đường đi chính thức:** các custom gesture như Morse cần nhãn, trạng thái và đường thay thế bằng nút/semantics.
7. **Không tự suy diễn năng lực:** cho phép người dùng chọn kênh giao tiếp, không tự suy ra người mù không thể nói hoặc người điếc không thể nói.

## Căn cứ chính thức

- Android Developers — [Accessibility in Jetpack Compose](https://developer.android.com/develop/ui/compose/accessibility): Compose hỗ trợ semantics, nội dung phóng to và kiểm thử accessibility.
- Android Developers — [API defaults và minimum touch target](https://developer.android.com/develop/ui/compose/accessibility/api-defaults): interactive target nên có kích thước tối thiểu 48dp.
- Android Developers — [Semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics): semantics giúp TalkBack và công cụ kiểm thử hiểu vai trò, trạng thái và hành động của composable.
- Android Developers — [Principles for improving app accessibility](https://developer.android.com/guide/topics/ui/accessibility/principles): custom gesture cần có accessibility action hoặc đường tương tác thay thế.
- Android Developers — [SpeechRecognizer API](https://developer.android.com/reference/android/speech/SpeechRecognizer.html): cần quyền microphone; API có thể gửi audio tới dịch vụ từ xa và không phù hợp cho nhận dạng liên tục.
- Android Developers — [TalkBack accessibility testing](https://developer.android.com/guide/topics/ui/accessibility/testing): TalkBack cho phép người dùng thị lực kém điều hướng thiết bị không cần nhìn màn hình.
- W3C — [WCAG 2.2, Success Criterion 1.2.4](https://www.w3.org/TR/WCAG22/#captions-live): nội dung audio trực tiếp cần có caption.

## Giới hạn và việc cần kiểm chứng

- Nghiên cứu tài liệu không thay thế usability test với người mù, người điếc, người khó nói và người dùng TalkBack.
- Ngưỡng Morse 250ms là điểm bắt đầu để prototype, không phải chuẩn production đã được phê duyệt.
- Speech-to-Text trên Android có thể phụ thuộc dịch vụ nhận dạng và mạng; production vẫn cần adapter backend theo `business-rules.md`.
- Video call có thể là media WebRTC ở giai đoạn sau; nhận diện ngôn ngữ ký hiệu bằng video vẫn nằm ngoài MVP hiện tại.
