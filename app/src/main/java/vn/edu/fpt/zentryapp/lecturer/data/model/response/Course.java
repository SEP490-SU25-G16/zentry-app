package vn.edu.fpt.zentryapp.lecturer.data.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Course {
    private String id;
    private String name;
    private String code;
    private String className;
    private String room;
    private int totalTasks;
    private int completedTasks;
    private String imageUrl;
    private String semester;
    private String academicYear;

    // Helper methods
    public int getProgressPercentage() {
        if (totalTasks == 0) return 0;
        return (completedTasks * 100) / totalTasks;
    }

    public String getTaskSummary() {
        return completedTasks + "/" + totalTasks + " tasks completed";
    }

    public String getClassInfo() {
        return className + " - " + room;
    }
}
