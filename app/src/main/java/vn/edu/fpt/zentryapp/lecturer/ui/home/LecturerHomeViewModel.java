package vn.edu.fpt.zentryapp.lecturer.ui.home;

import androidx.lifecycle.*;
import android.os.Handler;
import java.util.*;

import vn.edu.fpt.zentryapp.lecturer.data.model.response.ExamModel;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.SessionModel;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.WeeklyModel;

public class LecturerHomeViewModel extends ViewModel {

    private final MutableLiveData<List<ExamModel>>  _exams    = new MutableLiveData<>();
    private final MutableLiveData<List<SessionModel>> _sessions = new MutableLiveData<>();
    private final MutableLiveData<List<WeeklyModel>> _weekly   = new MutableLiveData<>();
    public LiveData<List<ExamModel>>  exams()    { return _exams; }
    public LiveData<List<SessionModel>> sessions(){ return _sessions; }
    public LiveData<List<WeeklyModel>> weekly()  { return _weekly; }

    public void loadMockData(){
        new Handler().postDelayed(() -> {
            _exams.setValue(mockExams());
            _sessions.setValue(mockSessions());
            _weekly.setValue(mockWeekly());

        }, 600);
    }

    /* ---------- Mock generators ---------- */
    private List<ExamModel> mockExams(){
        return Arrays.asList(
                new ExamModel("Math Exam","Your upcoming Math progress test!","25/8/2024"),
                new ExamModel("Physics Exam","Physics midterm examination","28/8/2024"),
                new ExamModel("Chemistry Exam","Final chemistry assessment","30/8/2024"));
    }

    private List<SessionModel> mockSessions(){
        return Arrays.asList(
                new SessionModel("English Grade - 07","Saturday 10.00 - 12.00","02:15:45"),
                new SessionModel("Math Advanced","Monday 14.00 - 16.00","1 day left"),
                new SessionModel("Physics Lab","Wednesday 09.00 - 11.00","3 days left"));
    }

    private List<WeeklyModel> mockWeekly(){
        return Arrays.asList(
                new WeeklyModel("English Grade - 07","2/3 Presented","19/20 Sessions","90%"),
                new WeeklyModel("Math Advanced","3/4 Presented","18/20 Sessions","85%"),
                new WeeklyModel("Physics Lab","1/2 Presented","15/18 Sessions","92%"));
    }
}
