package com.marko.turista
class RerouteGateTest {
 @org.junit.Test fun gpsEvidenceAndCooldown() {
 val g=RerouteGate()
 fun checkAt(t:Long,d:Double=90.0,a:Float=5f,enabled:Boolean=true)=g.check(t,d,a,false,enabled)
 check(!checkAt(0));check(!checkAt(4000));check(checkAt(8000))
 check(!checkAt(12000));check(!checkAt(16000));check(!checkAt(20000));check(checkAt(38000).not()) // long gap resets evidence
 check(!checkAt(42000));check(checkAt(46000))
 g.failed(46000);check(!checkAt(50000));check(!checkAt(54000));check(!checkAt(58000))
 g.reset();check(!checkAt(0));check(!checkAt(4000,a=70f));check(!checkAt(8000));check(!checkAt(12000));check(checkAt(16000))
 g.reset();check(!checkAt(0));check(!checkAt(4000,d=10.0));check(!checkAt(8000));check(!checkAt(12000));check(checkAt(16000))
 g.reset();check(!checkAt(0,enabled=false));check(!checkAt(4000,enabled=false));check(!checkAt(8000,enabled=false))
 g.reset();check(!checkAt(0,a=Float.NaN));check(!checkAt(4000));check(!checkAt(8000));check(checkAt(12000))
 println("PASS: sustained deviation, cooldown, failed request, inaccurate GPS, recovery, disabled/manual mode, gaps, NaN")
}

}
