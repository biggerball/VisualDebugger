package no.hvl.tk.visual.debugger.debugging.stackframe;

import com.intellij.debugger.engine.SuspendContext;
import com.intellij.execution.ui.RunnerLayoutUi;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.Pair;
import com.intellij.util.ui.UIUtil;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebugSessionListener;
import com.sun.jdi.*;
import no.hvl.tk.visual.debugger.DebugProcessListener;
import no.hvl.tk.visual.debugger.SharedState;
import no.hvl.tk.visual.debugger.debugging.visualization.DebuggingInfoVisualizer;
import no.hvl.tk.visual.debugger.debugging.visualization.PlantUmlDebuggingVisualizer;
import no.hvl.tk.visual.debugger.debugging.visualization.WebSocketDebuggingVisualizer;
import no.hvl.tk.visual.debugger.domain.ODObject;
import no.hvl.tk.visual.debugger.domain.PrimitiveTypes;
import no.hvl.tk.visual.debugger.settings.PluginSettingsState;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

import static no.hvl.tk.visual.debugger.debugging.stackframe.StackFrameSessionListenerHelper.*;

public class StackFrameSessionListener implements XDebugSessionListener {

    private static final Logger LOGGER = Logger.getInstance(StackFrameSessionListener.class);
    private static final String CONTENT_ID = "no.hvl.tk.VisualDebugger";
    private static final String TOOLBAR_ACTION = "VisualDebugger.VisualizerToolbar"; // has to match with plugin.xml
    public static final String KEY = "key";
    public static final String VALUE = "value";


    private JPanel userInterface;

    private final XDebugSession debugSession;
    private DebuggingInfoVisualizer debuggingVisualizer;
    private ThreadReference thread;

    private Set<Long> seenObjectIds = new HashSet<>();

    /*
    Converting actual heap objects requires running code on the suspended VM thread.
    However, once we start running code on the thread, we can no longer read frame locals.
    Therefore, we have to convert all heap objects at the very end.
    */
    private final TreeMap<Long, Pair<ObjectReference, ODObject>> rootObjects = new TreeMap<>();

    public StackFrameSessionListener(XDebugSession debugSession) {
        this.debugSession = debugSession;
    }

    @Override
    public void sessionPaused() {
        this.initUIIfNeeded();

        startVisualDebugging();
    }

    private void startVisualDebugging() {
        if (!SharedState.isDebuggingActive()) {
            return;
        }
        StackFrame stackFrame = this.getCorrectStackFrame(debugSession);

        visualizeThisObject(stackFrame);
        visualizeVariables(stackFrame);

        convertObjects();

        this.debuggingVisualizer.finishVisualization();
        this.seenObjectIds.clear();
    }

    private void convertObjects() {
        rootObjects.forEach((objID, objectReferenceODObjectPair) -> {
            final ObjectReference obRef = objectReferenceODObjectPair.getFirst();
            final ODObject odObject = objectReferenceODObjectPair.getSecond();
            // No parents at root
            this.exploreObjectReference(obRef, odObject, null, "");
        });
    }

    private void visualizeThisObject(StackFrame stackFrame) {
        ObjectReference thisObjectReference = stackFrame.thisObject();
        assert thisObjectReference != null;

        final ODObject thisObject = new ODObject(
                thisObjectReference.uniqueID(),
                thisObjectReference.referenceType().name(),
                "this");

        rootObjects.put(thisObjectReference.uniqueID(),
                Pair.create(thisObjectReference,
                        thisObject));
    }

    private void visualizeVariables(StackFrame stackFrame) {
        try {
            // All visible variables in the stackframe.
            final List<LocalVariable> methodVariables = stackFrame.visibleVariables();
            methodVariables.forEach(localVariable -> this.convertVariable(
                    localVariable,
                    stackFrame,
                    null,
                    null));
        } catch (AbsentInformationException e) {
            // OK
        }
    }

