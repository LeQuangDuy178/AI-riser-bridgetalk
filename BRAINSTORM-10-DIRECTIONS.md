# Brainstorm 10 directions — AI Riser Vietnam 2026

Ngày: 2026-08-20  
Trạng thái: mở rộng không gian ý tưởng, **chưa chốt project**.

Nguồn tham khảo: [25 project quốc tế và rubric chính thức](./INTERNATIONAL-HACKATHON-RESEARCH.md). Các project nước ngoài chỉ cung cấp pattern; không phải bằng chứng rằng cùng ý tưởng sẽ phù hợp Việt Nam.

## Nguyên tắc sàng lọc

Một candidate chỉ đáng đi tiếp nếu trả lời được:

1. User cụ thể nào có thể tiếp cận trước 25/08?
2. Khoảnh khắc khó khăn nào đã quan sát được, không phải pain tự nghĩ ra?
3. Artifact đầu vào thật là gì: ảnh, chat, voice, form, worksheet, video hay log?
4. AI cần thiết ở đâu; phần nào phải dùng rule/code deterministic?
5. Sau inference, ai duyệt và trạng thái nào thực sự thay đổi?
6. Đo được outcome gì trên 5–10 người trước 29/08?
7. Demo một vòng kín trong 60–90 giây được không?

## 1. Healthcare

Pattern quốc tế: Gaze Link và VITE VERE thắng bằng một micro-task đo được; WordFinder được đồng thiết kế với người có trải nghiệm thật. Tránh diagnostic chatbot.

| Candidate | Wedge Việt Nam | Vòng demo | Rủi ro/gate |
|---|---|---|---|
| **CareCard VN** | Người chăm bệnh khó chuyển giấy dặn dò thành lịch chăm sóc tại nhà | Chụp giấy → trích chỉ dẫn → đánh dấu chỗ mơ hồ → caregiver duyệt → checklist/Calendar | Chỉ tổ chức thông tin; không diễn giải liều hay chẩn đoán; cần người chăm bệnh thật |
| **AphasiaPic VN** | Người gặp khó khăn gọi tên đồ vật cần cue bằng tiếng Việt | Chụp vật → gợi ý từ liên quan theo tầng → user chọn → phát âm | Cần chuyên gia/người dùng thật; khó xác thực trong một tuần |
| **Clinic Intake Brief** | Thông tin bệnh nhân trước khám nằm rải rác, nhân viên phải nhập lại | Form/voice → bản tóm tắt có nguồn → nhân viên duyệt → phiếu intake | Privacy và high-stakes; chỉ làm nếu có partner y tế |

Verdict sơ bộ: **park**, trừ khi có quyền tiếp cận caregiver/chuyên gia ngay hôm nay.

## 2. Cultural Tourism & Sports

Pattern quốc tế: TrailBlaze kiểm tra giờ mở cửa/chi phí/thời gian rồi sửa itinerary; AthleteAI gắn AI vào một động tác video lặp lại. Tránh itinerary chatbot chung chung.

| Candidate | Wedge Việt Nam | Vòng demo | Rủi ro/gate |
|---|---|---|---|
| **LàngGuide** | Du khách tới một làng nghề nhưng thiếu etiquette, câu chuyện và tuyến tham quan khả thi | Chụp hiện vật → story có nguồn → route theo thời gian → check-in nhiệm vụ | Cần dữ liệu một địa điểm thật, không hallucinate lịch sử |
| **TripRepair VN** | Lịch đi chơi nhóm thường hỏng khi mưa, nơi đóng cửa hoặc vượt ngân sách | Import plan → phát hiện conflict bằng rule → AI đề xuất sửa → nhóm duyệt | Cạnh tranh cao; cần dữ liệu giờ mở cửa/di chuyển thật |
| **MoveMirror** | Người mới tập một động tác thể thao không biết sai ở pha nào | Video → pose/key moments → feedback một kỹ thuật → thử lại | Chỉ chọn động tác an toàn; không đưa advice y khoa |

