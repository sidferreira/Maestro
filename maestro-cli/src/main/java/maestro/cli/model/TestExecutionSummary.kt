package maestro.cli.model

import kotlin.time.Duration

// TODO: Some properties should be implemented as getters, but it's not possible.
//  See https://github.com/Kotlin/kotlinx.serialization/issues/805
data class TestExecutionSummary(
    val passed: Boolean,
    val suites: List<SuiteResult>,
    val passedCount: Int? = null,
    val totalTests: Int? = null,
) {

    data class SuiteResult(
        val passed: Boolean,
        val flows: List<FlowResult>,
        val duration: Duration? = null,
        val startTime: Long? = null,
        val deviceName: String? = null,
    ) {
        fun failures(): List<FlowResult> = flows.filter { it.status == FlowStatus.ERROR }
    }

    data class FlowResult(
        val name: String,
        val fileName: String?,
        val status: FlowStatus,
        val failure: Failure? = null,
        val duration: Duration? = null,
        val startTime: Long? = null,
        val networkSession: NetworkSessionSummary? = null,
    )

    data class Failure(
        val message: String,
    )
    
    /**
     * Network monitoring session summary
     */
    data class NetworkSessionSummary(
        val totalRequests: Int,
        val successfulRequests: Int,
        val failedRequests: Int,
        val totalErrors: Int,
        val totalBytesReceived: Long,
        val totalBytesSent: Long,
        val averageDuration: Long?,
        val uniqueDomains: Int,
        val domains: Set<String>,
        val events: List<NetworkEventSummary> = emptyList(),
    )
    
    /**
     * Network event summary
     */
    data class NetworkEventSummary(
        val id: String,
        val timestamp: Long,
        val type: String, // REQUEST, RESPONSE, ERROR, TIMEOUT
        val method: String?,
        val url: String,
        val statusCode: Int?,
        val duration: Long?,
        val bytesReceived: Long,
        val bytesSent: Long,
    )
}
