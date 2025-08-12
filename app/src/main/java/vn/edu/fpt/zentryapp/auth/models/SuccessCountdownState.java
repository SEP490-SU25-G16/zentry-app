package vn.edu.fpt.zentryapp.auth.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class SuccessCountdownState {
    private String buttonText;
    private String countdownText;
    private int remainingSeconds;
}
