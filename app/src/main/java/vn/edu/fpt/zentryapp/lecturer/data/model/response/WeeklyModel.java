package vn.edu.fpt.zentryapp.lecturer.data.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class WeeklyModel {
    public String subject, presented, sessions, attendance;

}