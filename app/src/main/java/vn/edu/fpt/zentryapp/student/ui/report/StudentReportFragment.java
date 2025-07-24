package vn.edu.fpt.zentryapp.student.ui.report;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.databinding.FragmentStudentReportBinding;
import vn.edu.fpt.zentryapp.student.adapter.StudentReportAdapter;
import vn.edu.fpt.zentryapp.student.data.model.response.StudentReport;

public class StudentReportFragment extends Fragment implements StudentReportAdapter.OnReportClickListener {

    private FragmentStudentReportBinding binding;
    private StudentReportViewModel viewModel;
    private StudentReportAdapter reportAdapter;
    private NavController navController;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentReportBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = NavHostFragment.findNavController(this);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(StudentReportViewModel.class);
        AuthManager authManager = AuthManager.getInstance(requireContext());
        viewModel.init(authManager);

        setupRecyclerView();
        observeViewModel();
    }

    private void setupRecyclerView() {
        reportAdapter = new StudentReportAdapter();
        reportAdapter.setOnReportClickListener(this);

        binding.rvReports.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvReports.setAdapter(reportAdapter);
    }

    private void observeViewModel() {
        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        viewModel.reports().observe(getViewLifecycleOwner(), reports -> {
            if (reports != null) {
                reportAdapter.setReports(reports);

                boolean isEmpty = reports.isEmpty();
                binding.rvReports.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                binding.layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.greeting().observe(getViewLifecycleOwner(), greeting -> {
            if (greeting != null) {
                binding.tvStudentReportGreeting.setText(greeting);
            }
        });

        viewModel.subGreeting().observe(getViewLifecycleOwner(), subGreeting -> {
            if (subGreeting != null) {
                binding.tvStudentReportSubGreeting.setText(subGreeting);
            }
        });

        viewModel.userProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                Log.d("StudentReport", "User loaded: " + profile.getName() + " (" + profile.getRole() + ")");
            }
        });

        viewModel.successMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Log.d("StudentReport", message);
            }
        });

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
                .setMessage("Unable to load reports. Would you like to retry?")
                .setPositiveButton("Retry", (dialog, which) -> {
                    viewModel.loadReports();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onReportClick(StudentReport report) {
        Toast.makeText(requireContext(), "Clicked: " + report.getCourseName(), Toast.LENGTH_SHORT).show();
        // Navigate to session list
        navController.navigate(R.id.action_studentReport_to_listSession);
        viewModel.onReportClicked(report);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
