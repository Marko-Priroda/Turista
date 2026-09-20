package com.marko.turista

import android.content.Context
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.time.LocalDate

/** Local-only favourites and incremental recordings; no account or upload. */
class TripStore private constructor(context: Context): SQLiteOpenHelper(context.applicationContext,"turista_trips.db",null,3) {
    data class Place(val id:Long,val name:String,val lat:Double,val lon:Double,val folder:Long?=null)
    data class Track(val id:Long,val name:String,val created:Long,val status:String,val distance:Double,val seconds:Long,val count:Int,val folder:Long?=null)
    data class Sample(val lat:Double,val lon:Double,val time:Long,val segment:Int,val accuracy:Float)
    override fun onCreate(db:SQLiteDatabase) {
        db.execSQL("CREATE TABLE folders(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL)")
        db.execSQL("CREATE TABLE step_counter(id INTEGER PRIMARY KEY,raw INTEGER NOT NULL,boot INTEGER NOT NULL,day TEXT NOT NULL)")
        db.execSQL("CREATE TABLE places(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,lat REAL NOT NULL,lon REAL NOT NULL,folder_id INTEGER REFERENCES folders(id) ON DELETE SET NULL)")
        db.execSQL("CREATE TABLE tracks(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,created INTEGER NOT NULL,status TEXT NOT NULL,distance REAL NOT NULL DEFAULT 0,seconds INTEGER NOT NULL DEFAULT 0,folder_id INTEGER REFERENCES folders(id) ON DELETE SET NULL)")
        db.execSQL("CREATE TABLE points(id INTEGER PRIMARY KEY AUTOINCREMENT,track_id INTEGER NOT NULL REFERENCES tracks(id) ON DELETE CASCADE,lat REAL NOT NULL,lon REAL NOT NULL,time INTEGER NOT NULL,segment INTEGER NOT NULL,accuracy REAL NOT NULL)")
        db.execSQL("CREATE INDEX track_points ON points(track_id,id)")
        db.execSQL("CREATE TABLE steps(day TEXT PRIMARY KEY,count INTEGER NOT NULL)")
    }
    override fun onConfigure(db:SQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }
    override fun onUpgrade(db:SQLiteDatabase,old:Int,new:Int) {
        if(old<2) db.execSQL("CREATE TABLE IF NOT EXISTS step_counter(id INTEGER PRIMARY KEY,raw INTEGER NOT NULL,boot INTEGER NOT NULL,day TEXT NOT NULL)")
        if(old<3) {
            db.execSQL("CREATE TABLE folders(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL)")
            db.execSQL("ALTER TABLE places ADD COLUMN folder_id INTEGER REFERENCES folders(id) ON DELETE SET NULL")
            db.execSQL("ALTER TABLE tracks ADD COLUMN folder_id INTEGER REFERENCES folders(id) ON DELETE SET NULL")
        }
    }
    data class Folder(val id:Long,val name:String)
    fun folders():List<Folder> = readableDatabase.rawQuery("SELECT id,name FROM folders ORDER BY name COLLATE NOCASE",null).use{c->buildList{while(c.moveToNext())add(Folder(c.getLong(0),c.getString(1)))}}
    fun createFolder(name:String):Long=writableDatabase.insertOrThrow("folders",null,ContentValues().apply{put("name",name.trim().take(120))})
    fun renameFolder(id:Long,name:String){writableDatabase.update("folders",ContentValues().apply{put("name",name.trim().take(120))},"id=?",arrayOf(id.toString()))}
    fun deleteFolder(id:Long){writableDatabase.delete("folders","id=?",arrayOf(id.toString()))}
    fun moveToFolder(track:Boolean,id:Long,folder:Long?){writableDatabase.update(if(track) "tracks" else "places",ContentValues().apply{if(folder==null)putNull("folder_id")else put("folder_id",folder)},"id=?",arrayOf(id.toString()))}
    fun savePlace(name:String,lat:Double,lon:Double):Long = writableDatabase.insertOrThrow("places",null,ContentValues().apply {put("name",name.take(120));put("lat",lat);put("lon",lon)})
    fun places():List<Place> = readableDatabase.rawQuery("SELECT * FROM places ORDER BY id DESC",null).use { c -> buildList {while(c.moveToNext()) add(Place(c.getLong(0),c.getString(1),c.getDouble(2),c.getDouble(3),if(c.isNull(4)) null else c.getLong(4))) } }
    fun deletePlace(id:Long) { writableDatabase.delete("places","id=?",arrayOf(id.toString())) }
    fun renamePlace(id:Long,name:String) { writableDatabase.update("places",ContentValues().apply {put("name",name.take(120))},"id=?",arrayOf(id.toString())) }
    fun createTrack(name:String):Long = writableDatabase.insertOrThrow("tracks",null,ContentValues().apply {put("name",name);put("created",System.currentTimeMillis());put("status","paused")})
    fun tracks():List<Track> = readableDatabase.rawQuery("SELECT t.id,t.name,t.created,t.status,t.distance,t.seconds, (SELECT count(*) FROM points p WHERE p.track_id=t.id)  ,t.folder_id FROM tracks t ORDER BY t.created DESC",null).use { c -> buildList { while(c.moveToNext()) add(Track(c.getLong(0),c.getString(1),c.getLong(2),c.getString(3),c.getDouble(4),c.getLong(5),c.getInt(6),if(c.isNull(7)) null else c.getLong(7))) } }
    fun track(id:Long):Track? = tracks().firstOrNull {it.id==id}
    fun unfinished():Track? = tracks().firstOrNull {it.status!="saved"}
    fun status(id:Long,status:String) { writableDatabase.update("tracks",ContentValues().apply {put("status",status)},"id=?",arrayOf(id.toString())) }
    fun recoverInterrupted() { writableDatabase.execSQL("UPDATE tracks SET status='paused' WHERE status='recording'") }
    fun renameTrack(id:Long,name:String) { writableDatabase.update("tracks",ContentValues().apply {put("name",name.take(120))},"id=?",arrayOf(id.toString())) }
    fun deleteTrack(id:Long) { writableDatabase.delete("tracks","id=?",arrayOf(id.toString())) }
    fun samples(id:Long):List<Sample> = readableDatabase.rawQuery("SELECT lat,lon,time,segment,accuracy FROM points WHERE track_id=? ORDER BY id",arrayOf(id.toString())).use { c -> buildList {while(c.moveToNext()) add(Sample(c.getDouble(0),c.getDouble(1),c.getLong(2),c.getInt(3),c.getFloat(4))) } }
    fun nextSegment(id:Long):Int = readableDatabase.rawQuery("SELECT COALESCE(MAX(segment),-1)+1 FROM points WHERE track_id=?",arrayOf(id.toString())).use {it.moveToFirst();it.getInt(0)}
    fun append(id:Long,point:Sample,distance:Double,seconds:Long) {
        val db=writableDatabase;db.beginTransaction()
        try {
            db.insertOrThrow("points",null,ContentValues().apply {put("track_id",id);put("lat",point.lat);put("lon",point.lon);put("time",point.time);put("segment",point.segment);put("accuracy",point.accuracy)})
            db.execSQL("UPDATE tracks SET distance=distance+?,seconds=seconds+? WHERE id=?",arrayOf<Any>(distance,seconds,id))
            db.setTransactionSuccessful()
        } finally {db.endTransaction()}
    }
    fun steps(day:String=LocalDate.now().toString()):Long = readableDatabase.rawQuery("SELECT count FROM steps WHERE day=?",arrayOf(day)).use {if(it.moveToFirst()) it.getLong(0) else 0L}
    fun addSteps(day:String,count:Long) {
        if(count<=0) return
        val db=writableDatabase;db.beginTransaction()
        try { db.execSQL("INSERT OR IGNORE INTO steps(day,count) VALUES(?,0)",arrayOf(day));db.execSQL("UPDATE steps SET count=count+? WHERE day=?",arrayOf<Any>(count,day));db.setTransactionSuccessful() } finally {db.endTransaction()}
    }
    fun stepDays():Map<String,Long> = readableDatabase.rawQuery("SELECT day,count FROM steps ORDER BY day",null).use { c -> buildMap {while(c.moveToNext()) put(c.getString(0),c.getLong(1))} }
    fun resetCounter() {writableDatabase.delete("step_counter",null,null)}
    fun acceptCounter(raw:Long,boot:Int,day:String) {
        val db=writableDatabase;db.beginTransaction()
        try {
            val delta=db.rawQuery("SELECT raw,boot,day FROM step_counter WHERE id=1",null).use {c ->
                if(c.moveToFirst()) StepCounterMath.delta(c.getLong(0),raw,c.getInt(1)==boot,c.getString(2)==day) else 0L
            }
            db.execSQL("INSERT OR REPLACE INTO step_counter(id,raw,boot,day) VALUES(1,?,?,?)",arrayOf<Any>(raw,boot,day))
            db.execSQL("INSERT OR IGNORE INTO steps(day,count) VALUES(?,0)",arrayOf(day))
            if(delta>0) db.execSQL("UPDATE steps SET count=count+? WHERE day=?",arrayOf<Any>(delta,day))
            db.setTransactionSuccessful()
        } finally {db.endTransaction()}
    }
    companion object {
        @Volatile private var instance:TripStore?=null
        fun get(context:Context):TripStore=instance?:synchronized(this) {instance?:TripStore(context).also {instance=it}}
    }
}
