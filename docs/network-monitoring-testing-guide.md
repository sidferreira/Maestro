# Network Monitoring Testing Guide

## Overview

This guide describes how to test the VPN network monitoring feature in Maestro, including setup requirements and CI/CD integration.

## Local Testing Requirements

### iOS Testing

**Hardware/Software**:
- macOS 12.0 or later
- Xcode 14.0 or later
- iOS Simulator (iOS 14+) or physical device
- At least 8GB RAM recommended

**Installation**:
```bash
# Install Maestro CLI
curl -fsSL "https://get.maestro.mobile.dev" | bash

# Verify installation
maestro --version
```

**Test Setup**:
1. Start iOS Simulator
2. Install a test app with network activity
3. Run network monitoring tests

### Android Testing

**Hardware/Software**:
- JDK 17 or later
- Android SDK with emulator
- Android Emulator (API 24+) or physical device
- At least 8GB RAM recommended

**Installation**:
```bash
# Install Android SDK and platform tools
# Ensure ANDROID_HOME is set

# Create an emulator
avdmanager create avd -n test-device -k "system-images;android-33;google_apis;x86_64"

# Start emulator
emulator -avd test-device
```

## Testing Strategy

### Unit Tests

Located in `maestro-network-monitor/src/test/kotlin/`

**Run unit tests**:
```bash
./gradlew :maestro-network-monitor:test
```

**Test coverage**:
- Data model serialization/deserialization
- Network event filtering
- Session statistics calculation
- VPN handler state management

### Integration Tests

Located in `maestro-cli/src/test/kotlin/`

**Run integration tests**:
```bash
./gradlew :maestro-cli:integrationTest
```

**Test scenarios**:
1. **Basic HTTP Capture**: Verify HTTP requests are captured correctly
2. **Domain Filtering**: Test include/exclude domain lists
3. **Session Management**: Start/stop monitoring correctly
4. **Report Generation**: Network data appears in HTML reports
5. **Error Handling**: Graceful failure when VPN setup fails

### End-to-End Tests

**Test Apps**:

1. **Simple HTTP App** (`e2e/test-apps/simple-http-app/`)
   - Makes basic HTTP GET/POST requests
   - No authentication
   - Known endpoints

2. **HTTPS App** (`e2e/test-apps/https-app/`)
   - Uses HTTPS for all requests
   - No certificate pinning
   - Tests HTTPS inspection

3. **Pinned App** (`e2e/test-apps/pinned-app/`)
   - Implements certificate pinning
   - Verifies behavior with pinning

**Run E2E tests**:
```bash
# iOS
./gradlew :maestro-test:iosIntegrationTest

# Android
./gradlew :maestro-test:androidIntegrationTest
```

## CI/CD Integration

### GitHub Actions

The repository includes workflows for automated testing:

#### Unit Tests Workflow
`.github/workflows/network-monitor-test.yaml`

```yaml
name: Network Monitor Tests

on:
  push:
    paths:
      - 'maestro-network-monitor/**'
      - '.github/workflows/network-monitor-test.yaml'
  pull_request:
    paths:
      - 'maestro-network-monitor/**'

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'zulu'
          java-version: '17'
          cache: 'gradle'
      
      - name: Run unit tests
        run: ./gradlew :maestro-network-monitor:test
      
      - name: Upload test reports
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: test-reports
          path: maestro-network-monitor/build/reports/
```

#### Android Integration Tests
`.github/workflows/network-monitor-android-e2e.yaml`

