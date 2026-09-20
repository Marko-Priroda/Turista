package com.marko.turista
class RecordingTest {
@org.junit.Test fun recordingAndStepRegression() {
    fun fix(lat:Double=49.0,t:Double=100.0,acc:Double=5.0)=RecordingFilter.Fix(lat,20.0,t,acc)
    val start=fix()
    check(RecordingFilter.evaluate(null,start,1.0).accept)
    check(!RecordingFilter.evaluate(null,start,21.0).accept)
    check(!RecordingFilter.evaluate(null,fix(acc=70.0),0.0).accept)
    check(!RecordingFilter.evaluate(start,fix(t=99.0),0.0).accept)
    check(!RecordingFilter.evaluate(start,fix(lat=49.000001,t=103.0),0.0).accept)
    check(!RecordingFilter.evaluate(start,fix(lat=50.0,t=103.0),0.0).accept)
    val walking=RecordingFilter.evaluate(start,fix(lat=49.0001,t=110.0),1.0)
    check(walking.accept && !walking.split && walking.distance in 10.0..12.0 && walking.seconds==10L)
    val gap=RecordingFilter.evaluate(start,fix(lat=50.0,t=250.0),0.0)
    check(gap.accept && gap.split && gap.distance==0.0 && gap.seconds==0L)
    val resume=RecordingFilter.evaluate(null,fix(lat=50.0,t=400.0),0.0)
    check(resume.accept && resume.split && resume.distance==0.0)
    val steps=StepDelta()
    check(steps.update(9000,"2026-09-19")==0L)
    check(steps.update(9012,"2026-09-19")==12L)
    check(steps.update(9012,"2026-09-19")==0L)
    check(steps.update(9015,"2026-09-20")==0L)
    check(steps.update(9025,"2026-09-20")==10L)
    check(steps.update(4,"2026-09-20")==0L)
    check(steps.update(9,"2026-09-20")==5L)
    steps.reset();check(steps.update(1000,"2026-09-20")==0L)
    println("PASS: GPS quality, stale/out-of-order fixes, stationary jitter, implausible jumps, gap/resume segment breaks, step baseline, duplicate counter, midnight, reboot, disabled interval")
}

}
