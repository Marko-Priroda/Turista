package com.marko.turista

/** Sustained deviation and cooldown prevent rerouting on a single noisy GPS fix. */
class RerouteGate {
    private var since=-1L
    private var fixes=0
    private var lastFix=-1L
    private var nextAttempt=0L
    fun check(now:Long,distance:Double,accuracy:Float,car:Boolean,enabled:Boolean):Boolean {
        if(lastFix>=0 && (now-lastFix>15000 || now<lastFix)) {since=-1;fixes=0}
        lastFix=now
        if(!enabled || !accuracy.isFinite() || accuracy>30 || accuracy<0 || distance.isNaN()) {since=-1;fixes=0;return false}
        val limit=maxOf(if(car) 60.0 else 35.0,accuracy*2.0)
        if(distance<=limit) {since=-1;fixes=0;return false}
        if(since<0) since=now
        fixes++
        if(fixes>=3 && now-since>=8000 && now>=nextAttempt) {
            nextAttempt=now+30000;since=-1;fixes=0;return true
        }
        return false
    }
    fun failed(now:Long) {nextAttempt=now+45000;since=-1;fixes=0}
    fun reset() {since=-1;fixes=0;nextAttempt=0;lastFix=-1}
}
