package com.marko.turista
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
/** Retains the launcher component identity for existing home-screen shortcuts. */
class MainActivity:AppCompatActivity() {
    override fun onCreate(state:Bundle?) {super.onCreate(state);startActivity(Intent(this,MapActivity::class.java));finish()}
}
