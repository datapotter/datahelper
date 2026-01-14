package xyz.jphil.arcadedb.datahelper.test;

import xyz.jphil.arcadedb.datahelper.ArcadeData;
import xyz.jphil.arcadedb.datahelper.TypeDef;

/**
 * Test DTO for embedded address object.
 * This demonstrates a simple embedded type.
 */
@ArcadeData
public final class AddressDTO extends AddressDTO_A {

    String street;
    String city;
    String state;
    String zipCode;
    String country;

    /**
     * Schema definition - embedded types usually don't need indexes.
     */
    public static final TypeDef<AddressDTO> TYPEDEF = schemaBuilder().__();
}
