package com.marko.turista
import java.time.LocalDate
object StepStats {
 fun longestStreak(days:Map<String,Long>):Int {
  var previous:LocalDate?=null;var current=0;var best=0
  days.filterValues{it>0}.keys.sorted().forEach {key ->val date=LocalDate.parse(key);current=if(previous?.plusDays(1)==date) current+1 else 1;best=maxOf(best,current);previous=date};return best
 }
 fun total(days:Map<String,Long>)=days.values.sum()
}
