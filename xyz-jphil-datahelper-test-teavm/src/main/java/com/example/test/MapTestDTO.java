package com.example.test;

import lombok.Data;
import xyz.jphil.datahelper.DataHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;

@DataHelper
@Data
public class MapTestDTO implements MapTestDTO_I<MapTestDTO> {
    // Map with simple value types
    String name;
    int age;

    // Map<String, String>
    Map<String, String> tags;

    // Map<String, Integer>
    Map<String, Integer> scores;

    // Map<Integer, String> (non-string key)
    Map<Integer, String> indexedValues;

    // Map<String, PersonDTO> (DataHelper value)
    Map<String, PersonDTO> people;

    // HashMap (explicit implementation)
    HashMap<String, String> hashMapField;

    // LinkedHashMap (explicit implementation)
    LinkedHashMap<String, Integer> linkedHashMapField;

    // Constructor
    public MapTestDTO() {}
}
