package com.marko.turista
import android.app.AlertDialog
import android.os.Bundle
import android.widget.*
import android.text.InputType
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

object AccountConfig {
 // Public project configuration only. NEVER put a service_role/secret key here.
 const val URL=""
 const val PUBLIC_KEY=""
}
class AccountActivity:NatureScreen(){
 private val config by lazy{getSharedPreferences("account_config",MODE_PRIVATE)}
 private val vault by lazy{AccountSessionStore(this)}
 private var busy=false
 private var registering=false
 private lateinit var email:EditText
 private lateinit var name:EditText
 private lateinit var password:EditText
 private lateinit var repeat:EditText
 private lateinit var status:TextView
 private var resetSession:String?=null
 private fun base()=config.getString("url",AccountConfig.URL)!!.trimEnd('/')
 private fun publicKey()=config.getString("public",AccountConfig.PUBLIC_KEY)!!
 override fun onCreate(state:Bundle?){super.onCreate(state);setup("Účet Turistu");render()}
 private fun field(hint:String,secret:Boolean=false)=EditText(this).apply{this.hint=hint;setSingleLine();inputType=if(secret)129 else InputType.TYPE_CLASS_TEXT;setTextColor(OfflineUi.ink);background=OfflineUi.surface(this@AccountActivity);setPadding(dp(14),dp(10),dp(14),dp(10));if(secret){isSaveEnabled=false;importantForAutofill=android.view.View.IMPORTANT_FOR_AUTOFILL_NO};body.addView(this,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(10)})}
 private fun render(){
  body.removeAllViews()
  val saved=runCatching{JSONObject(vault.get())}.getOrNull()
  if(saved!=null && saved.optString("access_token").isNotBlank()){
   val user=saved.optJSONObject("user")
   card("Tvoj účet",(user?.optJSONObject("user_metadata")?.optString("username")?:"")+"\n"+(user?.optString("email")?:""))
   label("Prihlásenie je uložené v tomto telefóne. Mapy, nálezy, kroky a uložené trasy zostávajú lokálne; tento účet ich zatiaľ nesynchronizuje.")
   status=label("")
   action("Overiť prihlásenie"){work{
    val fresh=api("/token?grant_type=refresh_token",JSONObject().put("refresh_token",saved.getString("refresh_token")))
    require(fresh.optString("access_token").isNotBlank()){"Prihlás sa znova."};vault.save(fresh.toString());"Prihlásenie je platné."
   }}
   action("Odhlásiť sa"){if(!busy){val token=saved.optString("access_token");vault.save("");thread{runCatching{api("/logout",JSONObject(),token)}};render()}}
   return
  }
  card(if(registering)"Vytvor si účet" else "Vitaj v Turistovi","Účet je voliteľný. Mapy aj nálezy môžeš používať bez prihlásenia.")
  email=field("E-mail").apply{inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS}
  name=field("Používateľské meno").apply{visibility=if(registering)android.view.View.VISIBLE else android.view.View.GONE}
  password=field("Heslo",true)
  repeat=field("Heslo znova",true).apply{visibility=if(registering)android.view.View.VISIBLE else android.view.View.GONE}
  status=label(if(base().isBlank())"Prihlasovacia služba zatiaľ nie je pripojená. Vlastník aplikácie ju musí nastaviť." else "E-mail a heslo sa odošlú prihlasovacej službe tohto Turistu cez HTTPS.",14f)
  action(if(registering)"Registrovať" else "Prihlásiť sa"){
   val mail=email.text.toString().trim();val pass=password.text.toString();val nick=name.text.toString().trim()
   if(!android.util.Patterns.EMAIL_ADDRESS.matcher(mail).matches()){email.error="Zadaj platný e-mail";return@action}
   if(pass.length<8){password.error="Aspoň 8 znakov";return@action}
   if(registering && (nick.length !in 2..40)){name.error="Meno musí mať 2 až 40 znakov";return@action}
   if(registering && pass!=repeat.text.toString()){repeat.error="Heslá sa nezhodujú";return@action}
   val signup=registering
   work{
    val payload=JSONObject().put("email",mail).put("password",pass)
    if(signup)payload.put("data",JSONObject().put("username",nick))
    val result=api(if(signup)"/signup" else "/token?grant_type=password",payload)
    if(result.optString("access_token").isNotBlank())vault.save(result.toString())
    if(signup && result.optString("access_token").isBlank())"Skontroluj e-mail. Ak je registrácia možná, dostaneš potvrdenie. Po overení sa prihlás." else "Prihlásenie bolo úspešné."
   }
  }
  action(if(registering)"Už mám účet" else "Vytvoriť účet"){if(!busy){registering=!registering;render()}}
  action("Potvrdiť e-mail kódom"){codeDialog("signup")}
  action("Zabudnuté heslo"){
   val mail=email.text.toString().trim()
   if(!android.util.Patterns.EMAIL_ADDRESS.matcher(mail).matches()){email.error="Najprv vyplň e-mail";return@action}
   work{api("/recover",JSONObject().put("email",mail));"Ak účet existuje, dostaneš e-mail na obnovu hesla. Potom použi Mám obnovovací kód."}
  }
  action("Mám obnovovací kód"){codeDialog("recovery")}
  action("Nastavenie služby · vlastník"){if(!busy)configure()}
 }
 private fun codeDialog(type:String){
  if(busy)return
  val mail=email.text.toString().trim()
  if(!android.util.Patterns.EMAIL_ADDRESS.matcher(mail).matches()){email.error="Najprv vyplň e-mail";return}
  val code=EditText(this).apply{hint="Kód z e-mailu";inputType=2;isSaveEnabled=false}
  AlertDialog.Builder(this).setTitle(if(type=="signup")"Overiť e-mail" else "Obnoviť heslo").setView(code).setPositiveButton("Overiť"){_,_->
   val value=code.text.toString().trim()
   work{
    val data=api("/verify",JSONObject().put("type",type).put("email",mail).put("token",value))
    if(type=="signup") {if(data.optString("access_token").isNotBlank())vault.save(data.toString());"E-mail bol overený."}
    else {resetSession=data.getString("access_token");"Kód bol overený. Zadaj nové heslo."}
   }
  }.setNegativeButton("Zrušiť",null).show()
 }
 private fun newPassword(){
  val a=EditText(this).apply{hint="Nové heslo";inputType=129;isSaveEnabled=false}
  val b=EditText(this).apply{hint="Heslo znova";inputType=129;isSaveEnabled=false}
  val box=LinearLayout(this).apply{orientation=1;addView(a);addView(b)}
  val dialog=AlertDialog.Builder(this).setTitle("Nové heslo").setView(box).setPositiveButton("Uložiť",null).setNegativeButton("Zrušiť"){_,_->resetSession=null}.create()
  dialog.setOnShowListener{dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener{
   val pass=a.text.toString();if(pass.length<8 || pass!=b.text.toString()){b.error="Aspoň 8 znakov a rovnaké heslá";return@setOnClickListener}
   val token=resetSession?:return@setOnClickListener;resetSession=null;dialog.dismiss()
   work{api("/user",JSONObject().put("password",pass),token,"PUT");"Heslo zmenené. Prihlás sa novým heslom."}
  }};dialog.setOnCancelListener{resetSession=null};dialog.show()
 }
 private fun configure(){
  val box=LinearLayout(this).apply{orientation=1;setPadding(dp(16),0,dp(16),0)}
  val url=EditText(this).apply{hint="https://projekt.supabase.co";setText(base());inputType=17};box.addView(url)
  val key=EditText(this).apply{hint="Publishable alebo anon public key";setText(publicKey());setSingleLine()};box.addView(key)
  val dialog=AlertDialog.Builder(this).setTitle("Supabase Auth").setMessage("Údaje projektu vlastníka aplikácie. Nepouži service_role ani secret key. Účet Turistu nevytvára kľúče Gemini alebo Pl@ntNet.").setView(box).setPositiveButton("Uložiť",null).setNegativeButton("Zrušiť",null).create()
  dialog.setOnShowListener{dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener{
   val host=url.text.toString().trim().trimEnd('/');val public=key.text.toString().trim()
   if(!host.matches(Regex("https://[a-z0-9-]+\\.supabase\\.co"))){url.error="Zadaj HTTPS adresu projektu Supabase";return@setOnClickListener}
   val role=runCatching{JSONObject(String(android.util.Base64.decode(public.split('.')[1],android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP))).optString("role")}.getOrDefault("")
   if(!(public.startsWith("sb_publishable_") || role=="anon")){key.error="Použi verejný publishable alebo anon kľúč";return@setOnClickListener}
   config.edit().putString("url",host).putString("public",public).apply();vault.save("");dialog.dismiss();render()
  }};dialog.show()
 }
 private fun work(operation:()->String){
  if(busy)return
  if(base().isBlank() || publicKey().isBlank()){status.text="Prihlasovacia služba nie je nastavená.";return}
  busy=true;status.text="Komunikujem so službou…"
  thread{
   val message=try{operation()}catch(e:Exception){if(e is IllegalStateException)e.message?:"Operácia zlyhala." else "Spojenie zlyhalo. Skontroluj internet."}
   runOnUiThread{if(!isDestroyed){busy=false;render();status.text=message;if(resetSession!=null)newPassword()}}
  }
 }
 private fun api(path:String,data:JSONObject,token:String?=null,method:String="POST"):JSONObject{
  val conn=URL(base()+"/auth/v1"+path).openConnection() as HttpURLConnection
  try{conn.requestMethod=method;conn.doOutput=true;conn.connectTimeout=15000;conn.readTimeout=20000;conn.instanceFollowRedirects=false;conn.setRequestProperty("Content-Type","application/json");conn.setRequestProperty("apikey",publicKey());if(token!=null)conn.setRequestProperty("Authorization","Bearer $token")
   conn.outputStream.use{it.write(data.toString().toByteArray(Charsets.UTF_8))}
   val code=conn.responseCode
   if(code !in 200..299)error(when(code){400,401,403,422->"Údaje alebo kód nie sú platné, e-mail nie je potvrdený alebo účet nie je dostupný.";429->"Príliš veľa pokusov. Skús to neskôr.";else->"Služba je nedostupná (HTTP $code)."})
   val value=conn.inputStream.bufferedReader().use{it.readText()};return if(value.isBlank())JSONObject()else JSONObject(value)
  }finally{conn.disconnect()}
 }
}
