---
module: bridge-talk
file: dataflow
type: spec
aspect: dataflow
version: 0.1.0
status: draft
owner: Duy, Thui
last_updated: 2026-08-25
related:
  - business-rules.md
depends_on:
  - business-rules.md
bug_refs: []
---

# Luồng dữ liệu - BridgeTalk MVP phát hành hỗ trợ cuộc gọi

> Đặc tả dữ liệu và hợp đồng API cho bản Android chính thức của BridgeTalk. Các entity giao tiếp dưới đây chủ yếu là dữ liệu phiên; profile, danh bạ, yêu cầu kết bạn, lịch sử phiên và metric đã được phép có thể được lưu theo chính sách của sản phẩm. Quy tắc lưu/xóa tuân theo BR-BT-09.

## 1. Quan hệ dữ liệu

```text
UserAccount 1 ---- N ContactRequest (sender)
UserAccount 1 ---- N ContactRequest (receiver)
UserAccount 1 ---- N CallParticipant
CallSession 1 ---- N CallParticipant
CallSession 1 ---- N Caption 1 ---- N CaptionTranslation
CallSession 1 ---- N TapSequence 1 ---- 1 MessageDraft 1 ---- 0..5 MessageSuggestion
MessageDraft 1 ---- N VoiceOutput
CallSession 1 ---- N ConsentRecord
CallSession 1 ---- N InteractionMetric
```

## 2. Bảng dữ liệu

### Entity: `UserAccount`

| Field | Type | Null? | Mô tả | Ghi chú |
|---|---|---:|---|---|
| `id` | uuid | NO | Khóa chính người dùng trong BridgeTalk. | PK |
| `firebaseUid` | string | NO | Mã định danh từ Firebase Authentication. | Unique |
| `bridgeId` | string | NO | Mã ngắn để thêm bạn. | Unique, không phải số điện thoại |
| `displayName` | string | NO | Tên hiển thị trong danh bạ. | |
| `createdAt` | datetime | NO | Thời điểm tạo tài khoản. | |

### Entity: `ContactRequest`

| Field | Type | Null? | Mô tả | Ghi chú |
|---|---|---:|---|---|
| `id` | uuid | NO | Khóa chính lời mời kết bạn. | PK |
| `senderUserId` | uuid | NO | Người gửi lời mời. | FK tới `UserAccount` |
| `receiverUserId` | uuid | NO | Người nhận lời mời. | FK tới `UserAccount` |
| `state` | enum | NO | `Pending`, `Accepted`, `Declined`. | BR-BT-14 |
| `createdAt` | datetime | NO | Thời điểm gửi lời mời. | |
| `respondedAt` | datetime | YES | Thời điểm nhận lời mời phản hồi. | |

### Entity: `CallSession`

| Field | Type | Null? | Mô tả | Ghi chú |
|---|---|---:|---|---|
| `id` | uuid | NO | Khóa chính của phiên gọi. | PK |
| `state` | enum | NO | `Pending`, `Connected`, `Ended`. | BR-BT-01 |
| `sourceLanguage` | string | NO | Mã ngôn ngữ nguồn, ví dụ `vi-VN` hoặc `en-US`. | BR-BT-02 |
| `targetLanguage` | string | YES | Mã ngôn ngữ đích khi B bật dịch. | BR-BT-03 |
| `createdAt` | datetime | NO | Thời điểm tạo phiên. | |
| `connectedAt` | datetime | YES | Thời điểm cả hai đã chấp nhận. | |
| `endedAt` | datetime | YES | Thời điểm phiên kết thúc. | BR-BT-09 |
| `endReason` | enum | YES | `Declined`, `Cancelled`, `TimedOut`, `ParticipantEnded`. | BR-BT-16 |

### Entity: `CallParticipant`

