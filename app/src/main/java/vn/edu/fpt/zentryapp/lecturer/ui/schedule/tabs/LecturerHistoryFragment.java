package vn.edu.fpt.zentryapp.lecturer.ui.schedule.tabs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.List;

import vn.edu.fpt.zentryapp.databinding.FragmentLecturerHistoryBinding;
import vn.edu.fpt.zentryapp.lecturer.adapter.LecturerHistoryAdapter;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.Round;

public class LecturerHistoryFragment extends Fragment {

    private FragmentLecturerHistoryBinding binding;
    private LecturerHistoryAdapter historyAdapter;
    private String sessionId;
    private OnRoundClickListener roundClickListener;

    public interface OnRoundClickListener {
        void onRoundClick(Round round);
    }

    public static LecturerHistoryFragment newInstance(String sessionId) {
        LecturerHistoryFragment fragment = new LecturerHistoryFragment();
        Bundle args = new Bundle();
        args.putString("sessionId", sessionId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLecturerHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            sessionId = getArguments().getString("sessionId", "");
        }

        setupRecyclerView();
    }

    private void setupRecyclerView() {
        historyAdapter = new LecturerHistoryAdapter();

        // Set listener cho adapter
        historyAdapter.setOnRoundClickListener(round -> {
            // Khi user click round trong adapter, forward lên parent fragment
            if (roundClickListener != null) {
                roundClickListener.onRoundClick(round);
            }
        });

        binding.rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvHistory.setAdapter(historyAdapter);
    }

    // Method để set listener từ parent fragment
    public void setOnRoundClickListener(OnRoundClickListener listener) {
        this.roundClickListener = listener;
    }

    public void updateRoundHistory(List<Round> rounds) {
        if (historyAdapter != null) {
            historyAdapter.setRounds(rounds);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
