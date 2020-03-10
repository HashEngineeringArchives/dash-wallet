package org.dash.android.lightpayprot

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.dash.android.lightpayprot.data.SimplifiedPaymentRequest
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.*
import java.util.concurrent.TimeUnit

class LightPaymentRepo {

    private val paymentApi: PaymentApi
    private val responseHandler = ResponseHandler()

    init {
        val httpClient = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(SupportInterceptor())
                .addInterceptor(HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
                    override fun log(message: String) {
                        println(message)
                    }
                }).setLevel(HttpLoggingInterceptor.Level.BODY))
                .build()

        val moshi: Moshi = Moshi.Builder()
                .add(Date::class.java, Rfc3339DateJsonAdapter().nullSafe())
                .build()

        val retrofit = Retrofit.Builder()
                .baseUrl("https://dash.org") //to be fully overridden in endpoint definitions
                .client(httpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

        paymentApi = retrofit.create(PaymentApi::class.java)
    }

    suspend fun getPaymentRequest(paymentRequestUrl: String): Resource<SimplifiedPaymentRequest> {
        return try {
            val response = paymentApi.getPaymentRequest(paymentRequestUrl)
            val headers = response.headers()
            val payeeVerifiedBy = headers[SupportInterceptor.EXT_HEADER_PAYEE_VERIFIED_BY]
            val payeeName = headers[SupportInterceptor.EXT_HEADER_PAYEE_NAME]
            val responseData = response.body()!!
            responseData.payeeVerifiedBy = payeeVerifiedBy
            responseData.payeeName = payeeName
            responseHandler.handleSuccess(responseData)
        } catch (ex: Exception) {
            responseHandler.handleException(ex)
        }
    }

}