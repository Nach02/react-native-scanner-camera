package com.zxingscanner

import android.content.Context
import android.graphics.SurfaceTexture
import android.util.Base64
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.FrameLayout
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactContext
import com.facebook.react.uimanager.events.RCTEventEmitter
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraView(context: Context) : FrameLayout(context), LifecycleOwner, TextureView.SurfaceTextureListener {

    private val lifecycleRegistry = LifecycleRegistry(this)
    override fun getLifecycle(): Lifecycle = lifecycleRegistry

    private val textureView = TextureView(context)
    private var readySurface: SurfaceTexture? = null

    private var cameraExecutor: ExecutorService? = null
    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraStarted = false

    init {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        textureView.surfaceTextureListener = this
        addView(textureView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        cameraExecutor = Executors.newSingleThreadExecutor()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val w = right - left
        val h = bottom - top
        textureView.measure(
            View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY),
        )
        textureView.layout(0, 0, w, h)
    }

    override fun onDetachedFromWindow() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        super.onDetachedFromWindow()
        cameraProvider?.unbindAll()
        cameraProvider = null
        cameraStarted = false
        readySurface = null
        imageCapture = null
        cameraExecutor?.shutdown()
        cameraExecutor = null
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        readySurface = surface
        if (!cameraStarted) startCamera()
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        // Stop camera BEFORE surface is released — prevents emulator virtual camera corruption
        cameraProvider?.unbindAll()
        cameraProvider = null
        readySurface = null
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}

    private fun startCamera() {
        cameraStarted = true
        val executor = cameraExecutor ?: return
        val st = readySurface ?: return

        ProcessCameraProvider.getInstance(context).also { future ->
            future.addListener({
                val provider = future.get()
                cameraProvider = provider

                val preview = Preview.Builder().build()
                preview.setSurfaceProvider { request ->
                    st.setDefaultBufferSize(request.resolution.width, request.resolution.height)
                    val surface = Surface(st)
                    request.provideSurface(surface, ContextCompat.getMainExecutor(context)) {
                        surface.release()
                    }
                }

                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                imageCapture = capture

                try {
                    provider.unbindAll()
                    provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
                    emitEvent("onReady", Arguments.createMap())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(context))
        }
    }

    fun takePhoto() {
        val capture = imageCapture ?: return
        val executor = cameraExecutor ?: return

        val file = File(context.cacheDir, "scanner_photo_${System.currentTimeMillis()}.jpg")
        val options = ImageCapture.OutputFileOptions.Builder(file).build()

        capture.takePicture(options, executor, object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                try {
                    val bytes = file.readBytes()
                    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    val event = Arguments.createMap().apply {
                        putString("uri", "file://${file.absolutePath}")
                        putString("base64", base64)
                    }
                    emitEvent("onPhotoTaken", event)
                } catch (e: Exception) {
                    emitEvent("onPhotoError", Arguments.createMap().apply {
                        putString("message", e.message ?: "Failed to read photo")
                    })
                }
            }

            override fun onError(e: ImageCaptureException) {
                emitEvent("onPhotoError", Arguments.createMap().apply {
                    putString("message", e.message ?: "Photo capture failed")
                })
            }
        })
    }

    private fun emitEvent(name: String, payload: com.facebook.react.bridge.WritableMap) {
        val reactContext = context as? ReactContext ?: return
        reactContext.getJSModule(RCTEventEmitter::class.java).receiveEvent(id, name, payload)
    }
}
