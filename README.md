# Android Phone Haptic Relay

This application acts as the gateway between the Smartwatch and the Backend. It handles environmental context (GPS) and provides the internet uplink.

## 🚀 Features
- **Bluetooth Client:** Maintains a stable connection to the Wear OS device.
- **Contextual Awareness:** Collects GPS coordinates to enable environmental data tracking.
- **Webhook Relay:** POSTs aggregated data (HR + GPS + Identifiers) to n8n endpoints.
- **Bi-directional Flow:** Relays haptic instructions from the backend back to the watch.

## 🛠 Configuration
> [!IMPORTANT]
> You must update the IP address to match your n8n server location.
1. Update `myIp` in `NetworkController.java`.
2. Update the domain in `res/xml/network_security_config.xml`.

## 🔗 Project Ecosystem
- [Researcher Dashboard](https://github.com/TTaliR/ResearcherSideApp-FULL) - Used to configure the rules for this device.
- [Haptic Backend](https://github.com/your-org/smartwatch-haptic-backend) - Processes the data sent by this relay.
- [Smartwatch Haptic App (Wear OS)](https://github.com/TTaliR/SmartWatchHapticFeedBackApp1) - Wear OS application serves as the primary edge node for the system.
