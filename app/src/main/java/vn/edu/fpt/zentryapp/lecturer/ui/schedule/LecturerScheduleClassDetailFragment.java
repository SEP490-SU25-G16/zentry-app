package vn.edu.fpt.zentryapp.lecturer.ui.schedule;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayoutMediator;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.databinding.FragmentLecturerScheduleClassDetailBinding;

public class LecturerScheduleClassDetailFragment extends Fragment {

    private FragmentLecturerScheduleClassDetailBinding binding;
    private long classId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate layout và binding view
        binding = FragmentLecturerScheduleClassDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Lấy classId từ argument nếu có
        if (getArguments() != null) {
            classId = getArguments().getLong("classId", 0L);
        }

        // Xử lý nút back toolbar, gọi back của Activity
        binding.ivScheduleClassDetailBack.setOnClickListener(v -> requireActivity().onBackPressed());

        // Hiển thị thông tin header lớp học (ví dụ tạm thời)
        binding.tvScheduleClassDetailGrade.setText("Grade " + classId);
        binding.tvScheduleClassDetailSubject.setText("Mathematics");

        // Thiết lập ViewPager2 với 2 tab: "Info" và "Students"
        String[] tabTitles = new String[]{"Info", "Students"};
        binding.viewPagerScheduleClassDetail.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                // TODO: Trả về fragment tương ứng, truyền classId nếu cần
                switch (position) {
                    case 0:
                        // Ví dụ: return ClassInfoFragment.newInstance(classId);
                        return new Fragment(); // Thay bằng fragment thực tế
                    case 1:
                        // Ví dụ: return ClassStudentsFragment.newInstance(classId);
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
        new TabLayoutMediator(binding.tabLayoutScheduleClassDetail, binding.viewPagerScheduleClassDetail,
                (tab, pos) -> tab.setText(tabTitles[pos])
        ).attach();

        // Xử lý nút Add để thêm mới (ví dụ mở dialog hoặc màn hình mới)
        binding.btnScheduleClassDetailAdd.setOnClickListener(v -> {
            // TODO: Thực hiện hành động thêm mới phù hợp
            Toast.makeText(requireContext(), "Add button clicked", Toast.LENGTH_SHORT).show();
        });

        // Xử lý nút thông báo (chưa implement)
        binding.btnScheduleClassDetailNotification.setOnClickListener(v -> {
            // TODO: Mở màn hình hoặc dialog thông báo
            Toast.makeText(requireContext(), "Notification button clicked", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
