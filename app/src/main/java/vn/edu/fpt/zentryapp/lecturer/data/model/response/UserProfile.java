package vn.edu.fpt.zentryapp.lecturer.data.model.response;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
public class UserProfile implements Serializable {
    private String userId;
    private String accountId;
    private String code;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String role;
    private String status;
    private String createdAt;
    private boolean hasFaceId;
    private String faceIdLastUpdated;

    public String getFormattedCreatedDate() {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date date = inputFormat.parse(createdAt);
            return date != null ? outputFormat.format(date) : createdAt;
        } catch (ParseException e) {
            return createdAt;
        }
    }

    public String getFormattedPhoneNumber() {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return "Not provided";
        }
        return phoneNumber;
    }

    public String getRoleDisplayName() {
        if (role == null) return "Unknown";

        switch (role.toLowerCase()) {
            case "lecturer":
                return "Lecturer";
            case "student":
                return "Student";
            case "admin":
                return "Administrator";
            default:
                return role;
        }
    }

    public String getStatusDisplayName() {
        if (status == null) return "Unknown";

        switch (status.toLowerCase()) {
            case "active":
                return "Active";
            case "inactive":
                return "Inactive";
            case "suspended":
                return "Suspended";
            default:
                return status;
        }
    }

    public String getFaceIdStatus() {
        return hasFaceId ? "Enabled" : "Not Set";
    }
}
