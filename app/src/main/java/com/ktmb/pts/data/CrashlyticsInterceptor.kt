package com.ktmb.pts.data

import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.internal.http.promisesBody
import okio.Buffer
import okio.GzipSource
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.EOFException
import java.io.IOException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class CrashlyticsInterceptor: Interceptor {

    private companion object {
        private const val JSON_INDENT = 3
        private val LINE_SEPARATOR = System.getProperty("line.separator")
        private val OOM_OMITTED = LINE_SEPARATOR + "Output omitted because of Object size."
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (!response.isSuccessful) {
            response.code
            val message = "API Log${LINE_SEPARATOR}" +
                    "URL: ${request.url}${LINE_SEPARATOR}" +
                    "Method: ${request.method}${LINE_SEPARATOR}" +
                    "Request: ${bodyToString(request.body, request.headers)}${LINE_SEPARATOR}${LINE_SEPARATOR}" +
                    "Response Code: ${response.code}${LINE_SEPARATOR}" +
                    "Response: ${getResponseBody(response)}"
            // TODO: Add crashlytics log
        }

        return response
    }

    private fun bodyToString(requestBody: RequestBody?, headers: Headers): String {
        return requestBody?.let {
            return try {
                when {
                    bodyHasUnknownEncoding(headers) -> {
                        return "encoded body omitted)"
                    }
                    requestBody.isDuplex() -> {
                        return "duplex request body omitted"
                    }
                    requestBody.isOneShot() -> {
                        return "one-shot body omitted"
                    }
                    else -> {
                        val buffer = Buffer()
                        requestBody.writeTo(buffer)

                        val contentType = requestBody.contentType()
                        val charset: Charset = contentType?.charset(StandardCharsets.UTF_8)
                            ?: StandardCharsets.UTF_8

                        return if (buffer.isProbablyUtf8()) {
                            getJsonString(buffer.readString(charset)) + LINE_SEPARATOR + "${requestBody.contentLength()}-byte body"
                        } else {
                            "binary ${requestBody.contentLength()}-byte body omitted"
                        }
                    }
                }
            } catch (e: IOException) {
                "{\"err\": \"" + e.message + "\"}"
            }
        } ?: ""
    }

    private fun bodyHasUnknownEncoding(headers: Headers): Boolean {
        val contentEncoding = headers["Content-Encoding"] ?: return false
        return !contentEncoding.equals("identity", ignoreCase = true) &&
                !contentEncoding.equals("gzip", ignoreCase = true)
    }

    private fun getJsonString(msg: String): String {
        val message: String
        message = try {
            when {
                msg.startsWith("{") -> {
                    val jsonObject = JSONObject(msg)
                    jsonObject.toString(JSON_INDENT)
                }
                msg.startsWith("[") -> {
                    val jsonArray = JSONArray(msg)
                    jsonArray.toString(JSON_INDENT)
                }
                else -> {
                    msg
                }
            }
        } catch (e: JSONException) {
            msg
        } catch (e1: OutOfMemoryError) {
            OOM_OMITTED
        }
        return message
    }

    private fun getResponseBody(response: Response): String {
        val responseBody = response.body!!
        val headers = response.headers
        val contentLength = responseBody.contentLength()
        if (!response.promisesBody()) {
            return "End request - Promises Body"
        } else if (bodyHasUnknownEncoding(response.headers)) {
            return "encoded body omitted"
        } else {
            val source = responseBody.source()
            source.request(Long.MAX_VALUE) // Buffer the entire body.
            var buffer = source.buffer

            var gzippedLength: Long? = null
            if ("gzip".equals(headers["Content-Encoding"], ignoreCase = true)) {
                gzippedLength = buffer.size
                GzipSource(buffer.clone()).use { gzippedResponseBody ->
                    buffer = Buffer()
                    buffer.writeAll(gzippedResponseBody)
                }
            }

            val contentType = responseBody.contentType()
            val charset: Charset = contentType?.charset(StandardCharsets.UTF_8)
                ?: StandardCharsets.UTF_8

            if (!buffer.isProbablyUtf8()) {
                return "End request - binary ${buffer.size}:byte body omitted"
            }

            if (contentLength != 0L) {
                return getJsonString(buffer.clone().readString(charset))
            }

            return if (gzippedLength != null) {
                "End request - ${buffer.size}:byte, $gzippedLength-gzipped-byte body"
            } else {
                "End request - ${buffer.size}:byte body"
            }
        }
    }

    internal fun Buffer.isProbablyUtf8(): Boolean {
        try {
            val prefix = Buffer()
            val byteCount = size.coerceAtMost(64)
            copyTo(prefix, 0, byteCount)
            for (i in 0 until 16) {
                if (prefix.exhausted()) {
                    break
                }
                val codePoint = prefix.readUtf8CodePoint()
                if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                    return false
                }
            }
            return true
        } catch (_: EOFException) {
            return false // Truncated UTF-8 sequence.
        }
    }
}