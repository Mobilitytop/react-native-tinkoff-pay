#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(TinkoffPayModule, NSObject)

RCT_EXTERN_METHOD(initialize:(NSString *)terminalKey
                  publicKey:(NSString *)publicKey
                  password:(NSString *)password
                  resolver:(RCTPromiseResolveBlock)resolver
                  rejecter:(RCTPromiseRejectBlock)rejecter)

RCT_EXTERN_METHOD(startPayment:(NSString *)orderId
                  amount:(nonnull NSNumber *)amount
                  description:(NSString *)description
                  customerKey:(NSString *)customerKey
                  resolver:(RCTPromiseResolveBlock)resolver
                  rejecter:(RCTPromiseRejectBlock)rejecter)

@end