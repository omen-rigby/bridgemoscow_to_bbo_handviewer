package com.bitkin.bridgemoscowtobbohandviewer

import org.jsoup.nodes.Document
import java.util.*

class BridgeMoscowParser(content: Document): GamblerParser(content) {
    override val content = content
    private val table = content.select("table")
        .first {t -> t.select("img[src$=spade.gif]").isNotEmpty()}

    override fun parse(): String {
        val holdings = getHoldings()
        val boardInfo = getBoardInfo()
        return "https://bridgebase.com/tools/handviewer.html?$holdings&$boardInfo"
    }

    fun getHoldings(): String {
        var return_value = "n=s_h_d_c_&w=s_h_d_c_&e=s_h_d_c_&s=s_h_d_c_"
        for (tr in table.select("tr")){
            for (suit in listOf("spade.gif", "heart.gif", "diamond.gif", "club.gif")){
                val chunks = tr.select("td").filter {el -> el.select(">img[src$=\"$suit\"]").isNotEmpty()}
//                val chunks = tr.split(suit).map {it.split("<")[0]
//                    .replace("--", "")}.drop(1)
                for (chunk in chunks){
                    return_value = return_value
                        .replaceFirst(suit[0] + "_", suit[0] + chunk.text().replace("--", ""))
                    if ("_" !in return_value) return return_value
                }
            }
        }
        throw Exception("Cannot parse hands data")
    }

    fun getBoardInfo(): String {
        val infoTable = content.select("table.deal")
        val number = infoTable.select("font[size],b").map{e->e.text()}.first {text -> text.toIntOrNull().toString() != "null"}
//        val number = infoTable.select("td")
//            .filter {td -> td.select(">img").isEmpty()}
//            .map {e -> e.text()}.joinToString().filter { e-> e.isDigit()}
        val dealer = table.select("td")
            .first {td -> "dlr:" in td.text().lowercase(Locale.ROOT)}.text()
            .lowercase(Locale.ROOT).split("dlr: ")[1][0]
        val vul = table.select("td")
            .first {td -> "vul:" in td.text().lowercase(Locale.ROOT)}.text()
                .lowercase(Locale.ROOT).split("vul: ")[1]
            .replace("none", "-").replace("a", "b")
            .replace(" ", "")[0]
        return "d=$dealer&v=$vul&b=$number"
    }
}