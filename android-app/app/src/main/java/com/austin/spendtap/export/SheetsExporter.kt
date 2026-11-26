package com.austin.spendtap.export

import com.austin.spendtap.model.YearlyReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Posts the yearly report to the user's own Google Apps Script web app (google-sheets/Code.gs).
 * Same JSON the iOS app sends, so one script serves both platforms.
 */
object SheetsExporter {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()  // follows the script's 302 redirect (POST becomes GET) by default

    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    @Serializable private data class Payload(val token: String, val report: YearlyReport)
    @Serializable private data class Reply(val ok: Boolean, val error: String? = null)

    suspend fun send(report: YearlyReport, url: String, token: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val clean = url.trim()
                require(clean.startsWith("https://")) { "The sheet link must be an https:// Apps Script web-app URL." }
                val body = json.encodeToString(Payload(token, report)).toRequestBody("application/json".toMediaType())
                client.newCall(Request.Builder().url(clean).post(body).build()).execute().use { resp ->
                    check(resp.isSuccessful) { "Google returned ${resp.code}. Check the web app is deployed for \"Anyone\"." }
                    val reply = runCatching { json.decodeFromString<Reply>(resp.body.string()) }.getOrNull()
                        ?: error("Unexpected reply. Re-deploy the script and paste the new /exec URL.")
                    check(reply.ok) { reply.error ?: "The script rejected the request." }
                }
            }
        }
}
