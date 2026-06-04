package kr.ac.dankook.smartshoppingcart.ui.shopping

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import kr.ac.dankook.smartshoppingcart.data.FakeMarketDatabase
import kr.ac.dankook.smartshoppingcart.detection.DetectionResult
import kr.ac.dankook.smartshoppingcart.R
import kr.ac.dankook.smartshoppingcart.ui.theme.SmartShoppingCartTheme

data class RecognizedProduct(
    val name: String,
    val code: String,
    val price: String
)

private enum class DetectionZone {
    ZoneA,
    ZoneB
}

@Composable
fun ShoppingCameraScreen(
    marketName: String,
    onChangeMarket: () -> Unit,
    onOpenMarketInfo: () -> Unit
) {
    val context = LocalContext.current
    val marketProducts = remember(marketName) {
        FakeMarketDatabase.getProducts(context, marketName)
    }
    val recognizedProducts = remember(marketName) {
        mutableStateListOf<RecognizedProduct>()
    }
    var latestDetections by remember(marketName) { mutableStateOf(emptyList<DetectionResult>()) }

    Column(modifier = Modifier.fillMaxSize()) {
        CameraPreviewLayout(
            marketName = marketName,
            productLabels = marketProducts.map { it.name },
            latestDetections = latestDetections,
            modifier = Modifier
                .fillMaxWidth()
                .weight(7f),
            onChangeMarket = onChangeMarket,
            onDetections = { detections ->
                latestDetections = detections
                detections.forEach { detection ->
                    val marketProduct = marketProducts.getOrNull(detection.classIndex)
                    val product = if (marketProduct != null) {
                        RecognizedProduct(
                            name = marketProduct.name,
                            code = marketProduct.id,
                            price = marketProduct.displayPrice
                        )
                    } else {
                        RecognizedProduct(
                            name = detection.label,
                            code = "confidence %.0f%%".format(detection.confidence * 100),
                            price = "-"
                        )
                    }
                    when (detection.zone()) {
                        DetectionZone.ZoneA -> {
                            recognizedProducts.removeAll {
                                it.code == product.code || it.name == product.name
                            }
                        }

                        DetectionZone.ZoneB -> {
                            if (recognizedProducts.none { it.code == product.code }) {
                                recognizedProducts.add(product)
                            }
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
            onOpenMarketInfo = onOpenMarketInfo
        )
    }
}

@Composable
private fun CameraPreviewLayout(
    marketName: String,
    productLabels: List<String>,
    latestDetections: List<DetectionResult>,
    modifier: Modifier = Modifier,
    onChangeMarket: () -> Unit,
    onDetections: (List<DetectionResult>) -> Unit
) {
    Box(
        modifier = modifier
            .background(colorResource(R.color.camera_preview_background))
    ) {
        CameraXDetectionPreview(
            labels = productLabels,
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
        val dividerY = size.height / 2f

        drawLine(
            color = zoneLineColor,
            start = Offset(0f, dividerY),
            end = Offset(size.width, dividerY),
            strokeWidth = 2f * density
        )
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawText(
                "Zone A",
                zoneLabelPadding,
                dividerY - zoneLabelPadding,
                zonePaint
            )
            canvas.nativeCanvas.drawText(
                "Zone B",
                zoneLabelPadding,
                dividerY + zoneLabelPadding + zoneTextSize,
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
    return if (centerY < 0.5f) DetectionZone.ZoneA else DetectionZone.ZoneB
}

@Preview(showBackground = true)
@Composable
private fun ShoppingCameraPreview() {
    SmartShoppingCartTheme {
        ShoppingCameraScreen(
            marketName = "Dankook GS25",
            onChangeMarket = {},
            onOpenMarketInfo = {}
        )
    }
}
