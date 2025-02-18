package com.sqsong.opengllib.utils

import android.app.ActivityManager
import android.content.Context
import android.opengl.GLES20
import android.opengl.GLES30
import android.util.Log

fun checkGLError(tag: String) {
    GLES30.glGetError().let {
        if (it != GLES30.GL_NO_ERROR) {
            throw RuntimeException("$tag: glError $it")
        }
    }
}

fun loadShader(type: Int, shaderCode: String): Int {
    return GLES30.glCreateShader(type).also { shader ->
        GLES30.glShaderSource(shader, shaderCode)
        checkGLError("glShaderSource")
        GLES30.glCompileShader(shader)
        checkGLError("glCompileShader")
        val compileStatus = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            throw RuntimeException("Error compiling shader: ${GLES30.glGetShaderInfoLog(shader)}, type: $type")
        }
    }
}

fun getDeviceOpenGLVersion(context: Context): String {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val configurationInfo = activityManager.deviceConfigurationInfo
    return configurationInfo.glEsVersion
}

fun isOpenGL30Supported(context: Context): Boolean {
    // 通过 ActivityManager 获取设备的 OpenGL ES 版本
    val configurationInfo = (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).deviceConfigurationInfo
    // 检查设备支持的 OpenGL ES 版本是否大于或等于 3.0
    return if (configurationInfo.reqGlEsVersion >= 0x30000) {
        true
    } else {
        try {
            val versionString = GLES20.glGetString(GLES20.GL_VERSION)
            if (versionString != null && versionString.contains("OpenGL ES 3.")) {
                true
            } else {
                Log.e("OpenGLSupport", "Device does not support OpenGL ES 3.0")
                false
            }
        } catch (e: Exception) {
            Log.e("OpenGLSupport", "Error checking OpenGL version", e)
            false
        }
    }
}
