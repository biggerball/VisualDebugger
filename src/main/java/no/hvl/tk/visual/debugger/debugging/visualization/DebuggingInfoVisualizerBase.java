package no.hvl.tk.visual.debugger.debugging.visualization;

import no.hvl.tk.visual.debugger.domain.*;

public abstract class DebuggingInfoVisualizerBase implements DebuggingInfoVisualizer {
    protected ObjectDiagram diagram;

    DebuggingInfoVisualizerBase() {
        this.diagram = new ObjectDiagram();
    }

    @Override
    public DebuggingInfoVisualizer addObject(final ODObject object) {
        this.diagram.addObject(object);
        return this;
    }

    @Override
    public DebuggingInfoVisualizer addAttributeToObject(final ODObject object, final String fieldName, final String fieldValue, final String fieldType) {
        object.addAttribute(new ODAttributeValue(fieldName, fieldType, fieldValue));
        return this;
    }

    @Override
    public DebuggingInfoVisualizer addLinkToObject(final ODObject from, final ODObject to, final String linkType) {
        from.addLink(new ODLink(from, to, linkType));
        return this;
    }

    @Override
    public void addPrimitiveRootValue(final String variableName, final String type, final String value) {
        this.diagram.addPrimitiveRootValue(new ODPrimitiveRootValue(variableName, type, value));
    }
}
