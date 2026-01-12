package com.example.performance;

import lombok.Getter;
import lombok.Setter;
import xyz.jphil.datahelper.DataHelper;

/**
 * Simple DTO for baseline performance testing.
 * Contains only primitive types and String for fast serialization.
 */
@DataHelper
@Getter
@Setter
public class SimpleDTO implements SimpleDTO_I<SimpleDTO> {
    String name;
    int age;
    String email;
    double salary;
    int status; // Using int instead of boolean to avoid Lombok isXxx() vs getXxx() conflict (see PRP-13)
}
