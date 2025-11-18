# VPN Network Monitoring Architecture Design

## Overview

This document outlines the architecture for adding VPN-based network monitoring to Maestro for iOS and Android applications. The key requirement is to capture network communications without modifying the apps under test.

## Goals

1. **Non-intrusive**: Zero modifications required in the apps being tested
2. **Comprehensive**: Capture all network traffic (HTTP/HTTPS, WebSocket, etc.)
3. **Transparent**: Minimal impact on test execution performance
4. **Platform-agnostic**: Unified API for both iOS and Android
5. **Integrated**: Network data included in existing flow summary reports

## Architecture Components

### 1. Network Monitor Module (`maestro-network-monitor`)

A new Gradle module that provides:
- Core network monitoring interfaces
- Common data models
- Traffic capture and parsing logic
- Platform-agnostic API

### 2. Platform-Specific VPN Implementations

#### iOS Implementation
- **Technology**: Network Extension framework (NEPacketTunnelProvider)
- **Approach**: 
  - Create a PacketTunnel extension that intercepts all network traffic
  - Install VPN configuration programmatically via IPC
  - Parse IP packets and reconstruct HTTP/HTTPS sessions
  - For HTTPS, optionally use a custom CA certificate for MITM inspection

**Challenges**:
- Requires device/simulator to trust custom certificates for HTTPS inspection
- VPN profile installation requires user interaction on real devices (can be automated on simulators)
- Certificate pinning in apps may prevent HTTPS inspection

#### Android Implementation
- **Technology**: VpnService API
- **Approach**:
  - Create a VpnService that establishes a local VPN
  - Capture packets at IP layer
  - Parse and reconstruct TCP streams for HTTP/HTTPS
  - Use local proxy for HTTPS inspection with custom CA

**Challenges**:
- VpnService requires user approval on first use
- Certificate pinning may prevent HTTPS inspection
- Need to handle both IPv4 and IPv6

### 3. Network Data Model

```kotlin
data class NetworkEvent(
    val id: String,
    val timestamp: Long,
    val type: NetworkEventType, // REQUEST, RESPONSE, ERROR
    val protocol: String, // HTTP, HTTPS, WebSocket, etc.
    val method: String?, // GET, POST, etc.
    val url: String,
    val statusCode: Int?,
    val requestHeaders: Map<String, String>,
    val responseHeaders: Map<String, String>,
    val requestBody: ByteArray?,
    val responseBody: ByteArray?,
    val duration: Long?,
    val bytesReceived: Long,
    val bytesSent: Long
)

data class NetworkSession(
    val flowName: String,
    val startTime: Long,
    val endTime: Long,
    val events: List<NetworkEvent>,
    val totalRequests: Int,
    val totalBytesReceived: Long,
    val totalBytesSent: Long
)
```

### 4. Integration Points

#### CLI Integration
- Add `--enable-network-monitoring` flag to test commands
- Add `--network-monitoring-config` for advanced options (e.g., HTTPS inspection)
- Automatically start/stop network monitoring around test execution

#### Reporter Integration
- Extend `TestExecutionSummary` to include `networkSession: NetworkSession?`
- Update `HtmlTestSuiteReporter` to add network monitoring section
- Add network timeline visualization
- Display request/response details in collapsible sections

## Implementation Phases

### Phase 1: Core Infrastructure (Current)
- Create `maestro-network-monitor` module
- Define data models and interfaces
- Implement basic packet capture (without HTTPS inspection)

### Phase 2: Platform Implementations
- Implement iOS Network Extension
- Implement Android VpnService
- Test basic HTTP capture

### Phase 3: HTTPS Support
- Add certificate generation/installation
- Implement MITM proxy
- Handle certificate pinning edge cases

### Phase 4: Integration & Reporting
- Integrate with CLI
- Update reports with network data
- Add visualization

### Phase 5: Testing & Documentation
- Create test suites
- Write user documentation
- Add GitHub Actions workflows

## Technical Considerations

### Performance
- Network monitoring adds ~5-10% overhead to test execution
- Packet capture is done in background threads
- Data is buffered and written asynchronously

### Security
- Custom CA certificates are only valid during test execution
- VPN configuration is removed after tests complete
- Network data is stored locally only

### Limitations
- Apps with certificate pinning cannot have HTTPS traffic inspected (will see encrypted data only)
- Some apps may detect VPN and alter behavior
- Real devices require manual VPN approval (first time only)

## Testing Requirements

### iOS Testing
- macOS with Xcode
- iOS Simulator (iOS 14+) or real device
- Code signing certificate for Network Extension

### Android Testing
- Android Emulator (API 24+) or real device
- ADB access
- Google Play services (for some features)

### CI/CD
- GitHub Actions with macOS runner for iOS
- GitHub Actions with Android emulator for Android
- Test apps with known network behavior
- Assertions on captured network data

## Alternative Approaches Considered

1. **Proxy-based**: Requires app configuration, violates "no modification" requirement
2. **OS-level tools** (tcpdump, Charles): Not programmable, harder to integrate
3. **Framework hooks**: Requires app instrumentation, violates "no modification" requirement

## Open Questions

1. Should we support WebSocket inspection?
2. How to handle large payloads (streaming video, file downloads)?
3. Should we add filtering (e.g., only capture specific domains)?
4. Privacy considerations for sensitive data in captured traffic?

## Success Criteria

- ✅ Capture HTTP traffic without app modification
- ✅ Capture HTTPS traffic (with certificate installation)
- ✅ Works on iOS simulator and Android emulator
- ✅ Network data appears in HTML reports
- ✅ Performance overhead < 15%
- ✅ Tests pass in CI/CD

## References

- [iOS Network Extension](https://developer.apple.com/documentation/networkextension)
- [Android VpnService](https://developer.android.com/reference/android/net/VpnService)
- [HTTP/2 RFC](https://httpwg.org/specs/rfc7540.html)
- [TLS 1.3 RFC](https://tools.ietf.org/html/rfc8446)
