package vn.edu.fpt.zentryapp.lecturer.data.model.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ClassSectionData {
    private String ClassSectionId;
    private String CourseCode;
    private String CourseName;
    private String CourseId;
    private String SectionCode;
    private int EnrolledStudents;
    private int TotalSessions;
    private String SessionProgress;
    private List<Schedule> Schedules;
    private String LecturerName;
    private String LecturerId;

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Schedule {
        private String ScheduleId;
        private String RoomId;
        private String RoomInfo;
        private String ScheduleInfo;
    }
}