package com.example.test;

import org.teavm.jso.JSObject;

/**
 * TeaVM wrapper interface that extends NullableNumberTestDTO_I and JSObject.
 * This is a workaround for TeaVM's restriction on JSObject overlay types.
 */
public interface NullableNumberTestDTO_T<E extends NullableNumberTestDTO_T<E>> extends NullableNumberTestDTO_I<E>, JSObject {
    // No additional methods needed - just combines _I and JSObject
}