| Field | Type | Null? | Mô tả | Ghi chú |
|---|---|---:|---|---|
| `id` | uuid | NO | Khóa chính người tham gia trong phiên. | PK |
| `callSessionId` | uuid | NO | Khóa ngoại tới `CallSession`. | FK |
| `userAccountId` | uuid | NO | Tài khoản tham gia phiên. | FK tới `UserAccount` |
| `role` | enum | NO | `Caller_A` hoặc `Receiver_B`. | MVP chỉ có hai vai trò |
| `acceptedAt` | datetime | YES | Thời điểm người này chấp nhận cuộc gọi; A được ghi nhận khi tạo phiên, B khi chấp nhận. | BR-BT-01 |
| `outputLanguage` | string | YES | Ngôn ngữ đọc của B. | BR-BT-06 |
| `tapThresholdMs` | integer | YES | Ngưỡng phân biệt chấm/gạch của B. | Quyết định OD-BT-01 |

### Entity: `Caption`

| Field | Type | Null? | Mô tả | Ghi chú |
|---|---|---:|---|---|
| `id` | uuid | NO | Khóa chính caption. | PK |
| `callSessionId` | uuid | NO | Phiên tạo caption. | FK |
| `sequenceNo` | integer | NO | Thứ tự caption trong phiên. | Tăng dần trong một phiên |
| `sourceText` | string | YES | Văn bản speech-to-text trả về. | BR-BT-02 |
| `status` | enum | NO | `Interim`, `Final`, `Failed`. | BR-BT-02 |
| `createdAt` | datetime | NO | Thời điểm nhận kết quả. | |

### Entity: `CaptionTranslation`

| Field | Type | Null? | Mô tả | Ghi chú |
|---|---|---:|---|---|
| `id` | uuid | NO | Khóa chính bản dịch. | PK |
| `captionId` | uuid | NO | Caption nguồn. | FK |
| `targetLanguage` | string | NO | Mã ngôn ngữ đích. | BR-BT-03 |
| `translatedText` | string | YES | Văn bản đã dịch. | Caption nguồn không bị thay thế |
| `status` | enum | NO | `Completed` hoặc `Failed`. | BR-BT-03 |

### Entity: `TapSequence`

| Field | Type | Null? | Mô tả | Ghi chú |
|---|---|---:|---|---|
| `id` | uuid | NO | Khóa chính chuỗi chạm. | PK |
| `callSessionId` | uuid | NO | Phiên chứa chuỗi chạm. | FK |
| `receiverId` | uuid | NO | `CallParticipant` có vai trò `Receiver_B`. | FK |
| `rawMorse` | string | YES | Chuỗi chấm-gạch chưa hoặc đã giải mã. | BR-BT-04, BR-BT-05 |
| `status` | enum | NO | `Collecting`, `Decoded`, `Invalid`. | |
| `createdAt` | datetime | NO | Thời điểm bắt đầu nhập. | |

### Entity: `MessageDraft`

| Field | Type | Null? | Mô tả | Ghi chú |
|---|---|---:|---|---|
| `id` | uuid | NO | Khóa chính bản nháp. | PK |
| `tapSequenceId` | uuid | YES | Chuỗi Morse tạo bản nháp. | FK; câu nhanh có thể không có |
| `callSessionId` | uuid | NO | Phiên chứa bản nháp. | FK |
| `text` | string | NO | Nội dung B thấy trước khi gửi. | BR-BT-06 |
| `sourceType` | enum | NO | `Morse` hoặc `QuickPhrase`. | BR-BT-07 |
| `status` | enum | NO | `Composing`, `Ready`, `Sent`, `Invalid`. | |
| `sentAt` | datetime | YES | Thời điểm B chọn Gửi. | BR-BT-06 |

### Entity: `VoiceOutput`

| Field | Type | Null? | Mô tả | Ghi chú |
|---|---|---:|---|---|
| `id` | uuid | NO | Khóa chính lượt tổng hợp giọng nói. | PK |
| `messageDraftId` | uuid | NO | Bản nháp đã gửi. | FK |
| `language` | string | NO | Ngôn ngữ tổng hợp giọng nói. | BR-BT-06 |
| `status` | enum | NO | `Queued`, `Playing`, `Completed`, `Failed`. | BR-BT-08 |
| `createdAt` | datetime | NO | Thời điểm tạo yêu cầu TTS. | |

