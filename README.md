# SMS/MMS Gateway

An Android app that acts as an SMS/MMS gateway, forwarding incoming messages (including MMS image attachments) to a webhook server via signed HTTP POST requests.

Forked from [EnvayaSMS](https://github.com/youngj/EnvayaSMS) and modernized for use with the [Olivine SMS Agent](https://github.com/olivine-ai/olivine-sms-agent) receipt tracking system.

## How It Works

The app runs as the **default SMS application** on an Android phone. When a message arrives:

1. Android delivers the SMS or MMS to the app via broadcast receivers
2. The app constructs a multipart HTTP POST request with the message content
3. For MMS messages, image attachments are included as binary form fields
4. The request is signed with HMAC-SHA1 and sent to the configured webhook URL
5. The server can respond with commands (e.g., send an outgoing SMS reply)

**In our deployment:** The webhook URL points to `https://api.petty-cash.net/api/envayasms/webhook`, where the Olivine worker extracts receipt data from photos using AI vision models.

### Architecture

```
[Crew Member]           [Android Phone]              [Olivine API]
     |                       |                            |
     |--- texts receipt ---->|                            |
     |    photo via SMS/MMS  |                            |
     |                       |--- HTTP POST webhook ----->|
     |                       |    (signed, multipart)     |
     |                       |                            |--- AI extracts
     |                       |<-- JSON response ---------|    receipt data
     |                       |    (send reply SMS)        |
     |<-- confirmation SMS --|                            |
```

The gateway handles **inbound** SMS/MMS on the physical Android device. **Outbound** reply messages are sent either through this app (via server commands) or through a separate cloud SMS API.

## Building from Source

### Prerequisites

- **Java JDK 17**
- **Android SDK** with:
  - Platform SDK 34 (Android 14)
  - Build Tools 34.0.0
  - Platform Tools

### macOS Setup (Homebrew)

```bash
# Install Java 17
brew install openjdk@17

# Install Android command-line tools
brew install --cask android-commandlinetools

# Install required SDK components
sdkmanager "platforms;android-34" "build-tools;34.0.0" "platform-tools"
```

### Environment Variables

Add to your shell profile (`~/.zshrc` or `~/.bashrc`):

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@17"
export ANDROID_HOME="/opt/homebrew/share/android-commandlinetools"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
```

### Build

```bash
# Debug build
./gradlew assembleDebug

# APK output location
app/build/outputs/apk/debug/app-debug.apk
```

## Installation on Android

1. **Transfer the APK** to the phone via USB, ADB, or file sharing:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

2. **Grant permissions** when prompted:
   - SMS (send, receive, read)
   - MMS (receive)
   - Phone (read phone state)
   - Storage (if needed for MMS parts)

3. **Set as default SMS app** in Android Settings > Apps > Default apps > SMS app

4. **Configure the app** in its Settings screen:
   - **Server URL:** The webhook endpoint (e.g., `https://api.petty-cash.net/api/envayasms/webhook`)
   - **Password:** Shared secret for request signing
   - **Phone Number:** The phone's number (sent with each request for identification)

## Server API / Webhook Protocol

### Incoming Message (SMS or MMS)

When a message is received, the app sends a `multipart/form-data` POST to the configured server URL.

#### Standard POST Fields

| Field | Description |
|-------|-------------|
| `action` | `incoming` for received messages, `forward_sent` for forwarded sent messages |
| `from` | Sender phone number (for incoming) |
| `to` | Recipient phone number (for forwarded sent messages) |
| `message` | Message body text |
| `message_type` | `sms`, `mms`, or `call` |
| `timestamp` | Unix timestamp in milliseconds |
| `phone_number` | The gateway phone's number |
| `phone_id` | Device identifier |
| `phone_token` | Device token |
| `send_limit` | Current outgoing SMS rate limit |
| `now` | Current time in milliseconds |
| `settings_version` | App settings version number |
| `battery` | Battery level percentage (0-100) |
| `power` | Power connection status |
| `network` | Active network type name (e.g., `WIFI`, `MOBILE`) |
| `log` | Recent app log entries |

#### MMS-Specific Fields

For MMS messages, additional fields are included:

| Field | Description |
|-------|-------------|
| `mms_parts` | JSON array of part metadata |
| `part0`, `part1`, ... | Binary data for each MMS part |

The `mms_parts` JSON array contains objects with:

```json
[
  {
    "name": "part0",
    "cid": "content-id",
    "type": "image/jpeg",
    "filename": "image.jpg"
  }
]
```

Each part's binary data is sent as a named multipart form field matching the `name` in the metadata.

#### Other Actions

| Action | When Sent |
|--------|-----------|
| `outgoing` | Polling for outgoing messages to send |
| `send_status` | Status update for an outgoing message (queued/sent/failed/cancelled) |
| `device_status` | Device events: `power_connected`, `power_disconnected`, `battery_low`, `battery_okay`, `send_limit_exceeded` |
| `test` | Connection test |
| `amqp_started` | AMQP consumer connected |

### Request Signature

Every request includes an `X-Request-Signature` header for authentication. The signature is computed as:

```
Base64(SHA1(url + "," + sorted_params + "," + password))
```

Where:
1. All POST parameters are sorted alphabetically by name
2. Concatenated as: `url,name1=value1,name2=value2,...,password`
3. SHA-1 hashed
4. Base64 encoded

**Verification pseudocode:**

```python
params = sorted(request.POST.items(), key=lambda x: x[0])
raw = url + "," + ",".join(f"{k}={v}" for k, v in params) + "," + password
expected = base64(sha1(raw.encode("utf-8")))
assert request.headers["X-Request-Signature"] == expected
```

### Server Response

The server should respond with `Content-Type: application/json`:

```json
{
  "events": [
    {
      "event": "send",
      "messages": [
        { "to": "+15551234567", "message": "Receipt received!" }
      ]
    }
  ]
}
```

Supported events:

| Event | Description |
|-------|-------------|
| `send` | Send outgoing SMS messages |
| `cancel` | Cancel a queued outgoing message |
| `cancel_all` | Cancel all queued outgoing messages |
| `log` | Write a message to the app's log |
| `settings` | Update app settings remotely |

## Modernization Changes

This fork has been updated from the original EnvayaSMS (circa 2011-2013) for compatibility with modern Android:

| Area | Original | Modernized |
|------|----------|------------|
| Build system | Apache Ant | Gradle 8.5 + AGP 8.2.2 |
| Target SDK | 4 (Android 1.6) | 34 (Android 14) |
| Min SDK | 4 (Android 1.6) | 21 (Android 5.0 Lollipop) |
| Java version | 1.5 | 17 |
| Dependencies | Vendored JARs in `libs/` | Maven Central declarations |
| Default SMS app | Not required | Full manifest declarations (required since Android 4.4) |
| Telephony APIs | `android.telephony.gsm.*` | `android.telephony.*` |
| Component exports | Implicit | Explicit `android:exported` on all components |

### Dependency Upgrades

| Library | Original Version | Current Version |
|---------|-----------------|-----------------|
| commons-cli | 1.1 (JAR) | 1.5.0 (Maven) |
| commons-io | 1.2 (JAR) | 2.15.1 (Maven) |
| httpmime | 4.1.2 (JAR) | 4.5.14 (Maven) |
| rabbitmq-client | unknown (JAR) | 5.20.0 (Maven) |

## Project Structure

```
sms-mms-gateway/
├── app/
│   ├── build.gradle                    # App module build config
│   ├── proguard-rules.pro              # ProGuard rules (unused)
│   ├── libs/                           # Legacy JARs (backup only)
│   └── src/main/
│       ├── AndroidManifest.xml         # App manifest with permissions & components
│       ├── java/org/envaya/sms/
│       │   ├── App.java                # Application class (config, HTTP client, logging)
│       │   ├── Inbox.java              # Incoming message queue management
│       │   ├── Outbox.java             # Outgoing message queue management
│       │   ├── IncomingMessage.java     # Base class for incoming SMS/MMS/calls
│       │   ├── IncomingMms.java         # MMS handling with multipart forwarding
│       │   ├── IncomingSms.java         # SMS handling
│       │   ├── IncomingCall.java        # Call notification handling
│       │   ├── OutgoingMessage.java     # Outgoing SMS from server commands
│       │   ├── MmsPart.java            # MMS attachment part (image, text, etc.)
│       │   ├── MessagingUtils.java      # SMS/MMS content provider queries
│       │   ├── MessagingObserver.java   # Content observer for messaging changes
│       │   ├── AmqpConsumer.java        # AMQP (RabbitMQ) consumer for push commands
│       │   ├── DatabaseHelper.java      # SQLite helper for local persistence
│       │   ├── JsonUtils.java           # JSON response parsing (events/commands)
│       │   ├── XmlUtils.java            # Legacy XML response parsing
│       │   ├── Base64Coder.java         # Base64 encoding for signatures
│       │   ├── receiver/                # Broadcast receivers
│       │   │   ├── SmsReceiver.java     #   SMS_RECEIVED handler
│       │   │   ├── ConnectivityChangeReceiver.java
│       │   │   ├── DeviceStatusReceiver.java
│       │   │   ├── OutgoingSmsReceiver.java
│       │   │   └── ...                  #   (12 more receivers)
│       │   ├── service/                 # Background services
│       │   │   ├── ForegroundService.java    # Persistent foreground service
│       │   │   ├── AmqpConsumerService.java  # AMQP connection service
│       │   │   ├── CheckMessagingService.java
│       │   │   └── ...
│       │   ├── task/                    # Async HTTP tasks
│       │   │   ├── HttpTask.java        # Base signed HTTP POST (signature generation)
│       │   │   ├── ForwarderTask.java   # Forwards incoming messages to server
│       │   │   ├── PollerTask.java      # Polls server for outgoing messages
│       │   │   └── CheckConnectivityTask.java
│       │   └── ui/                      # Activities (UI screens)
│       │       ├── Main.java            # Main activity with log view
│       │       ├── Prefs.java           # Settings screen
│       │       ├── LogView.java         # Detailed log viewer
│       │       └── ...                  #   (8 more activities)
│       └── res/                         # Android resources
│           ├── drawable-*/              # Icons (hdpi, mdpi, ldpi)
│           ├── layout/                  # XML layouts
│           ├── menu/                    # Menu definitions
│           ├── values/                  # Strings, arrays
│           └── xml/                     # Preferences XML
├── build.gradle                        # Root build script (AGP 8.2.2)
├── settings.gradle                     # Project settings
├── gradle.properties                   # Gradle configuration
├── gradlew                             # Gradle wrapper script (Unix)
├── gradlew.bat                         # Gradle wrapper script (Windows)
├── gradle/wrapper/                     # Gradle wrapper JAR + properties
├── LICENSE                             # MIT License
├── NOTICE                              # Third-party notices
└── CONTRIBUTORS                        # Project contributors
```

## License

MIT License -- see [LICENSE](LICENSE) for full text.

Copyright (C) 2011 by Nir Yariv, Jesse Young. Modernization by Olivine.

### Third-Party Libraries

- **Apache HttpMime** -- Apache License 2.0
- **Base64Coder** by Christian d'Heureuse -- MIT License
- **Android framework code** -- Apache License 2.0 (see [NOTICE](NOTICE))

## Credits

Forked from [EnvayaSMS by youngj](https://github.com/youngj/EnvayaSMS). Original authors: Nir Yariv, Jesse Young.

Modernized for use with the Olivine SMS Agent receipt tracking system.