    private void initUIIfNeeded() {
        if (this.userInterface != null) {
            return;
        }
        this.userInterface = new JPanel();
        this.getOrCreateDebuggingInfoVisualizer(); // make sure visualizer is initialized
        if (!SharedState.isDebuggingActive()) {
            this.resetUIAndAddActivateDebuggingButton();
        } else {
            this.debuggingVisualizer.debuggingActivated();
        }
        final var uiContainer = new SimpleToolWindowPanel(false, true);

        final var actionManager = ActionManager.getInstance();
        final var actionToolbar = actionManager.createActionToolbar(
                TOOLBAR_ACTION,
                (DefaultActionGroup) actionManager.getAction(TOOLBAR_ACTION),
                false
        );
        actionToolbar.setTargetComponent(this.userInterface);
        uiContainer.setToolbar(actionToolbar.getComponent());
        uiContainer.setContent(this.userInterface);

        final RunnerLayoutUi ui = this.debugSession.getUI();
        final var content = ui.createContent(
                CONTENT_ID,
                uiContainer,
                "Visual Debugger",
                IconLoader.getIcon("/icons/icon_16x16.png", DebugProcessListener.class),
                null);
        content.setCloseable(false);
        UIUtil.invokeLaterIfNeeded(() -> ui.addContent(content));
        LOGGER.debug("UI initialized!");
    }

    public void resetUIAndAddActivateDebuggingButton() {
        this.userInterface.removeAll();
        this.userInterface.setLayout(new FlowLayout());

        final var activateButton = new JButton("Activate visual debugger");
        activateButton.addActionListener(actionEvent -> {

            SharedState.setDebuggingActive(true);
            this.userInterface.remove(activateButton);
            this.debuggingVisualizer.debuggingActivated();
            this.userInterface.revalidate();
            this.startVisualDebugging();
        });
        this.userInterface.add(activateButton);

        this.userInterface.revalidate();
        this.userInterface.repaint();
    }

    @NotNull
    public DebuggingInfoVisualizer getOrCreateDebuggingInfoVisualizer() {
        if (this.debuggingVisualizer == null) {
            switch (PluginSettingsState.getInstance().getVisualizerOption()) {
                case WEB_UI:
                    this.debuggingVisualizer = new WebSocketDebuggingVisualizer(this.userInterface);
                    break;
                case EMBEDDED:
                    this.debuggingVisualizer = new PlantUmlDebuggingVisualizer(this.userInterface);
                    break;
                default:
                    LOGGER.warn("Unrecognized debugging visualizer chosen. Defaulting to web visualizer!");
                    this.debuggingVisualizer = new WebSocketDebuggingVisualizer(this.userInterface);
            }
        }
        return this.debuggingVisualizer;
    }

    private void exploreObjectReference(
            ObjectReference objectReference,
            ODObject odObject,
            ODObject parentIfExists,
            String linkTypeIfExists) {
        final String objectType = objectReference.referenceType().name();
        if (PrimitiveTypes.isBoxedPrimitiveType(objectType)) {
            final Value value = objectReference.getValue(objectReference.referenceType().fieldByName(VALUE));
            this.convertValue(value, odObject.getVariableName(), objectType, parentIfExists, linkTypeIfExists, true);
            return;
        }
        if (objectReference instanceof ArrayReference) {
            convertArray(
                    odObject.getVariableName(),
                    (ArrayReference) objectReference,
                    objectType,
                    parentIfExists,
                    linkTypeIfExists);
            return;
        }
        if ((doesImplementInterface(objectReference, "java.util.List")
                || doesImplementInterface(objectReference, "java.util.Set"))
                && isInternalPackage(objectType)) {
            convertListOrSet(odObject.getVariableName(), objectReference, objectType, parentIfExists, linkTypeIfExists);
            return;
        }

        if (doesImplementInterface(objectReference, "java.util.Map") && isInternalPackage(objectType)) {
            convertMap(odObject.getVariableName(), objectReference, objectType, parentIfExists, linkTypeIfExists);
            return;
        }


        if (this.seenObjectIds.contains(objectReference.uniqueID())) {
            return;
        }
        this.debuggingVisualizer.addObject(odObject);
        this.seenObjectIds.add(objectReference.uniqueID());

        if (parentIfExists != null) {
            debuggingVisualizer.addLinkToObject(parentIfExists, odObject, linkTypeIfExists);
        }

        // Filter static fields? Or non visible fields?
        for (Map.Entry<Field, Value> fieldValueEntry : objectReference.getValues(objectReference.referenceType().allFields()).entrySet()) {
            final String fieldName = fieldValueEntry.getKey().name();
            this.convertValue(
                    fieldValueEntry.getValue(),
                    fieldName,
                    fieldValueEntry.getKey().typeName(),
                    odObject,
                    fieldName,
                    true);
        }
    }

