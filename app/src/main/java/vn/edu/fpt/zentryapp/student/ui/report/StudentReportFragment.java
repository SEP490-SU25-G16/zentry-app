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
        setupSearchBar();
    }

    private void setupSearchBar() {
        binding.etStudentSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                reportAdapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) { }
        });

        binding.ivStudentSearch.setOnClickListener(v -> {
            String query = binding.etStudentSearch.getText().toString();
            reportAdapter.filter(query);
        });
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

        viewModel.errorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onReportClick(StudentReport report) {
        Bundle args = new Bundle();
        args.putSerializable("studentReport", report);

        navController.navigate(R.id.action_studentReport_to_listSession, args);    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