Verdict sơ bộ: LàngGuide đáng test nếu có local dataset/partner; còn lại thiếu wedge.

## 3. Game

Pattern quốc tế: Outdraw biến model thành đối thủ; Pen Apple để ngôn ngữ tự nhiên quyết định luật thẻ. Nếu bỏ AI mà game vẫn y hệt, AI chỉ là trang trí.

| Candidate | Cơ chế | Vòng demo | Rủi ro/gate |
|---|---|---|---|
| **CấmTừ Thành Ngữ** | Người chơi diễn đạt thành ngữ Việt mà không dùng nhóm từ cấm; Gemini là trọng tài semantic | Nhận đề → mô tả → AI bắt vi phạm/đoán → chấm điểm | Phải có rule chấm nhất quán và bộ test thành ngữ |
| **Trạng Quỳnh Court** | Người chơi tranh biện với NPC có mục tiêu/bí mật; bằng chứng thay đổi thái độ | Chọn evidence → đối thoại → NPC cập nhật belief → verdict | Dễ thành chatbot kể chuyện; cần state machine rõ |
| **LoreSmith VN** | Người chơi mô tả hiệu ứng thẻ; Gemini diễn giải trong giới hạn deterministic | Tạo thẻ → validator → trận ngắn → lore phát sinh | Gần Pen Apple; originality risk cao |

Verdict sơ bộ: CấmTừ Thành Ngữ có demo vui, nhưng khó tạo active-user impact để nhắm Gold.

## 4. Agritech

Pattern quốc tế: AI Farm nối dữ liệu với một quyết định; Family Farms Forever giải sell-through/inventory thay vì lặp crop-disease detector.

| Candidate | Wedge Việt Nam | Vòng demo | Rủi ro/gate |
|---|---|---|---|
| **ChợCuốiNgày** | Tiểu thương/nông hộ còn hàng cuối phiên, khó gom combo và tìm người mua gần | Ảnh/voice tồn → chuẩn hóa → bundle/recipe → người bán duyệt → listing | Cần người bán và dữ liệu tồn thật |
| **NôngNhậtKý** | Ghi chép đồng ruộng bằng ảnh/voice rời rạc, khó xem quyết định cũ | Ảnh/voice → log có cấu trúc → việc cần theo dõi → Sheets | Không chẩn đoán bệnh; cần 10 record thật |
| **HarvestDecision** | Quyết định thu hoạch/bán hôm nay thiếu tổng hợp thời tiết, đơn và tồn | Input dữ liệu nhỏ → rule tính constraint → AI giải thích options → human quyết định | Data access khó; không thể demo bằng claim giả |

Verdict sơ bộ: chỉ đi tiếp nếu tiếp cận được nông hộ/tiểu thương trong 24 giờ.

## 5. Scam & Fraud

Pattern quốc tế: ArthaRaksha và Uganda Scam Checker không dừng ở nhãn scam; chúng trích evidence, hướng dẫn và tạo report. Đây là khoảng trống tốt hơn binary classifier.

| Candidate | Wedge Việt Nam | Vòng demo | Rủi ro/gate |
|---|---|---|---|
| **ScamCase VN** | Nạn nhân có nhiều screenshot/voice nhưng không biết gom evidence và báo đúng nơi | Upload bundle → timeline/evidence → uncertainty → checklist → report draft | Cần taxonomy/kênh báo cáo Việt Nam; tuyệt đối không khẳng định pháp lý |
| **ElderVerify** | Người lớn tuổi cần một bước dừng an toàn trước yêu cầu chuyển tiền/gửi OTP | Chia sẻ screenshot/voice → red flags → gọi người tin cậy/official channel | False negative nguy hiểm; cần test với người lớn tuổi |
| **SellerTrust Pack** | Shop nhỏ bị giả mạo hoặc khách cần kiểm tra kênh bán chính thức | Nhập link/screenshot → so khớp kênh đã xác minh → evidence card → report | Phải có nguồn official/deterministic, không để model tự phán |

