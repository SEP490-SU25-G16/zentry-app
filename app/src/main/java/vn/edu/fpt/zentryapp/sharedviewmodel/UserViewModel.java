package vn.edu.fpt.zentryapp.sharedviewmodel;

/**
 * ViewModel: UserViewModel (Shared ViewModel)
 *
 * Giữ thông tin người dùng hiện tại (profile, role, quyền hạn, setting cá nhân).
 *
 * Shared ViewModel scoped Activity hoặc Navigation Graph để nhiều màn hình (Home, Setting, Report, Schedule...) có thể quan sát và cập nhật dữ liệu người dùng.
 *
 * Khi user update profile hoặc setting, UserViewModel cập nhật và đẩy dữ liệu mới cho các màn hình khác.
 */
public class UserViewModel {
}
