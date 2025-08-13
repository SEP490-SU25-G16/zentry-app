package vn.edu.fpt.zentryapp.auth.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
public class PasswordValidation {
    private String passwordError;
    private boolean isValid;
}
