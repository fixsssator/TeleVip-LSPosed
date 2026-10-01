package com.my.televip.virtuals.androidx;

import com.my.televip.hooks.ShapeResolver;
import com.my.televip.logging.Logger;
import com.my.televip.obfuscate.Obfuscate;

import java.lang.reflect.Method;
import java.util.ArrayList;

/** Wrapper over androidx.collection.LongSparseArray, whose method names are minified in Telegram builds. */
public class LongSparseArray {

    Object longSparseArray;

    public LongSparseArray(Object longSparseArray) {
        this.longSparseArray = longSparseArray;
    }

    @SuppressWarnings("unchecked")
    public ArrayList<Object> get(long id) {
        try {
            if (longSparseArray == null) return null;
            // get(long): the only instance method (long) -> Object in that class
            Method m = ShapeResolver.unique(longSparseArray.getClass(),
                    Obfuscate.getMethodName("LongSparseArray", "get"), Object.class, long.class);
            if (m == null) return null;
            return (ArrayList<Object>) m.invoke(longSparseArray, id);
        } catch (Throwable t) {
            Logger.e(t);
            return null;
        }
    }
}
