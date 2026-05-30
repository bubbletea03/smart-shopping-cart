package kr.ac.dankook.smartshoppingcart.ui.market

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kr.ac.dankook.smartshoppingcart.data.FakeMarketDatabase
import kr.ac.dankook.smartshoppingcart.data.ShoppingMarket

@Composable
fun MarketSelectionRoute(
    onMarketSelected: (ShoppingMarket) -> Unit
) {
    val context = LocalContext.current

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            MarketSelectionScreen(
                markets = FakeMarketDatabase.getMarkets(context),
                onMarketSelected = onMarketSelected
            )
        }
    }
}
