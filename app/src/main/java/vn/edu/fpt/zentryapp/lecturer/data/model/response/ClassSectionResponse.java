package vn.edu.fpt.zentryapp.lecturer.data.model.response;

import org.checkerframework.checker.units.qual.A;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ClassSectionResponse {
    private boolean Success;
    private List<ClassSectionData> Data;
    private String Error;
    private String Message;
}
