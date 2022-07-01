package li.garteroboter.pren.network

/** We want to manipulate data in GlobalStateViewModel from WebSocketManager,
 * instead of passing in the ViewModel as a Constructor Argument to WebSocketManager, a custom
 * listener is used.
 *
 * It's not recommended to pass in ViewModel to random classes, as it wants to be
 * lifecycle aware. That's why this approach is used. */

interface GlobalStateListener {
    /** Triggers all actions necessary to stop the roboter. */
    fun triggerStop()
}