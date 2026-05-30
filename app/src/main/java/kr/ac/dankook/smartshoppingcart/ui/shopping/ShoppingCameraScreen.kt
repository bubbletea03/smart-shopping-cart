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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
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
    val recognizedProducts = remember {
        mutableStateListOf<RecognizedProduct>()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CameraPreviewLayout(
            marketName = marketName,
            modifier = Modifier
                .fillMaxWidth()
                .weight(7f),
            onChangeMarket = onChangeMarket
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
    modifier: Modifier = Modifier,
    onChangeMarket: () -> Unit
) {
    Box(
        modifier = modifier
            .background(colorResource(R.color.camera_preview_background))
            .padding(dimensionResource(R.dimen.card_padding))
    ) {
        Column(
            modifier = Modifier.align(Alignment.TopStart)
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

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.72f)
                .height(dimensionResource(R.dimen.camera_placeholder_height)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(dimensionResource(R.dimen.card_corner_radius)))
                    .background(colorResource(R.color.camera_placeholder_background)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.camera_placeholder_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorResource(R.color.camera_preview_primary_text)
                )
            }
        }

        Row(
            modifier = Modifier.align(Alignment.TopEnd),
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
