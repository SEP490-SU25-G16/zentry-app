package vn.edu.fpt.zentryapp.lecturer.ui.schedule;


import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.os.Handler;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import vn.edu.fpt.zentryapp.auth.client.AuthManager;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.AttendanceRound;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.FinalAttendance;
import vn.edu.fpt.zentryapp.lecturer.data.model.response.SessionDetailInfoRound;

public class LecturerScheduleClassDetailViewModel extends ViewModel {

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<SessionDetailInfoRound> _sessionInfo = new MutableLiveData<>();
    private final MutableLiveData<List<AttendanceRound>> _attendanceRounds = new MutableLiveData<>();
    private final MutableLiveData<List<FinalAttendance>> _finalAttendance = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _canAddFaceId = new MutableLiveData<>(true);

    public LiveData<Boolean> canAddFaceId() {
        return _canAddFaceId;
    }

    private boolean isSessionCompleted(SessionDetailInfoRound sessionInfo) {
        if (sessionInfo == null) return false;

        String status = sessionInfo.getStatus();
        return "COMPLETED".equals(status) ||
                "FINISHED".equals(status) ||
                "ENDED".equals(status);
    }


    public LiveData<Boolean> isLoading() {
        return _isLoading;
    }

    public LiveData<SessionDetailInfoRound> sessionInfo() {
        return _sessionInfo;
    }

    public LiveData<List<AttendanceRound>> attendanceRounds() {
        return _attendanceRounds;
    }

    public LiveData<List<FinalAttendance>> finalAttendance() {
        return _finalAttendance;
    }

    public LiveData<String> errorMessage() {
        return _errorMessage;
    }

    private AuthManager authManager;
    private String sessionId;

    public void init(AuthManager authManager, String sessionId) {
        this.authManager = authManager;
        this.sessionId = sessionId;

        loadSessionInfo();
        loadAttendanceData();
    }

    private void loadSessionInfo() {
        _isLoading.setValue(true);

        new Handler().postDelayed(() -> {
            SessionDetailInfoRound sessionInfo = generateSessionInfo();
            _sessionInfo.setValue(sessionInfo);

            // Cập nhật trạng thái có thể thêm round
            boolean canAddFaceId = !isSessionCompleted(sessionInfo);
            _canAddFaceId.setValue(canAddFaceId);
        }, 500);
    }

    private void loadAttendanceData() {
        new Handler().postDelayed(() -> {
            List<AttendanceRound> rounds = generateAttendanceRounds();
            List<FinalAttendance> finalAttendance = generateFinalAttendance();

            _attendanceRounds.setValue(rounds);
            _finalAttendance.setValue(finalAttendance);
            _isLoading.setValue(false);
        }, 1000);
    }

    private SessionDetailInfoRound generateSessionInfo() {
        SessionDetailInfoRound info = new SessionDetailInfoRound();
        info.setSessionId(sessionId);
        info.setCourseCode("CSE101");
        info.setCourseName("Lập trình căn bản");
        info.setClassName("SE1801");
        info.setRoom("DE-201");
        info.setSessionDate(Calendar.getInstance().getTime());

        Calendar start = Calendar.getInstance();
        start.set(Calendar.HOUR_OF_DAY, 8);
        start.set(Calendar.MINUTE, 0);
        info.setStartTime(start.getTime());

        Calendar end = Calendar.getInstance();
        end.set(Calendar.HOUR_OF_DAY, 9);
        end.set(Calendar.MINUTE, 30);
        info.setEndTime(end.getTime());

        info.setTotalStudents(32);
        info.setTotalRounds(4);
        info.setStatus("IN PROGRESS");
        info.setDuration(90 * 60 * 1000L); // 1h30m

        return info;
    }

    private List<AttendanceRound> generateAttendanceRounds() {
        List<AttendanceRound> rounds = new ArrayList<>();
        Random random = new Random();

        Calendar baseTime = Calendar.getInstance();
        baseTime.set(Calendar.HOUR_OF_DAY, 8);
        baseTime.set(Calendar.MINUTE, 0);

        String[] roundTypes = {"START", "MIDDLE", "MIDDLE", "END"};

        for (int i = 1; i <= 4; i++) {
            AttendanceRound round = new AttendanceRound();
            round.setRoundId("R" + sessionId + "_" + i);
            round.setSessionId(sessionId);
            round.setRoundNumber(i);
            round.setTimestamp(baseTime.getTime());
            round.setTotalStudents(32);
            round.setPresentStudents(28 + random.nextInt(4)); // 28-31 students
            round.setRoundType(roundTypes[i - 1]);
            round.setLocation("DE-201");

            rounds.add(round);
            baseTime.add(Calendar.MINUTE, 20); // Next round after 20 minutes
        }

        return rounds;
    }

    private List<FinalAttendance> generateFinalAttendance() {
        List<FinalAttendance> finalAttendance = new ArrayList<>();
        Random random = new Random();

        String[] firstNames = {"Tharidu", "Hasitha", "Saman", "Kasun", "Nuwan", "Chamika"};
        String[] lastNames = {"Silva", "Perera", "Fernando", "Jayawardena", "Gunasekara"};

        for (int i = 1; i <= 32; i++) {
            FinalAttendance student = new FinalAttendance();
            student.setStudentId("ST" + String.format("%03d", i));
            student.setStudentCode("HS" + String.format("%06d", 1000 + i));

            String firstName = firstNames[random.nextInt(firstNames.length)];
            String lastName = lastNames[random.nextInt(lastNames.length)];
            student.setStudentName(firstName + " " + lastName);

            student.setEmail(student.getStudentCode().toLowerCase() + "@student.edu");
            student.setTotalRounds(4);
            student.setAttendedRounds(random.nextInt(4) + 1); // 1-4 rounds
            student.setFinalStatus(student.getAttendedRounds() >= 3); // Need 3/4 rounds to be considered attended

            finalAttendance.add(student);
        }

        return finalAttendance;
    }
}
