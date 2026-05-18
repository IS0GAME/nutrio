package com.nutrition.scanner.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class NutritionAnalyzer(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun analyzeFood(bitmap: Bitmap): Result<NutritionInfo> = withContext(Dispatchers.IO) {
        try {
            val base64Image = bitmapToBase64(bitmap)

            val jsonPrompt = """
            {
                "contents": [{
                    "parts": [
                        {
                            "text": "Analyze this food image and return ONLY a JSON object with this exact structure (no markdown, no explanation): {\"foodName\": \"name of the dish in English or Thai\", \"calories\": number (kcal per 100g), \"protein\": number (g per 100g), \"carbs\": number (g per 100g), \"fat\": number (g per 100g), \"fiber\": number (g per 100g), \"sugar\": number (g per 100g), \"sodium\": number (mg per 100g), \"servingSize\": \"estimated serving size like 1 bowl, 1 plate, 1 piece\", \"confidence\": number between 0 and 1}. Be as accurate as possible."
                        },
                        {
                            "inline_data": {
                                "mime_type": "image/jpeg",
                                "data": "$base64Image"
                            }
                        }
                    ]
                }],
                "generationConfig": {
                    "responseModalities": ["TEXT"]
                }
            }
            """.trimIndent()

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey"
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = jsonPrompt.toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("API error: ${response.code} ${response.message}"))
            }

            val responseBody = response.body?.string() ?: ""
            val jsonResponse = gson.fromJson(responseBody, JsonObject::class.java)

            val text = jsonResponse
                .getAsJsonArray("candidates")
                ?.get(0)?.asJsonObject
                ?.getAsJsonObject("content")
                ?.getAsJsonArray("parts")
                ?.get(0)?.asJsonObject
                ?.get("text")?.asString ?: ""

            // Parse nutrition from response
            val cleanedText = text
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val nutrition = gson.fromJson(cleanedText, NutritionInfo::class.java)

            Result.success(nutrition)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 800, 800, true)
        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }
}
