package kr.ac.dankook.smartshoppingcart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import kr.ac.dankook.smartshoppingcart.data.saveMarket
import kr.ac.dankook.smartshoppingcart.ui.market.MarketSelectionRoute
import kr.ac.dankook.smartshoppingcart.ui.theme.SmartShoppingCartTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            SmartShoppingCartTheme {
                MarketSelectionRoute(
                    onMarketSelected = { market ->
                        saveMarket(market.name)
                        startActivity(ShoppingActivity.newIntent(this, market.name))
                        finish()
                    }
                )
            }
        }
    }
}
