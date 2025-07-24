package vn.edu.fpt.zentryapp.lecturer.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.databinding.FragmentLecturerHomeBinding;
import vn.edu.fpt.zentryapp.lecturer.adapter.CourseAdapter;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.Course;

public class LecturerHomeFragment extends Fragment implements CourseAdapter.OnCourseClickListener {

    private FragmentLecturerHomeBinding binding;
    private LecturerHomeViewModel viewModel;
    private CourseAdapter courseAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLecturerHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(LecturerHomeViewModel.class);
        AuthManager authManager = AuthManager.getInstance(requireContext());
        viewModel.init(authManager);

        setupRecyclerView();
        observeViewModel();
    }

    private void setupRecyclerView() {
        courseAdapter = new CourseAdapter();
        courseAdapter.setOnCourseClickListener(this);

        binding.rvCourses.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvCourses.setAdapter(courseAdapter);
    }

    private void observeViewModel() {
        // Observe loading state
        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        // Observe courses
        viewModel.courses().observe(getViewLifecycleOwner(), courses -> {
            if (courses != null) {
                courseAdapter.setCourses(courses);
            }
        });

        // Observe greeting
        viewModel.greeting().observe(getViewLifecycleOwner(), greeting -> {
            if (greeting != null) {
                binding.tvHomeGreeting.setText(greeting);
            }
        });

        // Observe user profile
        viewModel.userProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                // Update greeting with user name
                String displayName = profile.getName();
                if (displayName != null && !displayName.isEmpty()) {
                    // Greeting sẽ được update từ ViewModel, nhưng có thể customize thêm ở đây
                }

            }
        });

        // Observe errors
        viewModel.errorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onCourseClick(Course course) {
        // Toast.makeText(requireContext(), "Clicked: " + course.getName(), Toast.LENGTH_SHORT).show();
        // TODO: Navigate to course detail screen
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}