package vn.edu.fpt.zentryapp.student.data.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor

public class ClassDetail {
    private final String id;
    private final String className;
    private final String grade;
    private final String subject;
    private final String duration;
    private final String timeRemaining;
    private final String lecturer;
    private final String room;
    private final String schedule;
    private final int totalStudents;
    private final int presentStudents;


    public String getStudentStats() {
        return presentStudents + "/" + totalStudents + " Students";
    }
}
