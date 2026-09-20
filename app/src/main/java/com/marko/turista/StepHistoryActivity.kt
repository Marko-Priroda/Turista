package com.marko.turista
import android.os.Bundle
import java.time.*
import java.time.format.DateTimeFormatter
class StepHistoryActivity:NatureScreen() {
 private var month=YearMonth.now()
 private val db by lazy{TripStore.get(this)}
 override fun onCreate(state:Bundle?) {super.onCreate(state);month=state?.getString("month")?.let{YearMonth.parse(it)}?:YearMonth.now();setup(if(intent.getBooleanExtra("awards",false)) "Tvoje úspechy" else "História krokov");render()}
 override fun onSaveInstanceState(out:Bundle){out.putString("month",month.toString());super.onSaveInstanceState(out)}
 private fun badge(title:String,current:Long,target:Long,detail:String){
  val earned=current>=target
  val row=android.widget.LinearLayout(this).apply{orientation=1;setPadding(dp(14),dp(12),dp(14),dp(12));background=OfflineUi.surface(this@StepHistoryActivity,if(earned)0xffe3ebd8.toInt() else android.graphics.Color.WHITE)}
  row.addView(android.widget.TextView(this).apply{text=(if(earned)"★  " else "◇  ")+title;textSize=18f;setTextColor(OfflineUi.ink);setTypeface(null,android.graphics.Typeface.BOLD)})
  row.addView(android.widget.TextView(this).apply{text=detail;textSize=13f;setTextColor(OfflineUi.ink);setPadding(0,dp(5),0,dp(7))})
  row.addView(android.widget.ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal).apply{max=100;progress=(current*100/target.coerceAtLeast(1)).coerceIn(0,100).toInt();progressTintList=android.content.res.ColorStateList.valueOf(OfflineUi.ink)})
  body.addView(row,android.widget.LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(8)})
 }
 private fun render() {
  body.removeAllViews();val days=db.stepDays();val total=StepStats.total(days)
  if(intent.getBooleanExtra("awards",false)) {
   val levels=listOf(0L,1000L,5000L,15000L,40000L,100000L,250000L,500000L,1000000L)
   val names=listOf("Prvý výlet","Objaviteľ","Chodec","Prieskumník","Turista","Stopár","Horár","Horal","Legenda chodníkov")
   val level=levels.indexOfLast{total>=it}.coerceAtLeast(0)
   card("Úroveň ${level+1} · ${names[level]}","Spolu $total krokov"+if(level<levels.lastIndex) "\nDo ďalšej úrovne: ${levels[level+1]-total} krokov" else "\nNajvyššia úroveň dosiahnutá")
   label("Denné míľniky",21f)
   listOf(1000,3000,5000,8000,10000,15000,20000,30000).forEach {limit->val count=days.values.count{it>=limit};badge("$limit krokov za deň",(days.values.maxOrNull()?:0L),limit.toLong(),"Dosiahnuté $count dní")}
   label("Pravidelnosť",21f);val streak=StepStats.longestStreak(days)
   listOf(3,7,14,30,60,100).forEach{n->badge("Séria $n dní",streak.toLong(),n.toLong(),"Najdlhšia séria: $streak dní")}
   label("Dni na nohách",21f);val active=days.values.count{it>0}
   listOf(1,10,30,60,100,180,365).forEach{n->badge("$n aktívnych dní",active.toLong(),n.toLong(),"Zaznamenaných $active dní")}
   label("Celkové kroky",21f)
   listOf(10000L,50000L,100000L,250000L,500000L,1000000L,2000000L).forEach{n->badge("$n krokov celkom",total,n,"${total.coerceAtMost(n)} / $n")}
   label("Úspechy vychádzajú z uložených krokov, nie z odhadu. Nevyžadujú nákup ani pripojenie.",14f)
  } else {
   if(days.isEmpty()){label("História sa začne prvým meraním krokomera.");return}
   val first=LocalDate.parse(days.keys.minOrNull()!!)
   card("Od ${first.format(DateTimeFormatter.ofPattern("d. M. yyyy"))}","Spolu $total krokov · ${days.values.count{it>0}} aktívnych dní")
   label(month.format(DateTimeFormatter.ofPattern("LLLL yyyy",java.util.Locale("sk"))),23f)
   val sum=days.filterKeys{it.startsWith(month.toString())}.values.sum();label("V tomto mesiaci: $sum krokov")
   action("‹ Predchádzajúci mesiac"){month=month.minusMonths(1);render()}.isEnabled=month>YearMonth.from(first)
   action("Nasledujúci mesiac ›"){month=month.plusMonths(1);render()}.isEnabled=month<YearMonth.now()
   val end=minOf(month.atEndOfMonth(),LocalDate.now())
   for(day in end.dayOfMonth downTo 1){val date=month.atDay(day);if(date>=first) card(date.format(DateTimeFormatter.ofPattern("EEE d. M.",java.util.Locale("sk"))),days[date.toString()]?.let{"$it krokov"}?:"Bez záznamu",days.containsKey(date.toString()))}
  }
 }
}
