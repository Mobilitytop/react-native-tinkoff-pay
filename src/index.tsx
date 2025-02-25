'use strict';

import { NativeModules, Platform } from 'react-native';

// Проверка на корректную линковку нативных модулей
const LINKING_ERROR =
  `The package 'react-native-tinkoff-pay' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({
    ios: "- You have run 'pod install'\n",
    default: '',
  }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

// Получение нативного модуля
const TinkoffPayModule = NativeModules.TinkoffPayModule
  ? NativeModules.TinkoffPayModule
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

// Интерфейс для параметров платежа
interface PaymentFlow {
  orderId: string;
  amount: number; // Сумма в копейках
  description: string;
  customerKey: string;
  terminalKey: string; // Добавляем как параметр
  publicKey: string; // Добавляем как параметр
  password: string;
}

// Интерфейс для результата платежа
interface PaymentResult {
  status: 'succeeded' | 'cancelled';
  paymentId?: string;
  cardId?: string;
}

export async function startPayment(
  paymentFlow: PaymentFlow
): Promise<PaymentResult> {
  const {
    orderId,
    amount,
    description,
    customerKey,
    terminalKey,
    publicKey,
    password,
  } = paymentFlow;
  try {
    await TinkoffPayModule.initialize(terminalKey, publicKey, password);
    return Platform.OS === 'ios'
      ? await TinkoffPayModule.startPayment(
          orderId,
          amount,
          description,
          customerKey
        )
      : await TinkoffPayModule.startPayment(
          orderId,
          amount,
          description,
          customerKey,
          terminalKey,
          publicKey
        );
  } catch (error: any) {
    throw new Error(`Payment failed: ${error.message}`);
  }
}

// Экспорт по умолчанию
export default {
  startPayment,
};
