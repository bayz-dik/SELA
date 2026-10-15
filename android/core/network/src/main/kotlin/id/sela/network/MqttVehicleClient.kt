package id.sela.network

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.buffer
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

data class MqttVehicle(
    val id: String,
    val routeId: String,
    val lat: Double,
    val lon: Double,
    val bearing: Float = 0f,
    val speedKmh: Double? = null,
    val observedAt: String,
    val tripId: String? = null,
)

@Singleton
class MqttVehicleClient @Inject constructor(
    private val gson: Gson,
) {
    companion object {
        const val BROKER_URL = "wss://mqtt.tj.co.id:8084/mqtt"
        const val TOPIC = "/mobile_armada/#"
    }

    private fun generateClientId(): String = newClientId()

    internal fun newClientId(): String {
        val hex = (1..8).map { Random.nextInt(16).toString(16) }.joinToString("")
        return "mqttjs_$hex"
    }

    fun vehicles(): Flow<MqttVehicle> = callbackFlow {
        val clientId = newClientId()
        val client = MqttClient(BROKER_URL, clientId, MemoryPersistence())

        val options = MqttConnectOptions().apply {
            isAutomaticReconnect = true
            isCleanSession = true
            connectionTimeout = 15
            keepAliveInterval = 30
        }

        client.setCallback(object : MqttCallbackExtended {
            override fun connectionLost(cause: Throwable?) {
                // Auto-reconnect handles this
            }

            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                // Re-subscribe after auto-reconnect (Bug #2 fix)
                // Paho auto-reconnect does not re-subscribe automatically
                try {
                    if (reconnect) {
                        client.subscribe(TOPIC, 0)
                    }
                } catch (_: Exception) {
                    // Will retry on next reconnect
                }
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                try {
                    val payload = message?.toString() ?: return
                    val json = gson.fromJson(payload, JsonObject::class.java) ?: return
                    val vehicle = parseVehicle(topic ?: return, json) ?: return
                    trySend(vehicle)
                } catch (_: Exception) {
                    // Skip malformed messages
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) = Unit
        })

        try {
            client.connect(options)
            client.subscribe(TOPIC, 0)
        } catch (e: Exception) {
            close(e)
            return@callbackFlow
        }

        awaitClose {
            try {
                if (client.isConnected) client.disconnect()
                client.close()
            } catch (_: Exception) {}
        }
    }.buffer(64)

    /** Parsing payload MQTT TransJakarta. Internal supaya bisa di-unit-test. */
    internal fun parseVehicle(topic: String, json: JsonObject): MqttVehicle? {
        // Topic: /mobile_armada/<namespace>/<route>/<trip>/<bus>
        val parts = topic.split("/")
        if (parts.size < 6) return null

        val route = (json.get("route_code")?.asStringOrNull() ?: parts.getOrNull(3) ?: return null).uppercase()
        val busId = json.get("bus_body_no")?.asStringOrNull()?.takeIf { it.isNotBlank() }
            ?: parts.getOrNull(5) ?: return null
        val tripId = json.get("trip_id")?.asStringOrNull() ?: parts.getOrNull(4)

        // Prefer snapped coordinates, fallback to raw
        val lat = json.get("snap_latitude")?.asDoubleOrNull()
            ?: json.get("latitude")?.asDoubleOrNull()
            ?: return null
        val lon = json.get("snap_longitude")?.asDoubleOrNull()
            ?: json.get("longitude")?.asDoubleOrNull()
            ?: return null

        // Validate Jakarta bounds
        if (lat < -7.5 || lat > -5 || lon < 105 || lon > 108) return null

        val bearing = json.get("bearing")?.asDoubleOrNull()?.let {
            (((it % 360) + 360) % 360).toFloat()
        } ?: 0f

        val timestamp = json.get("gps_timestamp")?.asStringOrNull()
            ?: json.get("timestamp")?.asStringOrNull()
            ?: java.time.Instant.now().toString()

        return MqttVehicle(
            id = busId,
            routeId = route,
            lat = lat,
            lon = lon,
            bearing = bearing,
            observedAt = timestamp,
            tripId = tripId,
        )
    }

    /**
     * B17: versi aman dari [com.google.gson.JsonElement.getAsString] — tidak
     * melempar [UnsupportedOperationException] bila field = JsonNull,
     * dan mengembalikan null untuk primitif non-string (number/boolean)
     * maupun object/array. Mengikuti pola [asDoubleOrNull] di bawah.
     */
    private fun com.google.gson.JsonElement.asStringOrNull(): String? = try {
        when {
            isJsonNull -> null
            isJsonPrimitive -> {
                val prim = asJsonPrimitive
                if (prim.isString) prim.asString else null
            }
            else -> null
        }
    } catch (_: Exception) { null }

    private fun com.google.gson.JsonElement.asDoubleOrNull(): Double? = try {
        when {
            isJsonPrimitive -> {
                val prim = asJsonPrimitive
                when {
                    prim.isNumber -> prim.asDouble
                    prim.isString -> prim.asString.toDoubleOrNull()
                    else -> null
                }
            }
            else -> null
        }
    } catch (_: Exception) { null }
}