### Entity: `MessageSuggestion`

| Field | Type | Null? | Mô tả | Ghi chú |
|---|---|---:|---|---|
| `id` | uuid | NO | Khóa chính câu gợi ý. | PK |
| `messageDraftId` | uuid | NO | Bản nháp gốc được B yêu cầu gợi ý. | FK |
| `text` | string | NO | Một câu Gemini đề xuất. | BR-BT-11 |
| `kind` | enum | NO | `Word` hoặc `Phrase`. | Hiển thị bằng nút chọn nhanh |
| `status` | enum | NO | `Available`, `Selected`, `Dismissed`. | Không tự động chọn |
| `createdAt` | datetime | NO | Thời điểm nhận gợi ý. | |

### Entity: `ConsentRecord`

| Field | Type | Null? | Mô tả | Ghi chú |
|---|---|---:|---|---|
| `id` | uuid | NO | Khóa chính bản ghi đồng ý. | PK |
| `callSessionId` | uuid | NO | Phiên mà đồng ý áp dụng. | FK |
| `participantId` | uuid | NO | Người đã xác nhận. | FK |
| `purpose` | enum | NO | `SaveTranscript`. | Danh sách đóng cho MVP |
| `decision` | enum | NO | `Granted` hoặc `Declined`. | BR-BT-09 |
| `decidedAt` | datetime | NO | Thời điểm người tham gia quyết định. | BR-BT-09 |

### Entity: `InteractionMetric`

| Field | Type | Null? | Mô tả | Ghi chú |
|---|---|---:|---|---|
| `id` | uuid | NO | Khóa chính metric. | PK |
| `callSessionId` | uuid | NO | Phiên phát sinh metric. | FK |
| `eventName` | enum | NO | Một event trong danh sách đóng của BR-BT-12. | Không chứa nội dung giao tiếp |
| `occurredAt` | datetime | NO | Thời điểm xảy ra sự kiện. | |

## 3. API endpoints

### `POST /api/profile`
- **Mô tả:** Tạo hoặc cập nhật `UserAccount` sau khi Firebase Authentication xác thực người dùng.
- **Request:**
```json
{ "bridgeId": "minh-1234", "displayName": "Minh" }
```
- **Response (200):**
```json
{
  "data": { "id": "uuid", "bridgeId": "minh-1234" },
  "error": null,
  "meta": {}
}
```
- **Error cases:** `BT_E_AUTH_REQUIRED`.
- **BR liên quan:** BR-BT-13

### `POST /api/contact-requests`
- **Mô tả:** Gửi lời mời kết bạn bằng `bridgeId` của người nhận.
- **Request:**
```json
{ "receiverBridgeId": "minh-1234" }
```
- **Response (201):**
```json
{
  "data": { "contactRequestId": "uuid", "state": "Pending" },
  "error": null,
  "meta": {}
}
```
- **Error cases:** `BT_E_AUTH_REQUIRED`.
- **BR liên quan:** BR-BT-13

### `POST /api/contact-requests/{id}/accept`
- **Mô tả:** Chuyển `ContactRequest` của người nhận thành `Accepted`.
- **Request:**
```json
{}
```
- **Response (200):**
```json
{
  "data": { "contactRequestId": "uuid", "state": "Accepted" },
  "error": null,
  "meta": {}
}
```
- **Error cases:** `BT_E_AUTH_REQUIRED`.
- **BR liên quan:** BR-BT-13, BR-BT-14

