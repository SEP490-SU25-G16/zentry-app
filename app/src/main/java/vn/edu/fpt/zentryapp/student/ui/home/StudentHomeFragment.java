package vn.edu.fpt.zentryapp.student.ui.home;

import android.os.Bundle;
import android.view.*;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.annotation.*;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.*;

import vn.edu.fpt.zentryapp.R;
import vn.edu.fpt.zentryapp.databinding.FragmentStudentHomeBinding;
import vn.edu.fpt.zentryapp.lecturer.adapter.ExamAdapter;
import vn.edu.fpt.zentryapp.lecturer.adapter.SessionAdapter;
import vn.edu.fpt.zentryapp.lecturer.adapter.WeeklyAdapter;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.ExamModel;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.SessionModel;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.WeeklyModel;
import vn.edu.fpt.zentryapp.student.data.model.response.*;

public class StudentHomeFragment extends Fragment {

    private FragmentStudentHomeBinding b;
    private StudentHomeViewModel vm;

    private int examPos = 0, weeklyPos = 0;
    private boolean showAllSessions = false;
    private java.util.List<SessionModel> cachedSessions = java.util.Collections.emptyList();

    @Override public View onCreateView(@NonNull LayoutInflater i, ViewGroup c, Bundle s){
        b = FragmentStudentHomeBinding.inflate(i, c, false);
        return b.getRoot();
    }

    @Override public void onViewCreated(@NonNull View v, Bundle s){
        vm = new ViewModelProvider(this).get(StudentHomeViewModel.class);
        vm.loadMockData();
        observeVm();
    }

    /* ---------- Observe ---------- */
    private void observeVm(){
        vm.exams().observe(getViewLifecycleOwner(), this::setupExamSlider);
        vm.sessions().observe(getViewLifecycleOwner(), list -> setupSessions(list, false));
        vm.weekly().observe(getViewLifecycleOwner(), this::setupWeeklySlider);
    }

    /* ---------- Exam slider ---------- */
    private void setupExamSlider(java.util.List<ExamModel> list){
        b.rvExams.setLayoutManager(new LinearLayoutManager(getContext(),RecyclerView.HORIZONTAL,false));
        b.rvExams.setAdapter(new ExamAdapter(list));
        new PagerSnapHelper().attachToRecyclerView(b.rvExams);

        buildDots(list.size(), b.dotsIndicator, examPos);
        b.rvExams.addOnScrollListener(scrollListener(p -> {
            examPos = p; updateDots(b.dotsIndicator,p);
        }));
    }

    /* ---------- Sessions ---------- */
    private void setupSessions(java.util.List<SessionModel> list, boolean rebuild){
        if(!rebuild) cachedSessions = list;
        b.sessionsContainer.removeAllViews();

        int count = showAllSessions ? list.size() : 1;
        for(int i=0;i<count;i++){
            View v = SessionAdapter.createSessionViews(b.sessionsContainer,list).get(i);
            if(i<count-1) addBottomMargin(v,12);
            b.sessionsContainer.addView(v);
        }

        b.btnSeeAllSessions.setText(showAllSessions ? "Show Less" : "See All");
        b.btnSeeAllSessions.setOnClickListener(v -> {
            showAllSessions = !showAllSessions;
            setupSessions(cachedSessions,true);
        });
    }

    /* ---------- Weekly slider ---------- */
    private void setupWeeklySlider(java.util.List<WeeklyModel> list){
        b.rvWeekly.setLayoutManager(new LinearLayoutManager(getContext(),RecyclerView.HORIZONTAL,false));
        b.rvWeekly.setAdapter(new WeeklyAdapter(list));
        new PagerSnapHelper().attachToRecyclerView(b.rvWeekly);

        buildDots(list.size(), b.weeklyDotsIndicator, weeklyPos);
        b.rvWeekly.addOnScrollListener(scrollListener(p -> {
            weeklyPos = p; updateDots(b.weeklyDotsIndicator,p);
        }));
    }

    /* ---------- Utilities ---------- */
    private RecyclerView.OnScrollListener scrollListener(java.util.function.IntConsumer cb){
        return new RecyclerView.OnScrollListener(){
            @Override public void onScrollStateChanged(@NonNull RecyclerView rv,int st){
                if(st==RecyclerView.SCROLL_STATE_IDLE){
                    int p=((LinearLayoutManager)rv.getLayoutManager())
                            .findFirstCompletelyVisibleItemPosition();
                    if(p!=RecyclerView.NO_POSITION) cb.accept(p);
                }
            }
        };
    }

    private void buildDots(int count, LinearLayout container, int active){
        container.removeAllViews();
        for(int i=0;i<count;i++){
            ImageView d = createDot(); updateDot(d,i==active); container.addView(d);
        }
    }
    private void updateDots(LinearLayout c,int act){
        for(int i=0;i<c.getChildCount();i++)
            updateDot((ImageView)c.getChildAt(i),i==act);
    }
    private ImageView createDot(){
        ImageView d=new ImageView(requireContext());
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(8,0,8,0); d.setLayoutParams(p); return d;
    }
    private void updateDot(ImageView d,boolean a){ d.setBackground(
            ContextCompat.getDrawable(requireContext(),a?R.drawable.dot_active:R.drawable.dot_inactive)); }
    private void addBottomMargin(View v,int dp){
        LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)v.getLayoutParams();
        p.bottomMargin=(int)(dp*getResources().getDisplayMetrics().density); v.setLayoutParams(p);
    }

    @Override public void onDestroyView(){ super.onDestroyView(); b=null; }
}
