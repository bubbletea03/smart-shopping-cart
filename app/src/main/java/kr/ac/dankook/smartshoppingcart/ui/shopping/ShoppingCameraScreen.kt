package kr.ac.dankook.smartshoppingcart.ui.shopping

import android.media.AudioAttributes
import android.media.SoundPool
import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kr.ac.dankook.smartshoppingcart.data.FakeMarketDatabase
import kr.ac.dankook.smartshoppingcart.detection.DetectionResult
import kr.ac.dankook.smartshoppingcart.R
import kr.ac.dankook.smartshoppingcart.ui.theme.SmartShoppingCartTheme
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

data class RecognizedProduct(
    val name: String,
    val code: String,
    val unitPrice: Int,
    val quantity: Int = 1
) {
    val displayTotalPrice: String
        get() = if (unitPrice > 0) {
            "KRW %,d".format(unitPrice * quantity)
        } else {
            "-"
        }
}

private enum class DetectionZone {
    ZoneA,
    ZoneB,
    ZoneC
}

@Composable
fun ShoppingCameraScreen(
    marketName: String,
    onChangeMarket: () -> Unit,
    onOpenMarketInfo: () -> Unit,
    onCheckout: (List<RecognizedProduct>) -> Unit
) {
    val context = LocalContext.current
    val marketProducts = remember(marketName) {
        FakeMarketDatabase.getProducts(context, marketName)
    }
    val recognizedProducts = remember(marketName) {
        mutableStateListOf<RecognizedProduct>()
    }
    val zoneHistoryByClass = remember(marketName) {
        mutableMapOf<Int, MutableList<DetectionZone>>()
    }
    var latestDetections by remember(marketName) { mutableStateOf(emptyList<DetectionResult>()) }
    val playAddProductSound = rememberStoreScannerBeepPlayer()

    Column(modifier = Modifier.fillMaxSize()) {
        CameraPreviewLayout(
            marketName = marketName,
            productLabels = marketProducts.map { it.name },
            latestDetections = latestDetections,
            modifier = Modifier
                .fillMaxWidth()
                .weight(7f),
            onChangeMarket = onChangeMarket,
            onOpenMarketInfo = onOpenMarketInfo,
            onDetections = { detections ->
                latestDetections = detections
                detections.forEach { detection ->
                    val marketProduct = marketProducts.getOrNull(detection.classIndex)
                    val product = if (marketProduct != null) {
                        RecognizedProduct(
                            name = marketProduct.name,
                            code = marketProduct.id,
                            unitPrice = marketProduct.price
                        )
                    } else {
                        RecognizedProduct(
                            name = detection.label,
                            code = "confidence %.0f%%".format(detection.confidence * 100),
                            unitPrice = 0
                        )
                    }
                    val currentZone = detection.zone()
                    val zoneHistory = zoneHistoryByClass.getOrPut(detection.classIndex) {
                        mutableListOf()
                    }
                    if (zoneHistory.lastOrNull() != currentZone) {
                        zoneHistory.add(currentZone)
                        if (zoneHistory.size > ZoneSequenceLength) {
                            zoneHistory.removeAt(0)
                        }
                    }

                    when (zoneHistory.toList()) {
                        AddProductZoneSequence -> {
                            recognizedProducts.increaseQuantity(product)
                            playAddProductSound()
                            zoneHistory.clear()
                        }
                        RemoveProductZoneSequence -> {
                            recognizedProducts.decreaseQuantity(product.code)
                            zoneHistory.clear()
                        }
                    }
                }
            }
        )
        RecognizedProductsLayout(
            products = recognizedProducts,
            modifier = Modifier
                .fillMaxWidth()
                .weight(3f),
            onDecreaseQuantity = { product ->
                recognizedProducts.decreaseQuantity(product.code)
            },
            onIncreaseQuantity = { product ->
                recognizedProducts.increaseQuantity(product)
                playAddProductSound()
            },
            onCheckout = { onCheckout(recognizedProducts.toList()) }
        )
    }
}

@Composable
private fun rememberStoreScannerBeepPlayer(): () -> Unit {
    if (LocalInspectionMode.current) return {}

    val context = LocalContext.current
    val appContext = remember(context) { context.applicationContext }
    val soundPool = remember {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(audioAttributes)
            .build()
    }
    val soundId = remember { AtomicInteger(0) }
    val isSoundLoaded = remember { AtomicBoolean(false) }

    DisposableEffect(appContext, soundPool) {
        isSoundLoaded.set(false)
        soundPool.setOnLoadCompleteListener { _, loadedSoundId, status ->
            if (loadedSoundId == soundId.get() && status == 0) {
                isSoundLoaded.set(true)
            }
        }
        soundId.set(soundPool.load(appContext, R.raw.store_scanner_beep, 1))

        onDispose {
            soundPool.release()
        }
    }

    return remember(soundPool, soundId) {
        {
            val loadedSoundId = soundId.get()
            if (isSoundLoaded.get() && loadedSoundId != 0) {
                soundPool.play(loadedSoundId, 1f, 1f, 1, 0, 1f)
            }
        }
    }
}

