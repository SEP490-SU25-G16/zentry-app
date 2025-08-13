package vn.edu.fpt.zentryapp.auth.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ValidationResult {
    private boolean valid;
    private String errorMessage;
}