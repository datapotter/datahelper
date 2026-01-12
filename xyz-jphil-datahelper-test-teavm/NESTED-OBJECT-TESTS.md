# Nested Object Tests

## Overview
This document describes the nested object testing implementation for the DataHelper annotation processor with TeaVM support.

## Test DTOs Created

### 1. AddressDTO
Simple DTO representing an address with fields:
- `street`
- `city`
- `state`
- `zipCode`
- `country`

**Location:** `src/main/java/com/example/test/AddressDTO.java`

### 2. CompanyDTO
Company DTO with nested AddressDTO:
- `name`
- `department`
- `address` (AddressDTO) - nested object
- `employeeCount` (Integer)

**Location:** `src/main/java/com/example/test/CompanyDTO.java`

### 3. PersonDTO
Complex DTO with multiple levels of nesting:
- `firstName`
- `lastName`
- `age` (Integer - nullable)
- `email`
- `homeAddress` (AddressDTO) - nested object
- `workAddress` (AddressDTO) - another nested object (can be null)
- `company` (CompanyDTO) - nested object with further nesting (Company -> Address)

**Location:** `src/main/java/com/example/test/PersonDTO.java`

## Comprehensive Test Suite

**Test File:** `src/main/java/com/example/test/NestedObjectTest.java`

### Test Cases

#### Test 1: Simple Nested Object
Tests Person with only homeAddress populated. Verifies:
- Basic nested object deserialization
- All nested properties are correctly populated

#### Test 2: Multiple Nested Objects
Tests Person with both homeAddress and workAddress. Verifies:
- Multiple nested objects at the same level
- Independent nested object handling

#### Test 3: Deep Nesting
Tests Person -> Company -> Address (three levels deep). Verifies:
- Deep nesting works correctly
- Nested objects within nested objects
- Data integrity through multiple nesting levels

#### Test 4: Null/Missing Nested Objects
Tests various null scenarios:
- **4a:** Missing optional nested object (workAddress undefined)
- **4b:** Explicit null nested objects (age: null, company: null)
- **4c:** Parent present but nested child missing (Company without address)

Verifies proper handling of:
- Undefined vs null distinction
- Partial nesting (parent exists, child doesn't)

#### Test 5: Serialization with Nesting
Tests round-trip conversion:
1. Build complex nested structure programmatically
2. Serialize to JSON using `toMapLike()`
3. Parse JSON back
4. Verify all nested data intact

Verifies:
- Bidirectional conversion works
- No data loss in round-trip
- Nested objects serialize correctly

## Running the Tests

### Build
```bash
cd xyz-jphil-datahelper-test
mvn clean compile
```

### Run in Browser
The build automatically copies HTML test files to the generated JS directories. Open in browser:

**Nested Object Test:**
```
target/generated/js/nested-object-test/test.html
```

**Value Provider Test:**
```
target/generated/js/value-provider-test/test.html
```

Open browser console (F12) to see test output.

## Architecture

### MapLike Abstraction
The tests use the `MapLike` abstraction layer that works across JVM and TeaVM:

```java
// TeaVM: Parse JSON and wrap in MapLike
JSMapLike<JSObject> jsonObj = (JSMapLike<JSObject>) JSON.parse(jsonString);
PersonDTO person = new PersonDTO();
person.fromMapLike(TeaVMMapLike.wrap(jsonObj));

// Serialize back
TeaVMMapLike mapLike = (TeaVMMapLike) person.toMapLike(false);
JSMapLike<JSObject> jsResult = mapLike.unwrap();
String json = JSON.stringify(jsResult);
```

### Generated Interface Pattern
Each DTO implements a generated interface (suffix `_I`):
- `AddressDTO implements AddressDTO_I<AddressDTO>`
- `CompanyDTO implements CompanyDTO_I<CompanyDTO>`
- `PersonDTO implements PersonDTO_I<PersonDTO>`

The annotation processor generates these interfaces with:
- Field name constants (e.g., `String $firstName = "firstName"`)
- `@JSProperty` annotated getters/setters (for TeaVM)
- `fromMapLike()` and `toMapLike()` methods
- Fluent setter methods

## Cleanup

### Archived Experiments
Old Integer/JSNumber experiment files moved to:
```
src/main/java/com/example/test/archived_experiments/
```

These files are preserved for reference but removed from active builds:
- `IntegerVsIntTest.java`
- `IntPrimitiveNullTest.java`
- `IntegerWithHelperTest.java`
- `IntegerProperConversionTest.java`
- `IntegerFinalSolutionTest.java`
- `JSNumberCastTest.java`
- `JSNumberDirectCastTest.java`
- `ManualMapperTest.java`

### Active Tests
Current active tests:
1. **TeaVMDemo** - Original demo
2. **NullableNumberDemo** - Nullable Integer handling
3. **ValueProviderTest** - MapLike with simple DTOs
4. **CanonicalMapperTest** - Canonical mapping approach
5. **NestedObjectTest** - Comprehensive nested object tests (NEW)

## Key Learnings

### TeaVM JSProperty Pattern
Following @konsoletyper's guidance from the TeaVM discussion:

1. **DO NOT** use wrapper types (Integer, Double) directly with `@JSProperty` - they return raw JS values
2. **DO** use custom converters or JSNumber for nullable numbers
3. **DO** follow the canonical mapping approach for type-safe conversions
4. Keep DTOs as pure POJOs when possible (no TeaVM dependency in DTO classes themselves)

### Nested Object Handling
The annotation processor correctly handles:
- Nested objects of any depth
- Multiple nested objects at same level
- Null vs undefined distinction
- Round-trip serialization/deserialization

## Next Steps

### For JVM Testing
Consider creating a separate JVM test module that uses:
- Jackson or json.org for JSON parsing
- Same DTOs (they're pure POJOs)
- JVM-specific MapLike implementation

This would verify the DTOs work identically in both JVM and TeaVM environments.

### Edge Cases to Test
- Collections of nested objects (List<AddressDTO>)
- Maps with nested objects (Map<String, CompanyDTO>)
- Circular references (if needed)
- Very deep nesting (5+ levels)
