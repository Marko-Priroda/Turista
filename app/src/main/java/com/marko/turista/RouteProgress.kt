package com.marko.turista

import kotlin.math.*

/** Metres along a polyline, constrained by previous progress to avoid jumping across loops. */
class RouteProgress(val points: List<Point>) {
    data class Point(val lat: Double, val lon: Double)
    data class Fix(val along: Double, val crossTrack: Double)
    val cumulative = DoubleArray(points.size)
    val length: Double get() = cumulative.lastOrNull() ?: 0.0
    var along = 0.0
        private set
    var crossTrack = Double.POSITIVE_INFINITY
        private set
    init { for (i in 1 until points.size) cumulative[i] = cumulative[i-1] + distance(points[i-1], points[i]) }
    fun update(point: Point, travelAllowance: Double = 150.0): Fix {
        val result = project(point, (along - 40).coerceAtLeast(0.0), along + travelAllowance.coerceAtLeast(60.0))
        crossTrack = result.crossTrack
        if (crossTrack < 60) along = max(along, result.along)
        return Fix(along, crossTrack)
    }
    fun project(point: Point, minimum: Double = 0.0, maximum: Double = length): Fix {
        var best = Fix(minimum, Double.POSITIVE_INFINITY)
        val scale = cos(Math.toRadians(point.lat)).coerceAtLeast(0.01)
        for (i in 0 until points.size-1) {
            if (cumulative[i+1] < minimum || cumulative[i] > maximum) continue
            val a = points[i]; val b = points[i+1]
            val dx = (b.lon-a.lon)*scale; val dy=b.lat-a.lat
            val den=dx*dx+dy*dy
            val t=if(den==0.0) 0.0 else (((point.lon-a.lon)*scale*dx+(point.lat-a.lat)*dy)/den).coerceIn(0.0,1.0)
            val candidate=Point(a.lat+t*(b.lat-a.lat),a.lon+t*(b.lon-a.lon))
            val offset=cumulative[i]+t*(cumulative[i+1]-cumulative[i])
            if (offset < minimum || offset > maximum) continue
            val error=distance(point,candidate)
            if(error < best.crossTrack - 0.5) best=Fix(offset,error)
        }
        return best
    }
    companion object {
        fun distance(a: Point,b: Point): Double {
            val x=sin(Math.toRadians(b.lat-a.lat)/2); val y=sin(Math.toRadians(b.lon-a.lon)/2)
            return 12742000.0*asin(sqrt((x*x+cos(Math.toRadians(a.lat))*cos(Math.toRadians(b.lat))*y*y).coerceIn(0.0,1.0)))
        }
        fun advanceDistance(speed: Double, car: Boolean) = if(car) (speed*14).coerceIn(100.0,650.0) else (speed*18).coerceIn(35.0,120.0)
        fun turnDistance(speed: Double, car: Boolean) = if(car) (speed*3).coerceIn(18.0,100.0) else (speed*4).coerceIn(10.0,30.0)
    }
}
