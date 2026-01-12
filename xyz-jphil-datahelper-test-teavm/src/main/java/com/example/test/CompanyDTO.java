package com.example.test;

import lombok.Data;
import org.teavm.jso.JSProperty;
import xyz.jphil.datahelper.DataHelper;

/**
 * Company DTO - will be embedded in PersonDTO
 */
@DataHelper(
    propertyAnnotations = {JSProperty.class}
)
@Data
public class CompanyDTO implements CompanyDTO_I<CompanyDTO> {
    String name;
    String department;
    AddressDTO address; // Nested object
    Integer employeeCount;
}
