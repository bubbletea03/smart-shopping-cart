package kr.ac.dankook.smartshoppingcart.ui.shopping

import androidx.compose.foundation.background
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
                    if (recognizedProducts.none { it.code == product.code }) {
                        recognizedProducts.add(product)
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
