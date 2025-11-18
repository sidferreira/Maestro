package maestro.network.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class NetworkModelsTest {
    
    @Test
    fun `NetworkEvent should be created with required fields`() {
        val event = NetworkEvent(
            id = "1",
            timestamp = System.currentTimeMillis(),
            type = NetworkEventType.REQUEST,
            protocol = "HTTP",
            url = "https://example.com/api/test"
        )
        
        assertThat(event.id).isEqualTo("1")
        assertThat(event.type).isEqualTo(NetworkEventType.REQUEST)
        assertThat(event.protocol).isEqualTo("HTTP")
        assertThat(event.url).isEqualTo("https://example.com/api/test")
    }
    
    @Test
    fun `NetworkSession should calculate stats correctly`() {
        val events = listOf(
            NetworkEvent(
                id = "1",
                timestamp = 1000,
                type = NetworkEventType.REQUEST,
                protocol = "HTTP",
                method = "GET",
                url = "https://example.com/api/test",
                statusCode = 200,
                duration = 100,
                bytesSent = 100,
                bytesReceived = 500
            ),
            NetworkEvent(
                id = "2",
                timestamp = 2000,
                type = NetworkEventType.RESPONSE,
                protocol = "HTTP",
                url = "https://example.com/api/test",
                statusCode = 200,
                duration = 50,
                bytesSent = 0,
                bytesReceived = 1000
            ),
            NetworkEvent(
                id = "3",
                timestamp = 3000,
                type = NetworkEventType.ERROR,
                protocol = "HTTP",
                url = "https://api.example.com/failed",
                statusCode = 500,
                bytesSent = 50,
                bytesReceived = 100
            )
        )
        
        val session = NetworkSession(
            flowName = "test-flow",
            startTime = 1000,
            endTime = 4000,
            events = events,
            totalRequests = 2,
            totalResponses = 1,
            totalErrors = 1,
            totalBytesReceived = 1600,
            totalBytesSent = 150,
            domains = setOf("example.com", "api.example.com")
        )
        
        val stats = session.calculateStats()
        
        assertThat(stats.totalRequests).isEqualTo(2)
        assertThat(stats.successfulRequests).isEqualTo(2)
        assertThat(stats.failedRequests).isEqualTo(1)
        assertThat(stats.totalErrors).isEqualTo(1)
        assertThat(stats.totalBytesReceived).isEqualTo(1600)
        assertThat(stats.totalBytesSent).isEqualTo(150)
        assertThat(stats.uniqueDomains).isEqualTo(2)
        assertThat(stats.averageDuration).isEqualTo(75) // (100 + 50) / 2
    }
    
    @Test
    fun `NetworkMonitorConfig should have sensible defaults`() {
        val config = NetworkMonitorConfig()
        
        assertThat(config.enabled).isFalse()
        assertThat(config.captureHttps).isFalse()
        assertThat(config.maxBodySize).isEqualTo(1024 * 1024)
        assertThat(config.captureHeaders).isTrue()
        assertThat(config.captureBody).isFalse()
    }
}
