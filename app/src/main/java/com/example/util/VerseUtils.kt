package com.example.util

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import java.util.Calendar

object VerseUtils {

    private const val PREFS_NAME = "daily_verse_prefs"
    private const val KEY_VERSES = "monthly_verses_json"

    val DEFAULT_31_VERSES = listOf(
        // Day 1 (Book of Mormon)
        """“I will go and do the things which the Lord hath commanded, for I know that the Lord giveth no commandments unto the children of men, save he shall prepare a way for them that they may accomplish the thing which he commandeth them.” — 1 Nephi 3:7

When God calls you to act or sets a task before you, move forward in faith. He always prepares the path for those who trust Him.""",

        // Day 2 (Holy Bible)
        """“Trust in the Lord with all thine heart; and lean not unto thine own understanding. In all thy ways acknowledge him, and he shall direct thy paths.” — Proverbs 3:5-6

When uncertainty arises, surrender your anxieties to God. His vision sees far beyond our current horizon. Trust His timing and guidance today.""",

        // Day 3 (Doctrine & Covenants)
        """“Look unto me in every thought; doubt not, fear not.” — Doctrine and Covenants 6:36

Center your mindset on the Savior whenever fear or doubt tries to enter. Keeping your gaze fixed on Him brings instant reassurance, clarity, and peace.""",

        // Day 4 (Book of Mormon)
        """“Adam fell that men might be; and men are, that they might have joy.” — 2 Nephi 2:25

Your divine purpose in this mortal life is centered on joy. Through the Savior, you can find authentic peace and happiness even in the midst of trials.""",

        // Day 5 (Holy Bible)
        """“Peace I leave with you, my peace I give unto you: not as the world giveth, give I unto you. Let not your heart be troubled, neither let it be afraid.” — John 14:27

The Savior's peace is not the absence of storms, but His calming presence within the storm. Breathe deeply and rest in His divine love today.""",

        // Day 6 (Doctrine & Covenants)
        """“My son, peace be unto thy soul; thine adversity and thine afflictions shall be but a small moment; and then, if thou endure it well, God shall exalt thee on high.” — Doctrine and Covenants 121:7-8

Difficult trials are temporary, but the spiritual refinement they bring is everlasting. Endure with quiet faith, knowing heaven's peace is already on its way.""",

        // Day 7 (Book of Mormon)
        """“When ye are in the service of your fellow beings ye are only in the service of your God.” — Mosiah 2:17

Every small act of kindness, listening ear, and helpful hand offered to another is sacred service directly rendered to God.""",

        // Day 8 (Holy Bible)
        """“I can do all things through Christ which strengtheneth me.” — Philippians 4:13

No challenge is too grand when you walk hand in hand with the Savior. Draw upon His boundless grace, resilience, and fortitude whenever your own strength feels weak.""",

        // Day 9 (Doctrine & Covenants)
        """“I will go before your face. I will be on your right hand and on your left, and my Spirit shall be in your hearts, and mine angels round about you, to bear you up.” — Doctrine and Covenants 84:88

You are surrounded by heavenly support. As you strive to do good, God sends His Spirit to guide you and His angels to uphold your steps.""",

        // Day 10 (Book of Mormon)
        """“Wherefore, ye must press forward with a steadfastness in Christ, having a perfect brightness of hope, and a love of God and of all men.” — 2 Nephi 31:20

Keep your heart anchored in Christ. Press ahead with hope in your eyes and love in your heart, trusting His promises.""",

        // Day 11 (Holy Bible)
        """“Be strong and of a good courage; be not afraid, neither be thou dismayed: for the Lord thy God is with thee whithersoever thou goest.” — Joshua 1:9

Walk into today with faith rather than fear. You are never left alone in your trials; heaven walks alongside you in every step you take.""",

        // Day 12 (Doctrine & Covenants)
        """“Learn of me, and listen to my words; walk in the meekness of my Spirit, and you shall have peace in me.” — Doctrine and Covenants 19:23

True peace is found in walking meekly with the Savior. Take time today to listen to His gentle promptings and follow His quiet guidance.""",

        // Day 13 (Book of Mormon)
        """“Counsel with the Lord in all thy doings, and he will direct thee for good; yea, when thou liest down at night lie down unto the Lord, that he may watch over you in your sleep; and when thou risest in the morning let thy heart be full of thanks unto God.” — Alma 37:37

Begin and end each day in close communion with Heaven. Bring all your daily plans to God in prayer, and He will guide your decisions for good.""",

        // Day 14 (Holy Bible)
        """“Come unto me, all ye that labour and are heavy laden, and I will give you rest.” — Matthew 11:28

You do not have to carry every burden on your own shoulders. Bring your worries, doubts, and fatigue to Christ, and receive His gentle renewal.""",

        // Day 15 (Doctrine & Covenants)
        """“Wherefore, be not weary in well-doing, for ye are laying the foundation of a great work. And out of small things proceedeth that which is great.” — Doctrine and Covenants 64:33

Do not underestimate the power of daily consistency in doing good. Your small, faithful efforts are building an enduring foundation.""",

        // Day 16 (Book of Mormon)
        """“And if men come unto me I will show unto them their weakness. I give unto men weakness that they may be humble; and my grace is sufficient for all men that humble themselves before me; for if they humble themselves before me, and have faith in me, then will I make weak things become strong unto them.” — Ether 12:27

Weaknesses are not disqualifications; they are invitations to draw near to Christ. Through His grace, your humility is transformed into spiritual power.""",

        // Day 17 (Holy Bible)
        """“The Lord is my shepherd; I shall not want. He maketh me to lie down in green pastures: he leadeth me beside the still waters.” — Psalm 23:1-2

Allow your soul to find rest in God's loving care. He knows your deepest needs before you even ask, and He tenderly restores your spirit.""",

        // Day 18 (Doctrine & Covenants)
        """“Be thou humble; and the Lord thy God shall lead thee by the hand, and give thee answer to thy prayers.” — Doctrine and Covenants 112:10

With a humble and receptive heart, God takes you by the hand and guides you through life's questions and crossroads.""",

        // Day 19 (Book of Mormon)
        """“And he will take upon him their infirmities, that his bowels may be filled with mercy, according to the flesh, that he may know according to the flesh how to succor his people according to their infirmities.” — Alma 7:12

The Savior understands your exact heartaches, pains, and fatigue. You never walk through suffering alone, for He knows intimately how to heal and comfort you.""",

        // Day 20 (Holy Bible)
        """“They that wait upon the Lord shall renew their strength; they shall mount up with wings as eagles; they shall run, and not be weary; and they shall walk, and not faint.” — Isaiah 40:31

Patience in God's promises is never wasted time. He is quietly renewing your spirit behind the scenes for heights you have yet to reach.""",

        // Day 21 (Doctrine & Covenants)
        """“Remember the worth of souls is great in the sight of God.” — Doctrine and Covenants 18:10

You are of infinite, eternal value to Heavenly Father. Treat yourself and every soul you meet today with reverence, patience, and divine love.""",

        // Day 22 (Book of Mormon)
        """“Remember, remember that it is upon the rock of our Redeemer, who is Christ, the Son of God, that ye must build your foundation; that when the devil shall send forth his mighty winds... it shall have no power over you to drag you down.” — Helaman 5:12

Build your daily foundation upon Jesus Christ. When the storms of life blow, an anchor in the Savior keeps your spirit unshakable.""",

        // Day 23 (Holy Bible)
        """“For God hath not given us the spirit of fear; but of power, and of love, and of a sound mind.” — 2 Timothy 1:7

Fear paralyzes, but faith empowers. Anchor your thoughts in divine truth, act with compassion, and let love guide your decisions today.""",

        // Day 24 (Doctrine & Covenants)
        """“Verily I say, men should be anxiously engaged in a good cause, and do many things of their own free will, and bring to pass much righteousness.” — Doctrine and Covenants 58:27

Use your agency proactively. Take initiative to bless someone's day, solve problems, and spread light wherever you find yourself.""",

        // Day 25 (Book of Mormon)
        """“Charity is the pure love of Christ, and it endureth forever; and whoso is found possessed of it at the last day, it shall be well with him.” — Moroni 7:47

Seek the gift of charity in all interactions. Let Christ’s pure, unconditional love shape how you speak, serve, and perceive others.""",

        // Day 26 (Holy Bible)
        """“Ask, and it shall be given you; seek, and ye shall find; knock, and it shall be opened unto you.” — Matthew 7:7

Heaven's doors are never closed to a sincere and humble prayer. Communicate honestly with your Heavenly Father today—He listens attentively.""",

        // Day 27 (Doctrine & Covenants)
        """“Seek ye diligently and teach one another words of wisdom; yea, seek ye out of the best books words of wisdom; seek learning, even by study and also by faith.” — Doctrine and Covenants 88:118

Pair study with faith. Continual learning expands the mind and enriches the soul when aligned with divine truth.""",

        // Day 28 (Book of Mormon)
        """“Faith is not to have a perfect knowledge of things; therefore if ye have faith ye hope for things which are not seen, which are true.” — Alma 32:21

Plant the seed of faith in your heart today. Even when you cannot see the full path ahead, act on divine truth and watch your faith grow.""",

        // Day 29 (Holy Bible)
        """“And we know that all things work together for good to them that love God.” — Romans 8:28

Every setback holds the seed of spiritual refinement. What may seem like an obstacle today is often God's way of preparing you for greater purpose tomorrow.""",

        // Day 30 (Doctrine & Covenants)
        """“Verily I say unto you my friends, fear not, let your hearts be comforted; yea, rejoice evermore, and in everything give thanks.” — Doctrine and Covenants 98:1

Choose gratitude and gladness over worry today. Rejoice in the blessings already given, and comfort will fill your spirit.""",

        // Day 31 (Book of Mormon)
        """“Believe in God; believe that he is, and that he created all things, both in heaven and in earth; believe that he has all wisdom, and all power, both in heaven and in earth; believe that man doth not comprehend all the things which the Lord can comprehend.” — Mosiah 4:9

Trust in God’s higher wisdom. When questions or uncertainties arise, rest your confidence in His infinite knowledge and love.”"""
    )

