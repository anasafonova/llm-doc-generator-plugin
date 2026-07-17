package com.example

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Minimal HTTP client for the Anthropic Messages API.
 *
 * Uses Gson (already on the IntelliJ Platform classpath) and
 * [java.net.http.HttpClient] to avoid pulling in extra dependencies
 * (e.g. Ktor or OkHttp) just for a single API call.
 *
 * The API key is read from the ANTHROPIC_API_KEY environment variable
 * (see README for how to set it when launching runIde). The model can be
 * overridden via DOC_GENERATOR_MODEL; defaults to claude-haiku-4-5-20251001.
 *
 * If the key is missing, or the request fails for any reason, the method
 * returns the prompt itself wrapped as a comment instead of throwing. This
 * makes it easy to inspect exactly what would have been sent to the model
 * without making a real API call.
 */
object LlmClient {

    private const val API_URL = "https://api.anthropic.com/v1/messages"
    private const val ANTHROPIC_VERSION = "2023-06-01"
    private const val MAX_TOKENS = 512

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build()

    private val apiKey: String? = System.getenv("ANTHROPIC_API_KEY")
    private val model: String = System.getenv("DOC_GENERATOR_MODEL") ?: "claude-haiku-4-5-20251001"

    fun generate(prompt: String): String {
        val key = apiKey
        if (key.isNullOrBlank()) {
            return wrapAsComment(
                "ANTHROPIC_API_KEY is not set. Prompt that would have been sent to the model:",
                prompt
            )
        }

        val body = JsonObject().apply {
            addProperty("model", model)
            addProperty("max_tokens", MAX_TOKENS)
            add("messages", JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("role", "user")
                    addProperty("content", prompt)
                })
            })
        }

        val request = HttpRequest.newBuilder()
            .uri(URI.create(API_URL))
            .timeout(Duration.ofSeconds(60))
            .header("Content-Type", "application/json")
            .header("x-api-key", key)
            .header("anthropic-version", ANTHROPIC_VERSION)
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build()

        return try {
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() !in 200..299) {
                return wrapAsComment(
                    "Model request returned HTTP ${response.statusCode()}: " +
                            response.body().take(300) + "\n\nPrompt that was sent:",
                    prompt
                )
            }

            val json = JsonParser.parseString(response.body()).asJsonObject
            val content = json.getAsJsonArray("content")
                ?.get(0)?.asJsonObject
                ?.get("text")?.asString

            content?.trim()?.takeIf { it.isNotEmpty() }
                ?: wrapAsComment("Model returned an empty response. Prompt:", prompt)

        } catch (e: Exception) {
            wrapAsComment(
                "Model request failed with ${e.javaClass.simpleName}: ${e.message}\n\nPrompt:",
                prompt
            )
        }
    }

    /**
     * Wraps a message (and optional extra text) into a valid /** ... */ block,
     * with each line prefixed by " * " so it parses correctly as both a
     * JavaDoc and a KDoc comment.
     */
    private fun wrapAsComment(message: String, extra: String? = null): String {
        val fullText = if (extra != null) "$message\n\n$extra" else message
        val body = fullText.trimEnd().lines().joinToString("\n") { " * $it" }
        return "/**\n$body\n */"
    }
}