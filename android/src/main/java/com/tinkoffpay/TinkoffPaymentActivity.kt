package com.reactnativetinkoffpay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.facebook.react.bridge.Arguments
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.redesign.mainform.MainFormLauncher
import ru.tinkoff.acquiring.sdk.utils.Money

class TinkoffPaymentActivity : ComponentActivity() {

    private lateinit var paymentLauncher: androidx.activity.result.ActivityResultLauncher<MainFormLauncher.StartData>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        paymentLauncher = registerForActivityResult(MainFormLauncher.Contract) { result ->
            val resultMap = Arguments.createMap()

            when (result) {
                is MainFormLauncher.Success -> {
                    resultMap.putString("status", "succeeded")
                    resultMap.putString("paymentId", result.paymentId.toString())
                    resultMap.putString("cardId", result.cardId ?: "")
                    TinkoffPayModule.resolvePayment(resultMap)
                }
                is MainFormLauncher.Canceled -> {
                    resultMap.putString("status", "cancelled")
                    TinkoffPayModule.resolvePayment(resultMap)
                }
                is MainFormLauncher.Error -> {
                    TinkoffPayModule.rejectPayment("PAYMENT_ERROR", result.error.message ?: "Unknown error")
                }
            }
            finish()
        }

        val orderId = intent.getStringExtra("orderId")
        val amount = intent.getDoubleExtra("amount", 0.0)
        val description = intent.getStringExtra("description")
        val customerKey = intent.getStringExtra("customerKey")
        val terminalKey = intent.getStringExtra("terminalKey")
        val publicKey = intent.getStringExtra("publicKey")

        if (orderId != null && description != null && customerKey != null && terminalKey != null && publicKey != null) {
            val paymentOptions = PaymentOptions().setOptions {
                setTerminalParams(terminalKey, publicKey)
                orderOptions {
                    this.orderId = orderId
                    this.amount = Money.ofRubles(amount.toLong() / 100)
                    this.description = description
                }
                customerOptions {
                    this.customerKey = customerKey
                }
            }
            val startData = MainFormLauncher.StartData(paymentOptions)
            paymentLauncher.launch(startData)
        } else {
            android.util.Log.e("TinkoffPayment", "Missing required intent extras")
            TinkoffPayModule.rejectPayment("PAYMENT_ERROR", "Missing required payment parameters")
            finish()
        }
    }
}