### `POST /api/call-sessions`
- **Mô tả:** Tạo `CallSession` ở trạng thái `Pending`.
- **Request:**
```json
{
  "receiverId": "uuid",
  "sourceLanguage": "vi-VN",
  "targetLanguage": "en-US"
}
```
- **Response (201):**
```json
{
  "data": { "id": "uuid", "state": "Pending" },
  "error": null,
  "meta": {}
}
```
- **Error cases:** `BT_E_AUTH_REQUIRED`, `BT_E_CONTACT_NOT_ACCEPTED`, `BT_E_SESSION_NOT_CONNECTED` khi thao tác sau đó chưa có hai bên chấp nhận.
- **Ghi chú quyền:** Server lấy Caller_A từ Firebase Authentication token; không nhận `callerId` từ client.
- **BR liên quan:** BR-BT-01, BR-BT-13, BR-BT-14, BR-BT-15

### `POST /api/call-sessions/{id}/accept`
- **Mô tả:** Ghi nhận Receiver_B chấp nhận lời mời; Caller_A đã được ghi nhận chấp nhận khi tạo phiên nên phiên chuyển `Connected` sau thao tác này.
- **Request:**
```json
{}
```
- **Response (200):**
```json
{
  "data": { "id": "uuid", "state": "Connected" },
  "error": null,
  "meta": {}
}
```
- **Error cases:** `BT_E_SESSION_NOT_CONNECTED`, `BT_E_FORBIDDEN`.
- **Ghi chú quyền:** Server suy ra Receiver_B từ Firebase Authentication token.
- **BR liên quan:** BR-BT-01, BR-BT-15

### `POST /api/call-sessions/{id}/end`
- **Mô tả:** Kết thúc `CallSession` đang `Pending` hoặc `Connected`. Server xác định `endReason` theo state hiện tại và người thực hiện.
- **Request:**
```json
{}
```
- **Response (200):**
```json
{
  "data": { "id": "uuid", "state": "Ended", "endReason": "ParticipantEnded" },
  "error": null,
  "meta": {}
}
```
- **Error cases:** `BT_E_SESSION_NOT_CONNECTED`, `BT_E_FORBIDDEN`.
- **Ghi chú quyền:** Server suy ra người kết thúc từ Firebase Authentication token; client không được tự truyền `endReason`.
- **BR liên quan:** BR-BT-15, BR-BT-16

### `POST /api/call-sessions/{id}/captions`
- **Mô tả:** Endpoint nội bộ của speech adapter, nhận kết quả speech-to-text và tạo hoặc cập nhật `Caption`; không công khai cho client Android.
- **Request:**
```json
{
  "sequenceNo": 12,
  "sourceText": "Tôi sẽ gọi lại sau năm phút.",
  "status": "Final"
}
```
- **Response (200):**
```json
{
  "data": { "captionId": "uuid", "status": "Final" },
  "error": null,
  "meta": {}
}
```
- **Error cases:** `BT_E_SESSION_NOT_CONNECTED`, `BT_E_CAPTION_UNAVAILABLE`.
- **BR liên quan:** BR-BT-01, BR-BT-02

### `POST /api/captions/{id}/translations`
- **Mô tả:** Tạo `CaptionTranslation` từ `Caption` cuối và ngôn ngữ đích.
- **Request:**
```json
{ "targetLanguage": "en-US" }
```
- **Response (200):**
```json
{
  "data": { "translationId": "uuid", "status": "Completed" },
  "error": null,
  "meta": {}
}
```
- **Error cases:** `BT_E_TRANSLATION_UNAVAILABLE`.
- **BR liên quan:** BR-BT-03

### `POST /api/call-sessions/{id}/tap-sequences`
- **Mô tả:** Thêm chuỗi Morse đã giải mã hoặc trạng thái không hợp lệ vào phiên.
- **Request:**
```json
{
  "rawMorse": ".... ..",
  "status": "Decoded"
}
```
- **Response (200):**
```json
{
  "data": { "tapSequenceId": "uuid", "draftText": "HI" },
  "error": null,
  "meta": {}
}
```
- **Error cases:** `BT_E_SESSION_NOT_CONNECTED`, `BT_E_INVALID_TAP_DURATION`, `BT_E_UNKNOWN_MORSE_SEQUENCE`.
- **Ghi chú quyền:** Server suy ra Receiver_B từ Firebase Authentication token và kiểm tra vai trò trong phiên.
- **BR liên quan:** BR-BT-01, BR-BT-04, BR-BT-05, BR-BT-15

