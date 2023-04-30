package hare.rsocket.client

sealed class RsocketStatus {
    object Loading: RsocketStatus()
    object Connect: RsocketStatus()
    object DisConnect: RsocketStatus()
    data class Error(val exception: Throwable?): RsocketStatus()
}