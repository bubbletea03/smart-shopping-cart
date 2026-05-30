package kr.ac.dankook.smartshoppingcart.ui.shopping

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import kr.ac.dankook.smartshoppingcart.R
import kr.ac.dankook.smartshoppingcart.data.MarketProduct
import kr.ac.dankook.smartshoppingcart.ui.theme.SmartShoppingCartTheme

@Composable
fun MarketInfoScreen(
    marketName: String,
    products: List<MarketProduct>,
    onBack: () -> Unit,
    onChangeMarket: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(
                horizontal = dimensionResource(R.dimen.content_horizontal_padding),
                vertical = dimensionResource(R.dimen.card_padding)
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = onBack) {
                Text(text = stringResource(R.string.action_back))
            }
            OutlinedButton(onClick = onChangeMarket) {
                Text(text = stringResource(R.string.action_change_market))
            }
        }
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_header)))
        Text(
            text = marketName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_xsmall)))
        Text(
            text = stringResource(R.string.registered_products_count_format, products.size),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.card_padding)))

        if (products.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.market_products_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.product_list_spacing))
            ) {
                items(products) { product ->
                    MarketProductRow(product = product)
                }
            }
        }
    }
}

@Composable
private fun MarketProductRow(product: MarketProduct) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimensionResource(R.dimen.card_corner_radius)),
        border = BorderStroke(
            dimensionResource(R.dimen.card_border_width),
            MaterialTheme.colorScheme.outlineVariant
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dimensionResource(R.dimen.content_vertical_padding),
                    vertical = dimensionResource(R.dimen.product_row_horizontal_padding)
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_xsmall)))
                Text(
                    text = stringResource(
                        R.string.product_category_stock_format,
                        product.category,
                        product.stockCount
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = product.displayPrice,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MarketInfoPreview() {
    SmartShoppingCartTheme {
        MarketInfoScreen(
            marketName = "Dankook GS25",
            products = previewProducts,
            onBack = {},
            onChangeMarket = {}
        )
    }
}

private val previewProducts = listOf(
    MarketProduct("gs25-001", "Americano", "Coffee", 2000, 30),
    MarketProduct("gs25-002", "Triangle Kimbap", "Meal", 1400, 12)
)
