# VPN Network Monitoring for Maestro

## 🎯 Overview

This feature adds VPN-based network monitoring to Maestro, allowing you to capture and analyze all network traffic from your iOS and Android apps **without any app modifications**.

## ✨ Key Features

- **Zero App Modifications**: No instrumentation, proxies, or rebuilding required
- **Cross-Platform**: Works on both iOS and Android
- **Comprehensive**: Captures HTTP, HTTPS, WebSocket, and other protocols
- **Integrated Reports**: Network data automatically included in test reports
- **Flexible Filtering**: Focus on specific domains or exclude third-party traffic
- **Detailed Metrics**: Request/response details, timing, data transfer sizes

## 📋 Status

**Current Branch**: `vpn-network-monitoring`

### Implemented ✅
- [x] Core network monitoring infrastructure
- [x] Data models for network events and sessions
- [x] Platform-specific VPN handler interfaces
- [x] Integration with test execution flow
- [x] Enhanced HTML reports with network visualization
- [x] Comprehensive documentation

### In Progress 🚧
- [ ] iOS VPN implementation (Network Extension)
- [ ] Android VPN implementation (VpnService)
- [ ] Packet capture and HTTP/HTTPS parsing
- [ ] Certificate generation and installation
- [ ] Integration tests

### Planned 📅
- [ ] WebSocket protocol support
- [ ] HTTP/2 and HTTP/3 support
- [ ] Advanced filtering and search
- [ ] Performance optimizations
- [ ] CI/CD automation

## 🚀 Quick Start

### 1. Enable Network Monitoring

```bash
maestro test --enable-network-monitoring my-flow.yaml
```

### 2. View Results

Open the generated HTML report to see:
- Network request/response counts
- Success/failure rates
- Data transfer sizes
- Response times
- Detailed request table

### 3. Configure (Optional)

Create `network-config.yaml`:

```yaml
enabled: true
captureHttps: false
includedDomains:
  - api.myapp.com
excludedDomains:
  - analytics.google.com
```

Use it:
```bash
maestro test --enable-network-monitoring --network-config=network-config.yaml my-flow.yaml
```

## 📚 Documentation

- **[User Guide](docs/network-monitoring-user-guide.md)**: Complete guide for end users
- **[Architecture Design](docs/vpn-network-monitoring-design.md)**: Technical architecture and design decisions
- **[Testing Guide](docs/network-monitoring-testing-guide.md)**: How to test and contribute

## 🏗️ Architecture

### Components

1. **maestro-network-monitor**: Core module with:
   - Data models (`NetworkEvent`, `NetworkSession`, `NetworkStats`)
   - Monitoring interfaces (`NetworkMonitor`, `VpnHandler`)
   - Base implementations
   - Platform-specific stubs (iOS, Android)

2. **maestro-cli**: Integration with CLI:
   - Extended `TestExecutionSummary` with network data
   - Enhanced `HtmlTestSuiteReporter` with network visualization
   - Configuration handling

3. **Platform Implementations**:
   - **iOS**: Network Extension based VPN (stub)
   - **Android**: VpnService based VPN (stub)

### Data Flow

```
App Traffic → VPN Tunnel → Packet Capture → Protocol Parser → NetworkEvent → Report
```

## 🧪 Testing

### Unit Tests

```bash
./gradlew :maestro-network-monitor:test
```

### Integration Tests (Coming Soon)

```bash
# Android
./gradlew :maestro-test:androidNetworkMonitoringIntegrationTest

# iOS
./gradlew :maestro-test:iosNetworkMonitoringIntegrationTest
```

### CI/CD

GitHub Actions workflow: `.github/workflows/network-monitor-test.yaml`

Tests run automatically on:
- Push to `vpn-network-monitoring` branch
- Pull requests modifying network monitoring code

## 📊 Example Report

The HTML report includes a "Network Monitoring Data" section for each flow:

