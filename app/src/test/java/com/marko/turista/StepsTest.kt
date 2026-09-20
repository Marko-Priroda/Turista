package com.marko.turista
class StepsTest {
 @org.junit.Test fun recoveryAndHistory(){
 check(StepCounterMath.delta(100,127,true,true)==27L)
 check(StepCounterMath.delta(127,127,true,true)==0L)
 check(StepCounterMath.delta(127,4,true,true)==0L)
 check(StepCounterMath.delta(127,200,false,true)==0L)
 check(StepCounterMath.delta(127,200,true,false)==0L)
 // A restored baseline counts only the difference, not the whole boot counter.
 check(StepCounterMath.delta(10000,10050,true,true)==50L)
 val days=mapOf("2026-09-01" to 50L,"2026-09-02" to 70L,"2026-09-03" to 0L,"2026-09-04" to 80L,"2026-09-05" to 90L,"2026-09-06" to 100L)
 check(StepStats.longestStreak(days)==3);check(StepStats.total(days)==390L)
 check(StepStats.longestStreak(emptyMap())==0)
 check(StepStats.longestStreak(mapOf("2026-08-31" to 1L,"2026-09-01" to 2L))==2)
 println("PASS: cumulative sensor recovery, duplicate/reset/boot/day boundaries, totals and streaks")
}

}
