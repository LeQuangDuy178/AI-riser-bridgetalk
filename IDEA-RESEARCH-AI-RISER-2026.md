# Idea research: AI Riser Vietnam 2026

Ngày cập nhật: 2026-08-20

> Trạng thái: **chưa chốt ý tưởng**. Bản brainstorm vòng 2 dựa trên 25 project quốc tế nằm tại [BRAINSTORM-10-DIRECTIONS.md](./BRAINSTORM-10-DIRECTIONS.md); nguồn benchmark chi tiết nằm tại [INTERNATIONAL-HACKATHON-RESEARCH.md](./INTERNATIONAL-HACKATHON-RESEARCH.md).

## Mục tiêu lựa chọn

Không tìm “ý tưởng AI hay nhất” chung chung. Chọn một vấn đề:

1. Có user thật mà bạn tiếp cận được trước 30/08.
2. Có thể dựng V1 end-to-end trong build day 22/08.
3. AI là phần thiết yếu, không phải chatbot gắn thêm.
4. Có thể đo thay đổi hành vi/kết quả thật.
5. Dễ deploy Cloud Run và thể hiện Google Tech.

## Brainstorm theo 10 directions

| Direction | Ý tưởng | User + vấn đề thật | Demo lõi |
|---|---|---|---|
| Healthcare | Discharge2Home | Người chăm bệnh khó chuyển giấy dặn dò thành lịch; chỉ tổ chức thông tin, không chẩn đoán | Chụp giấy → checklist/nhắc việc + mục cần hỏi bác sĩ |
| Tourism | LocalLens | Du khách thiếu bối cảnh và phép lịch sự khi trải nghiệm món ăn/di tích | Chụp ảnh → câu chuyện có nguồn + etiquette + route |
| Game | FolkloreQuest | Người học văn hóa khó tương tác với truyện dân gian | NPC ứng biến nhưng giữ lore → nhánh truyện ngắn |
| Agritech | NôngNhậtKý | Nông hộ ghi chép ảnh/voice rời rạc, khó theo dõi việc | Ảnh + voice → nhật ký ruộng + việc tiếp theo; không chẩn đoán bệnh |
| Scam/Fraud | ScamLens VN | Người nhận tin đáng ngờ không biết kiểm tra gì và làm gì tiếp | Screenshot → evidence/red flags → checklist xác minh |
| Marketing/Social Commerce | LiveOps Copilot | Shop nhỏ bỏ sót intent mua, FAQ và claim rủi ro trong comment livestream | Comment batch → intent, FAQ, lead và claim cần kiểm tra |
| Education | SaiỞĐâu | Học sinh nhận đáp án nhưng không hiểu misconception ở bước nào | Chụp bài → xác định bước sai → hỏi gợi mở |
| Business Utility | BànGiao AI | Nhân viên cửa hàng/SME bàn giao qua chat, ảnh, voice nên sót việc và thiếu owner/deadline | Input bàn giao → task có evidence → xác nhận → Sheets/Calendar |
| Inclusive Access | FormVoice VN | Người cao tuổi/người thị lực yếu khó hiểu biểu mẫu | Chụp form → đọc giọng nói → giải thích → checklist |
| General/Open | DecisionTrace | Quyết định họp bị mất lý do và điều kiện xem xét lại | Transcript → decision, evidence, dissent, owner, review trigger |

## Shortlist theo Product Mindset

Trọng số: tiếp cận user 25%, chứng minh impact 25%, khả thi V1 20%, Google AI 15%, demo/khác biệt 15%. Điểm 1–5.

| Ý tưởng | User access | Impact proof | V1 | Google AI | Demo | Điểm |
|---|---:|---:|---:|---:|---:|---:|
| BànGiao AI | 5* | 5 | 5 | 4 | 4 | **4.70** |
| ScamLens VN | 5 | 4 | 5 | 4 | 4 | **4.40** |
| SaiỞĐâu | 4 | 4 | 4 | 4 | 4 | **4.00** |
| FormVoice VN | 3 | 4 | 4 | 5 | 4 | **3.90** |
| LiveOps Copilot | 3 | 4 | 4 | 4 | 4 | **3.75** |

BànGiao AI chỉ có 5 điểm user access nếu bạn tiếp cận được ít nhất 5 nhân viên cửa hàng/SME. Nếu không, hạ xuống 2 và chọn theo nhóm tester bạn thực sự có.

## Candidate vòng 1: BànGiao AI — chưa chốt

### Vấn đề thật

> Trong bàn giao ca, thông tin nằm rải rác trong chat, ảnh và lời nói; việc quan trọng dễ thiếu owner, deadline hoặc bằng chứng, khiến ca sau bỏ sót hoặc phải hỏi lại.

### How Might We

> Làm thế nào để ca sau nắm đúng việc chưa hoàn tất và hành động trong dưới hai phút, mà người bàn giao không phải nhập lại toàn bộ?

### Outcome statement — vòng khám phá

> Để trưởng ca và nhân viên ca sau tiếp nhận đầy đủ việc chưa hoàn tất, đo bằng tỷ lệ critical items được ghi nhận đúng và thời gian hoàn tất bàn giao, vòng đầu đặt success criteria ≥ 90% critical items đúng, không tạo task thiếu bằng chứng, và hoàn tất trong ≤ 2 phút trên 10 tình huống trước 28/08/2026.

