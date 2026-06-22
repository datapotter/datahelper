package xyz.jphil.datahelper.json;

import xyz.jphil.datahelper.DataHelper_I;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Write side of the JSON trait: deserialization ({@code fromJson}).
 *
 * <p>Extends {@link Json_IR} (which carries {@code toJson}) and {@link DataHelper_I} (the
 * write contract). A DTO implementing {@code Json_I} therefore has both {@code toJson} and
 * {@code fromJson} — existing DTOs are unaffected by the split. The read-only {@code toJson}
 * half ({@link Json_IR}) can be mixed into readable {@code _IR} interfaces so immutable
 * {@code _R} record projections can also serialize.</p>
 *
 * <p>Records are not deserialized into directly: parse into the mutable form with
 * {@code fromJson}, then call {@code toRecord()}.</p>
 *
 * <p><strong>Usage:</strong></p>
 * <pre>{@code
 * @DataHelper
 * public class PersonDTO {
 *     String name;
 *     Integer age;
 * }
 *
 * // Use JSON methods directly
 * PersonDTO person = new PersonDTO();
 * String json = person.toJson();
 * person.fromJson(json);
 * }</pre>
 *
 * <p><strong>Performance Note:</strong></p>
 * <ul>
 *   <li>JVM: Good performance, suitable for production use</li>
 *   <li>TeaVM: Performance penalty - avoid using JSON on TeaVM, use JSObject directly</li>
 * </ul>
 *
 * @param <E> the implementing type (self-reference for fluent API)
 */
public interface Json_I<E extends DataHelper_I<E>> extends Json_IR<E>, DataHelper_I<E> {

    /**
     * Populate this DTO from a JSON string.
     *
     * @param json the JSON string (must be a JSON object)
     * @return this instance for chaining
     * @throws xyz.jphil.datahelper.json.MinimalJsonParser.JsonParseException if JSON is malformed
     */
    @SuppressWarnings("unchecked")
    default E fromJson(String json) {
        // Parse JSON to Map
        Map<String, Object> map = MinimalJsonParser.parseObject(json);

        // Use property accessors to populate fields
        for (String fieldName : fieldNames()) {
            if (!map.containsKey(fieldName)) continue;

            Object value = map.get(fieldName);
            if (value == null) continue;

            Class<?> fieldType = getPropertyType(fieldName);

            if (value instanceof Map && isNestedObjectField(fieldName)) {
                // Nested DataHelper object
                DataHelper_I<?> nested = createNestedObject(fieldName);
                if (nested instanceof Json_I) {
                    // Convert Map back to JSON and parse recursively
                    String nestedJson = MinimalJsonWriter.write(value);
                    ((Json_I<?>) nested).fromJson(nestedJson);
                    setPropertyByName(fieldName, nested);
                }
            } else if (value instanceof List && isListField(fieldName)) {
                // List field
                List<?> sourceList = (List<?>) value;
                List<Object> targetList = new ArrayList<>();

                for (Object item : sourceList) {
                    if (item instanceof Map) {
                        // List element is a DataHelper object
                        DataHelper_I<?> element = createListElement(fieldName);
                        if (element instanceof Json_I) {
                            String elementJson = MinimalJsonWriter.write(item);
                            ((Json_I<?>) element).fromJson(elementJson);
                            targetList.add(element);
                        }
                    } else {
                        // Simple type
                        targetList.add(item);
                    }
                }

                setPropertyByName(fieldName, targetList);
            } else if (value instanceof Map && isMapField(fieldName)) {
                // Map<K,V> field
                Map<?, ?> sourceMap = (Map<?, ?>) value;
                Map<Object, Object> targetMap = (Map<Object, Object>) createMapInstance(fieldName);

                Class<?> keyType = getMapKeyType(fieldName);
                Class<?> valueType = getMapValueType(fieldName);
                boolean isValueDataHelper = isMapValueDataHelper(fieldName);

                for (Map.Entry<?, ?> entry : sourceMap.entrySet()) {
                    Object convertedKey = DataHelper_I.convertType(entry.getKey(), keyType);
                    Object mapValue = entry.getValue();

                    if (mapValue instanceof Map && isValueDataHelper) {
                        // Map value is a DataHelper object
                        DataHelper_I<?> element = createMapValueElement(fieldName);
                        if (element instanceof Json_I) {
                            String elementJson = MinimalJsonWriter.write(mapValue);
                            ((Json_I<?>) element).fromJson(elementJson);
                            targetMap.put(convertedKey, element);
                        }
                    } else {
                        // Simple type
                        targetMap.put(convertedKey, DataHelper_I.convertType(mapValue, valueType));
                    }
                }

                setPropertyByName(fieldName, targetMap);
            } else {
                // Simple field - may need type conversion
                setPropertyByName(fieldName, DataHelper_I.convertType(value, fieldType));
            }
        }

        return (E) this;
    }
}
