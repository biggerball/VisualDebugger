package no.hvl.tk.visual.debugger.domain;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@XmlRootElement
public class ObjectDiagram {
    @XmlElement
    private final Set<ODObject> objects;
    @XmlElement
    public final Set<ODLink> links;
    @XmlElement
    private final Set<ODPrimitiveRootValue> primitiveRootValues;

    public ObjectDiagram() {
        this.objects = new HashSet<>();
        this.links = new HashSet<>();
        this.primitiveRootValues = new HashSet<>();
    }

    public Set<ODObject> getObjects() {
        return Collections.unmodifiableSet(this.objects);
    }

    public Set<ODPrimitiveRootValue> getPrimitiveRootValues() {
        return Collections.unmodifiableSet(this.primitiveRootValues);
    }

    public void addObject(final ODObject obj) {
        this.objects.add(obj);
    }

    public void addLink(final ODLink link) {
        this.links.add(link);
    }

    public void addPrimitiveRootValue(final ODPrimitiveRootValue primitiveRootValue) {
        this.primitiveRootValues.add(primitiveRootValue);
    }
}
