package kr.ac.dankook.smartshoppingcart.ui.market

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import kr.ac.dankook.smartshoppingcart.R
import kr.ac.dankook.smartshoppingcart.data.ShoppingMarket
import kr.ac.dankook.smartshoppingcart.ui.theme.SmartShoppingCartTheme

@Composable
fun MarketSelectionScreen(
    markets: List<ShoppingMarket>,
    onMarketSelected: (ShoppingMarket) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimensionResource(R.dimen.screen_horizontal_padding)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.list_item_spacing)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.screen_top_spacing)))
            Text(
                text = stringResource(R.string.market_selection_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))
            Text(
                text = stringResource(R.string.market_selection_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_large)))
        }

        items(markets) { market ->
            MarketCard(
                market = market,
                onClick = { onMarketSelected(market) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_large)))
        }
    }
}

@Composable
private fun MarketCard(
    market: ShoppingMarket,
    onClick: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                .padding(dimensionResource(R.dimen.card_padding)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                dimensionResource(R.dimen.row_content_spacing)
            )
        ) {
            Box(
                modifier = Modifier
                    .size(dimensionResource(R.dimen.market_badge_size))
                    .clip(CircleShape)
                    .background(colorResource(market.colorResId)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = market.initial,
                    color = colorResource(R.color.white),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = market.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_xsmall)))
                Text(
                    text = market.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(onClick = onClick) {
                Text(text = stringResource(R.string.action_select))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MarketSelectionPreview() {
    SmartShoppingCartTheme {
        MarketSelectionScreen(
            markets = previewMarkets,
            onMarketSelected = {}
        )
    }
}

private val previewMarkets = listOf(
    ShoppingMarket(
        name = "Dankook GS25",
        description = "Campus convenience store",
        initial = "D",
        colorResId = R.color.market_gs25
    )
)
