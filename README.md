# react-native-tinkoff-pay

[![npm version](https://img.shields.io/npm/v/react-native-tinkoff-pay.svg)](https://www.npmjs.com/package/react-native-tinkoff-pay)

A React Native module for integrating T-Bank (Tinkoff) Acquiring SDK into mobile applications on iOS and Android platforms. This library enables seamless payment processing using T-Bank's payment forms, supporting various payment methods like bank cards, T-Pay, SBP, and Mir Pay (Android only).

## Features

- Unified payment initiation across iOS and Android.
- Synchronous payment result delivery via `Promise`.
- Support for custom `terminalKey` and `publicKey` per payment.
- Built with Kotlin (Android) and Swift + Objective-C (iOS) for native performance.

## Requirements

- **iOS**: iOS 13.0 or higher, Xcode 15.1+, Swift 5.9.2+
- **Android**: Android 7.0 (API level 24) or higher
- **React Native**: 0.60.0 or higher

## Installation

### Install the Package

```bash
npm install react-native-tinkoff-pay
# or
yarn add react-native-tinkoff-pay
```

### iOS Setup

1. **Update Podfile**  
   Add the following to your `ios/Podfile`:

   ```ruby
   pod 'TASDKCore'
   pod 'TASDKUI'
   ```

   Then run:

   ```bash
   cd ios && pod install
   ```

2. **Configure `Info.plist`**  
   Add the following to `ios/YourApp/Info.plist` to support T-Pay and SBP:
   ```xml
   <key>LSApplicationQueriesSchemes</key>
   <array>
       <string>bank100000000004</string> <!-- T-Pay -->
       <string>bank100000000111</string> <!-- Sberbank SBP -->
       <string>bank110000000005</string> <!-- VTB SBP -->
       <!-- Add more SBP schemes as needed, up to 50 -->
   </array>
   <key>NSAppTransportSecurity</key>
        <dict>
            <key>NSAllowsArbitraryLoadsInWebContent</key>
            <true/>
            <key>NSAllowsArbitraryLoads</key>
            <false/>
            <key>NSExceptionDomains</key>
            <dict>
                <key>securepay.tinkoff.ru</key>
                <dict>
                <key>NSExceptionAllowsInsecureHTTPLoads</key>
                <true/>
                <key>NSIncludesSubdomains</key>
                <true/>
                </dict>
                <key>qr.nspk.ru</key>
                <dict>
                <key>NSExceptionAllowsInsecureHTTPLoads</key>
                <true/>
                <key>NSIncludesSubdomains</key>
                <true/>
                </dict>
            </dict>
        </dict>
   ```

### Android Setup

1. **Configure `network_security_config.xml`**  
   Create the `network_security_config.xml` and  Add the following to:
   ```xml
    <network-security-config>
        <base-config>
            <trust-anchors>
                <certificates src="system" />
                <certificates src="@raw/acq_tinkoff_root_cert" />
                <certificates src="@raw/acq_ministry_of_digital_development_root_cert" />
            </trust-anchors>
        </base-config>
    </network-security-config>
   ```
   Then add this line to `AndroidManifest.xml`

   ```xml
   <application 
        ...
        android:networkSecurityConfig="@xml/network_security_config" //<-Add this line
        >  
   ```

2. **Sync Project**
   ```bash
   cd android && ./gradlew clean build
   ```

## Usage

### Example

```typescript
import TinkoffPay from 'react-native-tinkoff-pay';

// Initiate a payment
async function makePayment() {
  try {
    const result = await TinkoffPay.startPayment({
      orderId: 'ORDER_123',
      amount: 100000, // 1000 RUB in kopecks
      description: 'Test payment',
      customerKey: 'CUSTOMER_KEY',
      terminalKey: 'your_terminal_key',
      publicKey:
        'your_public_key',
      password: 'your_password',
    });
    console.log('Payment result:', result);
  } catch (error) {
    console.error(error);
  }
}

makePayment();
```

### API

#### `startPayment(paymentFlow: PaymentFlow): Promise<PaymentResult>`

Initiates a payment using T-Bank's payment form.

- **Parameters**:

  - `paymentFlow: PaymentFlow`:
    - `orderId: string` - Unique order identifier in the merchant's system.
    - `amount: number` - Payment amount in kopecks (e.g., 100000 for 1000 RUB).
    - `description: string` - Brief description of the order.
    - `customerKey: string` - Unique customer identifier for card saving.
    - `terminalKey: string` - Terminal identifier from T-Bank personal account.
    - `publicKey: string` - Public key for data encryption from T-Bank personal account.
    - `password: string` - Password for token generation (should be handled server-side in production).

- **Returns**: `Promise<PaymentResult>`

  - `status: 'succeeded' | 'cancelled'` - Payment outcome.
  - `paymentId?: string` - Payment identifier (if succeeded).
  - `cardId?: string` - Card identifier (if succeeded and card was saved).

- **Throws**: `Error` - If payment fails or SDK initialization fails.

---

## Configuration

### Prerequisites

To use this library, you need:

- `TerminalKey`, `PublicKey`, and `Password` from your T-Bank personal account after enabling Internet Acquiring.
- See [Personal Account Settings](https://www.tinkoff.ru/kassa/dev/payments/index.html#section/Vvedenie/Podklyuchenie-ekvajringa) for details.

### Security Notes

- **Token Generation**: The `password` is used client-side for token generation in this implementation. Make shure you dont store password in application source.
- **SSL/TLS Certificates**: Configure your app to support MinTsifra certificates for reliability:
  - **iOS**: Update `Info.plist` as per [iOS SDK docs](#надёжность-и-безопасность).
  - **Android**: Add `network-security-config` as per [Android SDK docs](#надёжность-и-безопасность).

---

## Troubleshooting

- **"ActivityNotFoundException"**: Ensure `TinkoffPaymentActivity` is registered in your Android manifest (see [Android Setup](#android-setup)).
- **"SDK not initialized"**: Verify that `terminalKey`, `publicKey`, and `password` are valid. The library initializes the SDK on the first `startPayment` call.
- **Payment Fails**: Enable debug logging with `AcquiringSdk.isDebug = true` and check Logcat (Android) or Xcode logs (iOS) for details.

---

## Development

### Building the Library

```bash
yarn clean
yarn prepare
```

### Testing

```bash
yarn test
yarn typecheck
yarn lint
```

---

## Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository.
2. Create a feature branch:
   ```bash
   git checkout -b feature/your-feature
   ```
3. Commit your changes:
   ```bash
   git commit -am 'Add your feature'
   ```
4. Push to the branch:
   ```bash
   git push origin feature/your-feature
   ```
5. Open a Pull Request.

Report bugs or request features at [GitHub Issues](https://github.com/MobilityTop/react-native-tinkoff-pay/issues).

---

## License

MIT License. See [LICENSE](LICENSE) for details.

---

## Acknowledgments

- Built using T-Bank Acquiring SDK for [iOS](https://opensource.tbank.ru/mobile-tech/asdk-ios) and [Android](https://opensource.tbank.ru/mobile-tech/asdk-android).
- Authors:
  MobilityTop LTD <sales@mobility.top> ([MobilityTop](https://github.com/Mobilitytop)),
  Igor Orekhov <arahismsn@gmail.com> ([arahisman](https://github.com/arahisman)).
