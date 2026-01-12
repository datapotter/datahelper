package com.example.performance;

import lombok.Getter;
import lombok.Setter;
import org.teavm.jso.JSProperty;
import xyz.jphil.datahelper.DataHelper;

/**
 * Simple DTO for TeaVM performance testing.
 * Contains only primitive types and String for fast serialization.
 */
@DataHelper(propertyAnnotations = {JSProperty.class})
@Getter
@Setter
public class SimpleDTOTeaVM implements SimpleDTOTeaVM_I<SimpleDTOTeaVM> {
    String name;
    int age;
    String email;
    double salary;
    int status; // Using int instead of boolean (see PRP-13)
}
