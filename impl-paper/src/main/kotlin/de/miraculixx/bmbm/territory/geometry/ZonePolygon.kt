package de.miraculixx.bmbm.territory.geometry

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

data class Point2(val x: Double, val z: Double)

object ZonePolygon {

    /**
     * Builds a simple polygon from points in placement order: each point after the
     * third is inserted into the nearest edge that keeps the polygon free of
     * self-intersections, so banners placed sequentially connect sequentially and
     * a badly connected edge can be fixed by placing another banner near it.
     */
    fun build(points: List<Point2>): List<Point2> {
        val unique = points.distinct()
        if (unique.size <= 3) return unique
        val placementOrder = unique.withIndex().associate { (index, point) -> point to index }
        val polygon = unique.take(3).toMutableList()
        unique.drop(3).forEach { insert(polygon, placementOrder, it) }
        return polygon
    }

    fun rectangle(a: Point2, b: Point2): List<Point2> {
        val minX = min(a.x, b.x)
        val maxX = max(a.x, b.x)
        val minZ = min(a.z, b.z)
        val maxZ = max(a.z, b.z)
        return listOf(Point2(minX, minZ), Point2(maxX, minZ), Point2(maxX, maxZ), Point2(minX, maxZ))
    }

    fun area(polygon: List<Point2>): Double {
        if (polygon.size < 3) return 0.0
        var sum = 0.0
        polygon.forEachIndexed { i, p ->
            val q = polygon[(i + 1) % polygon.size]
            sum += p.x * q.z - q.x * p.z
        }
        return abs(sum) / 2.0
    }

    // On near-equal distances the edge starting at the most recently placed banner wins,
    // so a player walking the perimeter gets banners connected in walk order.
    private fun insert(polygon: MutableList<Point2>, placementOrder: Map<Point2, Int>, point: Point2) {
        val edgesByDistance = polygon.indices.sortedWith(
            compareBy<Int> { (distanceToEdge(polygon, it, point) * 10000).toLong() }
                .thenByDescending { placementOrder.getValue(polygon[it]) }
        )
        val edge = edgesByDistance.firstOrNull { fitsEdge(polygon, it, point) } ?: edgesByDistance.first()
        polygon.add(edge + 1, point)
    }

    private fun distanceToEdge(polygon: List<Point2>, edge: Int, point: Point2): Double {
        val a = polygon[edge]
        val b = polygon[(edge + 1) % polygon.size]
        val dx = b.x - a.x
        val dz = b.z - a.z
        val lengthSquared = dx * dx + dz * dz
        if (lengthSquared == 0.0) return hypot(point.x - a.x, point.z - a.z)
        val t = (((point.x - a.x) * dx + (point.z - a.z) * dz) / lengthSquared).coerceIn(0.0, 1.0)
        return hypot(point.x - (a.x + t * dx), point.z - (a.z + t * dz))
    }

    private fun fitsEdge(polygon: List<Point2>, edge: Int, point: Point2): Boolean {
        val a = polygon[edge]
        val b = polygon[(edge + 1) % polygon.size]
        polygon.indices.forEach { i ->
            val c = polygon[i]
            val d = polygon[(i + 1) % polygon.size]
            if (properlyIntersect(a, point, c, d) || properlyIntersect(point, b, c, d)) return false
        }
        return true
    }

    private fun properlyIntersect(p1: Point2, p2: Point2, p3: Point2, p4: Point2): Boolean {
        val d1 = cross(p3, p4, p1)
        val d2 = cross(p3, p4, p2)
        val d3 = cross(p1, p2, p3)
        val d4 = cross(p1, p2, p4)
        return ((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0)) && ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0))
    }

    private fun cross(a: Point2, b: Point2, c: Point2): Double =
        (b.x - a.x) * (c.z - a.z) - (b.z - a.z) * (c.x - a.x)
}
