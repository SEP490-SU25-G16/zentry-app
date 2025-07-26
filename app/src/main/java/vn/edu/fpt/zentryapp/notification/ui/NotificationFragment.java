package vn.edu.fpt.zentryapp.notification.ui;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.notification.adapter.NotificationAdapter;
import vn.edu.fpt.zentryapp.notification.sharedviewmodel.NotificationViewModel;

public class NotificationFragment extends Fragment {
    private NotificationViewModel viewModel;
    private NotificationAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notification, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        RecyclerView recyclerView = view.findViewById(R.id.rvNotification);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new NotificationAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(NotificationViewModel.class);
        viewModel.getNotifications().observe(getViewLifecycleOwner(), notifications -> {
            adapter.setItems(notifications);
        });

        viewModel.loadNotifications();
    }
}
