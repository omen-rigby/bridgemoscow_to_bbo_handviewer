package com.bitkin.bridgemoscowtobbohandviewer

import android.content.Intent
import android.content.Intent.*
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document


class MainActivity : AppCompatActivity() {
    var bboUrl = ""
    var gambler = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val editText = findViewById<EditText>(R.id.urlEntryField)
        val button = findViewById<Button>(R.id.getLinkButton)
        editText.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable) {}

            override fun beforeTextChanged(
                s: CharSequence, start: Int,
                count: Int, after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence, start: Int,
                before: Int, count: Int
            ) {
                findViewById<Button>(R.id.reportButton).visibility = View.INVISIBLE
                findViewById<TextView>(R.id.errorText).visibility = View.INVISIBLE
                button.setEnabled(isKnownURL(s.toString()))
                if (button.isEnabled()) {
                    button.setOnClickListener { view ->
                        openHandViewer(view)
                    }
                    Thread {
                        try {
                            val doc: Document = Jsoup.connect(editText.text.toString()).get()
                            runOnUiThread {
                                gambler = "gambler" in s.toString()

                                val parser =
                                    if (gambler) GamblerParser(doc) else BridgeMoscowParser(
                                        doc
                                    )
                                try {
                                    bboUrl = parser.parse()
                                    if (gambler) {
                                        setContractFromGambler()
                                    }
                                } catch (ex: Exception) {
//                                    Log.e("Exception", ex.toString())
                                    findViewById<Button>(R.id.reportButton).visibility =
                                        View.VISIBLE
                                    findViewById<TextView>(R.id.errorText).visibility = View.VISIBLE
                                    findViewById<Button>(R.id.getLinkButton).isEnabled = false
                                }
                            }
                        } catch (ex: Exception) {

                        }
                    }.start()


                }

            }
        })
    }

    fun setContractFromGambler() {
        val bidding = bboUrl.split("&a=")[1].split("&")[0]

        val contract = bidding.replace("p", "").replace("x", "").takeLast(2)
        val dealer = bboUrl.split("&d=")[1].split("&")[0]
        val biddingSimplified = bidding.replace("xx", "x")
            .replace("[0-9]".toRegex(), "") // 1 char for 1 bid
            .replace("[px]+$".toRegex(), "") // removing final dbl, rdbl, passes
//        Log.d("simple", biddingSimplified)
        val biddingLength = biddingSimplified.length
        val levelSpinner = findViewById<Spinner>(R.id.levelSpinner)
        val denominationSpinner =
            findViewById<Spinner>(R.id.denominationSpinner)
        val declarerSpinner = findViewById<Spinner>(R.id.declarerSpinner)
        levelSpinner.setSelection(contract[0].toString().toInt() - 1)
        denominationSpinner.setSelection("cdhsn".indexOf(contract[1]))
        val declarerDummyBidding = biddingSimplified.slice(biddingSimplified.length - 1 downTo 0 step 2)
        val swap = (declarerDummyBidding.lastIndexOf(declarerDummyBidding[0])) % 2
        //- 1 for extra seat
        declarerSpinner.setSelection(("NESW".indexOf(dealer) + biddingLength - 1 + 2 * swap) % 4)
    }

    fun openHandViewer(view: View?) {
        if (bboUrl != "") {
            val dealer = bboUrl.split("&d=")[1][0].uppercase()
            val level = findViewById<Spinner>(R.id.levelSpinner).getSelectedItem().toString()
            val denomination = findViewById<Spinner>(R.id.denominationSpinner)
                .getSelectedItem().toString()
            val denominationLatin = mapOf(
                "♣" to "c",
                "♦" to "d",
                "♥" to "h",
                "♠" to "s",
                "NT" to "n"
            )[denomination]
            val declarer = findViewById<Spinner>(R.id.declarerSpinner)
                .getSelectedItem().toString().uppercase(Locale.ROOT)
            val passes = "p".repeat(("NESW".indexOf(declarer) - "NESW".indexOf(dealer) + 4) % 4)
            if (gambler){
                val bidding = bboUrl.split("&a=")[1].split("&")[0]
                val currentContract = bidding.replace("x", "")
                    .replace("p+$".toRegex(), "").takeLast(2)
                val biddingSimplified = bidding
                    .replace("xx", "x")
                    .replace("[0-9]".toRegex(), "")  // 1 char for 1 bid
                    .replace("[px]+$".toRegex(), "") // removing final dbl, rdbl, passes

                val biddingLength = biddingSimplified.length
                val declarerDummyBidding = biddingSimplified.slice(biddingSimplified.length - 1 downTo 0 step 2)
//                Log.d("Compare", declarerDummyBidding)
                val swap = (declarerDummyBidding.lastIndexOf(declarerDummyBidding[0])) % 2
                val currentDeclarer = "NESW"[("NESW".indexOf(dealer) + biddingLength - 1 + 2 * swap) % 4].toString()
                val selectedContract = level + denominationLatin
//                Log.d("Compare", currentContract + " != " + selectedContract)
//                Log.d("Compare", currentDeclarer + " != " + declarer)
                if(!(currentContract == selectedContract && currentDeclarer == declarer)){
                    bboUrl = bboUrl.replace("&a=[^&]+".toRegex(), "&a=$passes$level$denominationLatin"+"ppp")
                }
            } else {
                bboUrl = bboUrl.replace("&a=[^&]+".toRegex(), "")
                bboUrl += "&a=$passes$level$denominationLatin" + "ppp"
            }
        }
        val browserIntent = Intent(ACTION_VIEW, Uri.parse(bboUrl))
        startActivity(browserIntent)
    }

    fun composeEmail(view: View?) {
        val url = findViewById<EditText>(R.id.urlEntryField).text.toString()
        val subject = "BridgeMoscow to BBO Handviewer Bug Report"
        val body = "App has failed to parse data from the following URL:\n$url"
        val address ="igor161b@gmail.com"

        val selectorIntent = Intent(ACTION_SENDTO)
            .setData(Uri.parse("mailto:$address"))
        val emailIntent = Intent(ACTION_SEND).apply {
            putExtra(EXTRA_EMAIL, arrayOf(address))
            putExtra(EXTRA_SUBJECT, subject)
            putExtra(EXTRA_TEXT, body)
            selector = selectorIntent
        }
        startActivity(createChooser(emailIntent, getString(R.string.report)))
    }

    fun isKnownURL(string: String): Boolean {
        try {
            val siteNames = string.lowercase(Locale.ROOT).split("//").takeLast(1)[0]
                .split('.')
            val supportedSites = arrayOf("bridgemoscow", "gambler", "nnbridge", "bridgesport",
                                         "bridgeresults")
            return supportedSites.any {it in siteNames}
        }
        catch (e: Exception) {return false}
    }
}