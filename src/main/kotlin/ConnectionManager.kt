import Utils.testPacket
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

class ConnectionManager : CoroutineScope {
    override val coroutineContext: CoroutineContext = SupervisorJob() + Dispatchers.IO

    private var sslSocket: SSLSocket? = null
    private var readerJob: Job? = null
    private var sequence: Short = 0

    suspend fun init() {
        try {
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, null, null)

            val plainSocket = Socket()
            withContext(Dispatchers.IO) {
                plainSocket.connect(InetSocketAddress(HOST, PORT), 10_000)
            }

            val factory = sslContext.socketFactory as SSLSocketFactory
            val socket = factory.createSocket(plainSocket, HOST, PORT, true) as SSLSocket
            socket.enabledProtocols = arrayOf("TLSv1.3")
            socket.sslParameters.serverNames = listOf(SNIHostName(HOST))
            withContext(Dispatchers.IO) {
                socket.startHandshake()
            }

            this.sslSocket = socket

            startReader()
        } catch (e: Exception) {
            println("Error in init: ${e.message}")
            e.printStackTrace()
            close()
            throw e
        }
    }

    suspend fun send(bytes: ByteArray) {
        val socket = sslSocket ?: throw IllegalStateException("Socket is not initialized")
        try {
            withContext(Dispatchers.IO) {
                socket.outputStream.write(bytes)
                socket.outputStream.flush()
            }
        } catch (e: Exception) {
            println("Error in send: ${e.message}")
            e.printStackTrace()
            close()
            throw e
        }
    }

    suspend fun send(packet: Packet) {
        packet.cmd = 0
        packet.sequence = this.sequence
        send(packet.toBytes())
        this.sequence++
        testPacket(packet, "Outcoming packet")
    }

    private fun startReader() {
        readerJob = launch {
            val socket = sslSocket ?: return@launch
            val buffer = ByteArray(8192)
            try {
                while (isActive) {
                    val read = withContext(Dispatchers.IO) {
                        socket.inputStream.read(buffer)
                    }
                    if (read == -1) {
                        break
                    }
                    onDataReceived(buffer.copyOf(read))
                }
            } catch (e: Exception) {
                println("Error in reader: ${e.message}")
                e.printStackTrace()
            } finally {
                close()
            }
        }
    }

    fun close() {
        try {
            sslSocket?.close()
            sslSocket = null
            coroutineContext.cancel()
        } catch (e: Exception) {
            println("Error in close: ${e.message}")
            e.printStackTrace()
        }
    }

    fun onDataReceived(data: ByteArray) {
        val packet: Packet = Packet.fromBytes(data)
        testPacket(packet, name = "Incoming packet")
    }

    companion object {
        const val HOST = "api.oneme.ru"
        const val PORT = 443
    }
}