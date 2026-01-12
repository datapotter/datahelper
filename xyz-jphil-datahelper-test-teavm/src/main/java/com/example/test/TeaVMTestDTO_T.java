package com.example.test;

import org.teavm.jso.JSObject;

/**
 * TeaVM wrapper interface that extends TeaVMTestDTO_I and JSObject.
 * This is a workaround for TeaVM's restriction on JSObject overlay types.
 * The _I interface stays clean with DataHelper_I, and this _T wrapper adds JSObject.
 */
public interface TeaVMTestDTO_T<E extends TeaVMTestDTO_T<E>> extends TeaVMTestDTO_I<E>, JSObject {
    // No additional methods needed - just combines _I and JSObject
}
