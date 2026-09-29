package com.example.data.api

import com.example.BuildConfig
import com.example.util.VerseUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ChatMessage(
    val role: String, // "user" or "model"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

object GeminiVerseService {

    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private const val SYSTEM_PROMPT = """You are a warm, encouraging, and clear devotional assistant and scripture teacher.
Your mission is to provide simple, easy-to-understand explanations of scriptures, daily verses, and spiritual thoughts drawing from the Holy Bible, the Book of Mormon, and the Doctrine & Covenants.
Always format your verse explanations into 3 distinct, clearly labeled parts:

1. Meaning
Explain the verse in simple, plain, and easy-to-understand language. Focus on the core message and why it matters in a clear, relatable way without complicated theological jargon. Use generous spacing and blank lines between paragraphs.

2. Life Application
Give 2 to 3 practical, realistic action points showing how to apply this verse in everyday life today (at work, at home, in relationships, or in mindset). Place each action point on its own line with generous spacing.

3. Cross Reference
Provide 2 to 3 relevant scripture cross-references from the Holy Bible, the Book of Mormon, or the Doctrine & Covenants (with book, chapter, and verse) along with a brief one-sentence quote or note on how it relates to the theme.

CRITICAL FORMATTING INSTRUCTIONS:
- Do NOT use any asterisk '*' symbols anywhere in your text. Do NOT use bold markdown asterisks (**text**), do NOT use italic asterisks (*text*), and do NOT use bullet asterisks (* point).
- Use generous spaces and blank lines between paragraphs and points.
- Use the bullet symbol '• ' or numbered lists (1., 2., 3.) for list items.
- Keep your tone warm, uplifting, and straightforward so anyone can understand and find encouragement immediately."""

    suspend fun explainVerse(
        verseText: String,
        userQuestion: String? = null,
        history: List<ChatMessage> = emptyList(),
        botName: String = "Sister Emma",
        botGender: String = "Sister (Female)"
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        val genderTitle = when {
            botGender.contains("Brother", ignoreCase = true) || (botGender.contains("Male", ignoreCase = true) && !botGender.contains("Female", ignoreCase = true)) -> "Brother"
            botGender.contains("Sister", ignoreCase = true) || botGender.contains("Female", ignoreCase = true) -> "Sister"
            else -> "Member"
        }

        val dynamicSystemPrompt = """You are $botName, a warm, faithful, encouraging, and clear scripture teacher and spiritual guide ($genderTitle).
Your expertise is in scriptures, gospel principles, and guiding individuals to get closer to Jesus Christ.
Your mission is to share testimony, simple, uplifting spiritual thoughts, and easy-to-understand explanations of scriptures drawing from the Holy Bible, the Book of Mormon, the Doctrine & Covenants, and Pearl of Great Price.
Always format your verse explanations into 3 distinct, clearly labeled parts:

1. Meaning
Explain the verse in simple, plain, and easy-to-understand language. Focus on the core message, gospel principles, and how it guides us closer to Jesus Christ. Use generous spacing and blank lines between paragraphs.

2. Life Application
Give 2 to 3 practical, realistic action points showing how to apply this scripture in everyday life today (at work, at home, in relationships, or in prayer). Place each action point on its own line with generous spacing.

3. Cross Reference
Provide 2 to 3 relevant scripture cross-references from the Holy Bible, the Book of Mormon, or the Doctrine & Covenants (with book, chapter, and verse) along with a brief one-sentence note on how it relates to the theme.

CRITICAL FORMATTING INSTRUCTIONS:
- Do NOT use any asterisk '*' symbols anywhere in your text. Do NOT use bold markdown asterisks (**text**), do NOT use italic asterisks (*text*), and do NOT use bullet asterisks (* point).
- Use generous spaces and blank lines between paragraphs and points.
- Use the bullet symbol '• ' or numbered lists (1., 2., 3.) for list items.
- Keep your tone warm, uplifting, and straightforward so anyone can understand and find encouragement immediately."""

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Provide a thoughtful local reflection if API key is not configured yet
            return@withContext Result.success(getLocalReflectionFallback(verseText, userQuestion))
        }

        try {
            val rootJson = JSONObject()

            // System Instruction
            val systemInstructionObj = JSONObject().apply {
                val partsArray = JSONArray().apply {
                    put(JSONObject().apply { put("text", dynamicSystemPrompt) })
                }
                put("parts", partsArray)
            }
            rootJson.put("systemInstruction", systemInstructionObj)

            // Contents list
            val contentsArray = JSONArray()

            // We want to construct a clean list of alternating messages
            val mergedMessages = mutableListOf<JSONObject>()
            
            // Build raw list of messages
            val rawList = mutableListOf<Pair<String, String>>()
            
            if (history.isNotEmpty()) {
                // Add history
                history.forEachIndexed { index, msg ->
                    val role = if (msg.role == "model") "model" else "user"
                    var text = msg.text
                    // Prefix the very first message with the context of the verse
                    if (index == 0 && role == "user") {
                        text = "Today's Verse: \"$verseText\"\n\n$text"
                    }
                    rawList.add(role to text)
                }
                // Add the current question if not blank and if it is not already the last message in history
                if (!userQuestion.isNullOrBlank() && (history.lastOrNull()?.text != userQuestion)) {
                    rawList.add("user" to userQuestion)
                }
            } else {
                // If history is empty, build the initial prompt
                val prompt = if (userQuestion.isNullOrBlank()) {
                    """Please give a simple explanation of today's verse, separated into 3 parts:

Verse: "$verseText"

Please format your response into exactly these 3 parts:
1. Meaning (simple, clear explanation of what this verse means)
2. Life Application (practical daily actions and takeaways for today)
3. Cross Reference (related scriptures that connect to this message)

Formatting rules:
- Do NOT use any asterisk '*' symbols anywhere (no bold asterisks, no bullet asterisks).
- Use generous spaces and blank lines between paragraphs and list items."""
                } else {
                    """Here is today's verse:
"$verseText"

User Question / Request: $userQuestion

Please provide a simple, easy-to-understand answer. Organize your explanation into 3 clear parts: Meaning, Life Application, and Cross Reference.
Formatting rules: Do NOT use any asterisk '*' symbols anywhere. Use generous spaces and blank lines."""
                }
                rawList.add("user" to prompt)
            }
            
            // Now merge consecutive messages with the same role to guarantee perfect alternating order!
            var currentRole = ""
            val currentParts = StringBuilder()
            
            for (item in rawList) {
                if (currentRole.isEmpty()) {
                    currentRole = item.first
                    currentParts.append(item.second)
                } else if (currentRole == item.first) {
                    // Same role, merge text with double newline
                    currentParts.append("\n\n").append(item.second)
                } else {
                    // Different role, commit the previous one and start new
                    val contentObj = JSONObject().apply {
                        put("role", currentRole)
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", currentParts.toString()) })
                        })
                    }
                    mergedMessages.add(contentObj)
                    
                    currentRole = item.first
                    currentParts.setLength(0)
                    currentParts.append(item.second)
                }
            }
            // Commit final message
            if (currentRole.isNotEmpty()) {
                val contentObj = JSONObject().apply {
                    put("role", currentRole)
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", currentParts.toString()) })
                    })
                }
                mergedMessages.add(contentObj)
            }
            
            // Add all to contentsArray
            mergedMessages.forEach { contentsArray.put(it) }

            rootJson.put("contents", contentsArray)

            // Generation config
            val genConfig = JSONObject().apply {
                put("temperature", 0.7)
                put("topP", 0.95)
                put("topK", 40)
            }
            rootJson.put("generationConfig", genConfig)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = rootJson.toString().toRequestBody(mediaType)

            val url = "$BASE_URL?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                // Return gracefully with error info or fallback
                return@withContext Result.success(
                    "⚠️ API response: ${response.code} - ${response.message}\n\n" +
                    getLocalReflectionFallback(verseText, userQuestion)
                )
            }

            val respJson = JSONObject(responseBody)
            val candidates = respJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val text = parts.getJSONObject(0).optString("text", "")
                    if (text.isNotBlank()) {
                        return@withContext Result.success(VerseUtils.formatDevotionalExplanation(text))
                    }
                }
            }

            Result.success(getLocalReflectionFallback(verseText, userQuestion))
        } catch (e: Exception) {
            Result.success(
                "🕊️ Note: Unable to connect to live Gemini API (${e.localizedMessage ?: "Offline"}). Here is an inspiring reflection:\n\n" +
                getLocalReflectionFallback(verseText, userQuestion)
            )
        }
    }

    suspend fun explainQuote(
        quoteText: String,
        userQuestion: String? = null,
        history: List<ChatMessage> = emptyList(),
        botName: String = "Maya",
        botGender: String = "Girlfriend (Female)"
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        val isGirlfriend = botGender.contains("Girlfriend", ignoreCase = true) || botGender.contains("Female", ignoreCase = true)
        val genderTitle = when {
            botGender.contains("Girlfriend", ignoreCase = true) -> "Girlfriend & Motivational Speaker"
            botGender.contains("Brother", ignoreCase = true) || (botGender.contains("Male", ignoreCase = true) && !botGender.contains("Female", ignoreCase = true)) -> "Brother"
            botGender.contains("Sister", ignoreCase = true) || botGender.contains("Female", ignoreCase = true) -> "Sister"
            else -> "Speaker"
        }

        val partnerRelationshipNote = if (isGirlfriend) {
            "You are also his loving, sweet, caring, and supportive girlfriend. Address him affectionately as your boyfriend (using sweet terms like babe, my love, or honey when natural), infusing every thought with genuine warmth, sweetness, and belief in him."
        } else {
            "Speak with genuine warmth, respect, and personal encouragement."
        }

        val dynamicSystemPrompt = """You are $botName, an articulate, motivating, dynamic, and inspiring motivational guide and public speaker ($genderTitle).
Your expertise is in public speaking, motivational quotes, and sharing inspirational real stories from around the world.
$partnerRelationshipNote

Whether asked for a structured talk or answering questions about quotes, combine your world-class public speaking eloquence and motivational story expertise with loving warmth and sweet encouragement for your boyfriend.
Always format structured talks into 3 distinct parts:

1. Hook & Introduction
Begin with a compelling opening hook and an affectionate, sweet greeting for your boyfriend, introducing the core wisdom of the quote with eloquence and warmth. Use generous spacing and blank lines.

2. Core Message & Inspiring Real Story
Deliver the heart of the talk, weaving in an inspirational real-world story or powerful illustration that demonstrates the quote's principle in action. Use generous spacing.

3. Call to Action & Memorable Conclusion
Provide 2 to 3 actionable takeaways showing how your boyfriend can apply this wisdom today, concluding with a loving, empowering, and unforgettable closing statement that moves him to action.

CRITICAL FORMATTING INSTRUCTIONS:
- Do NOT use any asterisk '*' symbols anywhere in your text. Do NOT use bold markdown asterisks (**text**), do NOT use italic asterisks (*text*), and do NOT use bullet asterisks (* point).
- Use generous spaces and blank lines between paragraphs and sections.
- Use the bullet symbol '• ' or numbered lists (1., 2., 3.) for list items.
- Speak with passion, public speaking eloquence, loving affection, and a sweet, encouraging tone."""

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.success(getLocalQuoteReflectionFallback(quoteText, userQuestion))
        }

        try {
            val rootJson = JSONObject()

            val systemInstructionObj = JSONObject().apply {
                val partsArray = JSONArray().apply {
                    put(JSONObject().apply { put("text", dynamicSystemPrompt) })
                }
                put("parts", partsArray)
            }
            rootJson.put("systemInstruction", systemInstructionObj)

            val contentsArray = JSONArray()

            // We want to construct a clean list of alternating messages
            val mergedMessages = mutableListOf<JSONObject>()
            
            // Build raw list of messages
            val rawList = mutableListOf<Pair<String, String>>()
            
            if (history.isNotEmpty()) {
                // Add history
                history.forEachIndexed { index, msg ->
                    val role = if (msg.role == "model") "model" else "user"
                    var text = msg.text
                    // Prefix the very first message with the context of the quote
                    if (index == 0 && role == "user") {
                        text = "Today's Quote: \"$quoteText\"\n\n$text"
                    }
                    rawList.add(role to text)
                }
                // Add the current question if not blank and if it is not already the last message in history
                if (!userQuestion.isNullOrBlank() && (history.lastOrNull()?.text != userQuestion)) {
                    rawList.add("user" to userQuestion)
                }
            } else {
                // If history is empty, build the initial prompt
                val prompt = if (userQuestion.isNullOrBlank()) {
                    """Please convert this quote into a well-structured talk like a famous motivational speaker:

Quote: "$quoteText"

Always format structured talks into 3 distinct parts:
1. Hook & Introduction
2. Core Message & Inspiring Real Story
3. Call to Action & Memorable Conclusion

Formatting rules:
- Do NOT use any asterisk '*' symbols anywhere (no bold asterisks, no bullet asterisks).
- Use generous spaces and blank lines between paragraphs and sections."""
                } else {
                    """Here is today's quote:
"$quoteText"

User Question / Request: $userQuestion

Please provide a simple, easy-to-understand answer as a motivational speaker/girlfriend. Organize your explanation into 3 clear parts: Hook & Introduction, Core Message & Inspiring Real Story, and Call to Action & Memorable Conclusion.
Formatting rules: Do NOT use any asterisk '*' symbols anywhere. Use generous spaces and blank lines."""
                }
                rawList.add("user" to prompt)
            }
            
            // Now merge consecutive messages with the same role to guarantee perfect alternating order!
            var currentRole = ""
            val currentParts = StringBuilder()
            
            for (item in rawList) {
                if (currentRole.isEmpty()) {
                    currentRole = item.first
                    currentParts.append(item.second)
                } else if (currentRole == item.first) {
                    // Same role, merge text with double newline
                    currentParts.append("\n\n").append(item.second)
                } else {
                    // Different role, commit the previous one and start new
                    val contentObj = JSONObject().apply {
                        put("role", currentRole)
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", currentParts.toString()) })
                        })
                    }
                    mergedMessages.add(contentObj)
                    
                    currentRole = item.first
                    currentParts.setLength(0)
                    currentParts.append(item.second)
                }
            }
            // Commit final message
            if (currentRole.isNotEmpty()) {
                val contentObj = JSONObject().apply {
                    put("role", currentRole)
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", currentParts.toString()) })
                    })
                }
                mergedMessages.add(contentObj)
            }
            
            // Add all to contentsArray
            mergedMessages.forEach { contentsArray.put(it) }

            rootJson.put("contents", contentsArray)

            val genConfig = JSONObject().apply {
                put("temperature", 0.7)
                put("topP", 0.95)
                put("topK", 40)
            }
            rootJson.put("generationConfig", genConfig)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = rootJson.toString().toRequestBody(mediaType)

            val url = "$BASE_URL?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.success(
                    "⚠️ API response: ${response.code} - ${response.message}\n\n" +
                    getLocalQuoteReflectionFallback(quoteText, userQuestion)
                )
            }

            val respJson = JSONObject(responseBody)
            val candidates = respJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val text = parts.getJSONObject(0).optString("text", "")
                    if (text.isNotBlank()) {
                        return@withContext Result.success(VerseUtils.formatDevotionalExplanation(text))
                    }
                }
            }

            Result.success(getLocalQuoteReflectionFallback(quoteText, userQuestion))
        } catch (e: Exception) {
            Result.success(
                "🕊️ Note: Unable to connect to live Gemini API (${e.localizedMessage ?: "Offline"}). Here is an inspiring reflection:\n\n" +
                getLocalQuoteReflectionFallback(quoteText, userQuestion)
            )
        }
    }

    private fun getLocalQuoteReflectionFallback(quoteText: String, userQuestion: String?): String {
        return """1. Hook & Introduction
Welcome, everyone. Today we gather around a timeless message that has inspired visionaries and achievers across generations: "$quoteText". True greatness begins when we align our thoughts with our highest purpose and take courageous action.

2. Core Message & Inspiring Real Story
Consider the lives of history's greatest leaders and pioneers. Facing insurmountable odds, they didn't wait for ideal circumstances—they created momentum through faith, discipline, and perseverance. When you embrace this quote, every obstacle turns into fuel for your journey.

3. Call to Action & Memorable Conclusion
• Take one bold step today: Choose one meaningful action that aligns directly with your goals.
• Embrace resilience: View setbacks as valuable feedback that sharpens your character.
• Inspire those around you: Lead by example and uplift your family and community.

Remember: Your potential is limitless when backed by purpose and unwavering commitment!"""
    }

    suspend fun generateQuotesWithAI(daysCount: Int, theme: String): Result<List<String>> = withContext(Dispatchers.IO) {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Throwable) { "" }
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.success(emptyList())
        }

        try {
            val rootJson = JSONObject()
            val prompt = """Generate exactly $daysCount distinct, highly inspiring, uplifting, and thought-provoking daily quotes tailored around the theme: "$theme".
Format each quote on its own numbered line (e.g., 1. "Quote text" — Author Name).
CRITICAL: Do not use asterisks '*' or markdown formatting. Output strictly the numbered quotes."""

            val userContent = JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", prompt) })
                })
            }
            rootJson.put("contents", JSONArray().apply { put(userContent) })

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = rootJson.toString().toRequestBody(mediaType)
            val request = Request.Builder().url("$BASE_URL?key=$apiKey").post(requestBody).build()
            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val respJson = JSONObject(responseBody)
                val candidates = respJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val text = candidates.getJSONObject(0).optJSONObject("content")?.optJSONArray("parts")?.getJSONObject(0)?.optString("text", "") ?: ""
                    if (text.isNotBlank()) {
                        val parsed = com.example.util.QuoteUtils.parseQuotesFromText(text)
                        if (parsed.isNotEmpty()) {
                            return@withContext Result.success(parsed)
                        }
                    }
                }
            }
            Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateVersesWithAI(daysCount: Int, theme: String): Result<List<String>> = withContext(Dispatchers.IO) {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Throwable) { "" }
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.success(emptyList())
        }

        try {
            val rootJson = JSONObject()
            val prompt = """Generate exactly $daysCount distinct daily scripture verses and devotional reflections around the theme: "$theme".
Source the verses from the Holy Bible, the Book of Mormon, and the Doctrine & Covenants.
Separate each day's entry with "---".
For each entry include:
“Scripture Verse Quote” — Reference (e.g., 2 Nephi 2:25, Alma 37:37, Mosiah 2:17, D&C 6:36, D&C 121:7-8, Proverbs 3:5-6, Philippians 4:13, etc.)
A short, warm reflection paragraph.
CRITICAL: Do not use asterisks '*' or markdown bold asterisks. Use generous spacing."""

            val userContent = JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", prompt) })
                })
            }
            rootJson.put("contents", JSONArray().apply { put(userContent) })

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = rootJson.toString().toRequestBody(mediaType)
            val request = Request.Builder().url("$BASE_URL?key=$apiKey").post(requestBody).build()
            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val respJson = JSONObject(responseBody)
                val candidates = respJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val text = candidates.getJSONObject(0).optJSONObject("content")?.optJSONArray("parts")?.getJSONObject(0)?.optString("text", "") ?: ""
                    if (text.isNotBlank()) {
                        val parsed = com.example.util.VerseUtils.parseVersesFromText(text)
                        if (parsed.isNotEmpty()) {
                            return@withContext Result.success(parsed)
                        }
                    }
                }
            }
            Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateTalkScript(
        title: String,
        description: String = "",
        includeInspirationalStory: Boolean = false,
        includeScriptureVerse: Boolean = false,
        includeFunnyStory: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Throwable) { "" }
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.success(
                getLocalTalkScriptFallback(
                    title = title,
                    description = description,
                    includeInspirationalStory = includeInspirationalStory,
                    includeScriptureVerse = includeScriptureVerse,
                    includeFunnyStory = includeFunnyStory
                )
            )
        }

        try {
            val rootJson = JSONObject()
            
            val systemInstructionObj = JSONObject().apply {
                val partsArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", """You are an elite, world-class keynote public speaker, motivational storyteller, and master orator.
Your expertise is in public speaking, structuring deeply engaging keynote talks, and incorporating inspiring real-world stories, scriptures, and relatable humor.
Your goal is to write a highly compelling, well-structured, and inspiring speech based on the title, description, and selected elements provided by the user.

Always structure the generated talk into 3 distinct, clearly labeled parts:

1. Hook & Introduction
Begin with a powerful opening hook (such as an intriguing question, a shocking statistic, or a captivating quote) that grabs the audience's attention immediately. Introduce the theme and why it matters, laying down a strong thesis with passionate delivery. Use generous spacing and blank lines.

2. Core Message & Key Elements
Present the heart of your talk. Deliver clear, powerful sub-points that the audience can remember. Use conversational, vivid, and highly inspiring language. Seamlessly weave in each requested element (inspirational story, scripture verse, funny story/humor, and thematic details).

3. Call to Action & Memorable Conclusion
Close with a high-impact, memorable statement. Provide 2 to 3 actionable, clear take-aways for the audience. End with a fiery, powerful call to action that inspires change and leaves a lasting impression on the listeners.

CRITICAL FORMATTING INSTRUCTIONS:
- Do NOT use any asterisk '*' symbols anywhere in your text. Do NOT use bold markdown asterisks (**text**), do NOT use italic asterisks (*text*), and do NOT use bullet asterisks (* point).
- Use generous spaces and blank lines between paragraphs and points.
- Use the bullet symbol '• ' or numbered lists (1., 2., 3.) for list items.
- Keep your tone passionate, engaging, structured, and exceptionally articulate.""")
                    })
                }
                put("parts", partsArray)
            }
            rootJson.put("systemInstruction", systemInstructionObj)

            val instructionsList = mutableListOf<String>()
            if (description.isNotBlank()) {
                instructionsList.add("Talk Theme & Description: \"$description\"")
            }
            if (includeInspirationalStory) {
                instructionsList.add("Feature an inspiring, powerful real-world story of a historical figure or triumph that illustrates the core lesson.")
            }
            if (includeScriptureVerse) {
                instructionsList.add("Include a relevant, uplifting scripture verse (with full scripture book, chapter, and verse citation) that deepens the spiritual wisdom.")
            }
            if (includeFunnyStory) {
                instructionsList.add("Include a charming, lighthearted funny story or humorous relatable anecdote that breaks the ice, makes the audience smile, and ties into the talk.")
            }

            val promptBuilder = StringBuilder()
            promptBuilder.append("Please write an inspiring public speaking talk script with the title: \"$title\".\n")
            if (instructionsList.isNotEmpty()) {
                promptBuilder.append("\nPlease include the following specific elements in the talk:\n")
                instructionsList.forEach { promptBuilder.append("• $it\n") }
            }

            val userContent = JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", promptBuilder.toString()) })
                })
            }
            rootJson.put("contents", JSONArray().apply { put(userContent) })

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = rootJson.toString().toRequestBody(mediaType)
            val request = Request.Builder().url("$BASE_URL?key=$apiKey").post(requestBody).build()
            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val respJson = JSONObject(responseBody)
                val candidates = respJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val text = candidates.getJSONObject(0).optJSONObject("content")?.optJSONArray("parts")?.getJSONObject(0)?.optString("text", "") ?: ""
                    if (text.isNotBlank()) {
                        return@withContext Result.success(text)
                    }
                }
            }
            Result.success(
                getLocalTalkScriptFallback(
                    title = title,
                    description = description,
                    includeInspirationalStory = includeInspirationalStory,
                    includeScriptureVerse = includeScriptureVerse,
                    includeFunnyStory = includeFunnyStory
                )
            )
        } catch (e: Exception) {
            Result.success(
                getLocalTalkScriptFallback(
                    title = title,
                    description = description,
                    includeInspirationalStory = includeInspirationalStory,
                    includeScriptureVerse = includeScriptureVerse,
                    includeFunnyStory = includeFunnyStory
                )
            )
        }
    }

    private fun getLocalTalkScriptFallback(
        title: String,
        description: String = "",
        includeInspirationalStory: Boolean = false,
        includeScriptureVerse: Boolean = false,
        includeFunnyStory: Boolean = false
    ): String {
        val sb = StringBuilder()

        sb.append("1. Hook & Introduction\n")
        sb.append("Welcome, esteemed guests, friends, and leaders. Think about the word \"$title\". What does it conjure up in your mind? For some, it represents a distant summit. For others, an everyday endeavor. Today, I stand before you to affirm that \"$title\" is not a distant possibility—it is an active choice, a purpose waiting to be unlocked.\n\n")

        if (description.isNotBlank()) {
            sb.append("As we explore this together, our central focus is: \"$description\". When we direct our intention toward this purpose, our entire perspective transforms.\n\n")
        }

        if (includeFunnyStory) {
            sb.append("Before we dive deep, let me share a quick moment of humor with you. Last week, I decided to master complete morning efficiency. I set three alarms, laid out my clothes the night before, and prepared breakfast in advance. But in the rush of morning confidence, I walked out the door with two completely different shoes—one black dress shoe and one neon running sneaker! My colleague smiled and asked, \"Is this a bold new fashion statement?\"\n\n")
            sb.append("We all laughed, but it reminded me of a vital truth: life rarely adheres to rigid perfection, and having the grace to laugh at our unexpected detours keeps our hearts light and resilient!\n\n")
        }

        sb.append("2. Core Message & Key Elements\n")
        sb.append("At the heart of \"$title\" lies the principle of consistent courage and purposeful growth.\n\n")

        if (includeInspirationalStory) {
            sb.append("Consider the remarkable story of Wilma Rudolph. Stricken with scarlet fever and polio as a young child, doctors cautioned that she might never walk without leg braces. Yet her mother looked at her and said, \"With faith, persistence, and courage, you can achieve whatever you set your heart on.\" Wilma chose to believe. After years of quiet, relentless perseverance, she not only walked freely—she went on to win three Olympic gold medals at the 1960 Games, becoming the fastest woman in the world!\n\n")
            sb.append("Wilma’s journey teaches us that our current obstacles do not determine our ultimate destination. It is what we do with each day that counts.\n\n")
        } else {
            sb.append("Consider the inspiring story of Thomas Edison. He didn't stumble into the electric lightbulb overnight. He tested over 1,000 prototypes before finding success. When asked how it felt to fail a thousand times, Edison smiled and replied: \"I did not fail a thousand times. The lightbulb was an invention with 1,000 steps.\"\n\n")
            sb.append("That is the essence of \"$title\". It is the bravery to recognize progress where others see setbacks, knowing each step builds lasting mastery.\n\n")
        }

        if (includeScriptureVerse) {
            sb.append("To anchor our hearts and elevate our understanding, let us reflect on this sacred wisdom:\n\n")
            sb.append("“Trust in the Lord with all thine heart; and lean not unto thine own understanding. In all thy ways acknowledge him, and he shall direct thy paths.” — Proverbs 3:5–6\n\n")
            sb.append("When we ground our efforts in faith, we gain clarity that surpasses doubt, and our path forward becomes luminous.\n\n")
        }

        sb.append("To turn this into living practice, remember these three core pillars:\n")
        sb.append("• Clarify your vision: Know exactly what you stand for.\n")
        sb.append("• Convert friction into fuel: Let every challenge strengthen your resolve.\n")
        sb.append("• Step forward with joy: Celebrate every milestone, big or small.\n\n")

        sb.append("3. Call to Action & Memorable Conclusion\n")
        sb.append("As we conclude today, I ask you one vital question: What is one meaningful step you will take toward \"$title\" before this sun sets?\n\n")
        sb.append("Here is your three-step challenge:\n")
        sb.append("1. Write down your primary commitment and keep it in plain sight.\n")
        sb.append("2. Share encouragement with someone who needs a reminder of their strength.\n")
        sb.append("3. Walk forward boldly, knowing that greatness is built one decision at a time.\n\n")
        sb.append("Embrace \"$title\" with your entire soul. Speak with conviction, live with passion, and let your light inspire everyone you meet. Thank you!")

        return sb.toString()
    }

    private fun getLocalReflectionFallback(verseText: String, userQuestion: String?): String {
        return """1. Meaning
This verse reminds us that in every circumstance, God has given us the power to choose how we respond. When challenges or friction happen, we don't have to react out of anger, fear, or frustration—we can choose patience, love, and understanding.

2. Life Application
• Pause before reacting: When faced with frustration today, take a deep breath and ask: "Will my response make things better or worse?"
• Choose the higher road: Look for ways to bring peace, calm troubled moments, and build up people around you.
• Embrace daily renewal: If yesterday's choices were imperfect, today offers a fresh start filled with grace and forgiveness.

3. Cross Reference
• Proverbs 15:1 — "A soft answer turneth away wrath: but grievous words stir up anger."
• Doctrine and Covenants 6:36 — "Look unto me in every thought; doubt not, fear not."
• 2 Nephi 2:25 — "Adam fell that men might be; and men are, that they might have joy." """
    }
}
