package maestro.network.capture

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import maestro.network.model.NetworkEvent
import maestro.network.model.NetworkMonitorConfig
import maestro.network.model.NetworkSession
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Base implementation of NetworkMonitor with common functionality
 */
abstract class BaseNetworkMonitor(
    private val flowName: String
) : NetworkMonitor {
    
    protected var config: NetworkMonitorConfig? = null
    protected var isMonitoring = false
    private val mutex = Mutex()
    
    private val listeners = CopyOnWriteArrayList<NetworkEventListener>()
    private val events = mutableListOf<NetworkEvent>()
    private var sessionStartTime: Long = 0
    
    override suspend fun start(config: NetworkMonitorConfig) {
        mutex.withLock {
            if (isMonitoring) {
                throw IllegalStateException("Network monitoring is already active")
            }
            
            this.config = config
            this.sessionStartTime = System.currentTimeMillis()
            this.events.clear()
            
            startPlatformSpecificMonitoring(config)
            isMonitoring = true
        }
    }
    
    override suspend fun stop() {
        mutex.withLock {
            if (!isMonitoring) {
                return
            }
            
            stopPlatformSpecificMonitoring()
            isMonitoring = false
        }
    }
    
    override fun isActive(): Boolean = isMonitoring
    
    override fun getCurrentSession(): NetworkSession? {
        if (!isMonitoring && events.isEmpty()) {
            return null
        }
        
        val domains = events.mapNotNull { event ->
            try {
                java.net.URL(event.url).host
            } catch (e: Exception) {
                null
            }
        }.toSet()
        
        val requests = events.count { it.type == maestro.network.model.NetworkEventType.REQUEST }
        val responses = events.count { it.type == maestro.network.model.NetworkEventType.RESPONSE }
        val errors = events.count { 
            it.type == maestro.network.model.NetworkEventType.ERROR || 
            it.type == maestro.network.model.NetworkEventType.TIMEOUT 
        }
        
        val totalBytesSent = events.sumOf { it.bytesSent }
        val totalBytesReceived = events.sumOf { it.bytesReceived }
        
        return NetworkSession(
            flowName = flowName,
            startTime = sessionStartTime,
            endTime = if (isMonitoring) null else System.currentTimeMillis(),
            events = events.toList(),
            totalRequests = requests,
            totalResponses = responses,
            totalErrors = errors,
            totalBytesReceived = totalBytesReceived,
            totalBytesSent = totalBytesSent,
            domains = domains
        )
    }
    
    override fun addListener(listener: NetworkEventListener) {
        listeners.add(listener)
    }
    
    override fun removeListener(listener: NetworkEventListener) {
        listeners.remove(listener)
    }
    
    /**
     * Called by platform-specific implementations when a network event is captured
     */
    protected fun onEventCaptured(event: NetworkEvent) {
        // Check if domain filtering is enabled
        config?.let { cfg ->
            val domain = try {
                java.net.URL(event.url).host
            } catch (e: Exception) {
                null
            }
            
            // Skip if domain is excluded
            if (domain != null && cfg.excludedDomains.isNotEmpty() && domain in cfg.excludedDomains) {
                return
            }
            
            // Skip if domain is not in included list (when list is not empty)
            if (domain != null && cfg.includedDomains.isNotEmpty() && domain !in cfg.includedDomains) {
                return
            }
        }
        
        events.add(event)
        notifyListeners(event)
    }
    
    /**
     * Called by platform-specific implementations when an error occurs
     */
    protected fun onMonitoringError(error: Throwable) {
        listeners.forEach { listener ->
            try {
                listener.onError(error)
            } catch (e: Exception) {
                // Ignore listener errors
            }
        }
    }
    
    private fun notifyListeners(event: NetworkEvent) {
        listeners.forEach { listener ->
            try {
                listener.onNetworkEvent(event)
            } catch (e: Exception) {
                // Ignore listener errors
            }
        }
    }
    
    /**
     * Platform-specific implementation should start monitoring
     */
    protected abstract suspend fun startPlatformSpecificMonitoring(config: NetworkMonitorConfig)
    
    /**
     * Platform-specific implementation should stop monitoring
     */
    protected abstract suspend fun stopPlatformSpecificMonitoring()
}
