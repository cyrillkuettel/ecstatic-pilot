package li.garteroboter.pren.socket;

public enum SocketType {
    /* different use cases for Websocket */
    Text("999"), Binary("888"), Command("777");

    public final String type;

    SocketType(String type) {
        this.type = type;
    }
}
