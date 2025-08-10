package vn.edu.fpt.zentryapp.lecturer.data.model.response;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class EndSessionRequest {
    private String userId;
}
