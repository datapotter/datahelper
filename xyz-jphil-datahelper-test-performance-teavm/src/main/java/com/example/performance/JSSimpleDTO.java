package com.example.performance;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

/**
 * JSObject version of SimpleDTO for native JSON testing.
 * This is what you'd typically use with TeaVM's JSON.parse/stringify.
 */
public interface JSSimpleDTO extends JSObject {

    @JSProperty
    String getName();

    @JSProperty
    void setName(String name);

    @JSProperty
    int getAge();

    @JSProperty
    void setAge(int age);

    @JSProperty
    String getEmail();

    @JSProperty
    void setEmail(String email);

    @JSProperty
    int getStatus();

    @JSProperty
    void setStatus(int status);

    @JSProperty
    double getSalary();

    @JSProperty
    void setSalary(double salary);
}
