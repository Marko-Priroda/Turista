package com.marko.turista

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.LocationManager
import android.os.*
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Foreground controls; a separate foreground service owns ongoing sensors and GPS. */
class TripActivity:AppCompatActivity() {
    private val db by lazy {TripStore.get(this)}
    private lateinit var content:LinearLayout
    private var page="record"
    private var placesTab=true
    private var pendingAction:String?=null
    private var dashboard:StepDashboardView?=null
    private val stepPreferences by lazy {getSharedPreferences("turista_steps",MODE_PRIVATE)}
    private var status:TextView?=null
    private var primary:Button?=null
    private var secondary:Button?=null
    private var history:TextView?=null
    private val handler=Handler(Looper.getMainLooper())
    private val refresh=object:Runnable {override fun run(){updateStatus();handler.postDelayed(this,1000)}}
    private fun dp(n:Int)=(n*resources.displayMetrics.density).toInt()
    override fun onCreate(state:Bundle?) {
        super.onCreate(state);page=intent.getStringExtra("page")?:"record"
        if(!TripService.alive) db.recoverInterrupted()
        val root=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(8),dp(16),dp(8));setBackgroundColor(Color.rgb(243,240,232))}
        root.addView(Button(this).apply {text="‹  Späť";setOnClickListener{finish()}})
        root.addView(TextView(this).apply {text=when(page){"steps"->"Krokomer";"saved"->"Uložené";"donate"->"Dobrovoľný príspevok";else->"Nahrávanie trasy"};textSize=26f;setTypeface(null,Typeface.BOLD);setTextColor(Color.rgb(23,53,44));setPadding(0,dp(12),0,dp(16))})
        content=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL;setPadding(0,0,0,dp(24))}
        root.addView(ScrollView(this).apply {isFillViewport=true;addView(content)},LinearLayout.LayoutParams(-1,0,1f))
        if(page=="steps") {
            val tabs=LinearLayout(this)
            listOf("Dnes","História","Úspechy").forEachIndexed { i,title -> tabs.addView(Button(this).apply {text=title;isAllCaps=false;setTextColor(if(i==0)Color.WHITE else OfflineUi.ink);background=OfflineUi.surface(this@TripActivity,if(i==0)OfflineUi.ink else Color.WHITE);setOnClickListener{if(i>0)startActivity(Intent(this@TripActivity,StepHistoryActivity::class.java).putExtra("awards",i==2))}},LinearLayout.LayoutParams(0,dp(56),1f).apply{setMargins(dp(2),dp(4),dp(2),0)}) }
            root.addView(tabs)
        }
        setContentView(root);ScreenInsets.applyTo(root)
        render()
    }
    private fun text(value:String,size:Float=16f):TextView=TextView(this).apply {text=value;textSize=size;setTextColor(Color.rgb(38,50,56));setPadding(0,dp(10),0,dp(10));content.addView(this)}
    private fun button(label:String,action:()->Unit):Button=Button(this).apply {
        text=label;isAllCaps=false;minHeight=dp(52);setTextColor(Color.WHITE)
        background=GradientDrawable().apply {setColor(Color.rgb(23,53,44));cornerRadius=dp(14).toFloat()}
        content.addView(this,LinearLayout.LayoutParams(-1,-2).apply {setMargins(0,dp(7),0,dp(7))});setOnClickListener{action()}
    }
    private fun render() {
        content.removeAllViews();status=null;primary=null;secondary=null;history=null
        when(page) {
            "steps" -> {
                status=text("",15f)
                dashboard=StepDashboardView(this).also {content.addView(it,LinearLayout.LayoutParams(-1,-2))}
                primary=button("Zapnúť krokomer") {request(if(TripService.counting) TripService.STOP_STEPS else TripService.START_STEPS)}
                button("Nastaviť denný cieľ") {
                    val input=EditText(this).apply {inputType=android.text.InputType.TYPE_CLASS_NUMBER;setText(stepPreferences.getInt("goal",6000).toString());selectAll()}
                    val dialog=AlertDialog.Builder(this).setTitle("Tvoj denný cieľ").setView(input).setPositiveButton("Uložiť",null).setNegativeButton("Zrušiť",null).create()
                    dialog.setOnShowListener {dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val goal=input.text.toString().toIntOrNull()
                        if(goal==null || goal !in 100..100000) input.error="Zadaj 100 až 100 000 krokov" else {stepPreferences.edit().putInt("goal",goal).apply();updateStatus();dialog.dismiss()}
                    }};dialog.show()
                }
                button("Keď krokomer nepočíta") {
                    AlertDialog.Builder(this).setTitle("Spoľahlivé meranie").setMessage("Povoľ Fyzickú aktivitu a oznámenia. V nastaveniach aplikácie otvor Batéria a zvoľ Neobmedzené. Na Samsungu pridaj Turistu medzi Nikdy neuspávané aplikácie. Po reštarte telefónu krokomer znovu otvor a zapni. Snímač môže kroky oznámiť s oneskorením. Počítanie pri systémovom Vynútiť zastavenie nie je možné.").setPositiveButton("Nastavenia aplikácie") {_,_->startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:$packageName")))}.setNegativeButton("Zavrieť",null).show()
                }
                history=text("",17f)
                text("Počítame skutočné údaje zo snímača, kým je krokomer zapnutý. Nulový deň znamená žiadne zaznamenané kroky. Denný cieľ si vyberáš sám.",14f)
            }
            "record" -> {
                status=text("",22f)
                text("Záznam GPS pokračuje aj so zhasnutou obrazovkou. Body sa priebežne ukladajú iba v tomto telefóne. Medzera pri strate GPS alebo pauze sa nespojí priamkou.")
                primary=button("Spustiť nahrávanie") {request(if(TripService.recording) TripService.PAUSE_TRACK else TripService.START_TRACK)}
                secondary=button("Ukončiť a uložiť") {
                    val track=db.unfinished()
                    if(track!=null) rename("Názov trasy",track.name) { name ->db.renameTrack(track.id,name);request(TripService.SAVE_TRACK)}
                }
                button("Otvoriť uložené trasy") {startActivity(Intent(this,SavedFoldersActivity::class.java).putExtra("tracks",true))}
            }
            "saved" -> {
                button(if(placesTab) "Miesta • prepnúť na trasy" else "Trasy • prepnúť na miesta") {placesTab=!placesTab;render()}
                if(placesTab) renderPlaces() else renderTracks()
            }
            "donate" -> {
                text("Ak ti Turista pomáha na cestách, môžeš jeho vývoj podporiť dobrovoľným príspevkom. Funkcie aplikácie nie sú podmienené príspevkom.")
                if(DonationConfig.URL.startsWith("https://")) button("Podporiť vývoj") {
                    try {startActivity(Intent(Intent.ACTION_VIEW,android.net.Uri.parse(DonationConfig.URL)))}catch(_:Exception){Toast.makeText(this,"V telefóne sa nenašiel prehliadač.",Toast.LENGTH_LONG).show()}
                }
                if(DonationConfig.IBAN.isNotBlank()) {
                    text(DonationConfig.RECIPIENT+"\n"+DonationConfig.IBAN)
                    button("Skopírovať IBAN") {
                        (getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(android.content.ClipData.newPlainText("IBAN",DonationConfig.IBAN))
                        Toast.makeText(this,"IBAN skopírovaný",Toast.LENGTH_SHORT).show()
                    }
                }
                if(DonationConfig.URL.isBlank() && DonationConfig.IBAN.isBlank()) text("Možnosť prispieť pripravujeme. Platobné údaje zatiaľ nie sú nastavené.")
            }
        }
        updateStatus()
    }
    private fun renderPlaces() {
        val places=db.places()
        if(places.isEmpty()) text("Zatiaľ nemáš uložené miesto. Na mape vyber bod a stlač Uložiť miesto.")
        places.forEach {place -> button("★ ${place.name}") {
            AlertDialog.Builder(this).setTitle(place.name).setItems(arrayOf("Ukázať na mape","Premenovať","Vymazať")) {_,i->when(i){
                0->openMap("saved_place",place.id)
                1->rename("Názov miesta",place.name){db.renamePlace(place.id,it);render()}
                2->confirmDelete(place.name){db.deletePlace(place.id);render()}
            }}.show()
        } }
    }
    private fun renderTracks() {
        button("Skryť zobrazenú trasu na mape") {openMap("saved_track",-1L)}
        val tracks=db.tracks()
        if(tracks.isEmpty()) text("Zatiaľ nemáš nahranú trasu. Spusti nahrávanie cez menu na mape.")
        tracks.forEach {track->button("${track.name}\n${TuristaSettings(this).formatDistance(track.distance)} · ${track.count} bodov"+if(track.status!="saved") " · rozpracovaná" else "") {
            AlertDialog.Builder(this).setTitle(track.name).setItems(arrayOf("Ukázať na mape","Premenovať","Vymazať")){_,i->when(i){
                0->if(track.count>0) openMap("saved_track",track.id) else Toast.makeText(this,"Trasa zatiaľ nemá GPS body.",Toast.LENGTH_SHORT).show()
                1->rename("Názov trasy",track.name){db.renameTrack(track.id,it);render()}
                2->if(track.status!="saved") Toast.makeText(this,"Najprv ukonči nahrávanie a ulož trasu.",Toast.LENGTH_LONG).show() else confirmDelete(track.name){db.deleteTrack(track.id);render()}
            }}.show()
        } }
    }
    private fun openMap(key:String,id:Long) {
        startActivity(Intent(this,MapActivity::class.java).putExtra(key,id).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP));finish()
    }
    private fun rename(title:String,current:String,save:(String)->Unit) {
        val input=EditText(this).apply {setSingleLine(true);setText(current);selectAll();filters=arrayOf(android.text.InputFilter.LengthFilter(120))}
        val dialog=AlertDialog.Builder(this).setTitle(title).setView(input).setPositiveButton("Uložiť",null).setNegativeButton("Zrušiť",null).create()
        dialog.setOnShowListener {dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val value=input.text.toString().trim();if(value.isBlank()) input.error="Zadaj názov" else {save(value);dialog.dismiss()}
        }};dialog.show()
    }
    private fun confirmDelete(name:String,delete:()->Unit) {AlertDialog.Builder(this).setTitle("Vymazať $name?").setMessage("Táto položka sa odstráni z telefónu.").setPositiveButton("Vymazať"){_,_->delete()}.setNegativeButton("Zrušiť",null).show()}
    private fun request(action:String) {
        val start=action==TripService.START_TRACK || action==TripService.START_STEPS
        val needed=mutableListOf<String>()
        if(action==TripService.START_TRACK && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED) {
            needed+=Manifest.permission.ACCESS_FINE_LOCATION;needed+=Manifest.permission.ACCESS_COARSE_LOCATION
        }
        if(action==TripService.START_STEPS && checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION)!=PackageManager.PERMISSION_GRANTED) needed+=Manifest.permission.ACTIVITY_RECOGNITION
        if(start && Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED && !getPreferences(0).getBoolean("notification_asked",false)) {
            needed+=Manifest.permission.POST_NOTIFICATIONS;getPreferences(0).edit().putBoolean("notification_asked",true).apply()
        }
        if(needed.isNotEmpty()) {pendingAction=action;requestPermissions(needed.toTypedArray(),801);return}
        if(action==TripService.START_TRACK && !(getSystemService(LOCATION_SERVICE) as LocationManager).isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            AlertDialog.Builder(this).setMessage("Pre nahrávanie zapni polohu GPS.").setPositiveButton("Nastavenia"){_,_->startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))}.setNegativeButton("Zavrieť",null).show();return
        }
        try {
            val intent=Intent(this,TripService::class.java).setAction(action)
            if(start) startForegroundService(intent) else startService(intent)
            handler.postDelayed({updateStatus()},350)
        }catch(_:Exception){Toast.makeText(this,"Funkciu sa nepodarilo spustiť. Skontroluj povolenia aplikácie.",Toast.LENGTH_LONG).show()}
    }
    override fun onRequestPermissionsResult(code:Int,permissions:Array<out String>,results:IntArray) {
        super.onRequestPermissionsResult(code,permissions,results)
        if(code!=801) return
        val action=pendingAction?:return;pendingAction=null
        val needed=if(action==TripService.START_TRACK) Manifest.permission.ACCESS_FINE_LOCATION else Manifest.permission.ACTIVITY_RECOGNITION
        if(checkSelfPermission(needed)==PackageManager.PERMISSION_GRANTED) request(action)
        else Toast.makeText(this,if(action==TripService.START_TRACK) "Nahrávanie potrebuje povolenú presnú polohu." else "Krokomer potrebuje povolenie Fyzická aktivita.",Toast.LENGTH_LONG).show()
    }
    private fun updateStatus() {
        when(page) {
            "steps" -> {
                val manager=getSystemService(SENSOR_SERVICE) as SensorManager
                val supported=manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)!=null || manager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)!=null
                status?.text=if(!supported) "Snímač krokov nie je dostupný" else if(TripService.counting) "Počítanie je zapnuté" + if(TripService.lastStepEvent==0L) " · čakám na snímač" else "\nPosledné hlásenie snímača: "+java.text.SimpleDateFormat("HH:mm:ss",java.util.Locale.getDefault()).format(java.util.Date(TripService.lastStepEvent)) else "Počítanie je vypnuté"
                primary?.isEnabled=supported;primary?.text=if(TripService.counting) "Vypnúť krokomer" else "Zapnúť krokomer"
                val week=(6L downTo 0L).map {db.steps(LocalDate.now().minusDays(it).toString())}
                dashboard?.update(week,stepPreferences.getInt("goal",6000))
                val all=db.stepDays();val total=StepStats.total(all)
                history?.text="OD ZAČIATKU MERANIA\n${all.keys.minOrNull()?:LocalDate.now()} → dnes\n\n$total krokov celkom · ${all.values.count{it>0}} aktívnych dní\nNajdlhšia séria: ${StepStats.longestStreak(all)} dní"
            }
            "record" -> {
                val track=db.unfinished()
                status?.text=if(track==null) "Pripravené na nahrávanie" else "${if(TripService.recording) "Nahrávanie beží" else "Nahrávanie je pozastavené"}\n${TuristaSettings(this).formatDistance(track.distance)} · ${track.seconds/60} min GPS záznamu · ${track.count} bodov"+if(track.count==0) "\nČakám na presnú polohu" else ""
                primary?.text=if(TripService.recording) "Pozastaviť" else if(track!=null) "Pokračovať v nahrávaní" else "Spustiť nahrávanie"
                secondary?.isEnabled=track!=null
            }
        }
    }
    override fun onResume() {super.onResume();if(page=="steps" && !TripService.counting && getSharedPreferences("step_service",MODE_PRIVATE).getBoolean("enabled",false) && checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION)==PackageManager.PERMISSION_GRANTED) request(TripService.START_STEPS);if(page=="saved") {if(intent.getBooleanExtra("tracks",false)){placesTab=false;intent.removeExtra("tracks")};render()};handler.post(refresh)}
    override fun onPause() {handler.removeCallbacksAndMessages(null);super.onPause()}
}

object DonationConfig {
    // Set by the app owner before publishing; never prefill another person's account.
    const val URL=""
    const val IBAN=""
    const val RECIPIENT=""
}
