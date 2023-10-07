package no.hvl.tk.visual.debugger.util;

import com.jetbrains.jdi.ObjectReferenceImpl;
import com.sun.jdi.Value;

import java.lang.reflect.Field;

public class ObjectUtil {
    public static long getRef(ObjectReferenceImpl objectReference) throws Exception {
        java.lang.reflect.Field ref = objectReference.getClass().getDeclaredField("ref");
        ref.setAccessible(true);
        return (long)ref.get(objectReference);
    }


    public static Long getRef(Value value) {
        if (value == null) {
            return null;
        }
        try {
            Field ref = value.getClass().getDeclaredField("ref");
            ref.setAccessible(true);
            return (long)ref.get(value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return null;
        }
    }
}
