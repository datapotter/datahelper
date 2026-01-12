///usr/bin/env jbang "$0" "$@" ; exit $?
// Standalone Java code (not part of main project) - replaces bash/python/batch scripts with IDE-friendly, maintainable code using JDK 11/21/25 enhancements. To know why, refer to Cay Horstmann's JavaOne 2025 talk "Java for Small Coding Tasks" (https://youtu.be/04wFgshWMdA)

//DEPS io.github.xyz-jphil:xyz-jphil-datahelper-base:1.0
//DEPS io.github.xyz-jphil:xyz-jphil-datahelper-annotations:1.0
//DEPS org.projectlombok:lombok:1.18.42

//FILES ../xyz-jphil-datahelper-test/target/classes

import java.util.*;
import java.lang.reflect.*;

public class MapSupportJavaTest {

    public static void main(String[] args) {
        System.out.println("=== Map Support Java Test ===\n");

        try {
            // Load the compiled MapTestDTO class
            Class<?> mapTestDTOClass = Class.forName("com.example.test.MapTestDTO");
            Class<?> personDTOClass = Class.forName("com.example.test.PersonDTO");

            System.out.println("✓ Successfully loaded MapTestDTO and PersonDTO classes\n");

            // Test 1: Map<String, String>
            test1_MapStringString(mapTestDTOClass);

            // Test 2: Map<String, Integer>
            test2_MapStringInteger(mapTestDTOClass);

            // Test 3: Map<Integer, String>
            test3_MapIntegerString(mapTestDTOClass);

            // Test 4: Map<String, PersonDTO>
            test4_MapStringDataHelper(mapTestDTOClass, personDTOClass);

            // Test 5: HashMap vs LinkedHashMap
            test5_MapImplementations(mapTestDTOClass);

            // Test 6: Round-trip
            test6_RoundTrip(mapTestDTOClass, personDTOClass);

            System.out.println("\n=== All Map Tests Passed! ===");

        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    // Test 1: Map<String, String>
    public static void test1_MapStringString(Class<?> dtoClass) throws Exception {
        System.out.println("Test 1: Map<String, String>");

        Object dto = dtoClass.getDeclaredConstructor().newInstance();

        // Set name
        Method setName = dtoClass.getMethod("setName", String.class);
        setName.invoke(dto, "Test");

        // Set Map<String, String>
        Map<String, String> tags = new HashMap<>();
        tags.put("category", "test");
        tags.put("priority", "high");

        Method setTags = dtoClass.getMethod("setTags", Map.class);
        setTags.invoke(dto, tags);

        // Verify getter
        Method getTags = dtoClass.getMethod("getTags");
        @SuppressWarnings("unchecked")
        Map<String, String> retrievedTags = (Map<String, String>) getTags.invoke(dto);

        assert retrievedTags.get("category").equals("test") : "category should be 'test'";
        assert retrievedTags.get("priority").equals("high") : "priority should be 'high'";

        // Serialize to Map (toMap with deep=true)
        Method toMap = dtoClass.getMethod("toMap", boolean.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) toMap.invoke(dto, true);

        // Deserialize back
        Object dto2 = dtoClass.getDeclaredConstructor().newInstance();
        Method fromMap = dtoClass.getMethod("fromMap", Map.class);
        fromMap.invoke(dto2, map);

        // Verify
        Method getName = dtoClass.getMethod("getName");
        String name2 = (String) getName.invoke(dto2);
        assert name2.equals("Test") : "Name should be 'Test'";

        @SuppressWarnings("unchecked")
        Map<String, String> tags2 = (Map<String, String>) getTags.invoke(dto2);
        assert tags2 != null : "Tags should not be null";
        assert tags2.get("category").equals("test") : "category should be 'test'";
        assert tags2.get("priority").equals("high") : "priority should be 'high'";

        System.out.println("✓ Test 1 passed: Map<String, String>\n");
    }

    // Test 2: Map<String, Integer>
    public static void test2_MapStringInteger(Class<?> dtoClass) throws Exception {
        System.out.println("Test 2: Map<String, Integer>");

        Object dto = dtoClass.getDeclaredConstructor().newInstance();

        // Set Map<String, Integer>
        Map<String, Integer> scores = new LinkedHashMap<>();
        scores.put("math", 95);
        scores.put("science", 87);
        scores.put("english", 92);

        Method setScores = dtoClass.getMethod("setScores", Map.class);
        setScores.invoke(dto, scores);

        // Verify
        Method getScores = dtoClass.getMethod("getScores");
        @SuppressWarnings("unchecked")
        Map<String, Integer> retrievedScores = (Map<String, Integer>) getScores.invoke(dto);
        assert retrievedScores.get("math") == 95 : "math should be 95";
        assert retrievedScores.get("science") == 87 : "science should be 87";

        // Serialize
        Method toMap = dtoClass.getMethod("toMap", boolean.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) toMap.invoke(dto, true);

        // Deserialize
        Object dto2 = dtoClass.getDeclaredConstructor().newInstance();
        Method fromMap = dtoClass.getMethod("fromMap", Map.class);
        fromMap.invoke(dto2, map);

        // Verify
        @SuppressWarnings("unchecked")
        Map<String, Integer> scores2 = (Map<String, Integer>) getScores.invoke(dto2);
        assert scores2 != null : "Scores should not be null";
        assert scores2.get("math") == 95 : "math should be 95";
        assert scores2.get("science") == 87 : "science should be 87";
        assert scores2.get("english") == 92 : "english should be 92";

        System.out.println("✓ Test 2 passed: Map<String, Integer>\n");
    }

    // Test 3: Map<Integer, String>
    public static void test3_MapIntegerString(Class<?> dtoClass) throws Exception {
        System.out.println("Test 3: Map<Integer, String>");

        Object dto = dtoClass.getDeclaredConstructor().newInstance();

        // Set Map<Integer, String>
        Map<Integer, String> indexedValues = new HashMap<>();
        indexedValues.put(1, "first");
        indexedValues.put(2, "second");
        indexedValues.put(10, "tenth");

        Method setIndexedValues = dtoClass.getMethod("setIndexedValues", Map.class);
        setIndexedValues.invoke(dto, indexedValues);

        // Verify
        Method getIndexedValues = dtoClass.getMethod("getIndexedValues");
        @SuppressWarnings("unchecked")
        Map<Integer, String> retrieved = (Map<Integer, String>) getIndexedValues.invoke(dto);
        assert retrieved.get(1).equals("first") : "1 should be 'first'";
        assert retrieved.get(10).equals("tenth") : "10 should be 'tenth'";

        // Serialize
        Method toMap = dtoClass.getMethod("toMap", boolean.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) toMap.invoke(dto, true);

        // Deserialize
        Object dto2 = dtoClass.getDeclaredConstructor().newInstance();
        Method fromMap = dtoClass.getMethod("fromMap", Map.class);
        fromMap.invoke(dto2, map);

        // Verify
        @SuppressWarnings("unchecked")
        Map<Integer, String> indexed2 = (Map<Integer, String>) getIndexedValues.invoke(dto2);
        assert indexed2 != null : "IndexedValues should not be null";
        assert indexed2.get(1).equals("first") : "1 should be 'first'";
        assert indexed2.get(2).equals("second") : "2 should be 'second'";
        assert indexed2.get(10).equals("tenth") : "10 should be 'tenth'";

        System.out.println("✓ Test 3 passed: Map<Integer, String>\n");
    }

    // Test 4: Map<String, PersonDTO>
    public static void test4_MapStringDataHelper(Class<?> dtoClass, Class<?> personClass) throws Exception {
        System.out.println("Test 4: Map<String, PersonDTO>");

        Object dto = dtoClass.getDeclaredConstructor().newInstance();

        // Create nested objects
        Object person1 = personClass.getDeclaredConstructor().newInstance();
        Method setFirstName1 = personClass.getMethod("setFirstName", String.class);
        Method setAge1 = personClass.getMethod("setAge", Integer.class);
        setFirstName1.invoke(person1, "Alice");
        setAge1.invoke(person1, 30);

        Object person2 = personClass.getDeclaredConstructor().newInstance();
        Method setFirstName2 = personClass.getMethod("setFirstName", String.class);
        Method setAge2 = personClass.getMethod("setAge", Integer.class);
        setFirstName2.invoke(person2, "Bob");
        setAge2.invoke(person2, 25);

        // Set Map<String, PersonDTO>
        Map<String, Object> people = new LinkedHashMap<>();
        people.put("person1", person1);
        people.put("person2", person2);

        Method setPeople = dtoClass.getMethod("setPeople", Map.class);
        setPeople.invoke(dto, people);

        // Verify
        Method getPeople = dtoClass.getMethod("getPeople");
        @SuppressWarnings("unchecked")
        Map<String, Object> retrievedPeople = (Map<String, Object>) getPeople.invoke(dto);

        Method getFirstName = personClass.getMethod("getFirstName");
        Method getAge = personClass.getMethod("getAge");

        String firstName1 = (String) getFirstName.invoke(retrievedPeople.get("person1"));
        int age2 = (int) getAge.invoke(retrievedPeople.get("person2"));

        assert firstName1.equals("Alice") : "person1 firstName should be 'Alice'";
        assert age2 == 25 : "person2 age should be 25";

        // Serialize (deep)
        Method toMap = dtoClass.getMethod("toMap", boolean.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) toMap.invoke(dto, true);

        // Deserialize
        Object dto2 = dtoClass.getDeclaredConstructor().newInstance();
        Method fromMap = dtoClass.getMethod("fromMap", Map.class);
        fromMap.invoke(dto2, map);

        // Verify nested objects
        @SuppressWarnings("unchecked")
        Map<String, Object> people2 = (Map<String, Object>) getPeople.invoke(dto2);
        assert people2 != null : "People should not be null";

        String firstName1_2 = (String) getFirstName.invoke(people2.get("person1"));
        int age1_2 = (int) getAge.invoke(people2.get("person1"));
        String firstName2_2 = (String) getFirstName.invoke(people2.get("person2"));
        int age2_2 = (int) getAge.invoke(people2.get("person2"));

        assert firstName1_2.equals("Alice") : "person1 firstName should be 'Alice'";
        assert age1_2 == 30 : "person1 age should be 30";
        assert firstName2_2.equals("Bob") : "person2 firstName should be 'Bob'";
        assert age2_2 == 25 : "person2 age should be 25";

        System.out.println("✓ Test 4 passed: Map<String, PersonDTO>\n");
    }

    // Test 5: HashMap vs LinkedHashMap
    public static void test5_MapImplementations(Class<?> dtoClass) throws Exception {
        System.out.println("Test 5: HashMap vs LinkedHashMap");

        Object dto = dtoClass.getDeclaredConstructor().newInstance();

        // Set HashMap field
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("key1", "value1");

        Method setHashMapField = dtoClass.getMethod("setHashMapField", HashMap.class);
        setHashMapField.invoke(dto, hashMap);

        // Set LinkedHashMap field
        LinkedHashMap<String, Integer> linkedHashMap = new LinkedHashMap<>();
        linkedHashMap.put("a", 1);
        linkedHashMap.put("b", 2);
        linkedHashMap.put("c", 3);

        Method setLinkedHashMapField = dtoClass.getMethod("setLinkedHashMapField", LinkedHashMap.class);
        setLinkedHashMapField.invoke(dto, linkedHashMap);

        // Verify types
        Method getHashMapField = dtoClass.getMethod("getHashMapField");
        Method getLinkedHashMapField = dtoClass.getMethod("getLinkedHashMapField");

        Object hashMapField = getHashMapField.invoke(dto);
        Object linkedHashMapField = getLinkedHashMapField.invoke(dto);

        assert hashMapField instanceof HashMap : "hashMapField should be HashMap";
        assert linkedHashMapField instanceof LinkedHashMap : "linkedHashMapField should be LinkedHashMap";

        // Serialize
        Method toMap = dtoClass.getMethod("toMap", boolean.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) toMap.invoke(dto, true);

        // Deserialize
        Object dto2 = dtoClass.getDeclaredConstructor().newInstance();
        Method fromMap = dtoClass.getMethod("fromMap", Map.class);
        fromMap.invoke(dto2, map);

        // Verify values
        @SuppressWarnings("unchecked")
        Map<String, String> hashMapField2 = (Map<String, String>) getHashMapField.invoke(dto2);
        @SuppressWarnings("unchecked")
        Map<String, Integer> linkedHashMapField2 = (Map<String, Integer>) getLinkedHashMapField.invoke(dto2);

        assert hashMapField2.get("key1").equals("value1") : "key1 should be 'value1'";
        assert linkedHashMapField2.get("a") == 1 : "a should be 1";
        assert linkedHashMapField2.get("c") == 3 : "c should be 3";

        System.out.println("✓ Test 5 passed: HashMap vs LinkedHashMap\n");
    }

    // Test 6: Round-trip
    public static void test6_RoundTrip(Class<?> dtoClass, Class<?> personClass) throws Exception {
        System.out.println("Test 6: Round-trip");

        // Create complex DTO
        Object dto = dtoClass.getDeclaredConstructor().newInstance();

        Method setName = dtoClass.getMethod("setName", String.class);
        Method setAge = dtoClass.getMethod("setAge", int.class);
        setName.invoke(dto, "Round Trip Test");
        setAge.invoke(dto, 42);

        // Multiple maps
        Map<String, String> tags = new HashMap<>();
        tags.put("env", "production");
        Method setTags = dtoClass.getMethod("setTags", Map.class);
        setTags.invoke(dto, tags);

        Map<String, Integer> scores = new LinkedHashMap<>();
        scores.put("q1", 100);
        scores.put("q2", 95);
        Method setScores = dtoClass.getMethod("setScores", Map.class);
        setScores.invoke(dto, scores);

        Map<Integer, String> indexed = new HashMap<>();
        indexed.put(0, "zero");
        indexed.put(100, "hundred");
        Method setIndexedValues = dtoClass.getMethod("setIndexedValues", Map.class);
        setIndexedValues.invoke(dto, indexed);

        // Nested DataHelper objects in Map
        Object person = personClass.getDeclaredConstructor().newInstance();
        Method setFirstName = personClass.getMethod("setFirstName", String.class);
        Method setAgeP = personClass.getMethod("setAge", Integer.class);
        setFirstName.invoke(person, "Charlie");
        setAgeP.invoke(person, 35);

        Map<String, Object> people = new HashMap<>();
        people.put("charlie", person);
        Method setPeople = dtoClass.getMethod("setPeople", Map.class);
        setPeople.invoke(dto, people);

        // Round-trip: DTO → Map → DTO
        Method toMap = dtoClass.getMethod("toMap", boolean.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) toMap.invoke(dto, true);

        Object dto2 = dtoClass.getDeclaredConstructor().newInstance();
        Method fromMap = dtoClass.getMethod("fromMap", Map.class);
        fromMap.invoke(dto2, map);

        // Verify all fields
        Method getName = dtoClass.getMethod("getName");
        Method getAge = dtoClass.getMethod("getAge");
        Method getTags = dtoClass.getMethod("getTags");
        Method getScores = dtoClass.getMethod("getScores");
        Method getIndexedValues = dtoClass.getMethod("getIndexedValues");
        Method getPeople = dtoClass.getMethod("getPeople");

        assert getName.invoke(dto2).equals("Round Trip Test") : "Name should match";
        assert (int) getAge.invoke(dto2) == 42 : "Age should be 42";

        @SuppressWarnings("unchecked")
        Map<String, String> tags2 = (Map<String, String>) getTags.invoke(dto2);
        assert tags2.get("env").equals("production") : "env should be 'production'";

        @SuppressWarnings("unchecked")
        Map<String, Integer> scores2 = (Map<String, Integer>) getScores.invoke(dto2);
        assert scores2.get("q1") == 100 : "q1 should be 100";
        assert scores2.get("q2") == 95 : "q2 should be 95";

        @SuppressWarnings("unchecked")
        Map<Integer, String> indexed2 = (Map<Integer, String>) getIndexedValues.invoke(dto2);
        assert indexed2.get(0).equals("zero") : "0 should be 'zero'";
        assert indexed2.get(100).equals("hundred") : "100 should be 'hundred'";

        @SuppressWarnings("unchecked")
        Map<String, Object> people2 = (Map<String, Object>) getPeople.invoke(dto2);
        Method getFirstName = personClass.getMethod("getFirstName");
        Method getAgeP = personClass.getMethod("getAge");

        String firstName = (String) getFirstName.invoke(people2.get("charlie"));
        int age = (int) getAgeP.invoke(people2.get("charlie"));

        assert firstName.equals("Charlie") : "charlie firstName should be 'Charlie'";
        assert age == 35 : "charlie age should be 35";

        System.out.println("✓ Test 6 passed: Round-trip\n");
    }
}
