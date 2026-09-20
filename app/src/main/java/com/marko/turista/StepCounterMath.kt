package com.marko.turista
object StepCounterMath {
 fun delta(previous:Long,current:Long,sameBoot:Boolean,sameDay:Boolean):Long =
  if(sameBoot && sameDay && current>=previous && previous>=0) current-previous else 0L
}
