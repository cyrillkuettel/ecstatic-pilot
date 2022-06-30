package li.garteroboter.pren.network;

/**
 * Context: We want to be able to access the Websocket Manger from other places. Like,
 * for example form inside other fragments.
 */
public interface WebSocketManagerInstance {
     // get reference from  WebsocketManger
    WebSocketManager getManager();
}
