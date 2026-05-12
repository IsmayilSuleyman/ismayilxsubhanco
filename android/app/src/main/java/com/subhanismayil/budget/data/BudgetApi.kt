package com.subhanismayil.budget.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object BudgetApi {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun submit(webAppUrl: String, req: TransactionRequest): TransactionResponse =
        withContext(Dispatchers.IO) {
            val body = json.encodeToString(TransactionRequest.serializer(), req)
                .toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(webAppUrl)
                .post(body)
                .build()
            client.newCall(request).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return@use TransactionResponse(ok = false, error = "HTTP ${resp.code}: ${text.take(200)}")
                }
                try {
                    json.decodeFromString(TransactionResponse.serializer(), text)
                } catch (e: Exception) {
                    TransactionResponse(ok = false, error = "Bad response: ${text.take(200)}")
                }
            }
        }

    suspend fun delete(webAppUrl: String, id: String): TransactionResponse =
        submit(webAppUrl, TransactionRequest(
            amount = 1.0,            // placeholder, ignored on delete
            isTopUp = false,         // placeholder, ignored
            transactionBy = "Sübhan",// placeholder, ignored
            category = "😀 Other",  // placeholder, ignored
            note = "",
            action = "delete",
            id = id
        ))

    suspend fun fetchCsv(sheetCsvUrl: String): String =
        withContext(Dispatchers.IO) {
            val sep = if (sheetCsvUrl.contains("?")) "&" else "?"
            val url = "$sheetCsvUrl${sep}_=${System.currentTimeMillis()}"
            val request = Request.Builder()
                .url(url)
                .header("Cache-Control", "no-store")
                .build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                resp.body?.string().orEmpty()
            }
        }
}
