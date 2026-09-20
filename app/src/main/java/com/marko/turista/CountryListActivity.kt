package com.marko.turista
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
class CountryListActivity:AppCompatActivity(){
 override fun onCreate(state:Bundle?){
  super.onCreate(state)
  val continent=intent.getStringExtra("continent")?:"Európa"
  val root=LinearLayout(this).apply{orientation=1;setBackgroundColor(OfflineUi.cream);setPadding(OfflineUi.dp(this@CountryListActivity,18),0,OfflineUi.dp(this@CountryListActivity,18),0)}
  root.addView(OfflineUi.back(this){finish()}.apply{text="‹  Všetky oblasti"})
  root.addView(OfflineUi.heading(this,continent))
  val search=OfflineUi.search(this,"Hľadať krajinu…")
  root.addView(search,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=OfflineUi.dp(this@CountryListActivity,16)})
  val list=LinearLayout(this).apply{orientation=1;setPadding(0,0,0,OfflineUi.dp(this@CountryListActivity,24))}
  root.addView(ScrollView(this).apply{addView(list)},LinearLayout.LayoutParams(-1,0,1f))
  fun render(query:String){
   list.removeAllViews()
   fun normalize(s:String)=java.text.Normalizer.normalize(s,java.text.Normalizer.Form.NFD).replace(Regex("\\p{M}"),"").lowercase(java.util.Locale.ROOT)
   val countries=CountryData.countries.filter{it.continent==continent && normalize(it.name).contains(normalize(query.trim()))}
   countries.forEach{country->OfflineUi.row(list,country.name,"Mapa a offline navigácia"){startActivity(Intent(this,CountryActivity::class.java).putExtra("country",country.name).putExtra("flag",country.flag))}}
   if(countries.isEmpty())list.addView(TextView(this).apply{text="Žiadna krajina nezodpovedá hľadaniu.";textSize=16f;setTextColor(OfflineUi.ink)})
  }
  render("")
  search.addTextChangedListener(object:android.text.TextWatcher{
   override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int){}
   override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int){render(s?.toString()?:"")}
   override fun afterTextChanged(s:android.text.Editable?){}
  })
  setContentView(root);ScreenInsets.applyTo(root)
 }
}
