import Foundation
import TinkoffASDKCore
import TinkoffASDKUI
import React
import CommonCrypto // Добавляем импорт для SHA-256

@objc(TinkoffPayModule)
class TinkoffPayModule: NSObject {
  
  private var sdk: AcquiringUISDK?
  
  // Реализация ITokenProvider для генерации токена
  private class TokenProvider: NSObject, ITokenProvider {
    private let terminalPassword: String
    
    init(terminalPassword: String) {
      self.terminalPassword = terminalPassword
      super.init()
    }
    
    func provideToken(forRequestParameters parameters: [String: String], 
                     completion: @escaping (Result<String, Error>) -> Void) {
      var tokenParams = parameters
      tokenParams["Password"] = terminalPassword
      
      // Сортируем ключи по алфавиту
      let sortedParams = tokenParams.sorted { $0.key < $1.key }
      // Конкатенируем значения
      let concatenatedValues = sortedParams.map { $0.value }.joined()
      // Вычисляем SHA-256 хэш
      if let token = sha256(concatenatedValues) {
        completion(.success(token))
      } else {
        completion(.failure(NSError(domain: "TokenGenerationError", code: -1, userInfo: [NSLocalizedDescriptionKey: "Failed to generate token"])))
      }
    }
    
    // Вспомогательная функция для вычисления SHA-256
    private func sha256(_ input: String) -> String? {
      guard let data = input.data(using: .utf8) else { return nil }
      var digest = [UInt8](repeating: 0, count: Int(CC_SHA256_DIGEST_LENGTH))
      data.withUnsafeBytes {
        _ = CC_SHA256($0.baseAddress, CC_LONG(data.count), &digest)
      }
      return digest.map { String(format: "%02x", $0) }.joined()
    }
  }
  
  // Инициализация SDK с переданными TerminalKey, PublicKey и Password
  @objc func initialize(_ terminalKey: String, 
                        publicKey: String, 
                        password: String, // Добавляем пароль терминала
                        resolver resolve: @escaping RCTPromiseResolveBlock, 
                        rejecter reject: @escaping RCTPromiseRejectBlock) {
    do {
      let credential = AcquiringSdkCredential(
        terminalKey: terminalKey,
        publicKey: publicKey
      )
      
      let tokenProvider = TokenProvider(terminalPassword: password)
      
      let coreSDKConfiguration = AcquiringSdkConfiguration(
        credential: credential,
        server: .prod, // или .test для тестового окружения
        logger: Logger(), // Включаем логирование
        tokenProvider: tokenProvider // Передаем провайдер токенов
      )
      
      let uiSDKConfiguration = UISDKConfiguration()
      
      self.sdk = try AcquiringUISDK(
        coreSDKConfiguration: coreSDKConfiguration,
        uiSDKConfiguration: uiSDKConfiguration
      )
      
      resolve("SDK initialized successfully")
    } catch {
      reject("INIT_ERROR", "Failed to initialize SDK: \(error.localizedDescription)", error)
    }
  }
  
  // Метод для отображения платежной формы
  @objc func startPayment(_ orderId: String, 
                          amount: NSNumber, 
                          description: String, 
                          customerKey: String, 
                          resolver resolve: @escaping RCTPromiseResolveBlock, 
                          rejecter reject: @escaping RCTPromiseRejectBlock) {
    guard let sdk = sdk else {
      reject("SDK_ERROR", "SDK not initialized", nil)
      return
    }
    
    DispatchQueue.main.async { [weak self] in
      guard let self = self else { return }
      
      let orderOptions = OrderOptions(
        orderId: orderId,
        amount: Int64(truncating: amount),
        description: description,
        savingAsParentPayment: false
      )
      
      let customerOptions = CustomerOptions(
        customerKey: customerKey,
        email: nil
      )
      
      let paymentOptions = PaymentOptions(
        orderOptions: orderOptions,
        customerOptions: customerOptions,
        paymentCallbackURL: nil,
        paymentInitData: nil
      )
      
      let paymentFlow: PaymentFlow = .full(paymentOptions: paymentOptions)
      
      guard let topController = self.getTopViewController() else {
        reject("UI_ERROR", "Unable to find top view controller", nil)
        return
      }
      
      guard topController.presentedViewController == nil else {
        reject("UI_ERROR", "Another presentation is in progress", nil)
        return
      }
      
      let configuration = MainFormUIConfiguration(orderDescription: description)
      
      sdk.presentMainForm(
        on: topController,
        paymentFlow: paymentFlow,
        configuration: configuration,
        cardScannerDelegate: nil
      ) { result in
        switch result {
        case .succeeded(let paymentInfo):
          resolve([
            "status": "succeeded",
            "paymentId": paymentInfo.paymentId,
            "orderId": paymentInfo.orderId,
            "amount": paymentInfo.amount
          ])
        case .failed(let error):
          reject("PAYMENT_ERROR", "Payment failed: \(error.localizedDescription)", error)
        case .cancelled:
          resolve(["status": "cancelled"])
        }
      }
    }
  }
  
  @objc static func requiresMainQueueSetup() -> Bool {
    return true
  }
  
  private func getTopViewController() -> UIViewController? {
    guard let window = UIApplication.shared.windows.first(where: { $0.isKeyWindow }),
          let rootViewController = window.rootViewController else {
      print("Warning: No key window or root view controller found")
      return nil
    }
    
    var topController = rootViewController
    while let presentedViewController = topController.presentedViewController {
      topController = presentedViewController
    }
    
    guard topController.view.window != nil else {
      print("Warning: Top controller is not in the view hierarchy")
      return nil
    }
    
    return topController
  }
}