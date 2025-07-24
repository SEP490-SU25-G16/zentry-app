package vn.edu.fpt.zentryapp.student.ui.schedule.tabs;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.databinding.FragmentClassHistoryBinding;
import vn.edu.fpt.zentryapp.student.adapter.ClassHistoryAdapter;
import vn.edu.fpt.zentryapp.student.data.model.response.ClassSession;

public class ClassHistoryFragment extends Fragment implements ClassHistoryAdapter.OnSessionClickListener {

    private FragmentClassHistoryBinding binding;
    private ClassHistoryViewModel viewModel;
    private ClassHistoryAdapter historyAdapter;
    private String classId;

    public static ClassHistoryFragment newInstance(String classId) {
        ClassHistoryFragment fragment = new ClassHistoryFragment();
        Bundle args = new Bundle();
        args.putString("classId", classId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            classId = getArguments().getString("classId");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentClassHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ClassHistoryViewModel.class);
        AuthManager authManager = AuthManager.getInstance(requireContext());
        viewModel.init(authManager, classId);

        setupRecyclerView();
        observeViewModel();
    }

    private void setupRecyclerView() {
        historyAdapter = new ClassHistoryAdapter();
        historyAdapter.setOnSessionClickListener(this);

        binding.rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvHistory.setAdapter(historyAdapter);
    }

    private void observeViewModel() {
        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        viewModel.sessions().observe(getViewLifecycleOwner(), sessions -> {
            if (sessions != null) {
                historyAdapter.setSessions(sessions);

                boolean isEmpty = sessions.isEmpty();
                binding.rvHistory.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                binding.layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.errorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.successMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Log.d("ClassHistory", message);
            }
        });
    }

    @Override
    public void onSessionClick(ClassSession session) {
      //  Toast.makeText(requireContext(), "Clicked: " + session.getTitle(), Toast.LENGTH_SHORT).show();
        viewModel.onSessionClicked(session);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
