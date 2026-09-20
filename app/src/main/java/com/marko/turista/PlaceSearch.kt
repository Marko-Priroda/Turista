package com.marko.turista

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.*

/** Photon supports search-as-you-type. Never use public Nominatim for autocomplete. */
class PlaceSearch(private val activity:Activity,private val input:EditText,private val online:()->Boolean,
    private val select:(Double,Double,String)->Unit) {
    data class Result(val name:String,val detail:String,val lat:Double,val lon:Double)
    private val handler=Handler(Looper.getMainLooper())
    private val executor=ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,LinkedBlockingQueue<Runnable>())
    private var work:Future<*>?=null
    private var scheduled:Runnable?=null
    private var generation=0
    private var nextRequestAt=0L
    private var popup:PopupWindow?=null
    private var selected=false
    private val cache=object:LinkedHashMap<String,List<Result>>(40,0.75f,true){override fun removeEldestEntry(e:MutableMap.MutableEntry<String,List<Result>>?)=size>40}
    init {
        input.addTextChangedListener(object:TextWatcher {
            override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int) {}
            override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int) {if(!selected) search()}
            override fun afterTextChanged(s:Editable?) {}
        })
        input.setOnEditorActionListener {_,action,_->if(action==EditorInfo.IME_ACTION_SEARCH || action==EditorInfo.IME_ACTION_DONE) {search();true} else false}
        input.setOnFocusChangeListener {_,focus->if(!focus) cancel() else if(input.text.length>=3) search()}
    }
    private fun dp(n:Int)=(n*activity.resources.displayMetrics.density).toInt()
    fun search() {
        cancel();val token=generation;val query=input.text.toString().trim()
        if(query.length<3) return
        val local=TripStore.get(activity).places().filter {it.name.contains(query,true)}.take(6).map {Result(it.name,"Uložené miesto",it.lat,it.lon)}
        if(!online()) {show(local,"Bez internetu: hľadám iba v uložených miestach.");return}
        val remembered=cache[query.lowercase()]
        if(remembered!=null) {show((local+remembered).distinctBy {"${it.lat},${it.lon}"},"© OpenStreetMap · Photon");return}
        show(local,"Hľadám miesta…")
        scheduled=Runnable {
            if(token!=generation) return@Runnable
            nextRequestAt=android.os.SystemClock.elapsedRealtime()+1200
            work=executor.submit {
                try {
                    val results=fetch(query)
                    handler.post {
                        if(token==generation && input.hasFocus() && !activity.isDestroyed) {
                            cache[query.lowercase()]=results
                            show((local+results).distinctBy {"${it.lat},${it.lon}"},if(local.isEmpty() && results.isEmpty()) "Žiadne výsledky. Skús doplniť obec alebo ulicu." else "© OpenStreetMap · Photon")
                        }
                    }
                }catch(_:Exception){handler.post {if(token==generation && !activity.isDestroyed) show(local,"Vyhľadávanie je nedostupné. Skús to o chvíľu.")}}
            }
        }.also {handler.postDelayed(it,maxOf(800L,nextRequestAt-android.os.SystemClock.elapsedRealtime()))}
    }
    private fun fetch(query:String):List<Result> {
        val url="https://photon.komoot.io/api/?q=${URLEncoder.encode(query,"UTF-8")}&limit=6"
        val connection=URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout=7000;connection.readTimeout=7000
            connection.setRequestProperty("User-Agent","Turista/1.8 (Android; personal hiking app)")
            connection.setRequestProperty("Accept","application/json")
            if(connection.responseCode!=200) error("Search unavailable")
            val json=JSONObject(connection.inputStream.bufferedReader().use {it.readText()}).getJSONArray("features")
            return buildList {
                for(i in 0 until json.length()) {
                    val feature=json.getJSONObject(i);val p=feature.getJSONObject("properties");val c=feature.getJSONObject("geometry").getJSONArray("coordinates")
                    val street=listOf(p.optString("street"),p.optString("housenumber")).filter {it.isNotBlank()}.joinToString(" ")
                    val name=p.optString("name").ifBlank {street.ifBlank {p.optString("city",query)}}
                    val detail=listOf(street,p.optString("city"),p.optString("state"),p.optString("country")).filter {it.isNotBlank() && it!=name}.distinct().joinToString(", ")
                    add(Result(name,detail,c.getDouble(1),c.getDouble(0)))
                }
            }
        }finally{connection.disconnect()}
    }
    private fun show(results:List<Result>,message:String) {
        if(!input.hasFocus() || !input.isAttachedToWindow) return
        popup?.dismiss()
        val items=LinearLayout(activity).apply {orientation=LinearLayout.VERTICAL;setPadding(dp(8),dp(6),dp(8),dp(6))}
        results.take(8).forEach {result->items.addView(TextView(activity).apply {
            text=result.name+if(result.detail.isBlank()) "" else "\n"+result.detail
            textSize=15f;setTextColor(Color.rgb(23,53,44));setPadding(dp(10),dp(10),dp(10),dp(10));minHeight=dp(52)
            maxLines=3;ellipsize=android.text.TextUtils.TruncateAt.END
            setOnClickListener {
                cancel();selected=true;input.setText(result.name);input.setSelection(input.text.length);selected=false;input.clearFocus()
                (activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager).hideSoftInputFromWindow(input.windowToken,0)
                select(result.lat,result.lon,result.name)
            }
        })}
        items.addView(TextView(activity).apply {text=message;textSize=12f;setTextColor(Color.DKGRAY);setPadding(dp(10),dp(10),dp(10),dp(10))})
        val scroll=ScrollView(activity).apply {addView(items)}
        val height=minOf(dp(if(results.isEmpty()) 68 else 280),activity.window.decorView.height/3).coerceAtLeast(dp(48))
        popup=PopupWindow(scroll,input.width.coerceAtLeast(dp(200)),height,false).apply {
            setBackgroundDrawable(GradientDrawable().apply {setColor(Color.WHITE);cornerRadius=dp(16).toFloat()})
            elevation=dp(12).toFloat();isOutsideTouchable=true;inputMethodMode=PopupWindow.INPUT_METHOD_NEEDED
            showAsDropDown(input,0,dp(4))
        }
    }
    fun cancel() {generation++;scheduled?.let {handler.removeCallbacks(it)};scheduled=null;work?.cancel(true);executor.purge();popup?.dismiss();popup=null}
    fun destroy() {cancel();handler.removeCallbacksAndMessages(null);executor.shutdownNow()}
}
