package com.marko.turista
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
class SavedFoldersActivity:NatureScreen(){
 private val db by lazy{TripStore.get(this)}
 // -1: all items, 0: unfiled, positive: named folder.
 private var selected=-1L
 private var tracks=false
 override fun onCreate(state:Bundle?){super.onCreate(state);selected=state?.getLong("folder",-1)?:-1;tracks=state?.getBoolean("tracks")?:intent.getBooleanExtra("tracks",false);setup("Uložené");render()}
 override fun onSaveInstanceState(out:Bundle){out.putLong("folder",selected);out.putBoolean("tracks",tracks);super.onSaveInstanceState(out)}
 private fun edit(title:String,current:String="",save:(String)->Unit){
  val input=EditText(this).apply{setSingleLine();setText(current);selectAll();filters=arrayOf(android.text.InputFilter.LengthFilter(120))}
  val dialog=AlertDialog.Builder(this).setTitle(title).setView(input).setPositiveButton("Uložiť",null).setNegativeButton("Zrušiť",null).create()
  dialog.setOnShowListener{dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener{val name=input.text.toString().trim();if(name.isBlank())input.error="Zadaj názov" else {save(name);dialog.dismiss();render()}}};dialog.show()
 }
 private fun chooseFolder(id:Long){
  val folders=db.folders();val labels=arrayOf("Bez priečinka")+folders.map{it.name}
  AlertDialog.Builder(this).setTitle("Presunúť do priečinka").setItems(labels){_,which->db.moveToFolder(tracks,id,if(which==0)null else folders[which-1].id);render()}.setNegativeButton("Zrušiť",null).show()
 }
 private fun openMap(id:Long){startActivity(Intent(this,MapActivity::class.java).putExtra(if(tracks)"saved_track" else "saved_place",id).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP));finish()}
 private fun render(){
  body.removeAllViews();val folders=db.folders();if(selected>0 && folders.none{it.id==selected})selected=0
  val places=db.places();val recordings=db.tracks()
  fun count(id:Long)=places.count{(it.folder?:0)==id}+recordings.count{(it.folder?:0)==id}
  action("+ Nový priečinok"){edit("Nový priečinok"){selected=db.createFolder(it)}}
  action("Všetky uložené · ${places.size+recordings.size}"){selected=-1;render()}
  action("Bez priečinka · ${count(0)}"){selected=0;render()}
  folders.forEach{f->OfflineUi.row(body,f.name,"${count(f.id)} položiek"+if(selected==f.id)" · otvorený" else ""){selected=f.id;render()}}
  val folder=folders.firstOrNull{it.id==selected}
  if(folder!=null){
   label(folder.name,23f)
   action("Upraviť priečinok"){AlertDialog.Builder(this).setTitle(folder.name).setItems(arrayOf("Premenovať","Odstrániť priečinok")){_,i->if(i==0)edit("Premenovať priečinok",folder.name){db.renameFolder(folder.id,it)}else AlertDialog.Builder(this).setTitle("Odstrániť priečinok?").setMessage("Miesta a trasy zostanú uložené v Bez priečinka.").setPositiveButton("Odstrániť"){_,_->db.deleteFolder(folder.id);selected=0;render()}.setNegativeButton("Zrušiť",null).show()}.show()}
  }else label(if(selected==0L)"Bez priečinka" else "Všetky položky",23f)
  action(if(tracks)"Trasy • prepnúť na miesta" else "Miesta • prepnúť na trasy"){tracks=!tracks;render()}
  if(tracks){
   action("Skryť zobrazenú trasu na mape"){openMap(-1)}
   val items=recordings.filter{selected==-1L || (it.folder?:0)==selected}
   if(items.isEmpty())label("V tomto výbere zatiaľ nie je žiadna trasa.")
   items.forEach{t->OfflineUi.row(body,t.name,TuristaSettings(this).formatDistance(t.distance)+if(t.status!="saved")" · rozpracovaná" else ""){
    AlertDialog.Builder(this).setTitle(t.name).setItems(arrayOf("Zobraziť na mape","Presunúť do priečinka","Premenovať","Vymazať")){_,i->when(i){
     0->if(t.count>0)openMap(t.id)else AlertDialog.Builder(this).setMessage("Trasa zatiaľ nemá GPS body.").setPositiveButton("OK",null).show()
     1->chooseFolder(t.id)
     2->edit("Názov trasy",t.name){db.renameTrack(t.id,it)}
     3->if(t.status!="saved")AlertDialog.Builder(this).setMessage("Najprv ukonči nahrávanie a ulož trasu.").setPositiveButton("OK",null).show()else remove(t.name){db.deleteTrack(t.id)}
    }}.show()
   }}
  }else{
   val items=places.filter{selected==-1L || (it.folder?:0)==selected}
   if(items.isEmpty())label("V tomto výbere zatiaľ nie je žiadne miesto. Ulož ho cez panel na mape.")
   items.forEach{p->OfflineUi.row(body,p.name,"Uložené miesto"){
    AlertDialog.Builder(this).setTitle(p.name).setItems(arrayOf("Zobraziť na mape","Presunúť do priečinka","Premenovať","Vymazať")){_,i->when(i){0->openMap(p.id);1->chooseFolder(p.id);2->edit("Názov miesta",p.name){db.renamePlace(p.id,it)};3->remove(p.name){db.deletePlace(p.id)}}}.show()
   }}
  }
 }
 private fun remove(name:String,operation:()->Unit){AlertDialog.Builder(this).setTitle("Vymazať $name?").setPositiveButton("Vymazať"){_,_->operation();render()}.setNegativeButton("Zrušiť",null).show()}
}
