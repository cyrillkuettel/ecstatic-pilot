package li.garteroboter.pren.network

/** We want to manipulate data in GlobalstateViewmodel from WebsocketManager,
 * instead of getting a reference of the viewmodel, we use a custom listener*/
interface GlobalStateListener {
    fun triggerStop()
}