    private void convertArray(
            String name,
            ArrayReference arrayRef,
            String objectType,
            ODObject parentIfExists,
            String linkTypeIfExists) {
        final ODObject parent = createParentIfNeededForCollection(arrayRef, parentIfExists, name, objectType);
        for (int i = 0; i < arrayRef.length(); i++) {
            final Value value = arrayRef.getValue(i);
            final String variableName = String.valueOf(i);
            this.convertValue(
                    value,
                    variableName,
                    value.type().name(),
                    parent,
                    parent.equals(parentIfExists) ? linkTypeIfExists : variableName, true); // link type is just the index in case of root collections.
        }
    }

    @NotNull
    private ODObject createParentIfNeededForCollection(
            ObjectReference obRef,
            ODObject parentIfExists,
            String obName,
            String objectType) {
        final ODObject parent;
        if (parentIfExists != null) {
            parent = parentIfExists;
        } else {
            parent = new ODObject(obRef.uniqueID(), objectType, obName);
            this.debuggingVisualizer.addObject(parent);
        }
        return parent;
    }

    private void convertListOrSet(
            String name,
            ObjectReference collectionRef,
            String objectType,
            ODObject parentIfExists,
            String linkTypeIfExists) {
        final ODObject parent = createParentIfNeededForCollection(collectionRef, parentIfExists, name, objectType);
        Iterator<Value> iterator = getIterator(thread, collectionRef);
        int i = 0;
        while (iterator.hasNext()) {
            final Value value = iterator.next();
            final String obName = String.valueOf(i);
            this.convertValue(
                    value,
                    obName,
                    value.type().name(),
                    parent,
                    parent.equals(parentIfExists) ? linkTypeIfExists : obName, true); // link type is just the index in case of root collections.
            i++;
        }
    }

    private void convertMap(
            String name,
            ObjectReference mapRef,
            String objectType,
            ODObject parentIfExists,
            String linkTypeIfExists) {
        final ODObject parent = createParentIfNeededForCollection(mapRef, parentIfExists, name, objectType);
        ObjectReference entrySet = (ObjectReference) invokeSimple(thread, mapRef, "entrySet");
        Iterator<Value> iterator = getIterator(thread, entrySet);
        int i = 0;
        while (iterator.hasNext()) {
            ObjectReference entry = (ObjectReference) iterator.next();
            final Value keyValue = invokeSimple(thread, entry, "getKey");
            final Value valueValue = invokeSimple(thread, entry, "getValue");

            final ODObject entryObject = new ODObject(entry.uniqueID(), entry.referenceType().name(), String.valueOf(i));

            this.debuggingVisualizer.addObject(entryObject);
            this.debuggingVisualizer.addLinkToObject(
                    parent,
                    entryObject,
                    i + (parentIfExists != null ? linkTypeIfExists : ""));

            if (keyValue != null) {
                this.convertValue(
                        keyValue,
                        KEY,
                        keyValue.type() == null ? "" : keyValue.type().name(),
                        entryObject,
                        KEY,
                        true);
            }
            if (valueValue != null) {
                this.convertValue(
                        valueValue,
                        VALUE,
                        valueValue.type() == null ? "" : valueValue.type().name(),
                        entryObject,
                        VALUE,
                        true);
            }
            i++;
        }
    }

