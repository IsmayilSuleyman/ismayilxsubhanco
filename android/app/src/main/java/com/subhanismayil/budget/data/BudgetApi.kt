package com.subhanismayil.budget.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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

    // Fetches Sheet2!A1:D15 from the Apps Script ?action=summary endpoint.
    // Returns a 2-D list of raw cell values (strings or numbers as strings).
    suspend fun fetchSummary(webAppUrl: String): List<List<String>> =
        withContext(Dispatchers.IO) {
            val url = "$webAppUrl?action=summary&_=${System.currentTimeMillis()}"
            val request = Request.Builder()
                .url(url)
                .header("Cache-Control", "no-store")
                .build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                val text = resp.body?.string().orEmpty()
                val root = json.parseToJsonElement(text).jsonObject
                val ok = root["ok"]?.jsonPrimitive?.content?.toBoolean() ?: false
                if (!ok) error(root["error"]?.jsonPrimitive?.content ?: "summary failed")
                val arr = root["values"]?.jsonArray ?: return@use emptyList()
                arr.map { row ->
                    row.jsonArray.map { cell -> cell.jsonPrimitive.content }
                }
            }
        }

    suspend fun fetchBudgets(webAppUrl: String): Map<String, Double> =
        withContext(Dispatchers.IO) {
            val url = "$webAppUrl?action=getBudgets&_=${System.currentTimeMillis()}"
            val request = Request.Builder()
                .url(url)
                .header("Cache-Control", "no-store")
                .build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                val text = resp.body?.string().orEmpty()
                val root = json.parseToJsonElement(text).jsonObject
                val ok = root["ok"]?.jsonPrimitive?.content?.toBoolean() ?: false
                if (!ok) error(root["error"]?.jsonPrimitive?.content ?: "getBudgets failed")
                val obj = root["budgets"]?.jsonObject ?: return@use emptyMap()
                obj.entries.associate { (k, v) -> k to v.jsonPrimitive.content.toDouble() }
            }
        }

    suspend fun saveBudgets(webAppUrl: String, budgets: Map<String, Double>): TransactionResponse =
        withContext(Dispatchers.IO) {
            val body = json.encodeToString(BudgetsRequest.serializer(), BudgetsRequest(budgets = budgets))
                .toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(webAppUrl)
                .post(body)
                .build()
            client.newCall(request).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) return@use TransactionResponse(ok = false, error = "HTTP ${resp.code}: ${text.take(200)}")
                runCatching { json.decodeFromString(TransactionResponse.serializer(), text) }
                    .getOrElse { TransactionResponse(ok = false, error = "Bad response: ${text.take(200)}") }
            }
        }

    // Fetches all transaction rows from the Apps Script ?action=list endpoint.
    // Returns a list of header-keyed maps, matching the shape CsvParser used to produce.
    suspend fun fetchTransactions(webAppUrl: String): List<Map<String, String>> =
        withContext(Dispatchers.IO) {
            val url = "$webAppUrl?action=list&_=${System.currentTimeMillis()}"
            val request = Request.Builder()
                .url(url)
                .header("Cache-Control", "no-store")
                .build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                val text = resp.body?.string().orEmpty()
                val root = json.parseToJsonElement(text).jsonObject
                val ok = root["ok"]?.jsonPrimitive?.content?.toBoolean() ?: false
                if (!ok) error(root["error"]?.jsonPrimitive?.content ?: "list failed")
                val arr = root["rows"]?.jsonArray ?: return@use emptyList()
                arr.map { elem ->
                    elem.jsonObject.entries.associate { (k, v) ->
                        k to v.jsonPrimitive.content
                    }
                }
            }
        }
}
