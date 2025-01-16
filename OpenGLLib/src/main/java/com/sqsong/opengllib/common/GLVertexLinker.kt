package com.sqsong.opengllib.common

import android.opengl.GLES30
import android.util.Log
import com.sqsong.opengllib.utils.checkGLError
import java.nio.ByteBuffer
import java.nio.ByteOrder

class GLVertexLinker(
    private val vertices: FloatArray,
    private val indices: ShortArray,
    private val vertexStride: Int
) {

    private val vaoId = IntArray(1)
    private val vboIds = IntArray(1)
    private val eboId = IntArray(1)

    fun setupVertices() {
        GLES30.glGenVertexArrays(1, vaoId, 0)
        checkGLError("glGenVertexArrays")
        GLES30.glBindVertexArray(vaoId[0])

        GLES30.glGenBuffers(1, vboIds, 0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboIds[0])
        val vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(vertices)
                position(0)
            }
        }
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertexBuffer.capacity() * 4, vertexBuffer, GLES30.GL_STATIC_DRAW)

        GLES30.glGenBuffers(1, eboId, 0)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, eboId[0])
        val indexBuffer = ByteBuffer.allocateDirect(indices.size * 2).run {
            order(ByteOrder.nativeOrder())
            asShortBuffer().apply {
                put(indices)
                position(0)
            }
        }
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, indexBuffer.capacity() * 2, indexBuffer, GLES30.GL_STATIC_DRAW)

        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, vertexStride, 0)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, vertexStride, 12)
        GLES30.glEnableVertexAttribArray(1)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
        GLES30.glBindVertexArray(GLES30.GL_NONE)
    }

    fun draw() {
        GLES30.glBindVertexArray(vaoId[0])
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, indices.size, GLES30.GL_UNSIGNED_SHORT, 0)
        GLES30.glBindVertexArray(GLES30.GL_NONE)
    }

    fun cleanup() {
        GLES30.glBindVertexArray(GLES30.GL_NONE)
        GLES30.glDeleteVertexArrays(1, vaoId, 0)
        GLES30.glDeleteBuffers(1, vboIds, 0)
        GLES30.glDeleteBuffers(1, eboId, 0)
    }

}