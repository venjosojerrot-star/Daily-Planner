package com.example.util

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import java.util.Calendar

object QuoteUtils {

    private const val PREFS_NAME = "quote_of_the_day_prefs"
    private const val KEY_QUOTES = "monthly_quotes_json"

    val DEFAULT_31_QUOTES = listOf(
        "\"The secret of getting ahead is getting started.\" — Mark Twain",
        "\"It always seems impossible until it's done.\" — Nelson Mandela",
        "\"Don't watch the clock; do what it does. Keep going.\" — Sam Levenson",
        "\"Believe you can and you're halfway there.\" — Theodore Roosevelt",
        "\"Start where you are. Use what you have. Do what you can.\" — Arthur Ashe",
        "\"Your time is limited, so don't waste it living someone else's life.\" — Steve Jobs",
        "\"Small daily improvements over time lead to stunning results.\" — Robin Sharma",
        "\"Action is the foundational key to all success.\" — Pablo Picasso",
        "\"You don't have to be great to start, but you have to start to be great.\" — Zig Ziglar",
        "\"Focus on being productive instead of busy.\" — Tim Ferriss",
        "\"Do what you can, with what you have, where you are.\" — Theodore Roosevelt",
        "\"Success is the sum of small efforts repeated day in and day out.\" — Robert Collier",
        "\"The only limit to our realization of tomorrow will be our doubts of today.\" — Franklin D. Roosevelt",
        "\"Hard work beats talent when talent fails to work hard.\" — Tim Notke",
        "\"Make each day your masterpiece.\" — John Wooden",
        "\"Simplicity is the soul of efficiency.\" — Austin Freeman",
        "\"Continuous improvement is better than delayed perfection.\" — Mark Twain",
        "\"Doubt kills more dreams than failure ever will.\" — Suzy Kassem",
        "\"The best way to predict your future is to create it.\" — Abraham Lincoln",
        "\"Discipline is the bridge between goals and accomplishment.\" — Jim Rohn",
        "\"You are never too old to set another goal or to dream a new dream.\" — C.S. Lewis",
        "\"Every moment is a fresh beginning.\" — T.S. Eliot",
        "\"Dream big and dare to fail.\" — Norman Vaughan",
        "\"Courage doesn't always roar. Sometimes courage is the quiet voice at the end of the day saying, 'I will try again tomorrow.'\" — Mary Anne Radmacher",
        "\"Happiness is not something readymade. It comes from your own actions.\" — Dalai Lama",
        "\"Keep your face always toward the sunshine—and shadows will fall behind you.\" — Walt Whitman",
        "\"Opportunities don't happen. You create them.\" — Chris Grosser",
        "\"Either you run the day or the day runs you.\" — Jim Rohn",
        "\"What we achieve inwardly will change outer reality.\" — Plutarch",
        "\"Great things are done by a series of small things brought together.\" — Vincent Van Gogh",
        "\"Be the energy you want to attract.\" — Unknown"
    )

