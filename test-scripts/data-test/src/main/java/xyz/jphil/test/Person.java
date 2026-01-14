package xyz.jphil.test;

import xyz.jphil.datahelper.Data;

import java.util.List;

/**
 * Test class for @Data annotation.
 * This should generate Person_A sealed abstract class.
 */
@Data
public final class Person extends Person_A {
    String name;
    String email;
    Integer age;
    List<String> hobbies;
}