Verdict sơ bộ: **ScamCase VN là candidate mạnh**, nhưng chỉ nếu scope là evidence-to-action và có abstention.

## 6. Marketing & Social Commerce

Pattern quốc tế: Remixify biến co-creation thành insight; SaFA đóng vòng sau cuộc hội thoại bằng CRM/task. Tránh content generator.

| Candidate | Wedge Việt Nam | Vòng demo | Rủi ro/gate |
|---|---|---|---|
| **LiveLead Rescue** | Shop livestream bỏ sót comment có ý định mua và follow-up | Comment export → intent/evidence → seller duyệt → follow-up/Sheet → trạng thái | Cần comment thật và shop tester |
| **ClaimProof Live** | Livestreamer dễ nói claim sản phẩm thiếu bằng chứng | Script/transcript → highlight claim → yêu cầu nguồn → safe rewrite | Cần policy/domain source; tránh moderation chung chung |
| **RemixSignal** | Brand không biết visual/copy nào khách thực sự thích | User remix asset → theo dõi choice → dashboard insight | Quá nhiều surface cho deadline; park |

Verdict sơ bộ: **LiveLead Rescue mạnh nếu có một shop thật** và đo missed-lead recovery.

## 7. Education

Pattern quốc tế: Claricode biến đúng artifact người học đang mắc; CheckMate giảm một job lặp lại của giáo viên nhưng giữ review. Tránh general tutor.

| Candidate | Wedge Việt Nam | Vòng demo | Rủi ro/gate |
|---|---|---|---|
| **SaiỞĐâu** | Học sinh nhận đáp án nhưng không biết bước suy luận sai | Chụp bài → map bước → hỏi gợi mở → học sinh sửa → kiểm tra lại | Cần giới hạn môn/lớp; benchmark đáp án |
| **TeacherBatch Review** | Giáo viên mất thời gian gom lỗi lặp lại từ bài giấy | Batch ảnh → draft rubric feedback → giáo viên duyệt → misconception dashboard | Cần giáo viên và bài thật có consent |
| **Rural Lesson Adapter** | Tài liệu chung thiếu ví dụ địa phương/phù hợp trình độ | Upload lesson → giữ mục tiêu → tạo ví dụ local → teacher approval | Khó chứng minh learning outcome trong một tuần |

Verdict sơ bộ: **SaiỞĐâu là candidate mạnh** nếu có 5 học sinh và một bộ bài cụ thể.

## 8. Business Utility

Pattern quốc tế: BidBridge để AI hiểu văn bản nhưng rule tính score; CitizenLink neo AI vào workflow state. Cơ hội tốt là một handoff hỏng, không phải company chatbot.

| Candidate | Wedge Việt Nam | Vòng demo | Rủi ro/gate |
|---|---|---|---|
| **StoreLoop** | Sự cố cửa hàng từ ảnh/voice thiếu evidence, owner và xác nhận đã xử lý | Báo sự cố → triage → human assign → ảnh sau xử lý → verify → close | Cần case vận hành thật; khác CitizenLink bằng taxonomy/metric retail |
| **BànGiao AI** | Bàn giao ca qua chat/ảnh/voice dễ sót việc | Input → task/evidence/missing fields → xác nhận → Sheets/Calendar | Có nguy cơ chỉ là extractor; phải chứng minh giảm missed items |
| **ReturnProof** | Hồ sơ đổi trả/hàng lỗi thiếu bằng chứng, phải hỏi lại nhiều vòng | Ảnh/video + mô tả → checklist evidence → nhân viên duyệt → case state | Không để AI tự quyết quyền lợi khách; rule/policy deterministic |

Verdict sơ bộ: **StoreLoop/ReturnProof là candidate mạnh nhất nếu có quyền tiếp cận case retail/SME**.