private fun MutableList<RecognizedProduct>.increaseQuantity(product: RecognizedProduct) {
    val productIndex = indexOfFirst { it.code == product.code }
    if (productIndex >= 0) {
        this[productIndex] = this[productIndex].copy(
            quantity = this[productIndex].quantity + 1
        )
    } else {
        add(product)
    }
}

private fun MutableList<RecognizedProduct>.decreaseQuantity(productCode: String) {
    val productIndex = indexOfFirst { it.code == productCode }
    if (productIndex < 0) return

    val product = this[productIndex]
    if (product.quantity <= 1) {
        removeAt(productIndex)
    } else {
        this[productIndex] = product.copy(quantity = product.quantity - 1)
    }
}

@Composable
private fun CameraPreviewLayout(
    marketName: String,
    productLabels: List<String>,
    latestDetections: List<DetectionResult>,
    modifier: Modifier = Modifier,
    onChangeMarket: () -> Unit,
    onOpenMarketInfo: () -> Unit,
    onDetections: (List<DetectionResult>) -> Unit
) {
    var focusLockRequestCount by remember { mutableStateOf(0) }
    var isFocusLockFeedbackVisible by remember { mutableStateOf(false) }

    LaunchedEffect(focusLockRequestCount) {
        if (focusLockRequestCount > 0) {
            isFocusLockFeedbackVisible = true
            delay(FocusLockFeedbackDurationMs)
            isFocusLockFeedbackVisible = false
        }
    }

    Box(
        modifier = modifier
            .background(colorResource(R.color.camera_preview_background))
    ) {
        val lockFocusDescription = stringResource(R.string.action_lock_focus)

        CameraXDetectionPreview(
            labels = productLabels,
            focusLockRequestCount = focusLockRequestCount,
            modifier = Modifier.fillMaxSize(),
            onDetections = onDetections
        )

        DetectionZoneOverlay(
            detections = latestDetections,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(dimensionResource(R.dimen.card_padding))
                .clip(RoundedCornerShape(dimensionResource(R.dimen.card_corner_radius)))
                .background(colorResource(R.color.camera_preview_background).copy(alpha = 0.72f))
                .padding(dimensionResource(R.dimen.spacing_small))
        ) {
            Text(
                text = marketName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colorResource(R.color.white)
            )
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_xsmall)))
            Text(
                text = stringResource(R.string.camera_preview_label),
                style = MaterialTheme.typography.bodyMedium,
                color = colorResource(R.color.camera_preview_secondary_text)
            )
        }

        if (latestDetections.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(dimensionResource(R.dimen.card_padding))
                    .clip(RoundedCornerShape(dimensionResource(R.dimen.card_corner_radius)))
                    .background(colorResource(R.color.camera_preview_background).copy(alpha = 0.72f))
                    .padding(dimensionResource(R.dimen.spacing_small)),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xsmall))
            ) {
                latestDetections.forEach { detection ->
                    Text(
                        text = "${detection.label} ${"%.0f%%".format(detection.confidence * 100)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorResource(R.color.camera_preview_primary_text)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(dimensionResource(R.dimen.card_padding)),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
        ) {
            OutlinedButton(onClick = onChangeMarket) {
                Text(text = stringResource(R.string.action_change_market))
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.card_padding))
        ) {
            FilledIconButton(
                onClick = { focusLockRequestCount++ },
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(58.dp)
                    .semantics { contentDescription = lockFocusDescription },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (isFocusLockFeedbackVisible) {
                        Color(0xFF26A69A)
                    } else {
                        colorResource(R.color.camera_preview_background).copy(alpha = 0.82f)
                    },
                    contentColor = colorResource(R.color.white)
                )
            ) {
                if (isFocusLockFeedbackVisible) {
                    FocusLockedIcon(
                        color = colorResource(R.color.white),
                        modifier = Modifier.size(30.dp)
                    )
                } else {
                    FocusLockIcon(
                        color = colorResource(R.color.white),
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            OutlinedButton(
                onClick = onOpenMarketInfo,
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Text(text = stringResource(R.string.action_market_info))
            }
        }
    }
}

@Composable
private fun FocusLockIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 2.2f * density
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.18f
        val innerGap = size.minDimension * 0.30f
        val outerInset = size.minDimension * 0.05f

        drawCircle(
            color = color,
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidth)
        )
        drawLine(
            color = color,
            start = Offset(size.width / 2f, outerInset),
            end = Offset(size.width / 2f, innerGap),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(size.width / 2f, size.height - outerInset),
            end = Offset(size.width / 2f, size.height - innerGap),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(outerInset, size.height / 2f),
            end = Offset(innerGap, size.height / 2f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(size.width - outerInset, size.height / 2f),
            end = Offset(size.width - innerGap, size.height / 2f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun FocusLockedIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 2.8f * density
        drawLine(
            color = color,
            start = Offset(size.width * 0.22f, size.height * 0.52f),
            end = Offset(size.width * 0.43f, size.height * 0.72f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.43f, size.height * 0.72f),
            end = Offset(size.width * 0.80f, size.height * 0.30f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun DetectionZoneOverlay(
    detections: List<DetectionResult>,
    modifier: Modifier = Modifier
) {
    val labelBackgroundColor = colorResource(R.color.camera_preview_background).copy(alpha = 0.78f)
    val labelTextColor = Color.White
    val zoneLineColor = Color.White.copy(alpha = 0.55f)
    val zoneTextColor = Color.White.copy(alpha = 0.82f)
    val zoneAColor = Color(0xFFEF5350)
    val zoneBColor = Color(0xFF26A69A)
    val zoneCColor = Color(0xFF42A5F5)

    Canvas(modifier = modifier) {
        val strokeWidth = 3f
        val labelTextSize = 14f * density
        val labelHorizontalPadding = 6f * density
        val labelVerticalPadding = 4f * density
        val labelGap = 2f * density
        val zoneTextSize = 13f * density
        val zoneLabelPadding = 10f * density
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = labelTextColor.toArgb()
            textSize = labelTextSize
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val zonePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = zoneTextColor.toArgb()
            textSize = zoneTextSize
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val fontMetrics = textPaint.fontMetrics
        val labelHeight = fontMetrics.bottom - fontMetrics.top + labelVerticalPadding * 2f
        val firstDividerY = size.height / 3f
        val secondDividerY = size.height * 2f / 3f

        drawLine(
            color = zoneLineColor,
            start = Offset(0f, firstDividerY),
            end = Offset(size.width, firstDividerY),
            strokeWidth = 2f * density
        )
        drawLine(
            color = zoneLineColor,
            start = Offset(0f, secondDividerY),
            end = Offset(size.width, secondDividerY),
            strokeWidth = 2f * density
        )
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawText(
                "Zone A",
                zoneLabelPadding,
                firstDividerY - zoneLabelPadding,
                zonePaint
            )
            canvas.nativeCanvas.drawText(
                "Zone B",
                zoneLabelPadding,
                firstDividerY + zoneLabelPadding + zoneTextSize,
                zonePaint
            )
            canvas.nativeCanvas.drawText(
                "Zone C",
                zoneLabelPadding,
                secondDividerY + zoneLabelPadding + zoneTextSize,
                zonePaint
            )
        }

        detections.forEach { detection ->
            val left = detection.boundingBox.left * size.width
            val top = detection.boundingBox.top * size.height
            val right = detection.boundingBox.right * size.width
            val bottom = detection.boundingBox.bottom * size.height
            val boxWidth = right - left
            val boxHeight = bottom - top

            if (boxWidth <= 0f || boxHeight <= 0f) return@forEach
            val detectionBoxColor = when (detection.zone()) {
                DetectionZone.ZoneA -> zoneAColor
                DetectionZone.ZoneB -> zoneBColor
                DetectionZone.ZoneC -> zoneCColor
            }

            drawRect(
                color = detectionBoxColor,
                topLeft = Offset(left, top),
                size = Size(boxWidth, boxHeight),
                style = Stroke(width = strokeWidth)
            )

            val label = "${detection.label} ${"%.0f%%".format(detection.confidence * 100)}"
            val labelWidth = textPaint.measureText(label) + labelHorizontalPadding * 2f
            val labelLeft = left.coerceIn(0f, (size.width - labelWidth).coerceAtLeast(0f))
            val labelTop = if (top - labelHeight - labelGap >= 0f) {
                top - labelHeight - labelGap
            } else {
                top + labelGap
            }

            drawRect(
                color = labelBackgroundColor,
                topLeft = Offset(labelLeft, labelTop),
                size = Size(labelWidth, labelHeight)
            )
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(
                    label,
                    labelLeft + labelHorizontalPadding,
                    labelTop + labelVerticalPadding - fontMetrics.top,
                    textPaint
                )
            }
        }
    }
}

private fun DetectionResult.zone(): DetectionZone {
    val centerY = (boundingBox.top + boundingBox.bottom) / 2f
    return when {
        centerY < 1f / 3f -> DetectionZone.ZoneA
        centerY < 2f / 3f -> DetectionZone.ZoneB
        else -> DetectionZone.ZoneC
    }
}

private const val FocusLockFeedbackDurationMs = 800L
private const val ZoneSequenceLength = 3
private val AddProductZoneSequence = listOf(
    DetectionZone.ZoneA,
    DetectionZone.ZoneB,
    DetectionZone.ZoneC
)
private val RemoveProductZoneSequence = listOf(
    DetectionZone.ZoneC,
    DetectionZone.ZoneB,
    DetectionZone.ZoneA
)

@Preview(showBackground = true)
@Composable
private fun ShoppingCameraPreview() {
    SmartShoppingCartTheme {
        ShoppingCameraScreen(
            marketName = "Dankook GS25",
            onChangeMarket = {},
            onOpenMarketInfo = {},
            onCheckout = {}
        )
    }
}
