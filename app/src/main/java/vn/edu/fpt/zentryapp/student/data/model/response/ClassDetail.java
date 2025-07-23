package vn.edu.fpt.zentryapp.student.data.model.response;

import lombok.Getter;

@Getter
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

    public ClassDetail(String id, String className, String grade, String subject,
                       String duration, String timeRemaining, String lecturer,
                       String room, String schedule, int totalStudents, int presentStudents) {
        this.id = id;
        this.className = className;
        this.grade = grade;
        this.subject = subject;
        this.duration = duration;
        this.timeRemaining = timeRemaining;
        this.lecturer = lecturer;
        this.room = room;
        this.schedule = schedule;
        this.totalStudents = totalStudents;
        this.presentStudents = presentStudents;
    }

    public String getStudentStats() {
        return presentStudents + "/" + totalStudents + " Students";
    }
}
