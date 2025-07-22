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

import vn.edu.fpt.zentryapp.databinding.FragmentFinalAttendanceBinding;
import vn.edu.fpt.zentryapp.lecturer.adapter.FinalAttendanceAdapter;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.FinalAttendance;

public class FinalAttendanceFragment extends Fragment implements FinalAttendanceAdapter.OnStudentClickListener {

    private FragmentFinalAttendanceBinding binding;
    private FinalAttendanceAdapter attendanceAdapter;
    private String sessionId;

    public static FinalAttendanceFragment newInstance(String sessionId) {
        FinalAttendanceFragment fragment = new FinalAttendanceFragment();
        Bundle args = new Bundle();
        args.putString("sessionId", sessionId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentFinalAttendanceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            sessionId = getArguments().getString("sessionId", "");
        }

        setupRecyclerView();
        loadFinalAttendance();
    }

    private void setupRecyclerView() {
        attendanceAdapter = new FinalAttendanceAdapter();
        attendanceAdapter.setOnStudentClickListener(this);

        binding.rvFinalAttendance.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvFinalAttendance.setAdapter(attendanceAdapter);
    }

    private void loadFinalAttendance() {
        // TODO: Load from ViewModel in parent fragment
    }

    public void updateFinalAttendance(List<FinalAttendance> finalAttendance) {
        if (attendanceAdapter != null) {
            attendanceAdapter.setFinalAttendance(finalAttendance);
        }
    }

    @Override
    public void onStudentClick(FinalAttendance student) {
        android.widget.Toast.makeText(requireContext(),
                student.getStudentName() + " - " + student.getAttendanceStatus(),
                android.widget.Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
