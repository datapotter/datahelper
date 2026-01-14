package xyz.jphil.arcadedb.datahelper.test;

import xyz.jphil.arcadedb.datahelper.ArcadeData;
import xyz.jphil.arcadedb.datahelper.ArcadeType;
import xyz.jphil.arcadedb.datahelper.TypeDef;
import xyz.jphil.datahelper.DataField;

/**
 * Test entity for VERTEX type with multiple LINK fields.
 * Demonstrates combined VERTEX + LINK usage.
 */
@ArcadeData(type = ArcadeType.VERTEX)
public final class Company extends Company_A {
    String name;
    String industry;

    // Multiple LINK fields
    static DataField<Company, Person> $ceoRef =
        new DataField<>("ceoRef", Person.class);

    static DataField<Company, Person> $founderRef =
        new DataField<>("founderRef", Person.class);

    public static TypeDef<Company> TYPEDEF =
        schemaBuilder()
            .unique($name)
            .addLinkType($ceoRef)
            .addLinkType($founderRef)
            .__();
}
