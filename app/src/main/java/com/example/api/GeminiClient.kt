package com.example.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val TAG = "GeminiClient"
    
    // Using guidelines task default model for basic text: 'gemini-3.5-flash'
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Sends the prompt to key-based Gemini REST API and returns the response text.
     */
    suspend fun generateAdvice(prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API Key is placeholder or missing. Please configure secrets.")
            return@withContext "API Key is not configured. Go to the AI Studio Secrets panel to configure your GEMINI_API_KEY to receive custom AI screen time coaching."
        }

        try {
            // Build the JSON request body
            val requestBodyJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            val partObj = JSONObject().apply {
                                put("text", prompt)
                            }
                            put(partObj)
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)

                // Optional system instruction to enforce expert persona
                val systemInstructionObj = JSONObject().apply {
                    val partsArray = JSONArray().apply {
                        val partObj = JSONObject().apply {
                            put("text", "You are an elite Digital Wellness & Productivity Guide. Your objective is to review social media screen times, evaluate goals/limits, and provide a short, supportive, actionable review. Write 3 bullet points. Max 140 words. Focus on productivity optimization.")
                        }
                        put(partObj)
                    }
                    put("parts", partsArray)
                }
                put("systemInstruction", systemInstructionObj)
            }

            val requestBodyStr = requestBodyJson.toString()
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestBodyStr.toRequestBody(mediaType)

            val url = "$BASE_URL?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val responseStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Log.e(TAG, "Unsuccessful response from Gemini: Code ${response.code}, message: $responseStr")
                    return@withContext "Gemini API error (code ${response.code}). Ensure your API key is valid in the secrets panel."
                }

                val responseJson = JSONObject(responseStr)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text")
                    }
                }
                return@withContext "No response from AI advisor. Try refreshing again."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Gemini content generation: ${e.message}", e)
            return@withContext "Could not connect to AI Productivity Advisor: ${e.localizedMessage}. Check internet connection."
        }
    }
}
