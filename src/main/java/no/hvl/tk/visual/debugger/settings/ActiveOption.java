package no.hvl.tk.visual.debugger.settings;

public enum ActiveOption {
    /**
     * Web ui visualizer using javascript and the browser.
     */
    TRUE,
    /**
     * Embedded visualizer using plant uml.
     */
    FALSE;

    @Override
    public String toString() {
        return switch (this) {
            case TRUE -> "true";
            case FALSE -> "false";
        };
    }
}
