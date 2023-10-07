package no.hvl.tk.visual.debugger.domain;

import com.jetbrains.jdi.ObjectReferenceImpl;
import com.sun.jdi.Field;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.Value;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ComponentObject {

    private String type;

    private Long refId;

    private String value;

    private final List<ComponentDecorate<ComponentMethod>> methods = new LinkedList<>();

    private final Map<String, ComponentDecorate<ComponentObject>> fields = new LinkedHashMap<>();

    private boolean ignore;


    public static boolean isObjectReferenceImpl(StackFrame stackFrame) {
        ObjectReference thisObject = stackFrame.thisObject();
        return thisObject instanceof ObjectReferenceImpl;
    }

    public ComponentObject(StackFrame stackFrame) throws Exception {
        ReferenceType referenceType = stackFrame.location().declaringType();
        ObjectReference thisObject = stackFrame.thisObject();
        //500
        this.refId = getRef((ObjectReferenceImpl) thisObject);
        //com.MyClass
        this.type = referenceType.name();
        this.ignore = false;
    }

    public ComponentObject(Field field, Value value, Long refId) {
        this.refId = refId;
        this.type = field.typeName();
        if (value != null ) {
            this.value = value.toString();
        }
        this.ignore = true;
    }


    public void addMethod(ComponentDecorate<ComponentMethod> method) {
        if (this.methods.stream().noneMatch(o -> o.component.equals(method.component))) {
            this.methods.add(method);
        }
    }

    public void addField(String name, ComponentDecorate<ComponentObject> field) {
        if (!this.fields.containsKey(name)) {
            this.fields.put(name, field);
        }
    }

    public boolean isIgnore() {
        return ignore;
    }

    public void lookIn() {
        this.value = null;
        this.ignore = false;
    }

    private static long getRef(ObjectReferenceImpl objectReference) throws Exception {
        java.lang.reflect.Field ref = objectReference.getClass().getDeclaredField("ref");
        ref.setAccessible(true);
        return (long)ref.get(objectReference);
    }
}
