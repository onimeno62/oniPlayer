package com.oniplayer.data.net

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Single shared OkHttp root for the whole app.
 *
 * Every consumer derives its client with [derive] so they all share one connection pool,
 * dispatcher and thread pool instead of each holding its own, while keeping per-use timeouts.
 */
object HttpClients {
    private val root: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /** Short timeouts, suited to provider metadata/search APIs. */
    fun metadata(): OkHttpClient = derive(connectSeconds = 10, readSeconds = 15)

    /**
     * @param retry when true, transient failures (IO errors, HTTP 429/5xx) are retried with
     * exponential backoff. Only enable this for idempotent requests.
     */
    fun derive(
        connectSeconds: Long,
        readSeconds: Long,
        writeSeconds: Long? = null,
        retry: Boolean = false
    ): OkHttpClient = root.newBuilder()
        .connectTimeout(connectSeconds, TimeUnit.SECONDS)
        .readTimeout(readSeconds, TimeUnit.SECONDS)
        .apply {
            if (writeSeconds != null) writeTimeout(writeSeconds, TimeUnit.SECONDS)
            if (retry) addInterceptor(RetryInterceptor())
        }
        .build()
}

/**
 * Retries transient failures with exponential backoff, honouring a short `Retry-After`.
 * Request bodies must be re-readable (true for String/ByteArray bodies).
 */
class RetryInterceptor(
    private val maxRetries: Int = 2,
    private val baseDelayMs: Long = 600L,
    private val maxDelayMs: Long = 5_000L
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var attempt = 0
        while (true) {
            var retryAfterMs: Long? = null
            try {
                val response = chain.proceed(chain.request())
                if (!response.isTransientFailure() || attempt >= maxRetries) return response
                retryAfterMs = response.header("Retry-After")?.toLongOrNull()?.times(1_000L)
                response.close()
            } catch (e: IOException) {
                if (attempt >= maxRetries || chain.call().isCanceled()) throw e
            }
            attempt++
            val backoff = baseDelayMs shl (attempt - 1)
            val delayMs = (retryAfterMs ?: backoff).coerceAtMost(maxDelayMs)
            try {
                Thread.sleep(delayMs)
            } catch (interrupted: InterruptedException) {
                Thread.currentThread().interrupt()
                throw IOException("Interrupted while waiting to retry", interrupted)
            }
        }
    }

    private fun Response.isTransientFailure(): Boolean =
        code == 429 || code == 500 || code == 502 || code == 503 || code == 504
}
