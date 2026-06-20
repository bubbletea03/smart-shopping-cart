package kr.ac.dankook.smartshoppingcart.ui.shopping

import android.util.Log
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kr.ac.dankook.smartshoppingcart.detection.DetectionResult
import kr.ac.dankook.smartshoppingcart.detection.YoloObjectDetector
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun CameraXDetectionPreview(
    labels: List<String>,
    focusLockRequestCount: Int,
    modifier: Modifier = Modifier,
    onDetections: (List<DetectionResult>) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val cameraExecutor = remember(labels) { Executors.newSingleThreadExecutor() }
    val detector = remember(labels) {
        YoloObjectDetector(
            context = context.applicationContext,
            labels = labels
        )
    }
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )

    LaunchedEffect(focusLockRequestCount, cameraControl) {
        if (focusLockRequestCount > 0) {
            cameraControl?.let { lockFocusAtCenter(previewView, it) }
        }
    }

    DisposableEffect(lifecycleOwner, labels, detector) {
        val isDisposed = AtomicBoolean(false)
        val detectorLock = Any()
        var imageAnalysis: ImageAnalysis? = null
        var cameraProvider: ProcessCameraProvider? = null
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener(
            {
                if (isDisposed.get()) return@addListener

                cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder()
                    .build()
                    .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { imageAnalysis ->
                        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            try {
                                if (isDisposed.get()) return@setAnalyzer

                                val detections = synchronized(detectorLock) {
                                    if (isDisposed.get()) {
                                        emptyList()
                                    } else {
                                        detector.detect(imageProxy)
                                    }
                                }
                                mainExecutor.execute {
                                    if (!isDisposed.get()) {
                                        onDetections(detections)
                                    }
                                }
                            } catch (exception: Exception) {
                                Log.e(TAG, "Detection failed", exception)
                            } finally {
                                imageProxy.close()
                            }
                        }
                    }

                cameraProvider?.unbindAll()
                val camera = cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
                camera?.let {
                    cameraControl = it.cameraControl
                    previewView.post {
                        lockFocusAtCenter(previewView, it.cameraControl)
                    }
                }
            },
            mainExecutor
        )

        onDispose {
            isDisposed.set(true)
            imageAnalysis?.clearAnalyzer()
            cameraControl = null
            cameraProvider?.unbindAll()
            cameraExecutor.shutdown()
            synchronized(detectorLock) {
                detector.close()
            }
        }
    }
}

private fun lockFocusAtCenter(
    previewView: PreviewView,
    cameraControl: androidx.camera.core.CameraControl
) {
    if (previewView.width == 0 || previewView.height == 0) return

    val centerPoint = previewView.meteringPointFactory.createPoint(
        previewView.width / 2f,
        previewView.height / 2f
    )
    val focusAction = FocusMeteringAction.Builder(
        centerPoint,
        FocusMeteringAction.FLAG_AF
    )
        .disableAutoCancel()
        .build()

    cameraControl.startFocusAndMetering(focusAction)
}

private const val TAG = "CameraXDetectionPreview"
