package xyz.jphil.arcadedb.datahelper.test;

import xyz.jphil.arcadedb.datahelper.ArcadeData;
import xyz.jphil.arcadedb.datahelper.TypeDef;

/**
 * Test DTO for phone number - will be used in lists.
 * This demonstrates List<CustomDataObj> support.
 */
@ArcadeData
public final class PhoneNumberDTO extends PhoneNumberDTO_A {

    String type;  // "home", "work", "mobile"
    String number;
    Boolean isPrimary;

    /**
     * Schema definition - embedded types usually don't need indexes.
     */
    public static final TypeDef<PhoneNumberDTO> TYPEDEF = schemaBuilder().__();
}
