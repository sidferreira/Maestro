package maestro.network.model

/**
 * Type of network event
 */
enum class NetworkEventType {
    REQUEST,
    RESPONSE,
    ERROR,
    TIMEOUT
}

/**
 * Represents a single network event (request or response)
 */
data class NetworkEvent(
    val id: String,
    val timestamp: Long,
    val type: NetworkEventType,
    val protocol: String, // HTTP, HTTPS, WebSocket, etc.
    val method: String? = null, // GET, POST, etc.
    val url: String,
    val statusCode: Int? = null,
    val requestHeaders: Map<String, String> = emptyMap(),
    val responseHeaders: Map<String, String> = emptyMap(),
    val requestBody: ByteArray? = null,
    val responseBody: ByteArray? = null,
    val duration: Long? = null, // milliseconds
    val bytesReceived: Long = 0,
    val bytesSent: Long = 0,
    val error: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NetworkEvent

        if (id != other.id) return false
        if (timestamp != other.timestamp) return false
        if (type != other.type) return false
        if (protocol != other.protocol) return false
        if (method != other.method) return false
        if (url != other.url) return false
        if (statusCode != other.statusCode) return false
        if (requestHeaders != other.requestHeaders) return false
        if (responseHeaders != other.responseHeaders) return false
        if (requestBody != null) {
            if (other.requestBody == null) return false
            if (!requestBody.contentEquals(other.requestBody)) return false
        } else if (other.requestBody != null) return false
        if (responseBody != null) {
            if (other.responseBody == null) return false
            if (!responseBody.contentEquals(other.responseBody)) return false
        } else if (other.responseBody != null) return false
        if (duration != other.duration) return false
        if (bytesReceived != other.bytesReceived) return false
        if (bytesSent != other.bytesSent) return false
        if (error != other.error) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + protocol.hashCode()
        result = 31 * result + (method?.hashCode() ?: 0)
        result = 31 * result + url.hashCode()
        result = 31 * result + (statusCode ?: 0)
        result = 31 * result + requestHeaders.hashCode()
        result = 31 * result + responseHeaders.hashCode()
        result = 31 * result + (requestBody?.contentHashCode() ?: 0)
        result = 31 * result + (responseBody?.contentHashCode() ?: 0)
        result = 31 * result + (duration?.hashCode() ?: 0)
        result = 31 * result + bytesReceived.hashCode()
        result = 31 * result + bytesSent.hashCode()
        result = 31 * result + (error?.hashCode() ?: 0)
        return result
    }
}

/**
 * Represents a complete network monitoring session for a flow
 */
data class NetworkSession(
    val flowName: String,
    val startTime: Long,
    val endTime: Long?,
    val events: List<NetworkEvent> = emptyList(),
    val totalRequests: Int = 0,
    val totalResponses: Int = 0,
    val totalErrors: Int = 0,
    val totalBytesReceived: Long = 0,
    val totalBytesSent: Long = 0,
    val domains: Set<String> = emptySet()
) {
    /**
     * Calculate summary statistics
     */
    fun calculateStats(): NetworkStats {
        val avgDuration = events
            .mapNotNull { it.duration }
            .takeIf { it.isNotEmpty() }
            ?.average()?.toLong()
        
        val successfulRequests = events.count { it.statusCode in 200..299 }
        val failedRequests = events.count { it.statusCode in 400..599 }
        
        return NetworkStats(
            totalRequests = totalRequests,
            successfulRequests = successfulRequests,
            failedRequests = failedRequests,
            totalErrors = totalErrors,
            totalBytesReceived = totalBytesReceived,
            totalBytesSent = totalBytesSent,
            averageDuration = avgDuration,
            uniqueDomains = domains.size
        )
    }
}

/**
 * Network statistics summary
 */
data class NetworkStats(
    val totalRequests: Int,
    val successfulRequests: Int,
    val failedRequests: Int,
    val totalErrors: Int,
    val totalBytesReceived: Long,
    val totalBytesSent: Long,
    val averageDuration: Long?,
    val uniqueDomains: Int
)

/**
 * Configuration for network monitoring
 */
data class NetworkMonitorConfig(
    val enabled: Boolean = false,
    val captureHttps: Boolean = false,
    val maxBodySize: Int = 1024 * 1024, // 1MB
    val includedDomains: Set<String> = emptySet(),
    val excludedDomains: Set<String> = emptySet(),
    val captureHeaders: Boolean = true,
    val captureBody: Boolean = false
)
