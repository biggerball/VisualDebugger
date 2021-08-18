package no.hvl.tk.visual.debugger.webAPI;

public class ServerConstants {
    public static final String HOST_NAME = "localhost";
    public static final int DEBUG_SERVER_PORT = 8071;

    public static final String STATIC_RESOURCE_PATH = "/public/";
    public static final int UI_SERVER_PORT = 8070;

    public static final String UI_SERVER_URL = String.format(
            "http://%s:%s",
            ServerConstants.HOST_NAME,
            ServerConstants.UI_SERVER_PORT);

    private ServerConstants() {
        // Only constants in this class
    }
}
