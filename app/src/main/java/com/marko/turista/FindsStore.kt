package com.marko.turista
import android.content.Context
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import java.io.File
class FindsStore(private val context:Context):SQLiteOpenHelper(context,"turista_finds.db",null,1){
 data class Find(val id:Long,val name:String,val scientific:String,val category:String,val part:String,val source:String,val created:Long,val images:List<String>,val description:String)
 override fun onCreate(db:SQLiteDatabase){db.execSQL("CREATE TABLE finds(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,scientific TEXT NOT NULL,category TEXT NOT NULL,part TEXT NOT NULL,source TEXT NOT NULL,created INTEGER NOT NULL,images TEXT NOT NULL,description TEXT NOT NULL)")}
 override fun onUpgrade(db:SQLiteDatabase,old:Int,new:Int){}
 fun list():List<Find> = readableDatabase.rawQuery("SELECT * FROM finds ORDER BY created DESC,id DESC",null).use{c->buildList{while(c.moveToNext()){val array=JSONArray(c.getString(7));add(Find(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getString(5),c.getLong(6),(0 until array.length()).map{array.getString(it)},c.getString(8)))}}}
 fun image(name:String)=File(File(context.filesDir,"find_photos"),File(name).name)
 fun save(name:String,scientific:String,category:String,part:String,source:String,files:List<File>,description:String):Long{
  require(files.isNotEmpty()){"Nález potrebuje fotografiu."}
  val dir=File(context.filesDir,"find_photos").apply{mkdirs()};val copied=mutableListOf<File>()
  try{
   files.forEach{file->val target=File(dir,java.util.UUID.randomUUID().toString()+".jpg");copied.add(target);file.copyTo(target)}
   return writableDatabase.insertOrThrow("finds",null,ContentValues().apply{put("name",name.take(200));put("scientific",scientific.take(200));put("category",category);put("part",part);put("source",source);put("created",System.currentTimeMillis());put("images",JSONArray(copied.map{it.name}).toString());put("description",description.take(12000))})
  }catch(e:Exception){copied.forEach{it.delete()};throw e}
 }
 fun delete(item:Find){writableDatabase.delete("finds","id=?",arrayOf(item.id.toString()));item.images.forEach{image(it).delete()}}
}