    fun loadVerses(context: Context): List<String> {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_VERSES, null) ?: return DEFAULT_31_VERSES
        return try {
            val jsonArray = JSONArray(json)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                val v = jsonArray.getString(i).trim()
                if (v.isNotEmpty()) {
                    list.add(v)
                }
            }
            if (list.isEmpty()) DEFAULT_31_VERSES else list
        } catch (e: Exception) {
            DEFAULT_31_VERSES
        }
    }

    fun saveVerses(context: Context, verses: List<String>) {
        val filtered = verses.map { it.trim() }.filter { it.isNotEmpty() }
        val finalVerses = if (filtered.isEmpty()) DEFAULT_31_VERSES else filtered
        val jsonArray = JSONArray()
        finalVerses.forEach { jsonArray.put(it) }
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_VERSES, jsonArray.toString()).apply()
    }

    fun parseVersesFromText(rawText: String): List<String> {
        val blocks = rawText.split(Regex("""(?m)^(?=(?:\d+[\.\):]|\bDay\s+\d+[\.\):]|===|---|###))"""))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (blocks.size > 1) {
            return blocks.map { cleanVerseBlock(it) }.filter { it.isNotBlank() }
        }

        // Fallback: split by double newlines if blocks aren't numbered
        val paragraphBlocks = rawText.split(Regex("""\n\s*\n\s*\n+"""))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (paragraphBlocks.size > 1) {
            return paragraphBlocks.map { cleanVerseBlock(it) }.filter { it.isNotBlank() }
        }

        return if (rawText.isNotBlank()) listOf(cleanVerseBlock(rawText.trim())) else DEFAULT_31_VERSES
    }

    private fun cleanVerseBlock(block: String): String {
        return block.replace(Regex("""^(\d+[\.\):]|\bDay\s+\d+[\.\):]|===|---|###)\s*"""), "").trim()
    }

    fun getVerseForDay(verses: List<String>, timestampMillis: Long): String {
        if (verses.isEmpty()) return DEFAULT_31_VERSES.first()
        val cal = Calendar.getInstance().apply { timeInMillis = timestampMillis }
        val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH) // 1..31
        val index = (dayOfMonth - 1) % verses.size
        return verses[index]
    }

    val EXTENDED_CURATED_VERSES = listOf(
        // Day 1 (Book of Mormon)
        """“I will go and do the things which the Lord hath commanded, for I know that the Lord giveth no commandments unto the children of men, save he shall prepare a way for them that they may accomplish the thing which he commandeth them.” — 1 Nephi 3:7

When God calls you to act or sets a task before you, move forward in faith. He always prepares the path for those who trust Him.""",

        // Day 2 (Holy Bible)
        """“Trust in the Lord with all thine heart; and lean not unto thine own understanding. In all thy ways acknowledge him, and he shall direct thy paths.” — Proverbs 3:5-6

When uncertainty arises, surrender your anxieties to God. His vision sees far beyond our current horizon. Trust His timing and guidance today.""",

        // Day 3 (Doctrine & Covenants)
        """“Look unto me in every thought; doubt not, fear not.” — Doctrine and Covenants 6:36

Center your mindset on the Savior whenever fear or doubt tries to enter. Keeping your gaze fixed on Him brings instant reassurance, clarity, and peace.""",

        // Day 4 (Book of Mormon)
        """“Adam fell that men might be; and men are, that they might have joy.” — 2 Nephi 2:25

Your divine purpose in this mortal life is centered on joy. Through the Savior, you can find authentic peace and happiness even in the midst of trials.""",

        // Day 5 (Holy Bible)
        """“Peace I leave with you, my peace I give unto you: not as the world giveth, give I unto you. Let not your heart be troubled, neither let it be afraid.” — John 14:27

The Savior's peace is not the absence of storms, but His calming presence within the storm. Breathe deeply and rest in His divine love today.""",

        // Day 6 (Doctrine & Covenants)
        """“My son, peace be unto thy soul; thine adversity and thine afflictions shall be but a small moment; and then, if thou endure it well, God shall exalt thee on high.” — Doctrine and Covenants 121:7-8

Difficult trials are temporary, but the spiritual refinement they bring is everlasting. Endure with quiet faith, knowing heaven's peace is already on its way.""",

        // Day 7 (Book of Mormon)
        """“When ye are in the service of your fellow beings ye are only in the service of your God.” — Mosiah 2:17

Every small act of kindness, listening ear, and helpful hand offered to another is sacred service directly rendered to God.""",

        // Day 8 (Holy Bible)
        """“I can do all things through Christ which strengtheneth me.” — Philippians 4:13

No challenge is too grand when you walk hand in hand with the Savior. Draw upon His boundless grace, resilience, and fortitude whenever your own strength feels weak.""",

        // Day 9 (Doctrine & Covenants)
        """“I will go before your face. I will be on your right hand and on your left, and my Spirit shall be in your hearts, and mine angels round about you, to bear you up.” — Doctrine and Covenants 84:88

You are surrounded by heavenly support. As you strive to do good, God sends His Spirit to guide you and His angels to uphold your steps.""",

        // Day 10 (Book of Mormon)
        """“Wherefore, ye must press forward with a steadfastness in Christ, having a perfect brightness of hope, and a love of God and of all men.” — 2 Nephi 31:20

Keep your heart anchored in Christ. Press ahead with hope in your eyes and love in your heart, trusting His promises.""",

        // Day 11 (Holy Bible)
        """“Be strong and of a good courage; be not afraid, neither be thou dismayed: for the Lord thy God is with thee whithersoever thou goest.” — Joshua 1:9

Walk into today with faith rather than fear. You are never left alone in your trials; heaven walks alongside you in every step you take.""",

        // Day 12 (Doctrine & Covenants)
        """“Learn of me, and listen to my words; walk in the meekness of my Spirit, and you shall have peace in me.” — Doctrine and Covenants 19:23

True peace is found in walking meekly with the Savior. Take time today to listen to His gentle promptings and follow His quiet guidance.""",

        // Day 13 (Book of Mormon)
        """“Counsel with the Lord in all thy doings, and he will direct thee for good; yea, when thou liest down at night lie down unto the Lord, that he may watch over you in your sleep; and when thou risest in the morning let thy heart be full of thanks unto God.” — Alma 37:37

Begin and end each day in close communion with Heaven. Bring all your daily plans to God in prayer, and He will guide your decisions for good.""",

        // Day 14 (Holy Bible)
        """“Come unto me, all ye that labour and are heavy laden, and I will give you rest.” — Matthew 11:28

You do not have to carry every burden on your own shoulders. Bring your worries, doubts, and fatigue to Christ, and receive His gentle renewal.""",

        // Day 15 (Doctrine & Covenants)
        """“Wherefore, be not weary in well-doing, for ye are laying the foundation of a great work. And out of small things proceedeth that which is great.” — Doctrine and Covenants 64:33

Do not underestimate the power of daily consistency in doing good. Your small, faithful efforts are building an enduring foundation.""",

        // Day 16 (Book of Mormon)
        """“And if men come unto me I will show unto them their weakness. I give unto men weakness that they may be humble; and my grace is sufficient for all men that humble themselves before me; for if they humble themselves before me, and have faith in me, then will I make weak things become strong unto them.” — Ether 12:27

Weaknesses are not disqualifications; they are invitations to draw near to Christ. Through His grace, your humility is transformed into spiritual power.""",

        // Day 17 (Holy Bible)
        """“The Lord is my shepherd; I shall not want. He maketh me to lie down in green pastures: he leadeth me beside the still waters.” — Psalm 23:1-2

Allow your soul to find rest in God's loving care. He knows your deepest needs before you even ask, and He tenderly restores your spirit.""",

        // Day 18 (Doctrine & Covenants)
        """“Be thou humble; and the Lord thy God shall lead thee by the hand, and give thee answer to thy prayers.” — Doctrine and Covenants 112:10

With a humble and receptive heart, God takes you by the hand and guides you through life's questions and crossroads.""",

        // Day 19 (Book of Mormon)
        """“And he will take upon him their infirmities, that his bowels may be filled with mercy, according to the flesh, that he may know according to the flesh how to succor his people according to their infirmities.” — Alma 7:12

The Savior understands your exact heartaches, pains, and fatigue. You never walk through suffering alone, for He knows intimately how to heal and comfort you.""",

        // Day 20 (Holy Bible)
        """“They that wait upon the Lord shall renew their strength; they shall mount up with wings as eagles; they shall run, and not be weary; and they shall walk, and not faint.” — Isaiah 40:31

Patience in God's promises is never wasted time. He is quietly renewing your spirit behind the scenes for heights you have yet to reach.""",

        // Day 21 (Doctrine & Covenants)
        """“Remember the worth of souls is great in the sight of God.” — Doctrine and Covenants 18:10

You are of infinite, eternal value to Heavenly Father. Treat yourself and every soul you meet today with reverence, patience, and divine love.""",

        // Day 22 (Book of Mormon)
        """“Remember, remember that it is upon the rock of our Redeemer, who is Christ, the Son of God, that ye must build your foundation; that when the devil shall send forth his mighty winds... it shall have no power over you to drag you down.” — Helaman 5:12

Build your daily foundation upon Jesus Christ. When the storms of life blow, an anchor in the Savior keeps your spirit unshakable.""",

        // Day 23 (Holy Bible)
        """“For God hath not given us the spirit of fear; but of power, and of love, and of a sound mind.” — 2 Timothy 1:7

Fear paralyzes, but faith empowers. Anchor your thoughts in divine truth, act with compassion, and let love guide your decisions today.""",

        // Day 24 (Doctrine & Covenants)
        """“Verily I say, men should be anxiously engaged in a good cause, and do many things of their own free will, and bring to pass much righteousness.” — Doctrine and Covenants 58:27

Use your agency proactively. Take initiative to bless someone's day, solve problems, and spread light wherever you find yourself.""",

        // Day 25 (Book of Mormon)
        """“Charity is the pure love of Christ, and it endureth forever; and whoso is found possessed of it at the last day, it shall be well with him.” — Moroni 7:47

Seek the gift of charity in all interactions. Let Christ’s pure, unconditional love shape how you speak, serve, and perceive others.""",

        // Day 26 (Holy Bible)
        """“Ask, and it shall be given you; seek, and ye shall find; knock, and it shall be opened unto you.” — Matthew 7:7

Heaven's doors are never closed to a sincere and humble prayer. Communicate honestly with your Heavenly Father today—He listens attentively.""",

        // Day 27 (Doctrine & Covenants)
        """“Seek ye diligently and teach one another words of wisdom; yea, seek ye out of the best books words of wisdom; seek learning, even by study and also by faith.” — Doctrine and Covenants 88:118

Pair study with faith. Continual learning expands the mind and enriches the soul when aligned with divine truth.""",

        // Day 28 (Book of Mormon)
        """“Faith is not to have a perfect knowledge of things; therefore if ye have faith ye hope for things which are not seen, which are true.” — Alma 32:21

Plant the seed of faith in your heart today. Even when you cannot see the full path ahead, act on divine truth and watch your faith grow.""",

        // Day 29 (Holy Bible)
        """“And we know that all things work together for good to them that love God.” — Romans 8:28

Every setback holds the seed of spiritual refinement. What may seem like an obstacle today is often God's way of preparing you for greater purpose tomorrow.""",

        // Day 30 (Doctrine & Covenants)
        """“Verily I say unto you my friends, fear not, let your hearts be comforted; yea, rejoice evermore, and in everything give thanks.” — Doctrine and Covenants 98:1

Choose gratitude and gladness over worry today. Rejoice in the blessings already given, and comfort will fill your spirit.""",

        // Day 31 (Book of Mormon)
        """“Believe in God; believe that he is, and that he created all things, both in heaven and in earth; believe that he has all wisdom, and all power, both in heaven and in earth; believe that man doth not comprehend all the things which the Lord can comprehend.” — Mosiah 4:9

Trust in God’s higher wisdom. When questions or uncertainties arise, rest your confidence in His infinite knowledge and love.”""",

        // Day 32 (Prophetic Devotional / Elder Holland)
        """“How we respond in any situation has to make things better not worse.” — Elder Jeffrey R. Holland

Isn’t it a marvelous gift from God that we can choose how we handle things in life. We can choose our responses when challenges come. Use your power to choose to bless your life and others today. ❤️""",

        // Day 33 (Book of Mormon)
        """“O remember, my son, and learn wisdom in thy youth; yea, learn in thy youth to keep the commandments of God.” — Alma 37:35

Start building habits of righteousness and integrity right where you are. Every good choice today becomes a lifetime foundation of peace.""",

        // Day 34 (Doctrine & Covenants)
        """“Draw near unto me and I will draw near unto you; seek me diligently and ye shall find me; ask, and ye shall receive; knock, and it shall be opened unto you.” — Doctrine and Covenants 88:63

The Lord is never far away. As you take intentional steps to draw closer to Him in quiet prayer and obedience, He draws near to your heart with peace.""",

        // Day 35 (Holy Bible)
        """“The joy we feel has little to do with the circumstances of our lives and everything to do with the focus of our lives.” — President Russell M. Nelson

Circumstances may shift unpredictably, but when our gaze is centered on the Savior and gratitude, joy remains steadfast and unwavering.""",

        // Day 36 (Book of Mormon)
        """“And my soul hungered; and I kneeled down before my Maker, and I cried unto him in mighty prayer and supplication for mine own soul.” — Enos 1:4

Poured-out, sincere prayers bring soul-deep forgiveness, healing, and personal assurance from Heaven.""",

        // Day 37 (Doctrine & Covenants)
        """“Lift up your heart and rejoice, for the hour of your mission is come; and your tongue shall be loosed, and you shall have great joy.” — Doctrine and Covenants 31:3

Rejoice in the opportunities God places before you today. He will grant you the words and abilities needed to uplift others around you.""",

        // Day 38 (Holy Bible)
        """“Cast thy burden upon the Lord, and he shall sustain thee: he shall never suffer the righteous to be moved.” — Psalm 55:22

You were never meant to carry life's heavy loads unaccompanied. Lay them gently at the Lord's feet and trust in His sustaining grace.""",

        // Day 39 (Book of Mormon)
        """“For I know that he granteth unto men according to their desire, whether it be unto death or unto life; yea, I know that he decreeth unto men, yea, appointeth unto them decrees which are unalterable, according to their wills.” — Alma 29:4

Align your heart's desires with righteousness and kindness, for God loves to fulfill holy desires with abundant blessings.""",

        // Day 40 (Doctrine & Covenants)
        """“All thrones and dominions, principalities and powers, shall be revealed and set forth upon all who have endured valiantly for the gospel of Jesus Christ.” — Doctrine and Covenants 121:29

Endure valiantly with faithful, steady steps. Every challenge endured with patience leads to eternal light and divine peace."""
    )

    fun generateVersesForDateSpan(startDateMillis: Long, endDateMillis: Long, includeDatesInHeader: Boolean = false): List<String> {
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

        val dateSdf = java.text.SimpleDateFormat("EEE, MMM d, yyyy", java.util.Locale.getDefault())
        val generated = mutableListOf<String>()
        val currCal = startCal.clone() as Calendar
        var dayIndex = 0

        while (!currCal.after(endCal) && generated.size < 366) {
            val versePool = EXTENDED_CURATED_VERSES
            val verseBody = versePool[dayIndex % versePool.size]
            val dateStr = dateSdf.format(currCal.time)
            val formatted = if (includeDatesInHeader) {
                "Day ${dayIndex + 1} ($dateStr)\n\n$verseBody"
            } else {
                verseBody
            }
            generated.add(formatted)
            currCal.add(Calendar.DAY_OF_YEAR, 1)
            dayIndex++
        }

        return if (generated.isNotEmpty()) generated else DEFAULT_31_VERSES
    }

    /**
     * Formats devotional explanation text to ensure:
     * - No asterisk ('*') symbols are used (removes markdown bold/italic asterisks, replaces bullet asterisks with bullet symbols •).
     * - Generous paragraph spacing and clean spacing between thoughts, points, and sections.
     */
    fun formatDevotionalExplanation(raw: String): String {
        if (raw.isBlank()) return ""

        val lines = raw.lines()
        val cleanedLines = mutableListOf<String>()

        for (line in lines) {
            var trimmed = line.trim()
            if (trimmed.isEmpty()) {
                if (cleanedLines.isNotEmpty() && cleanedLines.last().isNotEmpty()) {
                    cleanedLines.add("")
                }
                continue
            }

            // Replace bullet asterisk or dash at start of line with a clean bullet point
            if (trimmed.startsWith("* ") || trimmed.startsWith("- ")) {
                trimmed = "• " + trimmed.substring(2).trim()
            }

            // Remove all remaining asterisks (e.g. **bold**, *italic*, etc.)
            trimmed = trimmed.replace("*", "").trim()

            if (trimmed.isNotEmpty()) {
                val isListItem = trimmed.startsWith("•") || trimmed.matches(Regex("""^\d+[.)]\s+.*"""))
                // Add a blank line before list items or headers if preceded by text for clean spacing
                if (isListItem && cleanedLines.isNotEmpty() && cleanedLines.last().isNotEmpty()) {
                    cleanedLines.add("")
                }
                cleanedLines.add(trimmed)
            }
        }

        return cleanedLines.joinToString("\n")
            .replace(Regex("""\n{3,}"""), "\n\n")
            .trim()
    }
}
