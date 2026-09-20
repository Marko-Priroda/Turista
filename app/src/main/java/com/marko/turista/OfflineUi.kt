package com.marko.turista
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.content.res.ColorStateList
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.widget.*
object OfflineUi {
 val ink=Color.rgb(23,53,44)
 val cream=Color.rgb(246,244,237)
 fun dp(c:Context,n:Int)=(c.resources.displayMetrics.density*n).toInt()
 fun surface(c:Context,color:Int=Color.WHITE)=GradientDrawable().apply{setColor(color);cornerRadius=dp(c,18).toFloat();setStroke(dp(c,1),Color.rgb(222,228,215))}
 fun row(parent:LinearLayout,title:String,subtitle:String,click:()->Unit) {
  val c=parent.context
  val box=LinearLayout(c).apply{orientation=0;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(c,16),dp(c,17),dp(c,16),dp(c,17));background=RippleDrawable(ColorStateList.valueOf(0x22365C45),surface(c),null);isClickable=true;isFocusable=true;contentDescription="$title. $subtitle";setOnClickListener{click()}}
  box.addView(ImageView(c).apply{setImageResource(R.drawable.ic_offline_atlas);setColorFilter(ink);importantForAccessibility=2},LinearLayout.LayoutParams(dp(c,30),dp(c,30)).apply{marginEnd=dp(c,14)})
  val labels=LinearLayout(c).apply{orientation=1}
  labels.addView(TextView(c).apply{text=title;textSize=18f;setTextColor(ink);setTypeface(null,Typeface.BOLD)})
  labels.addView(TextView(c).apply{text=subtitle;textSize=13f;setTextColor(Color.rgb(99,112,103));setPadding(0,dp(c,4),0,0)})
  box.addView(labels,LinearLayout.LayoutParams(0,-2,1f))
  box.addView(TextView(c).apply{text="›";textSize=27f;setTextColor(ink);importantForAccessibility=2})
  parent.addView(box,LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,dp(c,10))})
 }
 fun heading(c:Context,text:String)=TextView(c).apply{this.text=text;textSize=30f;setTextColor(ink);setTypeface(null,Typeface.BOLD);setPadding(0,dp(c,8),0,dp(c,8))}
 fun search(c:Context,hint:String)=EditText(c).apply{this.hint=hint;setSingleLine();textSize=16f;setTextColor(ink);setHintTextColor(Color.rgb(106,118,109));background=surface(c);setPadding(dp(c,18),dp(c,14),dp(c,18),dp(c,14));minHeight=dp(c,54);inputType=android.text.InputType.TYPE_CLASS_TEXT;imeOptions=android.view.inputmethod.EditorInfo.IME_ACTION_DONE}
 fun back(c:Context,action:()->Unit)=TextView(c).apply{text="‹  Späť na mapu";textSize=16f;setTextColor(ink);gravity=Gravity.CENTER_VERTICAL;minHeight=dp(c,48);isFocusable=true;setOnClickListener{action()}}
}
