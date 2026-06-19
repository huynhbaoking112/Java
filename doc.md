HỌC VIỆN CÔNG NGHỆ BƯU CHÍNH VIỄN THÔNG
KHOA: CÔNG NGHỆ THÔNG TIN 2

Học phần: Phát triển ứng dụng cho các thiết bị di động
Trình độ đào tạo: Đại học    
Hình thức đào tạo: Chính qui  	 
THÔNG TIN ĐỀ TÀI DỰ ÁN
ĐỀ TÀI SỐ 18
1. Tên đề tài: Xây dựng ứng dụng quản lý công việc
2. Số lượng sinh viên yêu cầu: 2 sinh viên
1. Huỳnh Bảo King		MSSV: N22DCCN146 <Trưởng nhóm>
2. Văn Duy Khánh		MSSV: N22DCCN139 <Thành viên>
3. Mô tả đề tài
Các yêu cầu chính của đề tài:
Phân hệ Xác thực và Phân quyền
Hệ thống sử dụng cơ chế đăng nhập bằng tài khoản được cấp bởi quản trị viên. Người dùng được Admin tạo tài khoản trên hệ thống và sử dụng Username cùng Password để đăng nhập ứng dụng. Sau khi đăng nhập thành công, thông tin phiên làm việc được lưu trữ cục bộ trên thiết bị bằng SharedPreferences nhằm duy trì trạng thái đăng nhập.
Hệ thống hỗ trợ các chức năng đăng nhập, đăng xuất, đổi mật khẩu và cập nhật thông tin cá nhân. Hệ thống phân quyền người dùng theo các vai trò Admin, Manager và Member. Admin có quyền quản lý người dùng, dự án và toàn bộ dữ liệu hệ thống. Manager có quyền quản lý dự án, phân công công việc và theo dõi tiến độ thực hiện. Member thực hiện các công việc được giao và cập nhật trạng thái công việc. Người dùng chỉ được phép truy cập các chức năng phù hợp với quyền hạn được cấp.

Phân hệ Quản lý Công việc (Dashboard)
Giao diện chính (Dashboard) được thiết kế nhằm cung cấp nhanh tình trạng công việc cho người dùng. Màn hình hiển thị số lượng công việc đang thực hiện, công việc đã hoàn thành, công việc quá hạn và các công việc ưu tiên cao. Danh sách công việc được hiển thị thông qua RecyclerView giúp người dùng dễ dàng theo dõi và quản lý.
Ứng dụng hỗ trợ tìm kiếm công việc theo từ khóa, lọc theo trạng thái, mức độ ưu tiên hoặc dự án. Người dùng có thể thực hiện nhanh các thao tác tạo mới, chỉnh sửa, cập nhật trạng thái hoặc đánh dấu hoàn thành công việc ngay trên giao diện.

Phân hệ Tạo và Quản lý Công việc
Người dùng có thể tạo mới công việc với các thông tin bao gồm tiêu đề, mô tả, ngày bắt đầu, hạn hoàn thành, mức độ ưu tiên, danh mục và các nhãn phân loại (Tags). Hệ thống hỗ trợ chỉnh sửa nội dung công việc, cập nhật trạng thái thực hiện và theo dõi tiến độ hoàn thành.
Các công việc được xóa sẽ được chuyển vào thùng rác dưới hình thức xóa mềm (Soft Delete), cho phép khôi phục khi cần thiết trước khi bị xóa hoàn toàn khỏi hệ thống.

Phân hệ Chi tiết Công việc
Màn hình chi tiết công việc hiển thị đầy đủ các thông tin liên quan như tiêu đề, nội dung, trạng thái, độ ưu tiên, ngày tạo, hạn hoàn thành, người tạo và người thực hiện. Người dùng có thể cập nhật tiến độ, thay đổi trạng thái công việc hoặc bổ sung ghi chú trong quá trình thực hiện.
Hệ thống hỗ trợ đính kèm hình ảnh hoặc tài liệu liên quan giúp lưu trữ thông tin phục vụ công việc một cách thuận tiện.

Phân hệ Quản lý Dự án và Nhóm làm việc
Người dùng có thể tạo mới, chỉnh sửa hoặc xóa dự án. Mỗi dự án chứa nhiều công việc và nhiều thành viên tham gia. Manager hoặc Admin có thể phân công công việc cho các thành viên và theo dõi tiến độ thực hiện của từng người.
Trong mỗi dự án, hệ thống hiển thị danh sách công việc, trạng thái thực hiện và tỷ lệ hoàn thành nhằm hỗ trợ việc quản lý tiến độ dự án hiệu quả.

Phân hệ Báo cáo và Thống kê
Hệ thống cung cấp các báo cáo thống kê bao gồm tổng số công việc, số công việc đã hoàn thành, công việc đang thực hiện và công việc quá hạn. Ngoài ra, ứng dụng còn thống kê hiệu suất làm việc của từng thành viên trong dự án.
Nhóm sử dụng thư viện MPAndroidChart để hiển thị dữ liệu dưới dạng biểu đồ trực quan, giúp người dùng dễ dàng theo dõi tình hình thực hiện công việc theo ngày, tuần hoặc tháng.

Quản lý hồ sơ
Người dùng có thể cập nhật thông tin cá nhân, thay đổi ảnh đại diện và đổi mật khẩu. Ảnh đại diện được lưu trữ trên Server và hiển thị thông qua thư viện Glide.
Màn hình hồ sơ cá nhân hiển thị danh sách công việc được giao, danh sách công việc đã hoàn thành và các dự án mà người dùng đang tham gia.

Tài khoản người dùng:
• Đăng nhập, đăng xuất, đổi mật khẩu và cập nhật thông tin cá nhân.
• Quản lý công việc cá nhân.
• Tham gia và theo dõi các dự án được phân công.
Giao diện người dùng:
• Giao diện trực quan, dễ sử dụng và tối ưu trải nghiệm người dùng.
• Hỗ trợ Light/Dark Mode.
• Thiết kế Responsive phù hợp với nhiều kích thước màn hình Android.
• Áp dụng Material Design trong xây dựng giao diện.
4. Yêu cầu nhóm và học viên
•	Phát triển ứng dụng di động bằng Java đáp ứng các yêu cầu chức năng và giao diện. [CLO1]
•	Áp dụng các kiến thức về Activity, Fragment, RecyclerView, REST API, SharedPreferences, SQLite/Room Database. [CLO2]
•	Có cơ chế xác thực người dùng, phân quyền và bảo mật dữ liệu cá nhân. [CLO2]
•	Thuyết trình và bảo vệ được kết quả công việc cá nhân, mô hình kiến trúc, các nguyên tắc và tiêu chuẩn được áp dụng trong dự án. [CLO2]
