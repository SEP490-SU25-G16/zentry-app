package vn.edu.fpt.zentryapp.student.ui.schedule;

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
import vn.edu.fpt.zentryapp.databinding.FragmentStudentScheduleCalendarBinding;
import vn.edu.fpt.zentryapp.student.adapter.CalendarEventAdapter;
import vn.edu.fpt.zentryapp.student.data.model.response.CalendarEvent;

public class StudentScheduleCalendarFragment extends Fragment implements CalendarEventAdapter.OnEventClickListener {

    private FragmentStudentScheduleCalendarBinding binding;
    private StudentScheduleCalendarViewModel viewModel;
    private CalendarEventAdapter eventAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentScheduleCalendarBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(StudentScheduleCalendarViewModel.class);
        AuthManager authManager = AuthManager.getInstance(requireContext());
        viewModel.init(authManager);

        setupRecyclerView();
        setupCalendar();
        setupToolbar();
        observeViewModel();
    }

    private void setupRecyclerView() {
        eventAdapter = new CalendarEventAdapter();
        eventAdapter.setOnEventClickListener(this);

        binding.rvEvents.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvEvents.setAdapter(eventAdapter);
    }

    private void setupCalendar() {
        binding.calendarView.setOnDateChangeListener((calendarView, year, month, dayOfMonth) -> {
            viewModel.loadEventsForDate(year, month, dayOfMonth);
        });
    }

    private void setupToolbar() {
        binding.ivStudentScheduleCalendarBack.setOnClickListener(v ->
                requireActivity().onBackPressed());
    }

    private void observeViewModel() {
        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        viewModel.events().observe(getViewLifecycleOwner(), events -> {
            if (events != null) {
                eventAdapter.setEvents(events);

                boolean isEmpty = events.isEmpty();
                binding.rvEvents.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                binding.layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.successMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Log.d("CalendarFragment", message);
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
                .setMessage("Unable to load events. Would you like to retry?")
                .setPositiveButton("Retry", (dialog, which) -> {
                    viewModel.loadEventsForToday();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onEventClick(CalendarEvent event) {
        Toast.makeText(requireContext(), "Clicked: " + event.getTitle(), Toast.LENGTH_SHORT).show();
        viewModel.onEventClicked(event);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
