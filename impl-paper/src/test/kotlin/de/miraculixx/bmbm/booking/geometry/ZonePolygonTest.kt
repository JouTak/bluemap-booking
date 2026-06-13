package de.miraculixx.bmbm.booking.geometry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ZonePolygonTest {
    private fun p(x: Int, z: Int) = Point2(x.toDouble(), z.toDouble())

    @Test
    fun `square walk keeps all corners`() {
        val polygon = ZonePolygon.build(listOf(p(0, 0), p(10, 0), p(10, 10), p(0, 10)))
        assertEquals(4, polygon.size)
        assertEquals(100.0, ZonePolygon.area(polygon))
    }

    @Test
    fun `concave L-shape walk keeps walk order`() {
        val walk = listOf(p(0, 0), p(4, 0), p(4, 2), p(2, 2), p(2, 4), p(0, 4))
        val polygon = ZonePolygon.build(walk)
        assertEquals(6, polygon.size)
        assertEquals(12.0, ZonePolygon.area(polygon))
    }

    @Test
    fun `point near an edge refines that edge`() {
        val polygon = ZonePolygon.build(listOf(p(0, 0), p(10, 0), p(10, 10), p(0, 10), p(5, -3)))
        assertEquals(5, polygon.size)
        assertEquals(115.0, ZonePolygon.area(polygon))
        val bottom = polygon.indexOf(p(5, -3))
        val neighbors = setOf(polygon[(bottom + 1) % 5], polygon[(bottom + 4) % 5])
        assertEquals(setOf(p(0, 0), p(10, 0)), neighbors)
    }

    @Test
    fun `duplicate points are ignored`() {
        val polygon = ZonePolygon.build(listOf(p(0, 0), p(10, 0), p(10, 10), p(10, 0), p(0, 0)))
        assertEquals(3, polygon.size)
    }

    @Test
    fun `collinear points stay degenerate without crash`() {
        val polygon = ZonePolygon.build(listOf(p(0, 0), p(5, 0), p(10, 0)))
        assertEquals(3, polygon.size)
        assertEquals(0.0, ZonePolygon.area(polygon))
    }

    @Test
    fun `insertion does not create self-intersection`() {
        val walk = listOf(p(0, 0), p(20, 0), p(20, 20), p(0, 20), p(10, 5), p(10, 18))
        val polygon = ZonePolygon.build(walk)
        assertEquals(6, polygon.size)
        assertTrue(isSimple(polygon), "polygon must stay simple: $polygon")
    }

    @Test
    fun `rectangle from two corners`() {
        val rect = ZonePolygon.rectangle(p(7, 3), p(2, 9))
        assertEquals(listOf(p(2, 3), p(7, 3), p(7, 9), p(2, 9)), rect)
        assertEquals(30.0, ZonePolygon.area(rect))
    }

    private fun isSimple(polygon: List<Point2>): Boolean {
        val n = polygon.size
        for (i in 0 until n) {
            for (j in i + 1 until n) {
                val a = polygon[i]
                val b = polygon[(i + 1) % n]
                val c = polygon[j]
                val d = polygon[(j + 1) % n]
                if (a == c || a == d || b == c || b == d) continue
                val d1 = cross(c, d, a)
                val d2 = cross(c, d, b)
                val d3 = cross(a, b, c)
                val d4 = cross(a, b, d)
                if (((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0)) && ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0))) return false
            }
        }
        return true
    }

    private fun cross(a: Point2, b: Point2, c: Point2): Double =
        (b.x - a.x) * (c.z - a.z) - (b.z - a.z) * (c.x - a.x)
}
