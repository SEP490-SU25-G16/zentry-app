package vn.edu.fpt.zentryapp.student.ui.schedule;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.tabs.TabLayoutMediator;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.databinding.FragmentStudentScheduleClassDetailBinding;

public class StudentScheduleClassDetailFragment extends Fragment {

    private FragmentStudentScheduleClassDetailBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate layout và binding view
        binding = FragmentStudentScheduleClassDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Xử lý nút back toolbar, gọi back của Activity
        binding.ivStudentScheduleClassDetailBack.setOnClickListener(v -> requireActivity().onBackPressed());

        // TODO: Hiển thị thông tin header lớp học, ví dụ lấy từ argument hoặc ViewModel
        binding.tvStudentScheduleClassDetailGrade.setText("Grade 07");
        binding.tvStudentScheduleClassDetailSubject.setText("Mathematics");

        // Thiết lập ViewPager2 với các tab (ví dụ: Info và Students)
        String[] tabTitles = new String[]{"Info", "Students"};
        binding.viewPagerStudentScheduleClassDetail.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                // TODO: Trả về fragment tương ứng, truyền classId nếu cần
                switch (position) {
                    case 0:
                        // return StudentClassInfoFragment.newInstance(classId);
                        return new Fragment(); // Thay bằng fragment thực tế
                    case 1:
                        // return StudentClassStudentsFragment.newInstance(classId);
                        return new Fragment(); // Thay bằng fragment thực tế
                    default:
                        return new Fragment();
                }
            }

            @Override
            public int getItemCount() {
                return tabTitles.length;
            }
        });

        // Kết nối TabLayout và ViewPager2
        new TabLayoutMediator(binding.tabLayoutStudentScheduleClassDetail, binding.viewPagerStudentScheduleClassDetail,
                (tab, pos) -> tab.setText(tabTitles[pos])
        ).attach();

        // Xử lý nút Add (chưa implement)
        binding.btnStudentScheduleClassDetailAdd.setOnClickListener(v -> {
            // TODO: Thực hiện hành động thêm mới
        });

        // Xử lý nút thông báo (chưa implement)
        binding.btnStudentScheduleClassDetailNotification.setOnClickListener(v -> {
            // TODO: Mở màn hình hoặc dialog thông báo
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
