package xyz.jphil.arcadedb.datahelper.test;

import xyz.jphil.arcadedb.datahelper.ArcadeData;
import xyz.jphil.arcadedb.datahelper.ArcadeType;
import xyz.jphil.arcadedb.datahelper.TypeDef;

/**
 * Test entity for EDGE type.
 * Represents a "knows" relationship between two Person vertices.
 */
@ArcadeData(type = ArcadeType.EDGE)
public final class Knows extends Knows_A {
    String since;  // Using String instead of LocalDate for now
    String strength;  // "strong", "weak", etc.

    public static TypeDef<Knows> TYPEDEF = schemaBuilder().__();
}