## 9. Inclusive Access

Pattern quốc tế: Guiden và LetsHelp thiết kế lại modality theo hạn chế thật; không chỉ tăng font. Phải test với target user/partner.

| Candidate | Wedge Việt Nam | Vòng demo | Rủi ro/gate |
|---|---|---|---|
| **DeviceBuddy VN** | Người lớn tuổi kẹt ở một bước dùng smartphone và hướng dẫn từ xa không thấy màn hình | Screen/image → voice từng bước → user xác nhận → bước tiếp | Privacy/screen data; cần người lớn tuổi test thật |
| **FormVoice VN** | Người thị lực yếu/người già khó hiểu form giấy | Chụp form → đọc/giải thích từng trường → user xác nhận → checklist | Không tự điền/legal advice; OCR tiếng Việt phải đủ tốt |
| **QueueCompanion** | Người khiếm thính/khó đọc bỏ lỡ thông báo tại điểm dịch vụ | Capture thông báo → simplify/visual alert → confirm | Cần bối cảnh thật và accessibility expert |

Verdict sơ bộ: impact cao nhưng **không đi tiếp nếu không có target user thật**.

## 10. General / Open

Pattern quốc tế: SnapSell biến một video thành một quy trình hoàn chỉnh; Emergent dùng AI để mô phỏng việc khó diễn tập; Globot có nhiều AI job nhưng xoay quanh một biến cố.

| Candidate | Wedge Việt Nam | Vòng demo | Rủi ro/gate |
|---|---|---|---|
| **OneCapture Seller** | Người bán quay một video hàng tồn rồi phải tạo nhiều listing thủ công | Video/voice → item/evidence → seller duyệt → listing → inquiry state | Gần SnapSell; phải khác bằng local workflow/constraint |
| **OpsDrill** | Cửa hàng khó diễn tập sự cố hiếm như mất điện, ngập, POS hỏng | Chọn scenario → AI personas phản ứng → team chọn action → report/gap | Simulation là hypothesis, không phải evidence |
| **DecisionTrace** | Quyết định họp mất rationale và điều kiện review | Transcript → decision/evidence/dissent → owner duyệt → review trigger | Nhiều meeting summarizer; wedge yếu nếu không có workflow đặc thù |

Verdict sơ bộ: OpsDrill thú vị nhưng rủi ro demo-only; chưa vào top.

## Shortlist trước grilling

Điểm 1–5; chưa phải quyết định cuối. “User access” là giả định cần bạn xác nhận.

| Candidate | User access | Pain/evidence | Ship 22/08 | AI necessity | Proof trước 29/08 | Safety | Tổng sơ bộ |
|---|---:|---:|---:|---:|---:|---:|---:|
| StoreLoop | 5* | 4 | 4 | 4 | 5 | 4 | **4.35** |
| BànGiao AI | 5* | 4 | 5 | 4 | 5 | 4 | **4.55** |
| ScamCase VN | 5 | 4 | 5 | 4 | 4 | 2 | **4.15** |
| SaiỞĐâu | 4* | 4 | 4 | 4 | 4 | 3 | **3.95** |
| LiveLead Rescue | 3* | 4 | 4 | 4 | 4 | 4 | **3.75** |
| DeviceBuddy VN | 2* | 5 | 3 | 5 | 4 | 2 | **3.55** |

Các điểm có dấu * phải hạ mạnh nếu bạn không tiếp cận được user/case thật ngay.

## Phiên /grilling — phản biện trước khi chốt

### BànGiao AI

- Nếu người dùng chỉ cần một template bàn giao bắt buộc, tại sao cần Gemini?
- Bạn có bằng chứng sót việc là pain thường xuyên hay chỉ là cảm giác?
- Input đa modal tạo giá trị hay chỉ để khoe tech?
- Ai chịu trách nhiệm khi AI bỏ sót critical item?
- **Điều kiện sống:** 10 handover cases thật + baseline missed-item/time + human confirmation bắt buộc.

