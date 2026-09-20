package com.marko.turista

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat

class SettingsActivity : AppCompatActivity() {
    private lateinit var settings: TuristaSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = TuristaSettings(this)
        setContentView(R.layout.activity_settings)
        ScreenInsets.applyTo(findViewById(R.id.settingsRoot))
        findViewById<View>(R.id.settingsBack).setOnClickListener { finish() }
        findViewById<View>(R.id.trafficSettings).setOnClickListener { showTrafficKeyDialog() }
        findViewById<View>(R.id.voiceOptionsButton).setOnClickListener {startActivity(android.content.Intent(this,VoiceSettingsActivity::class.java))}
        bindControls()
        findViewById<View>(R.id.resetSettings).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Obnoviť pôvodné nastavenia?")
                .setMessage("Zmenia sa iba nastavenia. Stiahnuté mapy a navigačné dáta zostanú uložené.")
                .setNegativeButton("Zrušiť", null)
                .setPositiveButton("Obnoviť") { _, _ ->
                    settings.reset()
                    bindControls()
                    Toast.makeText(this, "Nastavenia boli obnovené", Toast.LENGTH_SHORT).show()
                }
                .show()
        }
    }

    private fun showTrafficKeyDialog() {
        val store = TrafficKeyStore(this)
        val input = android.widget.EditText(this).apply {
            hint = if (store.get().isBlank()) "Vlož API kľúč TomTom" else "Vlož nový kľúč (pôvodný je uložený)"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setSingleLine(true)
        }
        val dialog = AlertDialog.Builder(this).setTitle("Živá doprava · TomTom")
            .setMessage("Vlastný API kľúč s prístupom k Traffic API získaš na developer.tomtom.com. Platí limit tvojho účtu. Vrstva zobrazuje premávku; výpočet trasy zápchy nezohľadňuje.")
            .setView(input).setPositiveButton("Overiť a uložiť", null)
            .setNeutralButton("Odstrániť kľúč") { _, _ -> store.save(""); if(settings.mapType == "traffic") settings.mapType = "default" }
            .setNegativeButton("Zavrieť", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val value = input.text.toString().trim()
                if(value.isBlank()) { input.error = "Vlož kľúč"; return@setOnClickListener }
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                kotlin.concurrent.thread {
                    val ok = try {
                        val key = java.net.URLEncoder.encode(value,"UTF-8")
                        val connection = java.net.URL("https://api.tomtom.com/traffic/map/4/tile/flow/relative0/12/2044/1360.png?key=$key").openConnection() as java.net.HttpURLConnection
                        try { connection.connectTimeout=12000;connection.readTimeout=12000
                            connection.responseCode == 200 && connection.contentType?.startsWith("image/") == true
                        } finally { connection.disconnect() }
                    } catch(_: Exception) { false }
                    runOnUiThread {
                        if(isDestroyed || !dialog.isShowing) return@runOnUiThread
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                        if(ok) {
                            try { store.save(value); Toast.makeText(this,"Kľúč je overený. Dopravnú mapu zapni vo vrstvách.",Toast.LENGTH_LONG).show();dialog.dismiss() }
                            catch(_: Exception) { input.error = "Kľúč sa nepodarilo bezpečne uložiť." }
                        } else input.error = "Overenie zlyhalo. Skontroluj internet, kľúč a prístup k Traffic API."
                    }
                }
            }
        }
        dialog.show()
    }

    private fun bindControls() {
        bindSwitch(R.id.voiceSwitch, settings.voiceGuidance) { settings.voiceGuidance = it }
        bindSwitch(R.id.screenSwitch, settings.keepScreenOn) { settings.keepScreenOn = it }
        bindSwitch(R.id.followSwitch, settings.followNavigation) { settings.followNavigation = it }
        bindSwitch(R.id.rotateSwitch, settings.rotateMap) { settings.rotateMap = it }
        findViewById<RadioGroup>(R.id.distanceUnits).apply {
            setOnCheckedChangeListener(null)
            check(if (settings.imperialUnits) R.id.unitsImperial else R.id.unitsMetric)
            setOnCheckedChangeListener { _, checkedId ->
                settings.imperialUnits = checkedId == R.id.unitsImperial
            }
        }
    }

    private fun bindSwitch(id: Int, checked: Boolean, save: (Boolean) -> Unit) {
        findViewById<SwitchCompat>(id).apply {
            setOnCheckedChangeListener(null)
            isChecked = checked
            val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
            thumbTintList = ColorStateList(states, intArrayOf(
                ContextCompat.getColor(context, R.color.turista_forest), 0xFF7A8780.toInt()))
            trackTintList = ColorStateList(states, intArrayOf(0xFFB9D3BE.toInt(), 0xFFDEE4DD.toInt()))
            setOnCheckedChangeListener { _, enabled -> save(enabled) }
        }
    }
}
