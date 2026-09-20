package com.marko.turista
import android.content.Context
import android.graphics.*
import android.view.View
/** Decorative native vector artwork, no third-party photos or network requests. */
class RecognizerArt(context:Context):View(context){
 private val p=Paint(Paint.ANTI_ALIAS_FLAG)
 override fun onDraw(c:Canvas){super.onDraw(c);val w=width.toFloat();val h=height.toFloat()
  p.shader=LinearGradient(0f,0f,w,h,intArrayOf(Color.rgb(216,230,200),Color.rgb(246,244,237)),null,Shader.TileMode.CLAMP);c.drawRoundRect(0f,0f,w,h,28f,28f,p);p.shader=null
  p.color=Color.rgb(23,53,44);val ridge=Path().apply{moveTo(0f,h);lineTo(w*.28f,h*.15f);lineTo(w*.55f,h*.68f);lineTo(w*.73f,h*.28f);lineTo(w,h);close()};c.drawPath(ridge,p)
  p.color=Color.rgb(147,176,105);val front=Path().apply{moveTo(0f,h);cubicTo(w*.2f,h*.5f,w*.58f,h*.78f,w,h*.47f);lineTo(w,h);close()};c.drawPath(front,p)
  p.color=Color.rgb(245,236,191);c.drawCircle(w*.82f,h*.2f,h*.075f,p)
 }
}
