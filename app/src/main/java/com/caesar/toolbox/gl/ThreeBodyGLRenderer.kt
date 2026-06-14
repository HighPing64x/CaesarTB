package com.caesar.toolbox.gl

import android.opengl.GLES20
import android.opengl.Matrix
import android.opengl.GLSurfaceView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ThreeBodyGLRenderer : GLSurfaceView.Renderer {
    private var width = 1
    private var height = 1
    private val lock = Any()
    // pending body data in world coords: list of (x,y,r,g,b,size)
    private var pending: FloatArray? = null

    // Camera / projection
    private val viewMatrix = FloatArray(16)
    private val projMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private var cameraAzimuth = 0f
    private var cameraElevation = 20f
    private var cameraDistance = 800f
    private var cameraFocusX = 0f
    private var cameraFocusY = 0f

    fun setCamera(azimuth: Float, elevation: Float, distance: Float, focusX: Float, focusY: Float) {
        synchronized(lock) {
            cameraAzimuth = azimuth
            cameraElevation = elevation
            cameraDistance = distance
            cameraFocusX = focusX
            cameraFocusY = focusY
        }
    }

    fun setPendingBodies(data: FloatArray) {
        synchronized(lock) { pending = data }
    }

    private var program = 0
    private var aPos = 0
    private var aColor = 0
    private var uPointSize = 0
    private var uMVP = 0

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.06f, 0.06f, 0.06f, 1f)
        val vs = """
            attribute vec3 a_pos;
            attribute vec3 a_color;
            varying vec3 v_color;
            uniform float u_pointSize;
            uniform mat4 u_mvp;
            void main() {
                gl_Position = u_mvp * vec4(a_pos, 1.0);
                gl_PointSize = u_pointSize;
                v_color = a_color;
            }
        """.trimIndent()
        val fs = """
            precision mediump float;
            varying vec3 v_color;
            void main() {
                gl_FragColor = vec4(v_color, 1.0);
            }
        """.trimIndent()
        program = buildProgram(vs, fs)
        aPos = GLES20.glGetAttribLocation(program, "a_pos")
        aColor = GLES20.glGetAttribLocation(program, "a_color")
        uPointSize = GLES20.glGetUniformLocation(program, "u_pointSize")
        uMVP = GLES20.glGetUniformLocation(program, "u_mvp")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        this.width = if (width == 0) 1 else width
        this.height = if (height == 0) 1 else height
        GLES20.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / height.toFloat()
        Matrix.perspectiveM(projMatrix, 0, 45f, aspect, 1f, 5000f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        val data: FloatArray?
        synchronized(lock) { data = pending }
        if (data == null || data.isEmpty()) return

        // data layout: x,y,r,g,b,size repeated; x/y in world coords (z=0)
        val count = data.size / 6
        val arr = FloatArray(count * 6) // x,y,z,r,g,b
        for (i in 0 until count) {
            val x = data[i * 6]
            val y = data[i * 6 + 1]
            val r = data[i * 6 + 2]
            val g = data[i * 6 + 3]
            val b = data[i * 6 + 4]
            // size is at data[i*6+5]
            arr[i * 6] = x
            arr[i * 6 + 1] = y
            arr[i * 6 + 2] = 0f
            arr[i * 6 + 3] = r
            arr[i * 6 + 4] = g
            arr[i * 6 + 5] = b
        }

        val fb: FloatBuffer = ByteBuffer.allocateDirect(arr.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        fb.put(arr).position(0)

        GLES20.glUseProgram(program)
        GLES20.glEnableVertexAttribArray(aPos)
        GLES20.glEnableVertexAttribArray(aColor)

        // positions are three floats, colors are three floats interleaved
        fb.position(0)
        GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, 6 * 4, fb)
        fb.position(3)
        GLES20.glVertexAttribPointer(aColor, 3, GLES20.GL_FLOAT, false, 6 * 4, fb)

        // set point size based on first body's size if exists
        val firstSize = if (data.size >= 6) data[5] else 8f
        GLES20.glUniform1f(uPointSize, firstSize)

        // compute camera/view mvp
        val radA = Math.toRadians(cameraAzimuth.toDouble()).toFloat()
        val radE = Math.toRadians(cameraElevation.toDouble()).toFloat()
        val ex = cameraFocusX + cameraDistance * cos(radE) * sin(radA)
        val ey = cameraFocusY + cameraDistance * sin(radE)
        val ez = cameraDistance * cos(radE) * cos(radA)
        Matrix.setLookAtM(viewMatrix, 0, ex, ey, ez, cameraFocusX, cameraFocusY, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(mvpMatrix, 0, projMatrix, 0, viewMatrix, 0)
        GLES20.glUniformMatrix4fv(uMVP, 1, false, mvpMatrix, 0)

        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, count)

        GLES20.glDisableVertexAttribArray(aPos)
        GLES20.glDisableVertexAttribArray(aColor)
    }

    private fun buildProgram(vsSource: String, fsSource: String): Int {
        val vs = loadShader(GLES20.GL_VERTEX_SHADER, vsSource)
        val fs = loadShader(GLES20.GL_FRAGMENT_SHADER, fsSource)
        val prog = GLES20.glCreateProgram()
        GLES20.glAttachShader(prog, vs)
        GLES20.glAttachShader(prog, fs)
        GLES20.glLinkProgram(prog)
        return prog
    }

    private fun loadShader(type: Int, src: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, src)
        GLES20.glCompileShader(shader)
        return shader
    }
}
