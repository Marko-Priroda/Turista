package com.marko.turista

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.*
import android.location.*
import android.os.*
import android.widget.Toast
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class TripService:Service(),LocationListener,SensorEventListener {
    private val db by lazy {TripStore.get(this)}
    private val locations by lazy {getSystemService(LOCATION_SERVICE) as LocationManager}
    private val sensors by lazy {getSystemService(SENSOR_SERVICE) as SensorManager}
    private val stepState by lazy {getSharedPreferences("step_service",MODE_PRIVATE)}
    private var detector=false
    private var segment=0
    private var previous:RecordingFilter.Fix?=null
    private var lastNotification=0L
    private var isForeground=false
    override fun onBind(intent:Intent?) = null
    override fun onCreate() {
        super.onCreate();alive=true;db.recoverInterrupted()
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(NotificationChannel("turista_trip","Nahrávanie a krokomer",NotificationManager.IMPORTANCE_LOW))
    }
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
        try {
            when(intent?.action ?: if(stepState.getBoolean("enabled",false)) START_STEPS else "") {
                START_TRACK -> {
                    require(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED) {"Povoľ presnú polohu pre nahrávanie."}
                    require(locations.isProviderEnabled(LocationManager.GPS_PROVIDER)) {"Zapni polohu GPS v telefóne."}
                    if(trackId==0L) {
                        trackId=db.unfinished()?.id ?: db.createTrack("Trasa "+java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("d.M.yyyy HH:mm")))
                    }
                    if(!recording) {
                        recording=true;foreground();segment=db.nextSegment(trackId);previous=null
                        locations.requestLocationUpdates(LocationManager.GPS_PROVIDER,3000L,0f,this)
                        db.status(trackId,"recording")
                    }
                }
                PAUSE_TRACK -> pauseTrack()
                SAVE_TRACK -> {pauseTrack();val id=if(trackId!=0L) trackId else db.unfinished()?.id
                    if(id!=null) db.status(id,"saved");trackId=0L}
                START_STEPS -> {
                    require(checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION)==PackageManager.PERMISSION_GRANTED) {"Povoľ fyzickú aktivitu pre krokomer."}
                    if(!counting) {
                        if(intent!=null && !stepState.getBoolean("enabled",false)) db.resetCounter()
                        val sensor=sensors.getDefaultSensor(Sensor.TYPE_STEP_COUNTER,true)
                            ?:sensors.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
                            ?:sensors.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR,true)
                            ?:sensors.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
                        require(sensor!=null) {"Telefón nemá dostupný snímač krokov."}
                        detector=sensor.type==Sensor.TYPE_STEP_DETECTOR;counting=true;foreground()
                        if(!sensors.registerListener(this,sensor,SensorManager.SENSOR_DELAY_NORMAL,0)) {counting=false;error("Snímač krokov sa nepodarilo zapnúť.")}
                        stepState.edit().putBoolean("enabled",true).apply()
                    }
                }
                STOP_STEPS -> {counting=false;sensors.unregisterListener(this);db.resetCounter();stepState.edit().putBoolean("enabled",false).apply()}
                STOP_ALL -> {pauseTrack();if(trackId!=0L) db.status(trackId,"saved");trackId=0L;counting=false;sensors.unregisterListener(this);db.resetCounter();stepState.edit().putBoolean("enabled",false).apply()}
            }
            if(recording || counting) foreground() else {stopForeground(STOP_FOREGROUND_REMOVE);isForeground=false;stopSelf()}
        } catch(e:Exception) {
            pauseTrack();counting=false;sensors.unregisterListener(this)
            // A failed startForegroundService must still settle its foreground contract when possible.
            stopForeground(STOP_FOREGROUND_REMOVE);stopSelf()
            Toast.makeText(this,e.message?:"Funkciu sa nepodarilo spustiť.",Toast.LENGTH_LONG).show()
        }
        return if(counting) START_STICKY else START_NOT_STICKY
    }
    private fun pauseTrack() {
        runCatching { locations.removeUpdates(this) };recording=false;previous=null
        if(trackId!=0L) db.status(trackId,"paused")
    }
    private fun foreground() {
        val open=PendingIntent.getActivity(this,10,Intent(this,TripActivity::class.java).putExtra("page",if(recording) "record" else "steps"),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stop=PendingIntent.getService(this,11,Intent(this,TripService::class.java).setAction(STOP_ALL),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val text=buildList {if(recording) add("Nahrávam trasu");if(counting) add("Dnes ${db.steps()} krokov")}.joinToString(" · ")
        val notification=Notification.Builder(this,"turista_trip").setSmallIcon(R.drawable.ic_record).setContentTitle("Turista")
            .setContentText(text).setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true)
            .addAction(Notification.Action.Builder(null,"Uložiť a zastaviť",stop).build()).build()
        var type=if(recording) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0
        if(counting && Build.VERSION.SDK_INT>=34) type=type or ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
        if(!isForeground) {startForeground(701,notification,type);isForeground=true} else startForeground(701,notification,type)
        lastNotification=SystemClock.elapsedRealtime()
    }
    override fun onLocationChanged(location:Location) {
        if(!recording || !location.hasAccuracy()) return
        val fix=RecordingFilter.Fix(location.latitude,location.longitude,location.elapsedRealtimeNanos/1e9,location.accuracy.toDouble())
        val decision=RecordingFilter.evaluate(previous,fix,(SystemClock.elapsedRealtimeNanos()-location.elapsedRealtimeNanos)/1e9)
        if(!decision.accept) return
        if(decision.split && previous!=null) segment++
        try {
            db.append(trackId,TripStore.Sample(fix.lat,fix.lon,location.time,segment,location.accuracy),decision.distance,decision.seconds)
            previous=fix
        } catch(_:Exception) {
            pauseTrack();Toast.makeText(this,"Zápis trasy zlyhal. Skontroluj voľné miesto.",Toast.LENGTH_LONG).show()
            if(!counting) {stopForeground(STOP_FOREGROUND_REMOVE);stopSelf()}
        }
    }
    override fun onSensorChanged(event:SensorEvent) {
        if(!counting) return
        val eventMillis=System.currentTimeMillis()-(SystemClock.elapsedRealtimeNanos()-event.timestamp)/1000000
        val date=java.time.Instant.ofEpochMilli(eventMillis).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString()
        try {
            if(detector) db.addSteps(date,1) else db.acceptCounter(event.values[0].toLong(),android.provider.Settings.Global.getInt(contentResolver,android.provider.Settings.Global.BOOT_COUNT,0),date)
            lastStepEvent=System.currentTimeMillis()
            if(SystemClock.elapsedRealtime()-lastNotification>15000) foreground()}
        catch(_:Exception) {counting=false;sensors.unregisterListener(this);if(!recording){stopForeground(STOP_FOREGROUND_REMOVE);stopSelf()}}
    }
    override fun onAccuracyChanged(sensor:Sensor?,accuracy:Int) {}
    override fun onProviderDisabled(provider:String) {if(provider==LocationManager.GPS_PROVIDER && recording) {previous=null;segment++}}
    override fun onProviderEnabled(provider:String) {}
    @Deprecated("Android compatibility") override fun onStatusChanged(provider:String?,status:Int,extras:Bundle?) {}
    override fun onDestroy() {
        runCatching { locations.removeUpdates(this) };sensors.unregisterListener(this)
        if(trackId!=0L) db.status(trackId,"paused")
        alive=false;recording=false;counting=false;trackId=0L
        super.onDestroy()
    }
    companion object {
        const val START_TRACK="start_track";const val PAUSE_TRACK="pause_track";const val SAVE_TRACK="save_track"
        const val START_STEPS="start_steps";const val STOP_STEPS="stop_steps";const val STOP_ALL="stop_all"
        @Volatile var lastStepEvent=0L;private set
        @Volatile var alive=false;private set
        @Volatile var recording=false;private set
        @Volatile var counting=false;private set
        @Volatile var trackId=0L;private set
    }
}
