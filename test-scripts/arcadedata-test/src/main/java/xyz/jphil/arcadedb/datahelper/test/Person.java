package xyz.jphil.arcadedb.datahelper.test;

import xyz.jphil.arcadedb.datahelper.ArcadeData;
import xyz.jphil.arcadedb.datahelper.ArcadeType;
import xyz.jphil.arcadedb.datahelper.TypeDef;

/**
 * Test entity for VERTEX type.
 * Represents a person node in a graph.
 */
@ArcadeData(type = ArcadeType.VERTEX)
public final class Person extends Person_A {
    String name;
    String email;
    Integer age;

    public static TypeDef<Person> TYPEDEF =
        schemaBuilder()
            .unique($email)
            .__();
}
