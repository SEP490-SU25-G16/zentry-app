package vn.edu.fpt.zentryapp;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import java.util.List;
import java.util.ArrayList;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.AppBarLayout;

public class MainActivity2 extends AppCompatActivity {

    private RecyclerView rvExams;
    private ExamAdapter examAdapter;
    private List<ExamModel> examList;
    private LinearLayout dotsIndicator;
    private int currentPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main2);

        // Khởi tạo views
        initViews();

        // Setup exam slider
        setupExamSlider();

        // Setup AppBar scroll effects
        setupAppBarEffects();
    }

    private void initViews() {
        rvExams = findViewById(R.id.rvExams);
        dotsIndicator = findViewById(R.id.dotsIndicator);
    }

    private void setupExamSlider() {
        // Tạo dữ liệu mẫu
        examList = new ArrayList<>();
        examList.add(new ExamModel("Math Exam", "Your upcoming Math progress test!", "25/8/2024"));
        examList.add(new ExamModel("Physics Exam", "Physics midterm examination", "28/8/2024"));
        examList.add(new ExamModel("Chemistry Exam", "Final chemistry assessment", "30/8/2024"));
        examList.add(new ExamModel("English Exam", "Speaking and writing test", "2/9/2024"));
        examList.add(new ExamModel("History Exam", "World history final exam", "5/9/2024"));

        // Setup adapter
        examAdapter = new ExamAdapter(examList);

        // Setup RecyclerView với horizontal scroll
        LinearLayoutManager layoutManager = new LinearLayoutManager(this,
                LinearLayoutManager.HORIZONTAL, false);
        rvExams.setLayoutManager(layoutManager);
        rvExams.setAdapter(examAdapter);

        // Thêm PagerSnapHelper để có hiệu ứng snap như ViewPager
        PagerSnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(rvExams);

        // Setup dots indicator
        setupDotsIndicator();

        // Lắng nghe scroll để update dots
        rvExams.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    updateDotsIndicator();
                }
            }
        });
    }


    private void setupDotsIndicator() {
        dotsIndicator.removeAllViews();

        for (int i = 0; i < examList.size(); i++) {
            ImageView dot = new ImageView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 0, 8, 0);
            dot.setLayoutParams(params);

            if (i == currentPosition) {
                dot.setBackground(ContextCompat.getDrawable(this, R.drawable.dot_active));
            } else {
                dot.setBackground(ContextCompat.getDrawable(this, R.drawable.dot_inactive));
            }

            dotsIndicator.addView(dot);
        }
    }

    private void updateDotsIndicator() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) rvExams.getLayoutManager();
        if (layoutManager != null) {
            int firstVisiblePosition = layoutManager.findFirstCompletelyVisibleItemPosition();
            if (firstVisiblePosition != RecyclerView.NO_POSITION && firstVisiblePosition != currentPosition) {
                currentPosition = firstVisiblePosition;

                // Update dots
                for (int i = 0; i < dotsIndicator.getChildCount(); i++) {
                    ImageView dot = (ImageView) dotsIndicator.getChildAt(i);
                    if (i == currentPosition) {
                        dot.setBackground(ContextCompat.getDrawable(this, R.drawable.dot_active));
                    } else {
                        dot.setBackground(ContextCompat.getDrawable(this, R.drawable.dot_inactive));
                    }
                }
            }
        }
    }

    private void setupAppBarEffects() {
        // Code AppBar effects cũ của bạn
        AppBarLayout appBarLayout = findViewById(R.id.appbar);
        final ImageView bgExpanded = findViewById(R.id.bg_expanded);
        final ImageView bgCollapsed = findViewById(R.id.bg_collapsed);
        final TextView tvCollapsedTitle = findViewById(R.id.tvCollapsedTitle);

        // Trạng thái ban đầu
        if (bgExpanded != null) {
            bgExpanded.setAlpha(1f);
        }
        if (bgCollapsed != null) {
            bgCollapsed.setAlpha(0f);
        }
        if (tvCollapsedTitle != null) {
            tvCollapsedTitle.setAlpha(0f);
        }

        // Lắng nghe scroll để tạo hiệu ứng fade
        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                int range = appBarLayout.getTotalScrollRange();

                if (range == 0) return;

                // Tính phần trăm cuộn
                float percent = Math.abs((float) verticalOffset / (float) range);
                percent = Math.max(0f, Math.min(1f, percent));

                // Khi AppBarLayout scroll lên (biến mất), collapsed xuất hiện
                float expandedAlpha = 1f - percent;
                float collapsedAlpha = percent;

                if (bgExpanded != null) {
                    bgExpanded.setAlpha(expandedAlpha);
                }
                if (bgCollapsed != null) {
                    bgCollapsed.setAlpha(collapsedAlpha);
                }
                if (tvCollapsedTitle != null) {
                    tvCollapsedTitle.setAlpha(collapsedAlpha);
                }
            }
        });
    }

    // Model class cho Exam
    public static class ExamModel {
        private String title;
        private String description;
        private String date;

        public ExamModel(String title, String description, String date) {
            this.title = title;
            this.description = description;
            this.date = date;
        }

        // Getters
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getDate() { return date; }
    }

    // Adapter class cho RecyclerView
    public class ExamAdapter extends RecyclerView.Adapter<ExamAdapter.ExamViewHolder> {
        private List<ExamModel> examList;

        public ExamAdapter(List<ExamModel> examList) {
            this.examList = examList;
        }

        @NonNull
        @Override
        public ExamViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_exam, parent, false);

            // Set width của item bằng width của parent (full screen)
            RecyclerView.LayoutParams layoutParams = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            view.setLayoutParams(layoutParams);

            return new ExamViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ExamViewHolder holder, int position) {
            ExamModel exam = examList.get(position);
            holder.tvExamTitle.setText(exam.getTitle());
            holder.tvExamDescription.setText(exam.getDescription());
            holder.tvExamDate.setText(exam.getDate());
        }

        @Override
        public int getItemCount() {
            return examList != null ? examList.size() : 0;
        }

        class ExamViewHolder extends RecyclerView.ViewHolder {
            TextView tvExamTitle, tvExamDescription, tvExamDate;

            public ExamViewHolder(@NonNull View itemView) {
                super(itemView);
                tvExamTitle = itemView.findViewById(R.id.tvExamTitle);
                tvExamDescription = itemView.findViewById(R.id.tvExamDescription);
                tvExamDate = itemView.findViewById(R.id.tvExamDate);
            }
        }
    }
}
