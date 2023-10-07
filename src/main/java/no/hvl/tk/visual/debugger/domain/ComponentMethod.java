package no.hvl.tk.visual.debugger.domain;

import com.sun.jdi.Method;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

public class ComponentMethod {
    private final String name;

    private final String returnType;

    private final String parameters;

    public ComponentMethod(Method method) {
        this.returnType = method.returnTypeName();
        this.name = method.name();
        this.parameters = StringUtils.join(method.argumentTypeNames(), ",");
    }

    public String getName() {
        return name;
    }

    public String getReturnType() {
        return returnType;
    }

    public String getParameters() {
        return parameters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComponentMethod that = (ComponentMethod) o;
        return Objects.equals(name, that.name) && Objects.equals(returnType, that.returnType) && Objects.equals(parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, returnType, parameters);
    }
}
