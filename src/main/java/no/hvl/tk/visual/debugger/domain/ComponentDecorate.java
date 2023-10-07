package no.hvl.tk.visual.debugger.domain;

import com.jetbrains.jdi.ObjectReferenceImpl;
import com.sun.jdi.Field;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.TypeComponent;
import com.sun.jdi.Value;
import no.hvl.tk.visual.debugger.util.ObjectUtil;

import java.util.HashMap;
import java.util.Map;

public class ComponentDecorate<T> {

    private static Map<Long, ComponentObject> componentObjectMap = new HashMap<>();

    String access;

    boolean isStatic;

    T component;

    public static <C> ComponentDecorate<C> getInstance(TypeComponent typeComponent) {
        ComponentDecorate<C> fieldComponent = new ComponentDecorate<>();
        if (typeComponent.isPublic()) {
            fieldComponent.access = "public";
        } else if (typeComponent.isPrivate()) {
            fieldComponent.access = "private";
        } else {
            fieldComponent.access = "protected";
        }
        fieldComponent.isStatic = typeComponent.isStatic();
        return fieldComponent;
    }


    public static ComponentObject analyzeStackFrame(StackFrame stackFrame) throws Exception {
        ObjectReference thisObject = stackFrame.thisObject();
        long ref = ObjectUtil.getRef((ObjectReferenceImpl) thisObject);
        if (componentObjectMap.containsKey(ref) && !componentObjectMap.get(ref).isIgnore()) {
            return componentObjectMap.get(ref);
        }
        //new objectNode
        ComponentObject componentObject = componentObjectMap.get(ref);
        if (componentObject == null) {
            componentObject = new ComponentObject(stackFrame);
        } else if (componentObject.isIgnore()) {
            componentObject.lookIn();
        }

        componentObjectMap.put(ref, componentObject);
        //add method
        ReferenceType referenceType = stackFrame.location().declaringType();
        for (Method method : referenceType.methods()) {
            if (!method.isConstructor() && !method.isStaticInitializer()) {
                componentObject.addMethod(newDecoratedMethod(method));
            }
        }
        //add fields
        for (Field field : referenceType.fields()) {
            Value value;
            if (field.isStatic()) {
                value = referenceType.getValue(field);
            } else {
                value = stackFrame.thisObject().getValue(field);
            }
            String fieldName = field.name();
            Long refId = ObjectUtil.getRef(value);
            if (refId != null) {
                if (!componentObjectMap.containsKey(refId)) {
                    ComponentDecorate<ComponentObject> fieldObject = newDecoratedField(field, value, refId);
                    componentObject.addField(fieldName, fieldObject);
                    componentObjectMap.put(refId, fieldObject.component);
                }
            } else {
                ComponentDecorate<ComponentObject> fieldObject = newDecoratedField(field, value, null);
                componentObject.addField(fieldName, fieldObject);
            }
        }
        return componentObject;
    }


    private static ComponentDecorate<ComponentMethod> newDecoratedMethod(Method method) {
        ComponentDecorate<ComponentMethod> decorate = getInstance(method);
        decorate.component = new ComponentMethod(method);
        return decorate;
    }


    private static ComponentDecorate<ComponentObject> newDecoratedField(Field field, Value value, Long refId) {
        ComponentDecorate<ComponentObject> decorate = getInstance(field);
        decorate.component = new ComponentObject(field, value, refId);
        return decorate;
    }
}
