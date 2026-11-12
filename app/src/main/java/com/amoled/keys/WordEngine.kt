package com.amoled.keys

import android.content.Context
import android.content.SharedPreferences

class WordEngine(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("keys_words", Context.MODE_PRIVATE)

    private val userFreq: MutableMap<String, Int> by lazy {
        val map = mutableMapOf<String, Int>()
        prefs.all.forEach { (k, v) -> if (v is Int) map[k] = v }
        map
    }

    fun learnWord(word: String) {
        if (word.length < 2) return
        val w = word.lowercase().trim()
        val count = (userFreq[w] ?: 0) + 1
        userFreq[w] = count
        prefs.edit().putInt(w, count).apply()
    }

    fun getSuggestions(prefix: String): List<String> {
        if (prefix.length < 2) return emptyList()
        val p = prefix.lowercase()
        val results = mutableListOf<String>()

        // User words first (most frequent matching prefix)
        userFreq.entries
            .filter { it.key.startsWith(p) && it.key != p }
            .sortedByDescending { it.value }
            .take(2)
            .mapTo(results) { it.key }

        // Fill from common words
        COMMON_WORDS.filter { it.startsWith(p) && it !in results }
            .take(3 - results.size)
            .forEach { results.add(it) }

        // If exact word is in dictionary, offer it as first option
        if (p in COMMON_WORDS || p in userFreq) {
            if (p !in results) results.add(0, p)
        }

        return results.take(3)
    }

    fun autocorrect(word: String): String? {
        if (word.length < 3) return null
        val w = word.lowercase()
        if (w in COMMON_WORDS || w in userFreq) return null
        // Find closest word within edit distance 1
        return COMMON_WORDS.firstOrNull { candidate ->
            candidate.length in (w.length - 1)..(w.length + 1) &&
                editDistance(w, candidate) == 1
        }
    }

    private fun editDistance(a: String, b: String): Int {
        val m = a.length; val n = b.length
        val dp = Array(m + 1) { IntArray(n + 1) }
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        for (i in 1..m) for (j in 1..n) {
            dp[i][j] = if (a[i - 1] == b[j - 1]) dp[i - 1][j - 1]
            else 1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
        }
        return dp[m][n]
    }

    companion object {
        val COMMON_WORDS = setOf(
            "the","be","to","of","and","a","in","that","have","it","for","not","on",
            "with","he","as","you","do","at","this","but","his","by","from","they",
            "we","say","her","she","or","an","will","my","one","all","would","there",
            "their","what","so","up","out","if","about","who","get","which","go","me",
            "when","make","can","like","time","no","just","him","know","take","people",
            "into","year","your","good","some","could","them","see","other","than",
            "then","now","look","only","come","its","over","think","also","back",
            "after","use","two","how","our","work","first","well","way","even","new",
            "want","because","any","these","give","day","most","us","great","between",
            "need","large","often","hand","high","place","hold","turn","was","were",
            "been","has","had","did","said","each","which","she","do","how","their",
            "if","will","up","other","about","out","many","then","them","these",
            "so","some","her","would","make","like","him","into","time","has","look",
            "two","more","go","see","number","no","way","could","people","my","than",
            "first","been","call","who","oil","sit","now","find","long","down","day",
            "did","get","come","made","may","part","over","new","sound","take","only",
            "little","work","know","place","years","live","me","back","give","most",
            "very","after","thing","our","just","name","good","sentence","man","think",
            "say","great","where","help","through","much","before","line","right",
            "too","mean","old","any","same","tell","boy","follow","came","want",
            "show","also","around","form","three","small","set","put","end","does",
            "another","well","large","often","hand","high","mother","light","country",
            "point","play","off","never","start","city","earth","eyes","food","four",
            "head","home","kind","last","left","life","might","next","once","open",
            "own","real","seem","side","stop","story","sun","talk","those","while",
            "white","without","yes","above","always","being","best","book","both",
            "case","change","children","close","every","example","face","family",
            "far","feet","feel","few","going","here","hold","important","keep","let",
            "letter","list","miss","mountains","near","never","next","night","often",
            "order","page","paper","plant","problem","ready","real","river","road",
            "rock","room","second","simple","since","song","soon","stand","state",
            "still","stood","study","such","sure","take","tall","these","those",
            "together","took","top","toward","travel","tree","under","until","upon",
            "usually","voice","watch","water","were","whole","wind","word","write",
            "wrote","young","area","book","carry","class","draw","early","east",
            "enough","every","example","father","figure","five","four","girl","given",
            "group","half","heart","heavy","help","horse","however","hundred","idea",
            "include","known","learn","leave","less","map","mark","measure","miss",
            "move","music","north","note","nothing","notice","number","often","once",
            "open","outside","pattern","piece","plan","plane","product","pull","push",
            "question","reach","read","ready","rest","result","ride","rise","rule",
            "run","safe","send","seven","short","since","six","size","slow","south",
            "space","special","spring","step","stone","stood","straight","street",
            "strong","summer","surface","teach","ten","test","thank","their","there",
            "thought","thousand","thus","today","told","tomorrow","tonight","total",
            "touch","toward","town","trade","try","type","understand","unit","usually",
            "value","voice","walk","warm","west","whether","wide","within","wonder",
            "world","write","wrong","always","anyone","anything","anywhere","become",
            "before","believe","between","brother","building","business","cannot",
            "certain","change","coming","complete","consider","continue","control",
            "correct","culture","decide","different","dinner","direct","during",
            "effect","eight","either","entire","everyone","everything","except",
            "experience","explain","express","final","finish","follow","friend",
            "front","future","getting","giving","having","hearing","herself",
            "himself","history","hundred","inside","instead","interest","itself",
            "knowledge","language","leaders","listen","little","looking","making",
            "matter","meaning","meeting","moment","morning","mother","moving",
            "myself","national","nature","needed","neither","nothing","number",
            "object","office","others","outside","paragraph","parent","particular",
            "perhaps","person","picture","places","plants","playing","please",
            "police","political","possible","power","present","pretty","probably",
            "process","program","promise","provide","public","purpose","quite",
            "rather","reason","recent","region","remember","return","running",
            "school","sentence","service","several","should","simple","single",
            "sister","sitting","social","soldier","someone","something","sometimes",
            "source","south","standing","started","station","story","strong",
            "student","subject","summer","support","system","table","talking",
            "teacher","thank","things","thinking","though","through","together",
            "trying","twenty","under","voice","water","whether","without","worker",
            "writing","yellow","yourself",
        )
    }
}
