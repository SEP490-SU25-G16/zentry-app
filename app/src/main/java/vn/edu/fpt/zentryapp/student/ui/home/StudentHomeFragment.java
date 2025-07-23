package vn.edu.fpt.zentryapp.student.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.databinding.FragmentStudentHomeBinding;
import vn.edu.fpt.zentryapp.student.adapter.StudentCourseAdapter;
import vn.edu.fpt.zentryapp.student.data.model.response.StudentCourse;

public class StudentHomeFragment extends Fragment implements StudentCourseAdapter.OnCourseClickListener {

    private FragmentStudentHomeBinding binding;
    private StudentHomeViewModel viewModel;
    private StudentCourseAdapter courseAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(StudentHomeViewModel.class);
        AuthManager authManager = AuthManager.getInstance(requireContext());
        viewModel.init(authManager);

        setupRecyclerView();
        observeViewModel();
    }

    private void setupRecyclerView() {
        courseAdapter = new StudentCourseAdapter();
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

                // Show/hide empty state
                boolean isEmpty = courses.isEmpty();
                binding.rvCourses.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                binding.layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            }
        });

        // Observe greeting
        viewModel.greeting().observe(getViewLifecycleOwner(), greeting -> {
            if (greeting != null) {
                binding.tvStudentHomeGreeting.setText(greeting);
            }
        });

        // Observe sub greeting
        viewModel.subGreeting().observe(getViewLifecycleOwner(), subGreeting -> {
            if (subGreeting != null) {
                binding.tvStudentHomeSubGreeting.setText(subGreeting);
            }
        });

        // Observe user profile
        viewModel.userProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {

            }
        });

        // Observe success messages
        viewModel.successMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Log.d("StudentHome", message);
            }
        });

        // Observe errors
        viewModel.errorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();

                if (error.contains("network") || error.contains("connection")) {
                    showRetryDialog();
                }
            }
        });
    }

    private void showRetryDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Connection Error")
                .setMessage("Unable to load courses. Would you like to retry?")
                .setPositiveButton("Retry", (dialog, which) -> {
                    viewModel.loadCourses();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onCourseClick(StudentCourse course) {
        Toast.makeText(requireContext(), "Clicked: " + course.getName(), Toast.LENGTH_SHORT).show();
        viewModel.onCourseClicked(course);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
