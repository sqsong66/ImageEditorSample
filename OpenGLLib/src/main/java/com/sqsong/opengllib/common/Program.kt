package com.sqsong.opengllib.common

import android.opengl.GLES30
import com.sqsong.opengllib.utils.checkGLError
import com.sqsong.opengllib.utils.loadShader

class Program private constructor(
    vertexShaderCode: String,
    fragmentShaderCode: String
) {

    private var programId: Int = GLES30.GL_NONE

    init {
        create(vertexShaderCode, fragmentShaderCode)
    }

    private fun create(vertexShaderCode: String, fragmentShaderCode: String) {
        var vertexShaderId = GLES30.GL_NONE
        var fragmentShaderId = GLES30.GL_NONE
        try {
            vertexShaderId = loadShader(GLES30.GL_VERTEX_SHADER, vertexShaderCode)
            fragmentShaderId = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderCode)
            programId = GLES30.glCreateProgram()
            checkGLError("glCreateProgram")
            if (programId != GLES30.GL_NONE) {
                GLES30.glAttachShader(programId, vertexShaderId)
                GLES30.glAttachShader(programId, fragmentShaderId)
                checkGLError("glAttachShader")
                GLES30.glLinkProgram(programId)
                checkGLError("glLinkProgram")
                val linkStatus = IntArray(1)
                GLES30.glGetProgramiv(programId, GLES30.GL_LINK_STATUS, linkStatus, 0)
                if (linkStatus[0] == 0) {
                    throw RuntimeException("Error linking program: ${GLES30.glGetProgramInfoLog(programId)}")
                }
            }
        } finally {
            GLES30.glDetachShader(programId, vertexShaderId)
            GLES30.glDetachShader(programId, fragmentShaderId)
            GLES30.glDeleteShader(vertexShaderId)
            GLES30.glDeleteShader(fragmentShaderId)
        }
    }

    fun use() {
        GLES30.glUseProgram(programId)
    }

    fun delete() {
        if (programId != GLES30.GL_NONE) {
            GLES30.glDeleteProgram(programId)
        }
        GLES30.glUseProgram(GLES30.GL_NONE)
    }

    fun getUniformLocation(attributeName: String): Int {
        return GLES30.glGetUniformLocation(programId, attributeName)
    }

    override fun toString(): String {
        return "Program(programId=$programId)"
    }

    companion object {
        fun of(vertexShaderCode: String, fragmentShaderCode: String): Program {
            return Program(vertexShaderCode, fragmentShaderCode)
        }
    }
}