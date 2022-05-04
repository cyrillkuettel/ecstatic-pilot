package li.garteroboter.pren.socket;

public enum SocketType {
    /* There are two different use cases for Websocket */
    Text("999"), Binary("888");

    public final String id;

     SocketType(String id) {
        this.id = id;
    }
}
