package com.marko.turista
import kotlin.math.abs
private fun close(a: Double,b: Double,tolerance:Double=2.0) { check(abs(a-b)<tolerance) { "$a != $b" } }
class RouteProgressTest {
@org.junit.Test fun routeProgressRegression() {
    val a=RouteProgress.Point(49.0,20.0)
    val b=RouteProgress.Point(49.001,20.0)
    val c=RouteProgress.Point(49.002,20.0)
    val route=RouteProgress(listOf(a,b,c))
    close(route.length,222.39)
    val halfway=route.update(RouteProgress.Point(49.0005,20.0))
    close(halfway.along,55.60)
    close(halfway.crossTrack,0.0)
    // A noisier fix behind us cannot increase remaining distance or repeat turns.
    route.update(RouteProgress.Point(49.0004,20.0));close(route.along,55.60)
    // Poor cross-track projection must not skip a manoeuvre.
    val before=route.along
    route.update(RouteProgress.Point(49.001,20.003));close(route.along,before)
    check(route.crossTrack>60)
    // Accurate GPS recovery advances on the route.
    route.update(RouteProgress.Point(49.0015,20.0),180.0);close(route.along,166.79)
    // A loop near the start must not jump straight to its distant return segment.
    val loop=RouteProgress(listOf(a,RouteProgress.Point(49.01,20.0),RouteProgress.Point(49.01,20.01),a))
    loop.update(RouteProgress.Point(49.0001,20.00001)); check(loop.along<30)
    // Duplicate geometry points and zero-length paths are valid.
    val dup=RouteProgress(listOf(a,a,b));dup.update(a);check(dup.along.isFinite())
    close(dup.length,111.195)
    check(RouteProgress(emptyList()).length==0.0)
    // Hybrid total retains the walking tail at the road endpoint.
    val hybrid=RouteProgress(listOf(a,b,c));hybrid.update(b,200.0)
    close(hybrid.length-hybrid.along,111.195)
    hybrid.update(c,200.0);close(hybrid.length-hybrid.along,0.0)
    // Faster travel produces earlier announcements, bounded for noisy speed fixes.
    check(RouteProgress.advanceDistance(25.0,true)>RouteProgress.advanceDistance(8.0,true))
    check(RouteProgress.turnDistance(1.4,false)<RouteProgress.turnDistance(20.0,true))
    close(RouteProgress.advanceDistance(500.0,true),650.0)
    // Longitude scaling at high latitudes is handled by projection.
    val north=RouteProgress(listOf(RouteProgress.Point(70.0,20.0),RouteProgress.Point(70.0,20.01)))
    close(north.project(RouteProgress.Point(70.0,20.005)).along,north.length/2)
    println("PASS: projection, noisy/backwards fixes, recovery, loops, duplicate points, hybrid distance, speed thresholds, latitude scaling")
}

}
