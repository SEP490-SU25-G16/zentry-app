package vn.edu.fpt.zentryapp.lecturer.adapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.List;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.SessionModel;

/** Sinh View thủ công, trả về list View để Fragment add vào LinearLayout */
public class SessionAdapter {
    public static List<View> createSessionViews(ViewGroup parent, List<SessionModel> data){
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        java.util.ArrayList<View> views = new java.util.ArrayList<>();
        for (SessionModel s : data){
            View v = inf.inflate(R.layout.item_session_test, parent, false);
            ((TextView)v.findViewById(R.id.tvSessionTitle)).setText(s.title);
            ((TextView)v.findViewById(R.id.tvSessionSchedule)).setText(s.schedule);
            ((TextView)v.findViewById(R.id.tvTimer)).setText(s.timer);
            views.add(v);
        }
        return views;
    }
}