Chưa có baseline. Ngày 20/08 phải đo cách làm hiện tại với 5 người; không được trình bày target trên như kết quả đã đạt.

### Ba phương án cần test

1. **Extractor:** AI tạo task, user xác nhận owner/deadline.
2. **Checklist-first:** chỉ hỏi các trường còn thiếu, không tự hoàn thiện task.
3. **Shift digest:** tạo tổng hợp ca và highlight rủi ro, không đẩy từng task.

Chọn bằng kết quả test, không mặc định phương án 1.

### MVP ngày 22/08

1. Paste chat hoặc upload ảnh/voice bàn giao.
2. Gemini trả JSON: task, evidence, priority, owner, deadline, missing fields.
3. User bắt buộc xác nhận/sửa; không tự bịa trường thiếu.
4. Xuất task đã duyệt sang Google Sheets; Calendar là stretch goal.
5. Hiển thị việc mở, thiếu owner và quá hạn.

Không làm: login đa vai trò, notification phức tạp, tích hợp Zalo thật, analytics lớn hoặc chatbot đa năng.

### Bộ test tối thiểu

- 3 ca rõ, đủ owner/deadline.
- 3 ca thiếu thông tin cần hỏi lại.
- 2 ca có ảnh bằng chứng.
- 1 ca mâu thuẫn giữa chat và voice.
- 1 ca không chứa task để kiểm tra hallucination.

Metric: critical-item recall, task hallucination rate, correction count, time-to-confirm.

## Phương án dự phòng: ScamLens VN

Chọn nếu không tiếp cận được người dùng bàn giao ca. Lợi thế: dễ tìm screenshot/tester. Bất lợi: track dễ đông và rủi ro tạo niềm tin sai.

> Để người dùng đang phân vân trước tin nhắn/giao dịch đáng ngờ chọn được bước xác minh an toàn, đo bằng tỷ lệ chọn đúng hành động trên bộ test, mục tiêu ≥ 80% trên 10 tình huống, không đưa khuyến nghị nguy hiểm và phản hồi ≤ 30 giây trước 28/08/2026.

MVP: screenshot → evidence → mức không chắc chắn → checklist xác minh. Không kết luận chắc chắn scam/not scam, truy danh tính hay đưa kết luận pháp lý.

## Decision gate ngày 20/08

Chỉ chốt BànGiao AI nếu đạt ít nhất 4/5:

- [ ] Tiếp cận được ≥ 5 target users trước 25/08.
- [ ] Có 10 tình huống đã ẩn dữ liệu để test.
- [ ] Ít nhất 3/5 user từng sót việc hoặc phải hỏi lại.
- [ ] Prototype một flow dựng được trong một ngày.
- [ ] Đo được baseline và target bằng recall/time-to-confirm.

Nếu không đạt, test ScamLens VN và SaiỞĐâu bằng cùng checklist; chọn ý tưởng có evidence tốt hơn.

## Plan thực thi

### 19/08 — Discovery

- Liệt kê ba nhóm bạn tiếp cận được: vận hành/cửa hàng, người dùng smartphone, học sinh/giáo viên.
- Mỗi nhóm lấy 3–5 tình huống thật đã ẩn PII.
- Hẹn 5 người test 15 phút vào 20/08.

### 20/08 — Empathize và chọn problem

- Quan sát cách user xử lý hiện tại; đo thời gian và lỗi.
- Viết ba problem statements và ba HMW.
- Chạy decision gate, khóa một ý tưởng trước cuối ngày.

### 21/08 — Prototype

- Wireframe một happy path và prompt/structured-output spike.
- Chuẩn hóa 10 test cases và expected output.
- Test với 3 người; cắt scope theo lỗi lớn nhất.

### 22/08 — Build day

- Build: happy path → validation/fallback → Sheets → deploy.
- Giữ 60 phút cuối để test, quay màn hình và submit.
- Hỏi BTC submission có cập nhật trước 30/08 được không.

### 23–29/08 — Impact và final

- Deploy Cloud Run, sửa lỗi, thêm logging tối thiểu.
- Cho 5–10 user thật dùng; lưu số liệu trước/sau và feedback.
- Video ≤ 2 phút: problem 15s → demo 70s → tech 15s → evidence 20s.
- Đăng LinkedIn, kiểm tra AI Studio share link và submit Completion Form 29/08.

## Checklist Product Mindset hiện tại

### Outcome

- PASS: problem đã tách khỏi AI; metric đo giá trị thật.
- PASS có điều kiện: có success criteria vòng khám phá.
- FAIL: chưa có baseline; phải đo ngày 20/08.

### Design

- PASS: có 10 ideas và ba phương án cho shortlist.
- PASS: scope prototype/MVP đủ nhỏ.
- FAIL: chưa quan sát/test target user; BànGiao AI chưa phải lựa chọn cuối.

### Critical

- PASS: tách dữ kiện, giả định và khuyến nghị.
- PASS: có decision gate và phương án dự phòng.
- FAIL: chưa kiểm tra hallucination trên input đa định dạng/mâu thuẫn.

Kết luận vòng 1: **chưa được phép chốt BànGiao AI chỉ từ desk research.** Cần dùng shortlist và phiên grilling trong `BRAINSTORM-10-DIRECTIONS.md`, sau đó chọn theo nhóm user/case thật có thể tiếp cận.
