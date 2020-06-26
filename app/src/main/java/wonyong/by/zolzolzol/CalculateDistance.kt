package wonyong.by.zolzolzol

import kotlin.math.roundToInt

class CalculateDistance {
    fun distance(lat1: Double, logt1: Double, lat2: Double, logt2: Double): Double{
        var theta = logt1 - logt2
        var dist = Math.sin(deg2rad(lat1))*Math.sin(deg2rad(lat2)) +
                + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) *
                Math.cos(deg2rad(theta))

        dist =  Math.acos(dist)
        dist = rad2deg(dist)
        dist = dist * 60 * 1.1515

        dist = dist * 1609.344

        return dist

    }

    fun deg2rad(deg: Double):Double{
        return (deg * Math.PI / 180.0)
    }

    fun rad2deg(rad: Double):Double{
        return (rad*180 / Math.PI)
    }

    fun timeTake(distance: Int): Int{
        return (distance/66.67).toFloat().roundToInt()
    }
}