### StoreLoop

- Đây có phải CitizenLink đổi nhãn sang cửa hàng?
- Sự cố nào đủ thường xuyên và tốn kém để user quay lại dùng?
- “Ảnh sau xử lý” có thực sự chứng minh resolved hay cần checklist/rule?
- Có được dùng dữ liệu vận hành/cửa hàng trong demo công khai không?
- **Điều kiện sống:** một taxonomy retail cụ thể, 10 incident cases, owner/status thật và metric time-to-close/rework.

### ScamCase VN

- Điều gì khác một prompt “phân tích scam” trong Gemini?
- Report tạo ra gửi được đến đâu hay chỉ là PDF đẹp?
- False negative có khiến user tin nhầm và chuyển tiền không?
- Có nguồn chính thức cho taxonomy và verification step không?
- **Điều kiện sống:** evidence-to-action loop, uncertainty/abstention, nguồn official và test harmful cases.

### SaiỞĐâu

- Model phát hiện misconception thật hay chỉ đoán lại từ đáp án?
- Vì sao học sinh không dùng Gemini trực tiếp?
- Bạn giới hạn môn/lớp/dạng bài nào để đánh giá được?
- Có teacher review hay benchmark đáp án không?
- **Điều kiện sống:** một dạng bài hẹp, 20 câu có ground truth, đo self-correction thay vì số câu trả lời.

### LiveLead Rescue

- Shop có thật sự mất đơn vì bỏ sót comment hay do giá/sản phẩm?
- Bạn có access comment export và consent không?
- Follow-up có tạo spam hoặc sai thông tin hàng tồn không?
- Sau phân loại intent, state nào thực sự thay đổi?
- **Điều kiện sống:** một shop thật, comment thật đã ẩn PII, seller approval và recovered-lead metric.

### DeviceBuddy VN

- Người lớn tuổi có tự mở được công cụ khi họ vốn đang kẹt công nghệ không?
- Screen sharing có lộ OTP/tài khoản không?
- Voice guidance có chịu được giọng vùng miền và tiếng ồn?
- Bạn có test được với người lớn tuổi thật hay chỉ đóng vai?
- **Điều kiện sống:** zero/low-setup flow, privacy mask, target-user test và giới hạn task.

## Kết luận sau grilling

Chưa ý tưởng nào PASS toàn bộ Product Mindset:

- **Outcome:** mới có success hypothesis, chưa có baseline.
- **Design:** chưa có quan sát/case thật và user test.
- **Critical:** user access đang là giả định; high-stakes candidates còn safety gaps.

Vì vậy chưa chốt theo “điểm cao nhất”. Chốt theo nhánh access:

1. Có 5 người vận hành/cửa hàng + 10 case thật → test **BànGiao AI, StoreLoop, ReturnProof**.
2. Có shop livestream + comment thật → test **LiveLead Rescue**.
3. Có giáo viên/học sinh + bài làm → test **SaiỞĐâu**.
4. Không có domain access nhưng có screenshot scam đã ẩn PII → test **ScamCase VN**, với scope evidence/report chứ không classifier.
5. Không có bất kỳ user/case nào → chưa đủ điều kiện chốt; chọn candidate dễ thu case nhất trước, không chọn candidate “wow” nhất.

## Bài kiểm tra 90 phút để ra quyết định

Cho mỗi nhánh có access:

1. Thu 3 case thật và đo cách xử lý hiện tại.
2. Dựng prototype giả lập một happy path, chưa cần code.
3. Đưa cho 3 user; quan sát họ có hoàn thành job nhanh/đúng hơn không.
4. Ghi metric, lỗi và câu họ phải hỏi lại.
5. Candidate thắng là candidate có evidence mạnh nhất, không phải tổng điểm desk research cao nhất.
