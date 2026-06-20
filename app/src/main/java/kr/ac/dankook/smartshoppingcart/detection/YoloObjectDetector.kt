package kr.ac.dankook.smartshoppingcart.detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.max

data class DetectionResult(
    val label: String,
    val confidence: Float,
    val classIndex: Int,
    val boundingBox: RectF
)

class YoloObjectDetector(
    context: Context,
    private val labels: List<String>,
    modelAssetName: String = "best_float32.tflite",
    private val confidenceThreshold: Float = 0.65f
) : AutoCloseable {
    private val interpreter = Interpreter(
        loadModel(context, modelAssetName),
        Interpreter.Options().apply {
            setNumThreads(4)
        }
    )
    private val inputTensor = interpreter.getInputTensor(0)
    private val inputShape = inputTensor.shape()
    private val inputHeight = inputShape[1]
    private val inputWidth = inputShape[2]
    private val inputDataType = inputTensor.dataType()
    private val outputShape = interpreter.getOutputTensor(0).shape()

    fun detect(imageProxy: ImageProxy): List<DetectionResult> {
        val bitmap = imageProxy.toBitmap().rotate(imageProxy.imageInfo.rotationDegrees)
        val input = bitmap.toModelInput()
        val output = Array(outputShape[0]) {
            Array(outputShape[1]) {
                FloatArray(outputShape[2])
            }
        }

        interpreter.run(input, output)
        return parseYoloOutput(output[0])
    }

    override fun close() {
        interpreter.close()
    }

    private fun Bitmap.toModelInput(): ByteBuffer {
        val resized = Bitmap.createScaledBitmap(this, inputWidth, inputHeight, true)
        val bytesPerChannel = if (inputDataType == DataType.FLOAT32) 4 else 1
        val buffer = ByteBuffer
            .allocateDirect(inputWidth * inputHeight * 3 * bytesPerChannel)
            .order(ByteOrder.nativeOrder())
        val pixels = IntArray(inputWidth * inputHeight)
        resized.getPixels(pixels, 0, inputWidth, 0, 0, inputWidth, inputHeight)

        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            if (inputDataType == DataType.FLOAT32) {
                buffer.putFloat(r / 255f)
                buffer.putFloat(g / 255f)
                buffer.putFloat(b / 255f)
            } else {
                buffer.put(r.toByte())
                buffer.put(g.toByte())
                buffer.put(b.toByte())
            }
        }

        buffer.rewind()
        return buffer
    }

    private fun parseYoloOutput(output: Array<FloatArray>): List<DetectionResult> {
        val rows = output.size
        val columns = output.firstOrNull()?.size ?: return emptyList()
        val isTransposed = rows < columns
        val candidateCount = if (isTransposed) columns else rows
        val attributeCount = if (isTransposed) rows else columns
        val hasObjectness = attributeCount - 5 == labels.size
        val classStart = if (hasObjectness) 5 else 4

        return buildList {
            for (candidateIndex in 0 until candidateCount) {
                var bestClassIndex = -1
                var bestClassScore = 0f

                for (attributeIndex in classStart until attributeCount) {
                    val classScore = output.valueAt(candidateIndex, attributeIndex, isTransposed)
                    if (classScore > bestClassScore) {
                        bestClassScore = classScore
                        bestClassIndex = attributeIndex - classStart
                    }
                }

                val objectness = if (hasObjectness) {
                    output.valueAt(candidateIndex, 4, isTransposed)
                } else {
                    1f
                }
                val confidence = objectness * bestClassScore
                if (confidence >= confidenceThreshold && bestClassIndex >= 0) {
                    add(
                        DetectionResult(
                            label = labels.getOrElse(bestClassIndex) { "class-$bestClassIndex" },
                            confidence = confidence,
                            classIndex = bestClassIndex,
                            boundingBox = output.boundingBoxAt(candidateIndex, isTransposed)
                        )
                    )
                }
            }
        }
            .groupBy { it.classIndex }
            .map { (_, detections) -> detections.maxBy { it.confidence } }
            .sortedByDescending { it.confidence }
            .take(5)
    }

    private fun ImageProxy.toBitmap(): Bitmap {
        val nv21 = toNv21()
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        val outputStream = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 90, outputStream)
        return BitmapFactory.decodeByteArray(
            outputStream.toByteArray(),
            0,
            outputStream.size()
        )
    }

    private fun ImageProxy.toNv21(): ByteArray {
        val yPlane = planes[0]
        val uPlane = planes[1]
        val vPlane = planes[2]
        val nv21 = ByteArray(width * height * 3 / 2)
        var offset = 0

        val yBuffer = yPlane.buffer
        for (row in 0 until height) {
            yBuffer.position(row * yPlane.rowStride)
            yBuffer.get(nv21, offset, width)
            offset += width
        }

        val chromaHeight = height / 2
        val chromaWidth = width / 2
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer
        for (row in 0 until chromaHeight) {
            for (column in 0 until chromaWidth) {
                val vuOffset = row * vPlane.rowStride + column * vPlane.pixelStride
                val uuOffset = row * uPlane.rowStride + column * uPlane.pixelStride
                nv21[offset++] = vBuffer.get(vuOffset)
                nv21[offset++] = uBuffer.get(uuOffset)
            }
        }

        return nv21
    }

    private fun Bitmap.rotate(degrees: Int): Bitmap {
        if (degrees == 0) return this
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    private fun Array<FloatArray>.valueAt(
        candidateIndex: Int,
        attributeIndex: Int,
        isTransposed: Boolean
    ): Float {
        return if (isTransposed) {
            this[attributeIndex][candidateIndex]
        } else {
            this[candidateIndex][attributeIndex]
        }
    }

    private fun Array<FloatArray>.boundingBoxAt(
        candidateIndex: Int,
        isTransposed: Boolean
    ): RectF {
        var centerX = valueAt(candidateIndex, 0, isTransposed)
        var centerY = valueAt(candidateIndex, 1, isTransposed)
        var width = valueAt(candidateIndex, 2, isTransposed)
        var height = valueAt(candidateIndex, 3, isTransposed)

        if (max(max(centerX, centerY), max(width, height)) > 1f) {
            centerX /= inputWidth
            width /= inputWidth
            centerY /= inputHeight
            height /= inputHeight
        }

        return RectF(
            (centerX - width / 2f).coerceIn(0f, 1f),
            (centerY - height / 2f).coerceIn(0f, 1f),
            (centerX + width / 2f).coerceIn(0f, 1f),
            (centerY + height / 2f).coerceIn(0f, 1f)
        )
    }

    private fun loadModel(context: Context, assetName: String): MappedByteBuffer {
        val descriptor = context.assets.openFd(assetName)
        return descriptor.use {
            it.createInputStream().channel.map(
                FileChannel.MapMode.READ_ONLY,
                it.startOffset,
                it.declaredLength
            )
        }
    }
}
