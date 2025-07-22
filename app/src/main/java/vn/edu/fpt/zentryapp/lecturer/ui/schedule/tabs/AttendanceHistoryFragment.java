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

import vn.edu.fpt.zentryapp.databinding.FragmentAttendanceHistoryBinding;
import vn.edu.fpt.zentryapp.lecturer.adapter.AttendanceRoundAdapter;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.AttendanceRound;

public class AttendanceHistoryFragment extends Fragment implements AttendanceRoundAdapter.OnRoundClickListener {

    private FragmentAttendanceHistoryBinding binding;
    private AttendanceRoundAdapter roundAdapter;
    private String sessionId;

    public static AttendanceHistoryFragment newInstance(String sessionId) {
        AttendanceHistoryFragment fragment = new AttendanceHistoryFragment();
        Bundle args = new Bundle();
        args.putString("sessionId", sessionId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAttendanceHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            sessionId = getArguments().getString("sessionId", "");
        }

        setupRecyclerView();
        loadRoundHistory();
    }

    private void setupRecyclerView() {
        roundAdapter = new AttendanceRoundAdapter();
        roundAdapter.setOnRoundClickListener(this);

        binding.rvRounds.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvRounds.setAdapter(roundAdapter);
    }

    private void loadRoundHistory() {
        // TODO: Load from ViewModel in parent fragment
        // This will be called from parent fragment
    }

    public void updateRoundHistory(List<AttendanceRound> rounds) {
        if (roundAdapter != null) {
            roundAdapter.setRounds(rounds);
        }
    }

    @Override
    public void onRoundClick(AttendanceRound round) {
        android.widget.Toast.makeText(requireContext(),
                "Round " + round.getRoundNumber() + " - " + round.getAttendanceDisplay(),
                android.widget.Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
