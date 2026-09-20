package com.marko.turista

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class VoiceSettingsActivity:AppCompatActivity() {
    private var tts:TextToSpeech?=null
    private var ready=false
    private lateinit var status:TextView
    private lateinit var voices:Spinner
    private val preferences by lazy {VoicePreferences(this)}
    override fun onCreate(state:Bundle?) {
        super.onCreate(state)
        val column=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL;setPadding(24,12,24,32);setBackgroundColor(Color.rgb(243,240,232))}
        column.addView(Button(this).apply {text="‹ Späť";setOnClickListener{finish()}})
        column.addView(TextView(this).apply {text="Hlas navigácie";textSize=26f;setTextColor(Color.rgb(23,53,44))})
        status=TextView(this).apply {text="Načítavam slovenské hlasy…";textSize=16f;setPadding(0,24,0,24)};column.addView(status)
        voices=Spinner(this);column.addView(voices)
        column.addView(Switch(this).apply {text="Povoliť online hlasy";isChecked=preferences.online;setOnCheckedChangeListener{_,checked->preferences.online=checked;populate()}})
        val speed=TextView(this).apply {text="Tempo: ${String.format("%.2f",preferences.speed)}×"};column.addView(speed)
        column.addView(SeekBar(this).apply {max=40;progress=((preferences.speed-0.8f)*100).toInt();setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar:SeekBar?,value:Int,user:Boolean){if(user){preferences.speed=0.8f+value/100f;speed.text="Tempo: ${String.format("%.2f",preferences.speed)}×"}}
            override fun onStartTrackingTouch(bar:SeekBar?){}
            override fun onStopTrackingTouch(bar:SeekBar?){}
        })})
        column.addView(Button(this).apply {text="Vypočuť ukážku";setOnClickListener{
            val engine=tts
            if(ready && engine!=null && GuidanceVoice.apply(engine,preferences,connected())) engine.speak("O sto metrov odboč doprava. Potom pokračuj rovno. Prajem ti príjemnú cestu.",TextToSpeech.QUEUE_FLUSH,null,"voice_preview")
            else Toast.makeText(this@VoiceSettingsActivity,"Slovenský hlas nie je dostupný. Skontroluj internet alebo stiahni offline hlas.",Toast.LENGTH_LONG).show()
        }})
        column.addView(Button(this).apply {text="Nastavenia hlasu v telefóne";setOnClickListener{
            try {startActivity(Intent("com.android.settings.TTS_SETTINGS"))}catch(_:Exception){startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))}
        }})
        column.addView(TextView(this).apply {text="Prirodzenosť hlasu závisí od hlasového systému telefónu. Vyber a vypočuj si slovenský hlas. Pre navigáciu bez internetu stiahni slovenské hlasové dáta v nastaveniach telefónu. Online hlas môže pri slabom signále meškať; aplikácia vtedy použije dostupný offline hlas.";textSize=15f;setPadding(0,24,0,0)})
        val scroll=ScrollView(this).apply {addView(column)};setContentView(scroll);ScreenInsets.applyTo(scroll)
        tts=TextToSpeech(this){result->ready=result==TextToSpeech.SUCCESS;if(ready)populate() else status.text="Hlasový systém sa nepodarilo spustiť."}
    }
    private fun connected():Boolean {
        val c=getSystemService(CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        return c.getNetworkCapabilities(c.activeNetwork)?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)==true
    }
    private fun populate() {
        val engine=tts?:return;if(!ready)return
        val available=GuidanceVoice.voices(engine).filter {!it.isNetworkConnectionRequired || preferences.online}
        status.text=if(available.isEmpty()) "Slovenský hlas nie je nainštalovaný. Otvor nastavenia hlasu v telefóne." else "Vyber hlas a vypočuj si ukážku."
        voices.onItemSelectedListener=null
        val labels=listOf("Automaticky: najkvalitnejší dostupný")+available.map {"${it.name} · ${if(it.isNetworkConnectionRequired) "online" else "offline"}"}
        voices.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,labels)
        voices.setSelection((available.indexOfFirst{it.name==preferences.name}+1).coerceAtLeast(0),false)
        voices.onItemSelectedListener=object:android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent:android.widget.AdapterView<*>?,view:android.view.View?,position:Int,id:Long){preferences.name=available.getOrNull(position-1)?.name?:""}
            override fun onNothingSelected(parent:android.widget.AdapterView<*>?){}
        }
    }
    override fun onResume(){super.onResume();if(ready)populate()}
    override fun onPause(){tts?.stop();super.onPause()}
    override fun onDestroy(){tts?.shutdown();tts=null;super.onDestroy()}
}
