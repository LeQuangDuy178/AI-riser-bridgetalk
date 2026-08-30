---
module: bridge-talk
type: research-note
status: verified
owner: Duy, Thui
last_updated: 2026-08-25
scope: production-stack
---

# Kiểm chứng Google stack cho BridgeTalk

## Kết luận kiến trúc

| Nhu cầu | Dịch vụ/vai trò phù hợp | Không dùng cho |
|---|---|---|
| Đăng nhập và định danh | Firebase Authentication | Media cuộc gọi |
| Hồ sơ, danh bạ, trạng thái phiên, offer/answer/ICE candidate | Cloud Firestore | Luồng audio/video liên tục |
| Báo cuộc gọi đến, wake-up, thông báo nền | Firebase Cloud Messaging (FCM) | Kênh audio/video hoặc caption streaming |
| Audio/video cuộc gọi | WebRTC (`RTCPeerConnection`) + STUN/TURN | Thay thế backend signaling |
| Caption trực tiếp | Cloud Speech-to-Text streaming | Lưu transcript mặc định |
| Dịch caption | Cloud Translation | Dịch lại audio thô |
| Đọc câu B đã xác nhận | Cloud Text-to-Speech | Tự phát nội dung chưa được B xác nhận |
| Gợi ý câu/từ | Gemini qua backend | Tự gửi hoặc tự đọc thay B |

Firebase không phải kênh media. Đây là suy luận kiến trúc từ phạm vi dịch vụ được Google mô tả: FCM là notification/data message và payload tối đa 4,096 byte; Cloud Firestore là cơ sở dữ liệu document có realtime listeners. Ngược lại, WebRTC là phần giao tiếp peer-to-peer cho audio/video/data. [FCM message types](https://firebase.google.com/docs/cloud-messaging/customize-messages/set-message-type), [Firestore data model](https://firebase.google.com/docs/firestore/data-model), [WebRTC peer connections](https://webrtc.org/getting-started/peer-connections?hl=en)

## Chi tiết cần đưa vào đặc tả kỹ thuật

1. Dùng WebRTC cho media call. Signaling không thuộc chuẩn WebRTC; ứng dụng phải có kênh riêng để trao đổi SDP offer/answer và ICE candidate. Firestore có thể là kênh signaling cho MVP, nhưng không mang audio/video. Cấu hình ICE cần STUN/TURN để xử lý NAT; WebRTC nêu TURN là lựa chọn phổ biến cho ứng dụng thương mại. [WebRTC signaling và ICE](https://webrtc.org/getting-started/peer-connections?hl=en), [FirebaseRTC codelab](https://webrtc.org/getting-started/firebase-rtc-codelab?hl=en)

2. Firebase Auth chỉ xác thực danh tính. Firestore lưu `UserAccount`, danh bạ, `CallSession`, trạng thái signaling và consent theo policy. FCM chỉ đẩy `incoming_call`, `call_cancelled` hoặc wake-up; không đẩy caption/audio theo từng khung. [Firebase Authentication](https://firebase.google.com/docs/auth), [Firestore realtime listeners](https://firebase.google.com/docs/firestore/query-data/listen), [FCM architecture](https://firebase.google.com/docs/cloud-messaging/fcm-architecture)

3. Caption trực tiếp dùng Cloud Speech-to-Text streaming: gửi audio theo stream hai chiều và nhận kết quả `interim`/`final`. Bản đặc tả cần ghi rõ xử lý reconnect và giới hạn stream theo version API đã chọn; tài liệu v1 nêu streaming qua gRPC và stream tối đa 5 phút, có hướng dẫn endless streaming khi cần dài hơn. [Streaming recognition requests](https://cloud.google.com/speech-to-text/docs/v1/speech-to-text-requests), [Quotas and limits](https://cloud.google.com/speech-to-text/docs/quotas)

4. Cloud Translation nhận văn bản caption đã chốt và trả văn bản đích; không cần đưa audio thô vào dịch. Dùng mã ngôn ngữ được API hỗ trợ, xác định rõ cặp `vi`/`en` trước build. [Cloud Translation overview](https://cloud.google.com/translate/docs/api-overview), [Translate text](https://cloud.google.com/translate/docs/translate-text)

5. Cloud Text-to-Speech nhận text hoặc SSML và trả audio; chỉ gọi sau thao tác Gửi/Xác nhận của B. Có thể chọn voice, tốc độ nói, cao độ và định dạng output. [Create voice audio files](https://cloud.google.com/text-to-speech/docs/create-audio)

6. Gemini chỉ sinh tối đa 5 gợi ý và B phải chọn trước khi nội dung vào `MessageDraft`/TTS. Với backend production, dùng Google GenAI SDK hiện hành hoặc Gemini trên Vertex AI; không dùng SDK legacy. Không để Gemini API key trong ứng dụng Android: Google yêu cầu không expose key phía client trong production và khuyến nghị backend proxy/Secret Manager. [Google GenAI SDK](https://ai.google.dev/gemini-api/docs/libraries), [Bảo vệ Gemini API key](https://ai.google.dev/gemini-api/docs/api-key), [Vertex AI Gemini quickstart](https://cloud.google.com/vertex-ai/generative-ai/docs/start/quickstart)

## Điểm cần chốt trước khi code production

- Chọn TURN provider và cơ chế cấp credential; không giả định STUN là đủ cho mọi mạng.
- Chọn phương án chuyển audio vào Cloud Speech-to-Text, giới hạn/reconnect stream và đo độ trễ caption.
- Chốt backend giữ secrets và gọi Gemini/Cloud APIs; ứng dụng Android không giữ service credential hoặc Gemini API key.
- Ghi rõ thời hạn lưu/xóa transcript và audio. Nội dung giao tiếp chỉ được lưu khi có consent theo BR-BT-09.