    private void convertVariable(
            LocalVariable localVariable,
            StackFrame stackFrame,
            ODObject parentIfExists,
            String linkTypeIfExists) {
        final Value variableValue = stackFrame.getValue(localVariable);
        final String variableName = localVariable.name();
        final String variableType = localVariable.typeName();
        this.convertValue(variableValue, variableName, variableType, parentIfExists, linkTypeIfExists, false);
    }

    private void convertValue(
            Value variableValue,
            String variableName,
            String variableType,
            ODObject parentIfExists,
            String linkTypeIfExists,
            boolean exploreObjects) {
        if (variableValue instanceof BooleanValue) {
            final String value = String.valueOf(((BooleanValue) variableValue).value());
            this.addVariableToDiagram(variableName, variableType, value, parentIfExists);
            return;
        }
        if (variableValue instanceof ByteValue) {
            final String value = String.valueOf(((ByteValue) variableValue).value());
            this.addVariableToDiagram(variableName, variableType, value, parentIfExists);
            return;
        }
        if (variableValue instanceof ShortValue) {
            final String value = String.valueOf(((ShortValue) variableValue).value());
            this.addVariableToDiagram(variableName, variableType, value, parentIfExists);
            return;
        }
        if (variableValue instanceof IntegerValue) {
            final String value = Integer.toString(((IntegerValue) variableValue).value());
            this.addVariableToDiagram(variableName, variableType, value, parentIfExists);
            return;
        }
        if (variableValue instanceof LongValue) {
            final String value = Long.toString(((LongValue) variableValue).value());
            this.addVariableToDiagram(variableName, variableType, value, parentIfExists);
            return;
        }
        if (variableValue instanceof FloatValue) {
            final String value = Float.toString(((FloatValue) variableValue).value());
            this.addVariableToDiagram(variableName, variableType, value, parentIfExists);
            return;
        }
        if (variableValue instanceof DoubleValue) {
            final String value = Double.toString(((DoubleValue) variableValue).value());
            this.addVariableToDiagram(variableName, variableType, value, parentIfExists);
            return;
        }
        if (variableValue instanceof CharValue) {
            final String value = Character.toString(((CharValue) variableValue).value());
            this.addVariableToDiagram(variableName, variableType, String.format("'%s'", value), parentIfExists);
            return;
        }
        if (variableValue instanceof StringReference) {
            final String value = ((StringReference) variableValue).value();
            this.addVariableToDiagram(variableName, variableType, String.format("\"%s\"", value), parentIfExists);
            return;
        }
        ObjectReference obj = (ObjectReference) variableValue;
        if (obj == null) {
            this.addVariableToDiagram(variableName, variableType, "null", parentIfExists);
            return;
        }

        final ODObject odObject = new ODObject(obj.uniqueID(), variableType, variableName);
        if (exploreObjects) {
            this.exploreObjectReference(obj, odObject, parentIfExists, linkTypeIfExists);
        } else {
            this.rootObjects.put(obj.uniqueID(), Pair.create(obj, odObject));
        }
    }

    private void addVariableToDiagram(String variableName, String variableType, String value, ODObject parentIfExists) {
        if (parentIfExists != null) {
            this.debuggingVisualizer.addAttributeToObject(parentIfExists, variableName, value, variableType);
        } else {
            this.debuggingVisualizer.addPrimitiveRootValue(variableName, variableType, value);
        }
    }


    private StackFrame getCorrectStackFrame(XDebugSession debugSession) {
        SuspendContext sc = (SuspendContext) debugSession.getSuspendContext();
        thread = sc.getThread().getThreadReference();
        try {
            // TODO: We boldy assume the first stack frame is the right one, which it seems to be.
            final Optional<StackFrame> first = thread.frames().stream().findFirst();
            if (first.isPresent()) {
                return first.get();
            }
        } catch (IncompatibleThreadStateException e) {
            LOGGER.error(e);
            throw new RuntimeException("Correct stack frame for debugging not found!", e);
        }
        throw new RuntimeException("Correct stack frame for debugging not found!");
    }
}
