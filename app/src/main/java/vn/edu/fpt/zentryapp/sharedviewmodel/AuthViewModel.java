package vn.edu.fpt.zentryapp.sharedviewmodel;

/**
 * ViewModel: AuthViewModel
 *
 * Chịu trách nhiệm quản lý trạng thái đăng nhập, đăng xuất, quên mật khẩu, xác thực.
 *
 * Giữ trạng thái token, trạng thái đăng nhập hiện tại.
 *
 * Không nên để AuthViewModel này quá lớn, chỉ tập trung xử lý authentication.
 *
 * Các màn hình liên quan như Login, Forgot Password, Register có thể dùng chung AuthViewModel scoped Activity hoặc riêng nếu độc lập.
 */
public class AuthViewModel {
}
