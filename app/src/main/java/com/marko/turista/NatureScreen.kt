package com.marko.turista
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
open class NatureScreen:AppCompatActivity() {
 protected lateinit var body:LinearLayout
 protected fun dp(n:Int)=(n*resources.displayMetrics.density).toInt()
 protected fun setup(title:String) {
  val root=LinearLayout(this).apply {orientation=1;setPadding(dp(18),dp(8),dp(18),dp(8));setBackgroundColor(Color.rgb(243,240,232))}
  root.addView(Button(this).apply {text="‹ Späť";setOnClickListener{finish()}})
  root.addView(TextView(this).apply {text=title;textSize=27f;setTextColor(Color.rgb(23,53,44));setTypeface(null,Typeface.BOLD);setPadding(0,dp(12),0,dp(12))})
  body=LinearLayout(this).apply{orientation=1;setPadding(0,0,0,dp(24))}
  root.addView(ScrollView(this).apply{addView(body)},LinearLayout.LayoutParams(-1,0,1f));setContentView(root);ScreenInsets.applyTo(root)
 }
 protected fun label(value:String,size:Float=16f)=TextView(this).apply {text=value;textSize=size;setTextColor(Color.rgb(23,53,44));setPadding(dp(8),dp(10),dp(8),dp(10));body.addView(this)}
 protected fun action(value:String,run:()->Unit)=Button(this).apply {text=value;isAllCaps=false;setTextColor(Color.WHITE);minHeight=dp(50);background=GradientDrawable().apply{setColor(Color.rgb(23,53,44));cornerRadius=dp(15).toFloat()};body.addView(this,LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,dp(6),0,dp(6))});setOnClickListener{run()}}
 protected fun card(title:String,detail:String,earned:Boolean=true) {
  val box=LinearLayout(this).apply {orientation=1;setPadding(dp(14),dp(12),dp(14),dp(12));background=GradientDrawable().apply{setColor(if(earned) Color.WHITE else Color.rgb(229,232,222));cornerRadius=dp(18).toFloat()}}
  box.addView(TextView(this).apply{text=title;textSize=20f;setTextColor(Color.rgb(23,53,44));setTypeface(null,Typeface.BOLD)})
  box.addView(TextView(this).apply{text=detail;textSize=15f;setTextColor(Color.DKGRAY);setPadding(0,dp(6),0,0)})
  body.addView(box,LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,dp(6),0,dp(6))})
 }
}
