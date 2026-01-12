package com.example.test;

import lombok.Data;
import xyz.jphil.datahelper.DataHelper;

/**
 * Test DTO WITHOUT JSObject or JSProperty annotations.
 * This will show what happens without TeaVM-specific annotations.
 */
@DataHelper
// NO propertyAnnotations, NO superInterfaces
@Data
public class PlainTestDTO implements PlainTestDTO_I<PlainTestDTO> {
    String firstName;
    String lastName;
    int age;
    String email;
    
    
}
