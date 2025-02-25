package com.reactnativetinkoffpay

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.facebook.react.bridge.*
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring
import ru.tinkoff.acquiring.sdk.utils.SampleAcquiringTokenGenerator
import java.util.concurrent.Executors

class TinkoffPayModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String = "TinkoffPayModule"

    companion object {
        private var tinkoffAcquiring: TinkoffAcquiring? = null
        private var isInitialized: Boolean = false
        private val executor = Executors.newSingleThreadExecutor()
        private val mainHandler = Handler(Looper.getMainLooper())
        private var paymentPromise: Promise? = null // Для синхронного возврата результата

        fun initialize(context: Context, terminalKey: String, publicKey: String, password: String, promise: Promise) {
            synchronized(this) {
                if (isInitialized) {
                    promise.resolve("SDK already initialized")
                    return
                }

                try {
                    tinkoffAcquiring = TinkoffAcquiring(
                        context = context.applicationContext,
                        terminalKey = terminalKey,
                        publicKey = publicKey
                    )
                    AcquiringSdk.tokenGenerator = SampleAcquiringTokenGenerator(password)
                    AcquiringSdk.isDebug = true
                    isInitialized = true
                    android.util.Log.d("TinkoffPayModule", "Initialized with terminalKey: $terminalKey, publicKey: $publicKey")
                    promise.resolve("SDK initialized successfully")
                } catch (e: Exception) {
                    android.util.Log.e("TinkoffPayModule", "Initialization failed: ${e.message}")
                    promise.reject("INITIALIZATION_ERROR", "Failed to initialize SDK: ${e.message}")
                }
            }
        }

        fun setPaymentPromise(promise: Promise) {
            paymentPromise = promise
        }

        fun resolvePayment(result: ReadableMap) {
            paymentPromise?.resolve(result)
            paymentPromise = null
        }

        fun rejectPayment(code: String, message: String) {
            paymentPromise?.reject(code, message)
            paymentPromise = null
        }
    }

    @ReactMethod
    fun initialize(terminalKey: String, publicKey: String, password: String, promise: Promise) {
        val activity = currentActivity
        if (activity == null) {
            promise.reject("ACTIVITY_NOT_FOUND", "No current activity found")
            return
        }

        Companion.initialize(activity.applicationContext, terminalKey, publicKey, password, promise)
    }

    @ReactMethod
    fun startPayment(
        orderId: String,
        amount: Double,
        description: String,
        customerKey: String,
        terminalKey: String,  // Добавляем как параметр
        publicKey: String,    // Добавляем как параметр
        promise: Promise
    ) {
        val activity = currentActivity
        if (activity == null) {
            promise.reject("ACTIVITY_NOT_FOUND", "No current activity found")
            return
        }

        if (!isInitialized || tinkoffAcquiring == null) {
            promise.reject("SDK_NOT_INITIALIZED", "SDK not initialized. Call initialize first.")
            return
        }

        Companion.setPaymentPromise(promise)

        executor.execute {
            try {
                android.util.Log.d("TinkoffPayModule", "Starting payment with terminalKey: $terminalKey, publicKey: $publicKey")
                val intent = Intent(activity, TinkoffPaymentActivity::class.java).apply {
                    putExtra("orderId", orderId)
                    putExtra("amount", amount)
                    putExtra("description", description)
                    putExtra("customerKey", customerKey)
                    putExtra("terminalKey", terminalKey)
                    putExtra("publicKey", publicKey)
                }

                mainHandler.post {
                    activity.startActivity(intent)
                }
            } catch (e: Exception) {
                mainHandler.post {
                    android.util.Log.e("TinkoffPayModule", "Payment error: ${e.message}")
                    promise.reject("PAYMENT_ERROR", "Failed to start payment: ${e.message}")
                }
            }
        }
    }
}