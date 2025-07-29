package vn.edu.fpt.zentryapp.lecturer.data.model.response;

import com.google.gson.annotations.SerializedName;

// StartSessionResponse.java
public class StartSessionResponse {
    @SerializedName("Success")
    private boolean success;

    @SerializedName("Error")
    private String error;

    @SerializedName("Message")
    private String message;

    @SerializedName("Data")
    private SessionStartData data;

    // Getters
    public boolean isSuccess() { return success; }
    public String getError() { return error; }
    public String getMessage() { return message; }
    public SessionStartData getData() { return data; }
}