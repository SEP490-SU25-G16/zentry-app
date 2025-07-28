package vn.edu.fpt.zentryapp.service;


import android.util.Log;

public class AttendanceSubmissionHandler {
    private static final String TAG = "AttendanceSubmissionHandler";

    public void submitAttendance(AttendanceModels.AttendanceSubmission submission,
                                 AttendanceCallbacks.AttendanceSubmissionCallback callback) {
        callback.onSubmissionSuccess(submission);
    }
}
