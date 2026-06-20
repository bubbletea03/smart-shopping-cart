package kr.ac.dankook.smartshoppingcart.ui.shopping

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import kr.ac.dankook.smartshoppingcart.R

@Composable
fun RecognizedProductsLayout(
    products: List<RecognizedProduct>,
    modifier: Modifier = Modifier,
    onDecreaseQuantity: (RecognizedProduct) -> Unit,
    onIncreaseQuantity: (RecognizedProduct) -> Unit,
    onCheckout: () -> Unit
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(
                horizontal = dimensionResource(R.dimen.content_horizontal_padding),
                vertical = dimensionResource(R.dimen.content_vertical_padding)
            )
    ) {
        if (products.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.recognized_products_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
            ) {
                items(products) { product ->
                    RecognizedProductRow(
                        product = product,
                        onDecreaseQuantity = { onDecreaseQuantity(product) },
                        onIncreaseQuantity = { onIncreaseQuantity(product) }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.list_item_spacing)))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.total_price_format, products.sumOf { it.unitPrice * it.quantity }),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Button(
                onClick = onCheckout,
                enabled = products.isNotEmpty()
            ) {
                Text(text = stringResource(R.string.action_checkout))
            }
        }
    }
}

@Composable
private fun RecognizedProductRow(
    product: RecognizedProduct,
    onDecreaseQuantity: () -> Unit,
    onIncreaseQuantity: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimensionResource(R.dimen.card_corner_radius)))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(
                horizontal = dimensionResource(R.dimen.product_row_horizontal_padding),
                vertical = dimensionResource(R.dimen.product_row_vertical_padding)
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = product.code,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(
            modifier = Modifier.padding(start = dimensionResource(R.dimen.spacing_small)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xsmall))
        ) {
            IconButton(
                onClick = onDecreaseQuantity,
                modifier = Modifier.size(dimensionResource(R.dimen.quantity_button_size))
            ) {
                Text(
                    text = "-",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = product.quantity.toString(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(
                onClick = onIncreaseQuantity,
                modifier = Modifier.size(dimensionResource(R.dimen.quantity_button_size))
            ) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                modifier = Modifier.widthIn(min = dimensionResource(R.dimen.product_price_min_width)),
                text = product.displayTotalPrice,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                textAlign = TextAlign.End
            )
        }
    }
}
