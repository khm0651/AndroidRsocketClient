package hare.rsocket.client

import io.ktor.client.HttpClient
import io.rsocket.kotlin.ExperimentalMetadataApi
import io.rsocket.kotlin.RSocket
import io.rsocket.kotlin.ktor.client.rSocket
import io.rsocket.kotlin.metadata.RoutingMetadata
import io.rsocket.kotlin.metadata.metadata
import io.rsocket.kotlin.payload.Payload
import io.rsocket.kotlin.payload.buildPayload
import io.rsocket.kotlin.payload.data
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@OptIn(ExperimentalMetadataApi::class)
class RsocketClient(
    private val httpClient: HttpClient
) {

    private var rsocket: RSocket? = null

    private val _status: MutableStateFlow<RsocketStatus> = MutableStateFlow(RsocketStatus.DisConnect)
    val status: StateFlow<RsocketStatus>
        get() = _status

    private val lock = Mutex()
    suspend fun connect(
        address: String,
        port: Int,
        path: String = "/rsocket"
    ){
        lock.withLock {
            _status.value = RsocketStatus.Loading
            rsocket = httpClient.rSocket(host = address, port = port, path = path)
            rsocket?.coroutineContext?.job?.invokeOnCompletion {
                rsocket?.cancel()
                rsocket = null
                _status.value = RsocketStatus.Error(it)
            }
            _status.value = RsocketStatus.Connect
        }
    }

    suspend fun close(){
        lock.withLock {
            if(status.value !is RsocketStatus.Connect) return
            rsocket?.cancel()
            rsocket = null
            _status.value = RsocketStatus.DisConnect
        }
    }

    suspend fun fireAndForget(destination: String, data: String): Unit = rsocket?.run {
        fireAndForget(buildPayload {
            metadata(RoutingMetadata(destination))
            data(data)
        })
    }?: throw IllegalStateException("check rsocket connect status")

    suspend fun requestResponse(destination: String): Payload = rsocket?.run {
        requestResponse(buildPayload {
            metadata(RoutingMetadata(destination))
            data("")
        })
    }?: throw IllegalStateException("check rsocket connect status")

    suspend fun requestResponse(destination: String, data: String): Payload = rsocket?.run {
        requestResponse(buildPayload {
            metadata(RoutingMetadata(destination))
            data(data)
        })
    }?: throw IllegalStateException("check rsocket connect status")

    fun requestStream(destination: String): Flow<Payload> = rsocket?.run {
        requestStream(buildPayload {
            metadata(RoutingMetadata(destination))
            data("")
        })
    }?: throw IllegalStateException("check rsocket connect status")

    fun requestStream(destination: String, data: String): Flow<Payload> = rsocket?.run {
        requestStream(buildPayload {
            metadata(RoutingMetadata(destination))
            data(data)
        })
    }?: throw IllegalStateException("check rsocket connect status")

}