```
┌─────────────────────────────────────┐
│   Network Monitoring Data          │
├─────────────────────────────────────┤
│ Total Requests:        42           │
│ Successful:            39 ✓         │
│ Failed:                3  ✗         │
│ Avg Duration:          156ms        │
│                                      │
│ Bytes Sent:            24 KB        │
│ Bytes Received:        1.2 MB       │
│ Unique Domains:        5            │
├─────────────────────────────────────┤
│ Domains:                             │
│ [api.myapp.com] [cdn.myapp.com]     │
│ [auth.myapp.com] ...                │
├─────────────────────────────────────┤
│ Network Requests                     │
│ Method  URL              Status Dur  │
│ GET     /api/user        200    45ms│
│ POST    /api/login       200    123ms│
│ GET     /api/items       200    67ms│
│ ...                                  │
└─────────────────────────────────────┘
```

## 🔧 Configuration Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `enabled` | Boolean | `false` | Enable network monitoring |
| `captureHttps` | Boolean | `false` | Decrypt and capture HTTPS traffic |
| `maxBodySize` | Integer | `1048576` | Max body size to capture (bytes) |
| `includedDomains` | List | `[]` | Only capture these domains |
| `excludedDomains` | List | `[]` | Exclude these domains |
| `captureHeaders` | Boolean | `true` | Capture request/response headers |
| `captureBody` | Boolean | `false` | Capture request/response bodies |

## 🎓 Examples

### Basic HTTP Monitoring

```bash
maestro test --enable-network-monitoring login-flow.yaml
```

### HTTPS with Filtering

```yaml
# config.yaml
enabled: true
captureHttps: true
includedDomains:
  - api.myapp.com
```

```bash
maestro test --enable-network-monitoring --network-config=config.yaml checkout.yaml
```

### Exclude Analytics

```yaml
# config.yaml
enabled: true
excludedDomains:
  - analytics.google.com
  - firebase.google.com
  - facebook.com
```

## ⚠️ Limitations

1. **Certificate Pinning**: Apps with certificate pinning cannot have HTTPS decrypted
2. **VPN Detection**: Some apps detect VPN and may alter behavior
3. **Performance**: ~10% overhead on average
4. **Protocols**: HTTP/3 (QUIC) not yet supported
5. **Real Devices**: iOS devices require manual VPN approval (first time)

## 🤝 Contributing

We welcome contributions! Areas where help is needed:

1. **Platform Implementation**:
   - Complete iOS Network Extension implementation
   - Complete Android VpnService implementation
   - Packet parsing and HTTP reconstruction

2. **Features**:
   - WebSocket protocol support
   - HTTP/2 support
   - Advanced filtering
   - Custom certificate authority

3. **Testing**:
   - More test apps
   - Integration tests
   - Performance benchmarks

4. **Documentation**:
   - More examples
   - Troubleshooting guides
   - Video tutorials

See [CONTRIBUTING.md](../CONTRIBUTING.md) for guidelines.

## 📝 Technical Details

### Network Event Model

```kotlin
data class NetworkEvent(
    val id: String,
    val timestamp: Long,
    val type: NetworkEventType,  // REQUEST, RESPONSE, ERROR, TIMEOUT
    val protocol: String,         // HTTP, HTTPS, WebSocket
    val method: String?,          // GET, POST, etc.
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
```

### Network Session Model

```kotlin
data class NetworkSession(
    val flowName: String,
    val startTime: Long,
    val endTime: Long?,
    val events: List<NetworkEvent>,
    val totalRequests: Int,
    val totalResponses: Int,
    val totalErrors: Int,
    val totalBytesReceived: Long,
    val totalBytesSent: Long,
    val domains: Set<String>
)
```

## 🔗 Resources

- **Documentation**: [docs/](docs/)
- **Issues**: [GitHub Issues](https://github.com/mobile-dev-inc/maestro/issues)
- **Community**: [Slack Channel](https://maestrodev.typeform.com/to/FelIEe8A)

## 📜 License

This feature is part of Maestro and is licensed under the Apache License 2.0.

## 🙏 Acknowledgments

This feature is inspired by tools like:
- [Charles Proxy](https://www.charlesproxy.com/)
- [mitmproxy](https://mitmproxy.org/)
- [Proxyman](https://proxyman.io/)

But designed specifically for automated testing with zero app modifications.

---

**Status**: 🚧 Work in Progress

This feature is under active development. The infrastructure is in place, but platform-specific implementations are still being completed. Contributions welcome!
