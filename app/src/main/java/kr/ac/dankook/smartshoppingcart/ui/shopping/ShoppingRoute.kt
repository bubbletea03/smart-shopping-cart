package kr.ac.dankook.smartshoppingcart.ui.shopping

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kr.ac.dankook.smartshoppingcart.PaymentActivity
import kr.ac.dankook.smartshoppingcart.data.FakeMarketDatabase

@Composable
fun ShoppingRoute(
    marketName: String,
    onChangeMarket: () -> Unit
) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(ShoppingScreen.Camera) }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (currentScreen) {
                ShoppingScreen.Camera -> CameraPermissionGate(
                    marketName = marketName,
                    onChangeMarket = onChangeMarket,
                    onOpenMarketInfo = { currentScreen = ShoppingScreen.MarketInfo },
                    onCheckout = { products ->
                        context.startActivity(
                            PaymentActivity.newIntent(
                                context = context,
                                marketName = marketName,
                                products = products
                            )
                        )
                    }
                )

                ShoppingScreen.MarketInfo -> MarketInfoScreen(
                    marketName = marketName,
                    products = FakeMarketDatabase.getProducts(context, marketName),
                    onBack = { currentScreen = ShoppingScreen.Camera },
                    onChangeMarket = onChangeMarket
                )
            }
        }
    }
}

private enum class ShoppingScreen {
    Camera,
    MarketInfo
}
