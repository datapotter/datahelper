package com.example.test;

import lombok.Data;
import org.teavm.jso.JSProperty;
import xyz.jphil.datahelper.DataHelper;

/**
 * Address DTO - will be embedded in PersonDTO
 */
@DataHelper(
    propertyAnnotations = {JSProperty.class}
)
@Data
public class AddressDTO implements AddressDTO_I<AddressDTO> {
    String street;
    String city;
    String state;
    String zipCode;
    String country;
}
