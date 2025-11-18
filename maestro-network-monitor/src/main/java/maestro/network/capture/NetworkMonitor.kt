package maestro.network.capture

import maestro.network.model.NetworkEvent
import maestro.network.model.NetworkMonitorConfig
import maestro.network.model.NetworkSession

/**
 * Interface for network monitoring implementations
 */
interface NetworkMonitor {
    
    /**
     * Start monitoring network traffic
     * @param config Configuration for the monitoring session
     */
    suspend fun start(config: NetworkMonitorConfig)
    
    /**
     * Stop monitoring network traffic
     */
    suspend fun stop()
    
    /**
     * Check if monitoring is currently active
     */
    fun isActive(): Boolean
    
    /**
     * Get the current network session
     */
    fun getCurrentSession(): NetworkSession?
    
    /**
     * Add a listener for network events
     */
    fun addListener(listener: NetworkEventListener)
    
    /**
     * Remove a listener
     */
    fun removeListener(listener: NetworkEventListener)
}

/**
 * Listener interface for network events
 */
interface NetworkEventListener {
    /**
     * Called when a new network event is captured
     */
    fun onNetworkEvent(event: NetworkEvent)
    
    /**
     * Called when an error occurs during monitoring
     */
    fun onError(error: Throwable)
}

/**
 * Factory for creating platform-specific network monitors
 */
object NetworkMonitorFactory {
    
    /**
     * Create a network monitor for the current platform
     * @param platform The platform type (iOS or Android)
     * @param flowName The name of the flow being monitored
     */
    fun create(platform: Platform, flowName: String): NetworkMonitor {
        return when (platform) {
            Platform.IOS -> createIOSMonitor(flowName)
            Platform.ANDROID -> createAndroidMonitor(flowName)
        }
    }
    
    private fun createIOSMonitor(flowName: String): NetworkMonitor {
        // Will be implemented in platform-specific module
        throw NotImplementedError("iOS network monitoring not yet implemented")
    }
    
    private fun createAndroidMonitor(flowName: String): NetworkMonitor {
        // Will be implemented in platform-specific module
        throw NotImplementedError("Android network monitoring not yet implemented")
    }
}

/**
 * Supported platforms
 */
enum class Platform {
    IOS,
    ANDROID
}
