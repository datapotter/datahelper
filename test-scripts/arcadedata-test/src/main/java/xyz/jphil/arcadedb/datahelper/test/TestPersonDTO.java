package xyz.jphil.arcadedb.datahelper.test;

import xyz.jphil.arcadedb.datahelper.ArcadeData;
import xyz.jphil.arcadedb.datahelper.TypeDef;
import java.util.List;
//import static xyz.jphil.arcadedb.datahelper.test.TestPersonDTO_A.*;

/**
 * Test DTO using new @ArcadeData annotation.
 * This demonstrates the simplified pattern with sealed abstract parent class.
 *
 * IMPORTANT: First compilation generates TestPersonDTO_A, second compilation succeeds.
 */
@ArcadeData
public final class TestPersonDTO extends TestPersonDTO_A {

    String name;
    String email;
    Integer age;
    List<String> hobbies;
    
    static  {
        __.chars();
    }

    /**
     * Schema definition using schemaBuilder() helper from generated parent class.
     * Notice how schemaBuilder() pre-fills class and fields!
     */
    public static final TypeDef<TestPersonDTO> TYPEDEF =
            schemaBuilder()
                .unique($email)
                .__();
}
