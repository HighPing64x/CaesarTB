package com.caesar.toolbox.gl

import android.content.Context
import android.opengl.GLSurfaceView

class ThreeBodyGLSurfaceView(context: Context) : GLSurfaceView(context) {
    val renderer: ThreeBodyGLRenderer

    init {
        setEGLContextClientVersion(2)
        renderer = ThreeBodyGLRenderer()
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    fun updateBodiesScreen(data: FloatArray) {
        // queue update on GL thread
        queueEvent { renderer.setPendingBodies(data) }
        requestRender()
    }

    fun setCamera(azimuth: Float, elevation: Float, distance: Float, focusX: Float, focusY: Float) {
        queueEvent {
            renderer.setCamera(azimuth, elevation, distance, focusX, focusY)
        }
        requestRender()
    }
}
