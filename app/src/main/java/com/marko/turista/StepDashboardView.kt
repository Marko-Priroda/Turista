package com.marko.turista
import android.content.Context
import android.graphics.*
import android.view.View
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
/** Dashboard uses stored steps only. No guessed calories or fabricated walking time. */
class StepDashboardView(context:Context):View(context){
 private val p=Paint(Paint.ANTI_ALIAS_FLAG)
 private var counts=List(7){0L};private var goal=6000
 private val d=resources.displayMetrics.density
 fun update(values:List<Long>,target:Int){if(values==counts&&target==goal&&contentDescription!=null)return;counts=values;goal=target;contentDescription="Dnes ${counts.lastOrNull()?:0} krokov. Cieľ $goal. Týždeň ${counts.sum()} krokov.";invalidate()}
 override fun onMeasure(w:Int,h:Int){setMeasuredDimension(MeasureSpec.getSize(w),(460*d).toInt())}
 override fun onDraw(c:Canvas){
  super.onDraw(c);val w=width.toFloat();val ink=Color.rgb(23,53,44);val muted=Color.rgb(104,119,110)
  fun text(s:String,x:Float,y:Float,size:Float,bold:Boolean=false,color:Int=ink,align:Paint.Align=Paint.Align.LEFT){p.style=Paint.Style.FILL;p.color=color;p.textSize=size*d;p.typeface=if(bold)Typeface.DEFAULT_BOLD else Typeface.DEFAULT;p.textAlign=align;c.drawText(s,x,y,p)}
  fun rect(l:Float,t:Float,r:Float,b:Float,color:Int,radius:Float=14*d){p.color=color;p.style=Paint.Style.FILL;c.drawRoundRect(l,t,r,b,radius,radius,p)}
  val today=counts.lastOrNull()?:0L
  rect(0f,0f,w,455*d,Color.WHITE,22*d)
  text("Dnes",18*d,32*d,22f,true)
  text(LocalDate.now().format(DateTimeFormatter.ofPattern("d. MMMM yyyy",Locale("sk"))),18*d,54*d,13f,color=muted)
  text(String.format(Locale.getDefault(),"%,d",today),18*d,117*d,48f,true)
  text("KROKOV",18*d,141*d,12f,color=muted)
  text("Denný cieľ $goal",18*d,174*d,14f)
  text("${(today*100/goal.coerceAtLeast(1)).coerceAtMost(999)} %",w-18*d,174*d,14f,true,align=Paint.Align.RIGHT)
  rect(18*d,185*d,w-18*d,195*d,0xffe2e9dc.toInt(),5*d)
  rect(18*d,185*d,18*d+(w-36*d)*(today.toFloat()/goal.coerceAtLeast(1)).coerceIn(0f,1f),195*d,ink,5*d)
  text("POSLEDNÝCH 7 DNÍ",18*d,225*d,12f,true)
  val cell=(w-28*d)/7;val base=390*d;val max=maxOf(1L,counts.maxOrNull()?:1).toFloat()
  counts.forEachIndexed{i,n->
   val x=14*d+cell*(i+.5f);val top=base-135*d*(n/max)
   rect(x-cell*.36f,if(n==0L)base-4*d else top,x+cell*.36f,base,if(i==6)ink else 0xffa2b58c.toInt(),7*d)
   text(if(n>=100000)"${n/1000}k" else n.toString(),x,top-7*d,10f,true,align=Paint.Align.CENTER)
   val date=LocalDate.now().minusDays((6-i).toLong())
   text(if(i==6)"Dnes" else date.format(DateTimeFormatter.ofPattern("EE",Locale("sk"))),x,412*d,11f,i==6,align=Paint.Align.CENTER)
   if(n>=goal)text("★",x,base-8*d,14f,color=0xfff0d28b.toInt(),align=Paint.Align.CENTER)
  }
  text("Denný priemer: ${counts.sum()/7} krokov",w/2,443*d,14f,color=muted,align=Paint.Align.CENTER)
 }
}