```yaml
name: Network Monitor Android E2E

on:
  push:
    branches: [ main, vpn-network-monitoring ]
  pull_request:
    branches: [ main ]

jobs:
  android-e2e:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Enable KVM group perms
        run: |
          echo 'KERNEL=="kvm", GROUP="kvm", MODE="0666"' | sudo tee /etc/udev/rules.d/99-kvm4all.rules
          sudo udevadm control --reload-rules
          sudo udevadm trigger --name-match=kvm
      
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'zulu'
          java-version: '17'
          cache: 'gradle'
      
      - name: Build test app
        run: |
          cd e2e/test-apps/simple-http-app
          ./gradlew assembleDebug
      
      - name: Run Android E2E tests
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 33
          arch: x86_64
          target: google_apis
          script: ./gradlew :maestro-test:androidIntegrationTest
      
      - name: Upload artifacts
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: android-e2e-reports
          path: |
            maestro-test/build/reports/
            ~/.maestro/logs/
```

#### iOS Integration Tests
`.github/workflows/network-monitor-ios-e2e.yaml`

```yaml
name: Network Monitor iOS E2E

on:
  push:
    branches: [ main, vpn-network-monitoring ]
  pull_request:
    branches: [ main ]

jobs:
  ios-e2e:
    runs-on: macos-13
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'zulu'
          java-version: '17'
          cache: 'gradle'
      
      - name: Select Xcode version
        run: sudo xcode-select -s /Applications/Xcode_15.0.app
      
      - name: Build test app
        run: |
          cd e2e/test-apps/simple-http-app
          xcodebuild -workspace SimpleHTTPApp.xcworkspace \
                     -scheme SimpleHTTPApp \
                     -sdk iphonesimulator \
                     -configuration Debug \
                     -derivedDataPath build
      
      - name: Boot Simulator
        run: |
          DEVICE_ID=$(xcrun simctl create TestDevice com.apple.CoreSimulator.SimDeviceType.iPhone-14 com.apple.CoreSimulator.SimRuntime.iOS-17-0)
          xcrun simctl boot $DEVICE_ID
          echo "SIMULATOR_ID=$DEVICE_ID" >> $GITHUB_ENV
      
      - name: Install app
        run: |
          xcrun simctl install $SIMULATOR_ID e2e/test-apps/simple-http-app/build/Build/Products/Debug-iphonesimulator/SimpleHTTPApp.app
      
      - name: Run iOS E2E tests
        run: ./gradlew :maestro-test:iosIntegrationTest
      
      - name: Upload artifacts
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: ios-e2e-reports
          path: |
            maestro-test/build/reports/
            ~/.maestro/logs/
```

## Test Data

### Sample Test App

A minimal test app that makes predictable network requests:

**Android** (`e2e/test-apps/simple-http-app/android/`):
```kotlin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Make a simple HTTP request
        thread {
            val url = URL("http://httpbin.org/get")
            val connection = url.openConnection() as HttpURLConnection
            val response = connection.inputStream.bufferedReader().readText()
            Log.d("HTTP", "Response: $response")
        }
    }
}
```

**iOS** (`e2e/test-apps/simple-http-app/ios/`):
```swift
class ViewController: UIViewController {
    override func viewDidLoad() {
        super.viewDidLoad()
        
        // Make a simple HTTP request
        let url = URL(string: "http://httpbin.org/get")!
        URLSession.shared.dataTask(with: url) { data, response, error in
            if let data = data {
                print("Response: \(String(data: data, encoding: .utf8) ?? "")")
            }
        }.resume()
    }
}
```

### Test Flows

**Basic HTTP test** (`e2e/flows/network-monitoring-http.yaml`):
```yaml
appId: com.maestro.test.httpapp
---
- launchApp
- tapOn: "Make Request"
- assertVisible: "Response received"
```

**Domain filtering test** (`e2e/flows/network-monitoring-filtering.yaml`):
```yaml
appId: com.maestro.test.httpapp
networkConfig:
  enabled: true
  includedDomains:
    - httpbin.org
  excludedDomains:
    - analytics.google.com
---
- launchApp
- tapOn: "Make Multiple Requests"
- assertVisible: "All requests completed"
```

## Validating Test Results

### Automated Assertions

Tests should verify:

