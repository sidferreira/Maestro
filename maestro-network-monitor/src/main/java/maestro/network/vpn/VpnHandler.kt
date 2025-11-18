package maestro.network.vpn

import maestro.network.model.NetworkMonitorConfig

/**
 * Interface for VPN connection management
 */
interface VpnHandler {
    
    /**
     * Establish VPN connection
     * @param config Network monitoring configuration
     * @return true if VPN connection was established successfully
     */
    suspend fun connect(config: NetworkMonitorConfig): Boolean
    
    /**
     * Disconnect VPN
     */
    suspend fun disconnect()
    
    /**
     * Check if VPN is currently connected
     */
    fun isConnected(): Boolean
    
    /**
     * Get the VPN interface name (e.g., utun0, tun0)
     */
    fun getInterfaceName(): String?
}

/**
 * Base implementation for VPN handlers
 */
abstract class BaseVpnHandler : VpnHandler {
    
    protected var connected = false
    protected var interfaceName: String? = null
    
    override fun isConnected(): Boolean = connected
    
    override fun getInterfaceName(): String? = interfaceName
}
