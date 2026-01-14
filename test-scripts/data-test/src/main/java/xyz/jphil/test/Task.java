package xyz.jphil.test;

import xyz.jphil.datahelper.Data;

/**
 * Test class for @Data annotation with boolean fields.
 * Tests both primitive boolean and Boolean wrapper.
 */
@Data
public final class Task extends Task_A {
    String title;
    boolean done;           // primitive boolean
    Boolean active;         // Boolean wrapper
    boolean isCompleted;    // field starting with "is"
}
