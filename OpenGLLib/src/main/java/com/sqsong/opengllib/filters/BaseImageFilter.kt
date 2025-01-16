package com.sqsong.opengllib.filters

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES30
import android.opengl.Matrix
import android.util.Log
import com.sqsong.opengllib.common.FrameBuffer
import com.sqsong.opengllib.common.GLVertexLinker
import com.sqsong.opengllib.common.Program
import com.sqsong.opengllib.common.Texture
import com.sqsong.opengllib.utils.ext.readAssetsText

open class BaseImageFilter(
    private val context: Context,
    private val vertexAssets: String = "shader/no_filter_ver.vert",
    private val fragmentAssets: String = "shader/no_filter_frag.frag",
    // 是否需要将帧缓冲区中渲染的纹理输出到屏幕上，正常单一滤镜是需要将渲染结果输出到屏幕上的，
    // 但如果当前滤镜是添加到组合滤镜中去的则不需要将渲染结果输出到屏幕上，只需要在FBO中渲染
    // 当前的滤镜效果即可。最终是在组合滤镜中将所有的滤镜效果渲染到屏幕上去。
    private val initOutputBuffer: Boolean = true
) {

    private val TAG = this.javaClass.simpleName

    private var viewWidth = 0
    private var viewHeight = 0
    private var inputTextureWidth = 0
    private var inputTextureHeight = 0
    private var isInitialized = false
    private val mvpMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)

    private val tempMatrix = FloatArray(16)
    private val baseMatrix = FloatArray(16)
    private val userMatrix by lazy { FloatArray(16) }
    private val deltaMatrix by lazy { FloatArray(16) }

    private val vertTexCoords = floatArrayOf(
        -1f, 1f, 0.0f, 0.0f, 0.0f,  // top left
        -1f, -1f, 0.0f, 0.0f, 1.0f,  // bottom left
        1f, -1f, 0.0f, 1.0f, 1.0f,  // bottom right
        1f, 1f, 0.0f, 1.0f, 0.0f, // top right
    )

    private val fboVertTexCoords = floatArrayOf(
        -1f, 1f, 0.0f, 0.0f, 1.0f,  // top left
        -1f, -1f, 0.0f, 0.0f, 0.0f,  // bottom left
        1f, -1f, 0.0f, 1.0f, 0.0f,  // bottom right
        1f, 1f, 0.0f, 1.0f, 1.0f, // top right
    )

    private val vertIndices = shortArrayOf(0, 1, 2, 0, 2, 3)

    // 屏幕着色器程序
    private var screenProgram: Program? = null

    // 屏幕着色器顶点数据链接器
    private val screenVertexLinker by lazy {
        GLVertexLinker(vertTexCoords, vertIndices, 5 * 4)
    }

    // 离屏渲染着色器程序
    private var fboProgram: Program? = null

    // 离屏渲染帧缓冲区
    private var frameBuffer: FrameBuffer? = null

    // 离屏渲染顶点数据链接器
    private val fboVertexLinker by lazy {
        GLVertexLinker(fboVertTexCoords, vertIndices, 5 * 4)
    }

    /**
     * 初始化。主要是在GLSurfaceView.Renderer的onSurfaceCreated()方法中调用,
     * 因为这是OpenGL的环境已创建好。
     */
    fun ifNeedInit() {
        if (!isInitialized) onInit()
    }

    private fun onInit() {
        if (initOutputBuffer) initScreenParams()
        fboVertexLinker.setupVertices()
        val fboVertexShader = context.readAssetsText(vertexAssets)
        val fboFragmentShader = context.readAssetsText(fragmentAssets)
        fboProgram = Program.of(fboVertexShader, fboFragmentShader).apply {
            onInitialized(this)
        }
        isInitialized = true
    }

    private fun initScreenParams() {
        // Initialize matrices
        Matrix.setIdentityM(userMatrix, 0)
        Matrix.setIdentityM(viewMatrix, 0)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.setIdentityM(projectionMatrix, 0)
        Matrix.orthoM(projectionMatrix, 0, -1.0f, 1.0f, -1.0f, 1.0f, -1.0f, 1.0f)
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f)

        screenVertexLinker.setupVertices()
        val vertexShader = context.readAssetsText("shader/base_filter_ver.vert")
        val fragmentShader = context.readAssetsText("shader/base_filter_frag.frag")
        screenProgram = Program.of(vertexShader, fragmentShader)
    }

    protected open fun onInitialized(program: Program) {}

    open fun onInputTextureLoaded(textureWidth: Int, textureHeight: Int) {
        inputTextureWidth = textureWidth
        inputTextureHeight = textureHeight
        frameBuffer?.delete()
        frameBuffer = null
        frameBuffer = FrameBuffer(textureWidth, textureHeight)
        initBaseMatrix()
    }

    open fun onBeforeFrameBufferDraw(inputTexture: Texture, fboProgram: Program?, defaultFboGLVertexLinker: GLVertexLinker): Texture? {
        return inputTexture
    }

    /**
     * 在离屏缓冲区绘制前的操作，如设置shader中的uniform变量等参数
     */
    protected open fun onPreDraw(program: Program, texture: Texture) {}

    /**
     * 在离屏缓冲区绘制后的操作，如解绑纹理等
     */
    protected open fun onAfterDraw() {}

    open fun setProgress(progress: Float, extraType: Int = 0) {}

    fun onViewSizeChanged(width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
        initBaseMatrix()
    }

    open fun onDrawFrame(inputTexture: Texture) {
        // 绑定离屏缓冲区的program
        fboProgram?.use()
        // 在离屏缓冲区处理前先将纹理交给底下的子类来进行处理，比如高斯模糊的横向模糊，横向模糊完后，将处理的纹理交给
        // 当前的离屏缓冲区来进行进一步的纵向模糊处理
        onBeforeFrameBufferDraw(inputTexture, fboProgram, fboVertexLinker)?.let { processTexture ->
            fboProgram?.use()
            frameBuffer?.bindFrameBuffer()
            GLES30.glViewport(0, 0, processTexture.textureWidth, processTexture.textureHeight)
            onBindTexture(processTexture)
            // 在离屏缓冲区绘制前的操作，如设置shader中的uniform变量等参数
            fboProgram?.let { onPreDraw(it, inputTexture) }
            fboVertexLinker.draw()
            processTexture.unbindTexture()
            // 在离屏缓冲区绘制后的操作，如解绑纹理等
            onAfterDraw()
            frameBuffer?.unbindFrameBuffer()
        }

        // 离屏缓冲区处理完成后，将离屏缓冲区处理完的纹理交给默认缓冲来输出到屏幕
        if (initOutputBuffer) drawTextureOnScreen(frameBuffer?.texture)
    }

    private fun drawTextureOnScreen(texture: Texture?) {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glViewport(0, 0, viewWidth, viewHeight)
        screenProgram?.use()
        onBindScreenTexture(texture, inputTextureWidth, inputTextureHeight)
        screenVertexLinker.draw()
    }

    private fun onBindTexture(texture: Texture) {
        fboProgram?.getUniformLocation("uTexture")?.let {
            // Log.d("BaseImageFilter", "$TAG onBindTexture, viewWidth: $viewWidth, viewHeight: $viewHeight, textureId: ${texture.textureId}, uTexture location: $it")
            texture.bindTexture(it)
        }
    }

    private fun onBindScreenTexture(texture: Texture?, width: Int, height: Int) {
        // Log.d("BaseImageFilter", "$TAG onBindScreenTexture, viewWidth: $viewWidth, viewHeight: $viewHeight")
        // 进行视图变换将纹理正确的显示在屏幕(GLSurfaceView)上(根据纹理的宽高以及控件的宽高比来计算模型矩阵)
//        val textureAspectRatio = width.toFloat() / height.toFloat()
//        val viewAspectRatio = viewWidth.toFloat() / viewHeight.toFloat()
//        val destWidth: Int
//        val destHeight: Int
//        if (textureAspectRatio > viewAspectRatio) {
//            destWidth = viewWidth
//            destHeight = (viewWidth.toFloat() / textureAspectRatio).toInt()
//        } else {
//            destHeight = viewHeight
//            destWidth = (viewHeight.toFloat() * textureAspectRatio).toInt()
//        }
//        Matrix.setIdentityM(modelMatrix, 0)
//        Matrix.scaleM(modelMatrix, 0, destWidth / viewWidth.toFloat(), destHeight / viewHeight.toFloat(), 1.0f)
//
//        // 处理用户手势缩放
//        Matrix.translateM(modelMatrix, 0, userPivotX, userPivotY, 0f)
//        Matrix.scaleM(modelMatrix, 0, userScale, userScale, 1.0f)
//        Matrix.translateM(modelMatrix, 0, -userPivotX, -userPivotY, 0f)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.multiplyMM(modelMatrix, 0, baseMatrix, 0, userMatrix, 0)

        Matrix.setIdentityM(mvpMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)
        screenProgram?.getUniformLocation("uMvpMatrix")?.let {
            // Log.i("BaseImageFilter", "$TAG onBindScreenTexture, uMvpMatrix location: $it")
            GLES30.glUniformMatrix4fv(it, 1, false, mvpMatrix, 0)
        }

        // 绑定离谱渲染的纹理，显示到屏幕上
        screenProgram?.getUniformLocation("uTexture")?.let {
            // Log.i("BaseImageFilter", "$TAG onBindScreenTexture, uTexture location: $it, textureId: ${frameBuffer?.texture?.textureId}")
            texture?.textureId?.let { textureId ->
                GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
                GLES30.glUniform1i(it, 0)
            }
        }
    }

    fun fboTexture(): Texture? {
        return frameBuffer?.texture
    }

    fun getRenderedBitmap(): Bitmap? {
        return frameBuffer?.getRenderedBitmap()
    }

    protected fun range(value: Float, start: Float, end: Float): Float {
        return (end - start) * value + start
    }

    open fun onDestroy() {
        screenProgram?.delete()
        screenProgram = null
        fboProgram?.delete()
        fboProgram = null
        frameBuffer?.delete()
        frameBuffer = null
        screenVertexLinker.cleanup()
        fboVertexLinker.cleanup()
        isInitialized = false
    }

    private fun initBaseMatrix() {
        Matrix.setIdentityM(userMatrix, 0)
        Matrix.setIdentityM(baseMatrix, 0)
        if (inputTextureWidth == 0 || inputTextureHeight == 0 || viewWidth == 0 || viewHeight == 0) return
        val textureAspectRatio = inputTextureWidth.toFloat() / inputTextureHeight.toFloat()
        val viewAspectRatio = viewWidth.toFloat() / viewHeight.toFloat()
        val destWidth: Int
        val destHeight: Int
        if (textureAspectRatio > viewAspectRatio) {
            destWidth = viewWidth
            destHeight = (viewWidth.toFloat() / textureAspectRatio).toInt()
        } else {
            destHeight = viewHeight
            destWidth = (viewHeight.toFloat() * textureAspectRatio).toInt()
        }
        Matrix.scaleM(baseMatrix, 0, destWidth / viewWidth.toFloat(), destHeight / viewHeight.toFloat(), 1.0f)
    }

    /****************** 处理手势缩放 *********************/
    private fun normalizeCoordinate(x: Float, y: Float): Pair<Float, Float> {
        val width = if (viewWidth == 0) 1 else viewWidth
        val height = if (viewHeight == 0) 1 else viewHeight
        val normalizedX = (x / width) * 2 - 1
        val normalizedY = 1 - (y / height) * 2
        return normalizedX to normalizedY
    }

    fun setUserScale(scaleFactor: Float, focusX: Float, focusY: Float, tx: Float, ty: Float) {
        val (normalizedX, normalizedY) = normalizeCoordinate(focusX, focusY)
        val (normalizedTx, normalizedTy) = normalizeCoordinate(tx, ty)
        Log.d("songmao", "normalizedTx: $normalizedTx, normalizedTy: $normalizedTy, tx: $tx, ty: $ty, viewWidth: $viewWidth, viewHeight: $viewHeight")
        Matrix.setIdentityM(deltaMatrix, 0)
        Matrix.translateM(deltaMatrix, 0, normalizedX, normalizedY, 0f)
        Matrix.scaleM(deltaMatrix, 0, scaleFactor, scaleFactor, 1.0f)
        Matrix.translateM(deltaMatrix, 0, -normalizedX, -normalizedY, 0f)
        val deltaTx = (tx / viewWidth) * 2
        val deltaTy = -(ty / viewHeight) * 2
        Matrix.translateM(deltaMatrix, 0, deltaTx, deltaTy, 0f)

        Matrix.setIdentityM(tempMatrix, 0)
        Matrix.multiplyMM(tempMatrix, 0, userMatrix, 0, deltaMatrix, 0)
        System.arraycopy(tempMatrix, 0, userMatrix, 0, 16)
    }
}