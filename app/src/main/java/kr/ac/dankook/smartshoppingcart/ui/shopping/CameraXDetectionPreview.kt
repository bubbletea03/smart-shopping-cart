package kr.ac.dankook.smartshoppingcart.ui.shopping

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kr.ac.dankook.smartshoppingcart.detection.DetectionResult
import kr.ac.dankook.smartshoppingcart.detection.YoloObjectDetector
import java.util.concurrent.Executors

@Composable
fun CameraXDetectionPreview(
    labels: List<String>,
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

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )

    DisposableEffect(detector, cameraExecutor) {
        onDispose {
            detector.close()
            cameraExecutor.shutdown()
        }
    }

    DisposableEffect(lifecycleOwner, labels, detector) {
        var lastAnalyzedAt = 0L
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener(
            {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder()
                    .build()
                    .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { imageAnalysis ->
                        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            try {
                                val now = System.currentTimeMillis()
                                if (now - lastAnalyzedAt >= ANALYSIS_INTERVAL_MS) {
                                    lastAnalyzedAt = now
                                    val detections = detector.detect(imageProxy)
                                    mainExecutor.execute {
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

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
            },
            mainExecutor
        )

        onDispose {
            if (cameraProviderFuture.isDone) {
                cameraProviderFuture.get().unbindAll()
            }
        }
    }
}

private const val ANALYSIS_INTERVAL_MS = 500L
private const val TAG = "CameraXDetectionPreview"