    fun loadQuotes(context: Context): List<String> {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_QUOTES, null) ?: return DEFAULT_31_QUOTES
        return try {
            val jsonArray = JSONArray(json)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                val q = jsonArray.getString(i).trim()
                if (q.isNotEmpty()) {
                    list.add(q)
                }
            }
            if (list.isEmpty()) DEFAULT_31_QUOTES else list
        } catch (e: Exception) {
            DEFAULT_31_QUOTES
        }
    }

    fun saveQuotes(context: Context, quotes: List<String>) {
        val filtered = quotes.map { it.trim() }.filter { it.isNotEmpty() }
        val finalQuotes = if (filtered.isEmpty()) DEFAULT_31_QUOTES else filtered
        val jsonArray = JSONArray()
        finalQuotes.forEach { jsonArray.put(it) }
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_QUOTES, jsonArray.toString()).apply()
    }

    fun parseQuotesFromText(rawText: String): List<String> {
        val lines = rawText.lines()
        val parsed = mutableListOf<String>()
        var currentBlock = StringBuilder()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                if (currentBlock.isNotEmpty()) {
                    parsed.add(cleanQuoteLine(currentBlock.toString()))
                    currentBlock = StringBuilder()
                }
            } else {
                // Check if the line starts with a new item pattern like "1.", "Day 1:", "•", "-"
                val isNewItem = trimmed.matches(Regex("""^(\d+[\.\):]|\bDay\s+\d+[\.\):]|•|-|\*)\s+.*"""))
                if (isNewItem && currentBlock.isNotEmpty()) {
                    parsed.add(cleanQuoteLine(currentBlock.toString()))
                    currentBlock = StringBuilder(trimmed)
                } else {
                    if (currentBlock.isNotEmpty()) {
                        currentBlock.append(" ").append(trimmed)
                    } else {
                        currentBlock.append(trimmed)
                    }
                }
            }
        }
        if (currentBlock.isNotEmpty()) {
            parsed.add(cleanQuoteLine(currentBlock.toString()))
        }

        return parsed.filter { it.isNotBlank() }
    }

    private fun cleanQuoteLine(line: String): String {
        return line.replace(Regex("""^(\d+[\.\):]|\bDay\s+\d+[\.\):]|•|-|\*)\s+"""), "").trim()
    }

    fun getQuoteForDay(quotes: List<String>, timestampMillis: Long): String {
        if (quotes.isEmpty()) return DEFAULT_31_QUOTES.first()
        val cal = Calendar.getInstance().apply { timeInMillis = timestampMillis }
        val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH) // 1..31
        val index = (dayOfMonth - 1) % quotes.size
        return quotes[index]
    }

    val EXTENDED_CURATED_QUOTES = listOf(
        "\"The secret of getting ahead is getting started.\" — Mark Twain",
        "\"It always seems impossible until it's done.\" — Nelson Mandela",
        "\"Don't watch the clock; do what it does. Keep going.\" — Sam Levenson",
        "\"Believe you can and you're halfway there.\" — Theodore Roosevelt",
        "\"Start where you are. Use what you have. Do what you can.\" — Arthur Ashe",
        "\"Your time is limited, so don't waste it living someone else's life.\" — Steve Jobs",
        "\"Small daily improvements over time lead to stunning results.\" — Robin Sharma",
        "\"Action is the foundational key to all success.\" — Pablo Picasso",
        "\"You don't have to be great to start, but you have to start to be great.\" — Zig Ziglar",
        "\"Focus on being productive instead of busy.\" — Tim Ferriss",
        "\"Do what you can, with what you have, where you are.\" — Theodore Roosevelt",
        "\"Success is the sum of small efforts repeated day in and day out.\" — Robert Collier",
        "\"The only limit to our realization of tomorrow will be our doubts of today.\" — Franklin D. Roosevelt",
        "\"Hard work beats talent when talent fails to work hard.\" — Tim Notke",
        "\"Make each day your masterpiece.\" — John Wooden",
        "\"Simplicity is the soul of efficiency.\" — Austin Freeman",
        "\"Continuous improvement is better than delayed perfection.\" — Mark Twain",
        "\"Doubt kills more dreams than failure ever will.\" — Suzy Kassem",
        "\"The best way to predict your future is to create it.\" — Abraham Lincoln",
        "\"Discipline is the bridge between goals and accomplishment.\" — Jim Rohn",
        "\"You are never too old to set another goal or to dream a new dream.\" — C.S. Lewis",
        "\"Every moment is a fresh beginning.\" — T.S. Eliot",
        "\"Dream big and dare to fail.\" — Norman Vaughan",
        "\"Courage doesn't always roar. Sometimes courage is the quiet voice at the end of the day saying, 'I will try again tomorrow.'\" — Mary Anne Radmacher",
        "\"Happiness is not something readymade. It comes from your own actions.\" — Dalai Lama",
        "\"Keep your face always toward the sunshine—and shadows will fall behind you.\" — Walt Whitman",
        "\"Opportunities don't happen. You create them.\" — Chris Grosser",
        "\"Either you run the day or the day runs you.\" — Jim Rohn",
        "\"What we achieve inwardly will change outer reality.\" — Plutarch",
        "\"Great things are done by a series of small things brought together.\" — Vincent Van Gogh",
        "\"Be the energy you want to attract.\" — Unknown",
        "\"Wisdom begins in wonder.\" — Socrates",
        "\"Kindness is a language which the deaf can hear and the blind can see.\" — Mark Twain",
        "\"In the middle of every difficulty lies opportunity.\" — Albert Einstein",
        "\"Patience, persistence and perspiration make an unbeatable combination for success.\" — Napoleon Hill",
        "\"Gratitude turns what we have into enough, and more.\" — Melody Beattie",
        "\"Peace comes from within. Do not seek it without.\" — Buddha",
        "\"The future belongs to those who believe in the beauty of their dreams.\" — Eleanor Roosevelt",
        "\"He who has a why to live can bear almost any how.\" — Friedrich Nietzsche",
        "\"Turn your wounds into wisdom.\" — Oprah Winfrey",
        "\"Energy and persistence conquer all things.\" — Benjamin Franklin",
        "\"Act as if what you do makes a difference. It does.\" — William James",
        "\"Never let the fear of striking out keep you from playing the game.\" — Babe Ruth",
        "\"Live each day as if life had just begun.\" — Johann Wolfgang von Goethe",
        "\"Strive not to be a success, but rather to be of value.\" — Albert Einstein",
        "\"The journey of a thousand miles begins with one step.\" — Lao Tzu",
        "\"Smile, breathe, and go slowly.\" — Thich Nhat Hanh",
        "\"Joy is not in things; it is in us.\" — Richard Wagner",
        "\"Character is power.\" — Booker T. Washington",
        "\"Light tomorrow with today.\" — Elizabeth Barrett Browning"
    )

    fun generateQuotesForDateSpan(startDateMillis: Long, endDateMillis: Long, includeDatesInText: Boolean = false): List<String> {
        val startCal = Calendar.getInstance().apply {
            timeInMillis = startDateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = Calendar.getInstance().apply {
            timeInMillis = endDateMillis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        if (endCal.before(startCal)) {
            endCal.timeInMillis = startCal.timeInMillis + (30L * 24 * 60 * 60 * 1000)
        }

        val dateSdf = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
        val generated = mutableListOf<String>()
        val currCal = startCal.clone() as Calendar
        var dayIndex = 0

        while (!currCal.after(endCal) && generated.size < 366) {
            val quotePool = EXTENDED_CURATED_QUOTES
            val quote = quotePool[dayIndex % quotePool.size]
            val dateStr = dateSdf.format(currCal.time)
            val formatted = if (includeDatesInText) {
                "Day ${dayIndex + 1} ($dateStr): $quote"
            } else {
                quote
            }
            generated.add(formatted)
            currCal.add(Calendar.DAY_OF_YEAR, 1)
            dayIndex++
        }

        return if (generated.isNotEmpty()) generated else DEFAULT_31_QUOTES
    }

    fun getDayNumber(timestampMillis: Long): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = timestampMillis }
        return cal.get(Calendar.DAY_OF_MONTH)
    }
}
