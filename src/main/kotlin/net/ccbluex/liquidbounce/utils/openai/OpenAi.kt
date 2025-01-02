/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.utils.openai

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.io.*

const val OPENAI_BASE_URL = "https://api.openai.com/v1"

/**
 * OpenAI API
 */
class OpenAi(
    private val baseUrl: String = OPENAI_BASE_URL,
    private val openAiKey: String,
    private val model: String,
    private val prompt: String
) {

    /**
     * {
     *     "model": "gpt-3.5-turbo",
     *     "messages": [
     *       {
     *         "role": "system",
     *         "content": "You are a helpful assistant."
     *       },
     *       {
     *         "role": "user",
     *         "content": "Hello!"
     *       }
     *     ]
     *   }
     */
    fun requestNewAnswer(question: String): String {
        val systemRole = JsonObject()
        systemRole.addProperty("role", "system")
        systemRole.addProperty("content", prompt)

        val userRole = JsonObject()
        userRole.addProperty("role", "user")
        userRole.addProperty("content", question)

        val messages = JsonArray()
        messages.add(systemRole)
        messages.add(userRole)

        val body = JsonObject()
        body.addProperty("model", model)
        body.add("messages", messages)

        // Send request
        try {
            val response = runBlocking(Dispatchers.IO) {
                HttpClient.request("$baseUrl/chat/completions",
                    HttpMethod.POST,
                    headers = {
                        add("Authorization", "Bearer $openAiKey")
                    },
                    body = body.toString().asJson()
                ).parse<JsonObject>()
            }

            return response["choices"]
                .asJsonArray[0]
                .asJsonObject["message"]
                .asJsonObject["content"]
                .asString
        } catch (e: HttpException) {
            /**
             * {
             *     "error": {
             *         "message": "Incorrect API key provided: dasdasdw.
             *         You can find your API key at https://platform.openai.com/account/api-keys.",
             *         "type": "invalid_request_error",
             *         "param": null,
             *         "code": "invalid_api_key"
             *     }
             * }
             */
            val responseJson = JsonParser.parseString(e.message).asJsonObject
            val errorJson = responseJson["error"].asJsonObject

            error("OpenAI returned an error: ${errorJson["message"].asString}")
            logger.error("Failed to send request to OpenAI", e)
        } catch (e: Exception) {
            logger.error("Failed to send request to OpenAI", e)
            throw e
        }
    }

}
