package vn.edu.fpt.zentryapp.auth.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@Getter
@NoArgsConstructor
@Setter
public class PasswordStrength {
    private int progress; // 0-100
    private String feedback;
    private int color;
    private StrengthLevel level;

    public enum StrengthLevel {
        WEAK, FAIR, GOOD, STRONG
    }
}
