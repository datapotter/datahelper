package xyz.jphil.arcadedb.datahelper.test;

import xyz.jphil.arcadedb.datahelper.ArcadeData;
import xyz.jphil.arcadedb.datahelper.TypeDef;
import xyz.jphil.datahelper.DataField;

/**
 * Test entity for DOCUMENT type with LINK field.
 * Demonstrates manual LINK field declaration with $ prefix.
 */
@ArcadeData  // Default type = DOCUMENT
public final class Order extends Order_A {
    String orderId;
    Double amount;
    String orderDate;  // Using String instead of LocalDateTime for now

    // LINK field - manually declared with $ prefix
    static DataField<Order, Person> $customerRef =
        new DataField<>("customerRef", Person.class);

    public static TypeDef<Order> TYPEDEF =
        schemaBuilder()
            .unique($orderId)
            .addLinkType($customerRef)
            .__();
}
