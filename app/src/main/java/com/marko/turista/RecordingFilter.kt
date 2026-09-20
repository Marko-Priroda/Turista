package com.marko.turista

/** Pure rules shared by GPS recording and tests. Gaps form separate segments. */
object RecordingFilter {
    data class Fix(val lat:Double,val lon:Double,val elapsedSeconds:Double,val accuracy:Double)
    data class Decision(val accept:Boolean,val split:Boolean=false,val distance:Double=0.0,val seconds:Long=0)
    fun evaluate(previous:Fix?,next:Fix,ageSeconds:Double):Decision {
        if(!next.lat.isFinite() || !next.lon.isFinite() || next.lat !in -90.0..90.0 || next.lon !in -180.0..180.0 ||
            !next.accuracy.isFinite() || next.accuracy !in 0.0..35.0 || ageSeconds !in -1.0..20.0) return Decision(false)
        if(previous==null) return Decision(true,true)
        val dt=next.elapsedSeconds-previous.elapsedSeconds
        if(dt<=0) return Decision(false)
        if(dt>120) return Decision(true,true)
        val distance=RouteProgress.distance(RouteProgress.Point(previous.lat,previous.lon),RouteProgress.Point(next.lat,next.lon))
        if(distance/dt>85 || (dt<15 && distance<5)) return Decision(false)
        return Decision(true,false,distance,dt.toLong())
    }
}

class StepDelta {
    private var raw:Long?=null
    private var day=""
    fun update(value:Long,date:String):Long {
        val previous=raw;val sameDay=day==date
        raw=value;day=date
        return if(previous==null || !sameDay || value<previous) 0 else value-previous
    }
    fun reset() {raw=null;day=""}
}
