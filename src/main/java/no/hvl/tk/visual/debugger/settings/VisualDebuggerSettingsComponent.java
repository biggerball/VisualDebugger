package no.hvl.tk.visual.debugger.settings;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.ComponentValidator;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;

public class VisualDebuggerSettingsComponent {
    static final String NUMBER_GREATER_EQUALS_0 = "The depth must be 0 or higher.";

    private final JPanel myMainPanel;
    private final JBTextField visualizationDepthField = new JBTextField();
    private final JBTextField loadingDepthField = new JBTextField();
    private final ComboBox<DebuggingVisualizerOption> visualizerOptionsCombobox =
            new ComboBox<>(DebuggingVisualizerOption.values());

    private final ComboBox<ActiveOption> activeOptionsCombobox =
            new ComboBox<>(ActiveOption.values());


    public VisualDebuggerSettingsComponent(final VisualDebuggerSettingsConfigurable parent) {
        this.myMainPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("Active: "), this.activeOptionsCombobox, 1, false)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();

        this.addInputFieldValidators(parent);
    }

    private void addInputFieldValidators(final Disposable parent) {
        new ComponentValidator(parent).withValidator(() -> validateDepthField(VisualDebuggerSettingsComponent.this.visualizationDepthField)).installOn(this.visualizationDepthField);
        this.visualizationDepthField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull final DocumentEvent e) {
                ComponentValidator.getInstance(VisualDebuggerSettingsComponent.this.visualizationDepthField)
                                  .ifPresent(ComponentValidator::revalidate);
            }
        });

        new ComponentValidator(parent).withValidator(() -> validateDepthField(VisualDebuggerSettingsComponent.this.loadingDepthField)).installOn(this.loadingDepthField);
        this.loadingDepthField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull final DocumentEvent e) {
                ComponentValidator.getInstance(VisualDebuggerSettingsComponent.this.loadingDepthField)
                                  .ifPresent(ComponentValidator::revalidate);
            }
        });
    }

    @Nullable
    static ValidationInfo validateDepthField(JBTextField depthField) {
        final String enteredDepth = depthField.getText();
        if (StringUtil.isEmpty(enteredDepth) || !StringUtils.isNumeric(enteredDepth)) {
            return new ValidationInfo(
                    VisualDebuggerSettingsComponent.NUMBER_GREATER_EQUALS_0,
                    depthField);
        }
        int depth = Integer.parseInt(enteredDepth);
        if (depth < 0) {
            return new ValidationInfo(
                    VisualDebuggerSettingsComponent.NUMBER_GREATER_EQUALS_0,
                    depthField);
        }
        // Means everything is ok.
        return null;
    }

    public JPanel getPanel() {
        return this.myMainPanel;
    }

    public JComponent getPreferredFocusedComponent() {
        return this.visualizationDepthField;
    }

    @NotNull
    public String getVisualizationDepthText() {
        return this.visualizationDepthField.getText();
    }

    public void setVisualizationDepthText(@NotNull final String visualizationDepth) {
        this.visualizationDepthField.setText(visualizationDepth);
    }

    public ActiveOption getActiveOption() {
        return this.activeOptionsCombobox.getItem();
    }

    public void setActiveOptionsCombobox(ActiveOption activeOption) {
        this.activeOptionsCombobox.setItem(activeOption);
    }

    public DebuggingVisualizerOption getDebuggingVisualizerOptionChoice() {
        return this.visualizerOptionsCombobox.getItem();
    }

    public void chooseDebuggingVisualizerOption(final DebuggingVisualizerOption option) {
        this.visualizerOptionsCombobox.setItem(option);
    }

    public void setLoadingDepthText(@NotNull final String loadingDepth) {
        this.loadingDepthField.setText(loadingDepth);
    }

    @NotNull
    public String getLoadingDepthText() {
        return loadingDepthField.getText();
    }
}
