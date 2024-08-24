package com.bitkin.bridgemoscowtobbohandviewer

import java.util.*
import kotlin.collections.ArrayList
import org.jsoup.nodes.Document

open class GamblerParser(content: Document) {
    open val content = content

    open fun parse(): String {
        val hands = getHands()
//        Log.d("hands", hands.toString())
        val holdings = getHoldings(hands)
//        Log.d("holdings", holdings.toString())
        val bidding = getBidding(content)
//        Log.d("hands", bidding.toString())
        val boardInfo = getBoardInfo(content)
//        Log.d("hands", boardInfo.toString())
        return "https://bridgebase.com/tools/handviewer.html?$holdings&$bidding&$boardInfo"
    }

    open fun getHands(): List<String> {
        return content.select("[data-toclipboard]")
            .map { e -> e.attr("data-toclipboard").toString() }
    }

    fun getHoldings(hands: List<String>): String {
        if (hands.size < 4) {
            throw Exception("Found less than 4 hands")
        }
        val chunks = ArrayList<String>(hands.size)
        for (i in hands.indices) {
            val s = "nwes"[i]
            val p = replaceSuits(hands[i])
            if (p.replace("[shdc]".toRegex(), "").length != 13) {
                // sxxxxhxxxdxxxcxxx
                throw Exception(s.uppercase() + " hand is incorrect")
            }
            chunks.add("$s=$p")
        }
        return chunks.joinToString("&")
    }

    fun replaceSuits(string: String): String {
        var newString = string
        val suits = hashMapOf(
            "&spades;" to "s", "&hearts;" to "h", "&diams;" to "d", "&clubs;" to "c",
            "♠" to "s", "♥" to "h", "♦" to "d", "♣" to "c", "10" to "t"
        )
        for ((k, v) in suits) newString = newString.replace(k, v)
        return newString
    }

    fun getBidding(content: Document): String {
        val summary = content.selectFirst(".center table")
        var bidding = replaceSuits(summary.text().replace("[!← ↑→↓, ]".toRegex(), ""))
        val replaceMap = hashMapOf(
            "&nbsp;" to "", "pass" to "p", "БК" to "n"
        )
        for ((k, v) in replaceMap) bidding = bidding.replace(k, v)
        bidding = bidding.lowercase(Locale.ROOT)
        return "a=$bidding"
    }

    fun getBoardInfo(content: Document): String {
        val numDealerAndVul = content.select("div")
            .first {div -> div.select(">button").isNotEmpty() &&
                    div.selectFirst("button").text().toIntOrNull().toString() != "null"}
        val number = numDealerAndVul.selectFirst("button").text()
        val dealer = numDealerAndVul.text().split(" / ")[0].last().toString()
        val vul = numDealerAndVul.text().split(" / ")[1][0]
            .toString().lowercase(Locale.ROOT).replace("a", "b")
        return "d=$dealer&v=$vul&b=$number"
    }
}