1. **Network session is created**:
```kotlin
@Test
fun `network session should be captured`() {
    val result = runFlow("network-monitoring-http.yaml", enableNetworkMonitoring = true)
    assertThat(result.networkSession).isNotNull()
}
```

2. **Request count is correct**:
```kotlin
@Test
fun `should capture all HTTP requests`() {
    val result = runFlow("network-monitoring-http.yaml", enableNetworkMonitoring = true)
    assertThat(result.networkSession?.totalRequests).isEqualTo(3)
}
```

3. **Domain filtering works**:
```kotlin
@Test
fun `should filter domains correctly`() {
    val result = runFlow("network-monitoring-filtering.yaml", enableNetworkMonitoring = true)
    assertThat(result.networkSession?.domains).containsExactly("httpbin.org")
}
```

4. **Report includes network data**:
```kotlin
@Test
fun `HTML report should include network section`() {
    val result = runFlow("network-monitoring-http.yaml", enableNetworkMonitoring = true)
    val report = File("build/reports/test-report.html").readText()
    assertThat(report).contains("Network Monitoring Data")
    assertThat(report).contains("Total Requests")
}
```

### Manual Validation

For manual testing:

1. Run a flow with network monitoring:
   ```bash
   maestro test --enable-network-monitoring e2e/flows/network-monitoring-http.yaml
   ```

2. Open the HTML report:
   ```bash
   open ~/.maestro/reports/latest.html
   ```

3. Verify:
   - [ ] "Network Monitoring Data" section appears
   - [ ] Request counts are accurate
   - [ ] Domains list is correct
   - [ ] Request table shows URLs and status codes
   - [ ] Metrics (bytes, duration) are reasonable

## Performance Testing

### Overhead Measurement

Test to measure performance impact:

```bash
# Run without monitoring (baseline)
time maestro test my-flow.yaml

# Run with monitoring
time maestro test --enable-network-monitoring my-flow.yaml

# Calculate overhead percentage
```

Expected overhead: 5-15% increase in execution time

### Memory Usage

Monitor memory during tests:

```bash
# Start monitoring
./gradlew :maestro-cli:run --args="test --enable-network-monitoring my-flow.yaml" &
PID=$!

# Track memory
while kill -0 $PID 2>/dev/null; do
  ps -p $PID -o rss=
  sleep 1
done
```

Expected memory increase: 50-100MB depending on traffic volume

## Troubleshooting Tests

### Common Issues

1. **VPN fails to establish**:
   - Check device/emulator is available
   - Verify no other VPN is active
   - Check logs: `~/.maestro/logs/network-monitor.log`

2. **No network data captured**:
   - Verify app makes network requests
   - Check VPN is established
   - Review domain filters

3. **Tests timeout**:
   - Increase timeout in test configuration
   - Check if app is waiting for network
   - Review test app behavior

4. **Flaky tests**:
   - Add wait conditions
   - Implement retry logic
   - Check network stability

### Debug Mode

Run tests with debug logging:

```bash
MAESTRO_DEBUG=1 ./gradlew :maestro-test:integrationTest
```

This will output detailed logs to:
- Console
- `~/.maestro/logs/network-monitor-debug.log`

## Adding New Tests

When adding new test cases:

1. Create test app if needed
2. Write test flow YAML
3. Add automated assertions
4. Update CI workflow if needed
5. Document in this guide

## Test Metrics

Track these metrics over time:

- Test execution time (with/without monitoring)
- Memory usage
- Test stability (pass rate)
- Coverage percentage
- Number of captured requests per test

## Resources

- **CI Dashboard**: View test results in GitHub Actions
- **Test Reports**: Stored in `maestro-test/build/reports/`
- **Logs**: Available at `~/.maestro/logs/`
- **Coverage**: Generate with `./gradlew jacocoTestReport`

## Contributing

When contributing to network monitoring:

1. Write tests for new features
2. Update this guide
3. Ensure CI passes
4. Add documentation

Happy testing! 🎉