### `POST /api/message-drafts/{id}/send`
- **Mô tả:** Xác nhận gửi `MessageDraft` và tạo `VoiceOutput`.
- **Request:**
```json
{ "outputLanguage": "vi-VN" }
```
- **Response (200):**
```json
{
  "data": { "voiceOutputId": "uuid", "status": "Queued" },
  "error": null,
  "meta": {}
}
```
- **Error cases:** `BT_E_MESSAGE_NOT_SENDABLE`, `BT_E_PROVIDER_UNAVAILABLE`, `BT_E_FORBIDDEN`.
- **BR liên quan:** BR-BT-06, BR-BT-08, BR-BT-15

### `POST /api/message-drafts/{id}/suggestions`
- **Mô tả:** Yêu cầu Gemini tạo tối đa năm câu gợi ý từ `MessageDraft` mà B đã chọn.
- **Request:**
```json
{ "outputLanguage": "vi-VN" }
```
- **Response (200):**
```json
{
  "data": { "suggestions": [{ "id": "uuid", "text": "Tôi cần thêm thời gian để trả lời." }] },
  "error": null,
  "meta": {}
}
```
- **Error cases:** `BT_E_AI_SUGGESTION_UNAVAILABLE`, `BT_E_FORBIDDEN`.
- **BR liên quan:** BR-BT-11, BR-BT-15

### `POST /api/call-sessions/{id}/transcript-consents`
- **Mô tả:** Ghi nhận quyết định lưu transcript của từng người tham gia. Chỉ lưu khi mọi `CallParticipant` đều có `decision` là `Granted`.
- **Request:**
```json
{
  "purpose": "SaveTranscript",
  "decision": "Granted"
}
```
- **Response (201):**
```json
{
  "data": { "consentId": "uuid", "decision": "Granted" },
  "error": null,
  "meta": {}
}
```
- **Error cases:** `BT_E_SAVE_CONSENT_REQUIRED`, `BT_E_FORBIDDEN`.
- **Ghi chú quyền:** Server suy ra `participantId` từ Firebase Authentication token và lưu quyết định mới nhất của mỗi người tham gia/mục đích cho đến khi transcript được xử lý lưu hoặc xóa.
- **BR liên quan:** BR-BT-09, BR-BT-15

### `POST /api/call-sessions/{id}/interaction-metrics`
- **Mô tả:** Gửi metric ẩn danh tới Firebase Analytics.
- **Request:**
```json
{ "eventName": "MorseMessageSent" }
```
- **Response (202):**
```json
{
  "data": { "accepted": true },
  "error": null,
  "meta": {}
}
```
- **Error cases:** `BT_E_ANALYTICS_CONSENT_REQUIRED`, `BT_E_FORBIDDEN`.
- **BR liên quan:** BR-BT-12, BR-BT-15

## 4. Ghi chú migration

- Bản phát hành phải có backend và schema lưu rõ ràng cho `UserAccount`, `ContactRequest`, `CallSession`, `ConsentRecord` và `InteractionMetric`.
- Nội dung giao tiếp như audio thô, caption, `TapSequence`, `MessageDraft`, `VoiceOutput` và `MessageSuggestion` có thể chỉ giữ tạm trong phiên hoặc lưu theo consent, nhưng không được mặc định lưu dài hạn.
- Client truyền audio qua media session thời gian thực đã xác thực; API REST chỉ quản lý phiên, dữ liệu trợ năng, consent và metric. Firebase không thay thế media transport.
- Nếu bổ sung lưu transcript, cần chốt nơi lưu, thời hạn lưu, cơ chế xóa và quyền truy cập trước khi mở rộng schema lưu dài hạn. Transcript chỉ được lưu khi cả hai `ConsentRecord` cho `SaveTranscript` là `Granted`.

<!-- Append CR sections below when logic changes -->
