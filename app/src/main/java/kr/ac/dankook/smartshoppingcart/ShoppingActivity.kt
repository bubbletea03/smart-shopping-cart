package kr.ac.dankook.smartshoppingcart

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import kr.ac.dankook.smartshoppingcart.data.clearSavedMarket
import kr.ac.dankook.smartshoppingcart.data.getSavedMarket
import kr.ac.dankook.smartshoppingcart.ui.shopping.ShoppingRoute
import kr.ac.dankook.smartshoppingcart.ui.theme.SmartShoppingCartTheme

private const val MarketNameExtra = "market_name"

class ShoppingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val marketName = intent.getStringExtra(MarketNameExtra) ?: getSavedMarket()
        if (marketName == null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        setContent {
            SmartShoppingCartTheme {
                ShoppingRoute(
                    marketName = marketName,
                    onChangeMarket = {
                        clearSavedMarket()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }

    companion object {
        fun newIntent(context: Context, marketName: String): Intent {
            return Intent(context, ShoppingActivity::class.java)
                .putExtra(MarketNameExtra, marketName)
        }
    }
}
