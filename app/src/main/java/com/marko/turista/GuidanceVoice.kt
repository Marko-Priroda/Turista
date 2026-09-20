package com.marko.turista

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import java.util.Locale

class VoicePreferences(context:Context) {
    private val p=context.getSharedPreferences("turista_voice",Context.MODE_PRIVATE)
    var name:String
        get()=p.getString("name","")?:""
        set(v){p.edit().putString("name",v).apply()}
    var online:Boolean
        get()=p.getBoolean("online",false)
        set(v){p.edit().putBoolean("online",v).apply()}
    var speed:Float
        get()=p.getFloat("speed",0.95f)
        set(v){p.edit().putFloat("speed",v.coerceIn(0.8f,1.2f)).apply()}
}
object GuidanceVoice {
    fun voices(tts:TextToSpeech):List<Voice> = (tts.voices?:emptySet()).filter {
        it.locale.language=="sk" && !(it.features?:emptySet()).contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
    }.sortedWith(compareByDescending<Voice>{it.quality}.thenBy{it.latency})
    fun apply(tts:TextToSpeech,preferences:VoicePreferences,connected:Boolean):Boolean {
        val allowed=voices(tts).filter {!it.isNetworkConnectionRequired || (preferences.online && connected)}
        val selected=allowed.firstOrNull {it.name==preferences.name}?:allowed.firstOrNull()
        tts.setSpeechRate(preferences.speed);tts.setPitch(1f)
        return if(selected!=null) tts.setVoice(selected)==TextToSpeech.SUCCESS else {
            val result=tts.setLanguage(Locale("sk","SK"))
            result>=TextToSpeech.LANG_AVAILABLE && (tts.voice?.isNetworkConnectionRequired!=true || (preferences.online && connected))
        }
    }
}
