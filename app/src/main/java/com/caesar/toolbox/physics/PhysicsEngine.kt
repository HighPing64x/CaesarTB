package com.caesar.toolbox.physics

import kotlin.math.*
import kotlinx.coroutines.*

/**
 * Simple fixed-step physics engine for N-body simulation.
 * Runs on a background coroutine and invokes a tick callback with a snapshot.
 */
class PhysicsEngine(private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) {
    data class Body(var x: Float, var y: Float, var vx: Float, var vy: Float, val mass: Float)

    private val lock = Any()
    private var bodies = mutableListOf<Body>()
    private var job: Job? = null
    private var running = false
    private var paused = false
    private var baseDt = 0.016f
    private var speed = 1f
    private var onTick: ((List<Body>)->Unit)? = null
    private var onCollision: (() -> Unit)? = null

    fun setOnTickListener(cb: (List<Body>)->Unit) { onTick = cb }
    fun setOnCollision(cb: ()->Unit) { onCollision = cb }

    fun setSpeedMultiplier(v: Float) { speed = v.coerceAtLeast(0.001f) }
    fun isRunning() = running

    fun setBodies(list: List<Body>) {
        synchronized(lock) {
            bodies = list.map { Body(it.x, it.y, it.vx, it.vy, it.mass) }.toMutableList()
        }
    }

    fun getSnapshot(): List<Body> = synchronized(lock) { bodies.map { Body(it.x, it.y, it.vx, it.vy, it.mass) } }

    fun start() {
        if (running) return
        running = true; paused = false
        job = scope.launch {
            while (running) {
                if (!paused) step(baseDt * speed)
                onTick?.invoke(getSnapshot())
                delay(16)
            }
        }
    }

    fun stop() {
        running = false; job?.cancel(); job = null
    }

    fun pause() { paused = true }
    fun resume() { paused = false }
    fun singleStep() { if (!running) { step(baseDt * speed); onTick?.invoke(getSnapshot()) } else { step(baseDt * speed); onTick?.invoke(getSnapshot()) } }

    fun reset() { synchronized(lock) { bodies.clear() } }

    private fun step(dt: Float) {
        val b = synchronized(lock) { bodies.toMutableList() }
        val n = b.size
        if (n < 2) {
            synchronized(lock) { bodies = b }
            return
        }
        val ax = FloatArray(n) { 0f }
        val ay = FloatArray(n) { 0f }
        val G = 800f
        val SOFTEN = 15f
        for (i in 0 until n) for (j in i + 1 until n) {
            val dx = b[j].x - b[i].x; val dy = b[j].y - b[i].y
            val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(SOFTEN)
            val force = G * b[i].mass * b[j].mass / (dist * dist)
            val fx = force * dx / dist; val fy = force * dy / dist
            ax[i] += (fx / b[i].mass); ay[i] += (fy / b[i].mass)
            ax[j] -= (fx / b[j].mass); ay[j] -= (fy / b[j].mass)
        }
        for (i in 0 until n) {
            val nvx = b[i].vx + ax[i] * dt
            val nvy = b[i].vy + ay[i] * dt
            val nx = b[i].x + nvx * dt
            val ny = b[i].y + nvy * dt
            b[i].vx = nvx; b[i].vy = nvy; b[i].x = nx; b[i].y = ny
        }
        // collision detection and fragment generation
        val CRASH_DIST = 30f
        val removed = BooleanArray(n)
        val newFragments = mutableListOf<Body>()
        val rng = kotlin.random.Random.Default
        for (i in 0 until n) {
            if (removed[i]) continue
            for (j in i + 1 until n) {
                if (removed[j]) continue
                val dx = b[i].x - b[j].x; val dy = b[i].y - b[j].y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist < CRASH_DIST) {
                    // collision: generate fragments at midpoint and remove both bodies
                    removed[i] = true; removed[j] = true
                    val cx = (b[i].x + b[j].x) / 2f
                    val cy = (b[i].y + b[j].y) / 2f
                    val totalMass = b[i].mass + b[j].mass
                    val num = rng.nextInt(10, 21)
                    for (k in 0 until num) {
                        val ang = rng.nextDouble(0.0, Math.PI * 2).toFloat()
                        val speed = rng.nextDouble(0.5, 6.0).toFloat()
                        val px = cx + (rng.nextDouble(-1.0, 1.0).toFloat() * 8f)
                        val py = cy + (rng.nextDouble(-1.0, 1.0).toFloat() * 8f)
                        val mv = (totalMass * (0.02f + rng.nextFloat() * 0.08f))
                        val vxF = ((cos(ang) * speed) + (b[i].vx + b[j].vx) * 0.5f)
                        val vyF = ((sin(ang) * speed) + (b[i].vy + b[j].vy) * 0.5f)
                        newFragments.add(Body(px, py, vxF, vyF, mv))
                    }
                }
            }
        }
        // build final body list
        val finalList = mutableListOf<Body>()
        for (i in 0 until n) if (!removed[i]) finalList.add(b[i])
        finalList.addAll(newFragments)
        synchronized(lock) { bodies = finalList }
        // notify collision if any occurred
        if (newFragments.isNotEmpty()) {
            onCollision?.invoke()
        }
    }
}
