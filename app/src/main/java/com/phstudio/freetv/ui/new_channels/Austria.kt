package com.phstudio.freetv.ui.new_channels

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.phstudio.freetv.R
import com.phstudio.freetv.player.HTMLActivity
import com.phstudio.freetv.player.PlayerActivity
import com.phstudio.freetv.ui.ItemAdapter2
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import javax.net.ssl.HttpsURLConnection


class Austria : AppCompatActivity() {

    @Suppress("DEPRECATION")
    private fun internet(): Boolean {
        val connectivityManager =
            this@Austria.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_austria)

        val recyclerView: RecyclerView = findViewById(R.id.rvAustria)
        customAdapter =
            ItemAdapter2(
                this@Austria,
                austriaList,
                object : ItemAdapter2.OnItemClickListener {
                    override fun onItemClick(position: Int) {
                        val (_, _, stringList2) = splitList(austriaList)
                        val url = stringList2[position]
                        if (url.contains("https://www.youtube.com")) {
                            val intent = Intent(this@Austria, HTMLActivity::class.java)
                            intent.putExtra("Name", url)
                            startActivity(intent)
                        } else {
                            val intent = Intent(this@Austria, PlayerActivity::class.java)
                            intent.putExtra("Name", url)
                            startActivity(intent)
                        }

                    }
                },
                object : ItemAdapter2.OnItemLongClickListener {
                    override fun onItemLongClick(position: Int): Boolean {
                        Toast.makeText(
                            this@Austria,
                            getString(R.string.unavailable),
                            Toast.LENGTH_SHORT
                        ).show()

                        return true
                    }
                })

        val layoutManager = LinearLayoutManager(applicationContext)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = customAdapter
        getPublic()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun getPublic() {
        if (internet()) {
            Thread {
                try {
                    val website =
                        URL("https://raw.githubusercontent.com/Free-TV/IPTV/master/playlists/playlist_austria.m3u8")
                    val uc: HttpsURLConnection = website.openConnection() as HttpsURLConnection
                    val br = BufferedReader(InputStreamReader(uc.inputStream))
                    var line: String?
                    val lin2 = StringBuilder()
                    while (br.readLine().also { line = it } != null) {
                        lin2.append(line)
                    }


                    val inputText = lin2.toString().trimIndent()
                    val parts =
                        inputText.split("#EXTINF:-1").filter { it.isNotBlank() }.map { it.trim() }
                    val austriaToAdd = ArrayList<Triple<String, String, String>>()

                    parts.forEachIndexed { index, part ->
                        if (index != 0) {
                            val regex =
                                Regex("""tvg-name="([^"]+)" tvg-logo="([^"]+)" tvg-id="(.*?)" group-title="(.*?)",([^"]+)""")

                            val matchResult = regex.find(part)

                            if (matchResult != null) {
                                val (name, logo, _, _, url) = matchResult.destructured
                                austriaToAdd.add(
                                    Triple(
                                        name,
                                        logo,
                                        url.replaceBefore("http", "")
                                    )
                                )
                            }
                        }
                    }


                    runOnUiThread {
                        austriaList.addAll(austriaToAdd)
                        customAdapter.notifyDataSetChanged()
                    }
                } catch (e: IOException) {
                    println(e.localizedMessage!!.toString())
                }
            }.start()
        } else {
            Toast.makeText(
                this@Austria,
                getString(R.string.no_internet), Toast.LENGTH_SHORT
            ).show()
        }

    }

    private fun splitList(austriaList: ArrayList<Triple<String, String, String>>): Triple<ArrayList<String>, ArrayList<String>, ArrayList<String>> {
        val stringList1 = ArrayList<String>()
        val intList = ArrayList<String>()
        val stringList2 = ArrayList<String>()

        for (pair in austriaList) {
            stringList1.add(pair.first)
            intList.add(pair.second)
            stringList2.add(pair.third)
        }
        return Triple(stringList1, intList, stringList2)
    }

    private val austriaList = ArrayList<Triple<String, String, String>>()

    private lateinit var customAdapter: ItemAdapter2
}
