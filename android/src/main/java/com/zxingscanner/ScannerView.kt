package com.zxingscanner

import android.content.Context
import android.graphics.SurfaceTexture
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.FrameLayout
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactContext
import com.facebook.react.uimanager.events.RCTEventEmitter
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScannerView(context: Context) : FrameLayout(context), LifecycleOwner, TextureView.SurfaceTextureListener {

    private val lifecycleRegistry = LifecycleRegistry(this)
    override fun getLifecycle(): Lifecycle = lifecycleRegistry

    private val textureView = TextureView(context)
    private var readySurface: SurfaceTexture? = null

    private var cameraExecutor: ExecutorService? = null
    private val reader = MultiFormatReader()
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraStarted = false
    private var lastScannedMs = 0L

    var scanAreaFraction: Float = 1.0f

    var codeTypes: List<BarcodeFormat> = listOf(
        BarcodeFormat.QR_CODE,
        BarcodeFormat.CODE_128,
        BarcodeFormat.CODE_39,
    )
        set(value) {
            field = value
            reader.setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to value))
        }

    init {
        Log.d("Scanner", "init")
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        textureView.surfaceTextureListener = this
        addView(textureView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        reader.setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to codeTypes))
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.d("Scanner", "onAttachedToWindow")
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
        Log.d("Scanner", "onLayout ${w}x${h}")
    }

    override fun onDetachedFromWindow() {
        Log.d("Scanner", "onDetachedFromWindow")
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        super.onDetachedFromWindow()
        cameraProvider?.unbindAll()
        cameraProvider = null
        cameraStarted = false
        readySurface = null
        cameraExecutor?.shutdown()
        cameraExecutor = null
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        Log.d("Scanner", "onSurfaceTextureAvailable ${width}x${height}")
        readySurface = surface
        if (!cameraStarted && cameraExecutor != null) startCamera()
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        Log.d("Scanner", "onSurfaceTextureDestroyed")
        // Stop camera BEFORE surface is released — prevents emulator virtual camera corruption
        cameraProvider?.unbindAll()
        cameraProvider = null
        readySurface = null
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}

    private fun startCamera() {
        cameraStarted = true
        Log.d("Scanner", "startCamera")
        val executor = cameraExecutor ?: return
        val st = readySurface ?: return

        ProcessCameraProvider.getInstance(context).also { future ->
            future.addListener({
                Log.d("Scanner", "ProcessCameraProvider ready")
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

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(executor, ::analyzeFrame) }

                try {
                    provider.unbindAll()
                    provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                    Log.d("Scanner", "bindToLifecycle SUCCESS")
                } catch (e: Exception) {
                    Log.e("Scanner", "bindToLifecycle FAILED: ${e.message}", e)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    }

    private fun analyzeFrame(imageProxy: ImageProxy) {
        val now = System.currentTimeMillis()
        if (now - lastScannedMs < 300) {
            imageProxy.close()
            return
        }

        try {
            val yPlane = imageProxy.planes[0]
            val buffer = yPlane.buffer
            val rowStride = yPlane.rowStride
            val width = imageProxy.width
            val height = imageProxy.height

            val yBytes = if (rowStride == width) {
                ByteArray(buffer.remaining()).also { buffer.get(it) }
            } else {
                ByteArray(width * height).also { dest ->
                    for (row in 0 until height) {
                        buffer.position(row * rowStride)
                        buffer.get(dest, row * width, width)
                    }
                }
            }

            val result = decode(yBytes, width, height)
            if (result != null) {
                lastScannedMs = now
                emitScanEvent(result)
            }
        } catch (e: Exception) {
            // ignore frame errors
        } finally {
            imageProxy.close()
        }
    }

    private fun decode(yBytes: ByteArray, width: Int, height: Int): String? {
        tryDecode(yBytes, width, height, invert = false)?.let { return it }
        tryDecode(yBytes, width, height, invert = true)?.let { return it }
        val rotated = rotateYBytes(yBytes, width, height)
        tryDecode(rotated, height, width, invert = false)?.let { return it }
        tryDecode(rotated, height, width, invert = true)?.let { return it }
        return null
    }

    private fun tryDecode(data: ByteArray, width: Int, height: Int, invert: Boolean): String? {
        val fraction = scanAreaFraction.coerceIn(0.1f, 1.0f)
        val cropW = (width * fraction).toInt().coerceAtLeast(10)
        val cropH = (height * fraction).toInt().coerceAtLeast(10)
        val left = (width - cropW) / 2
        val top = (height - cropH) / 2
        return try {
            val source = PlanarYUVLuminanceSource(data, width, height, left, top, cropW, cropH, false)
            val luminance = if (invert) source.invert() else source
            reader.decodeWithState(BinaryBitmap(HybridBinarizer(luminance))).text
        } catch (e: NotFoundException) {
            null
        } finally {
            reader.reset()
        }
    }

    private fun rotateYBytes(data: ByteArray, width: Int, height: Int): ByteArray {
        val rotated = ByteArray(data.size)
        for (y in 0 until height) {
            for (x in 0 until width) {
                rotated[x * height + (height - y - 1)] = data[y * width + x]
            }
        }
        return rotated
    }

    private fun emitScanEvent(value: String) {
        val reactContext = context as? ReactContext ?: return
        val event = Arguments.createMap().apply { putString("value", value) }
        reactContext.getJSModule(RCTEventEmitter::class.java).receiveEvent(id, "onScan", event)
    }
}
