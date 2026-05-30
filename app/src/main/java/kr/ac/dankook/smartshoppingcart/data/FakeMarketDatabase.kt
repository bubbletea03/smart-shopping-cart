package kr.ac.dankook.smartshoppingcart.data

import android.content.Context
import androidx.annotation.ColorRes
import kr.ac.dankook.smartshoppingcart.R
import org.json.JSONObject

data class ShoppingMarket(
    val name: String,
    val description: String,
    val initial: String,
    @ColorRes val colorResId: Int
)

data class MarketProduct(
    val id: String,
    val name: String,
    val category: String,
    val price: Int,
    val stockCount: Int
) {
    val displayPrice: String
        get() = "KRW %,d".format(price)
}

object FakeMarketDatabase {
    private var cache: MarketDatabaseSnapshot? = null

    fun getMarkets(context: Context): List<ShoppingMarket> {
        return loadSnapshot(context).markets
    }

    fun getProducts(context: Context, marketName: String): List<MarketProduct> {
        return loadSnapshot(context).productsByMarket[marketName].orEmpty()
    }

    private fun loadSnapshot(context: Context): MarketDatabaseSnapshot {
        cache?.let { return it }

        val json = context.resources.openRawResource(R.raw.fake_market_database)
            .bufferedReader()
            .use { it.readText() }
        val root = JSONObject(json)
        val marketItems = root.getJSONArray("markets")

        val markets = buildList {
            for (index in 0 until marketItems.length()) {
                val market = marketItems.getJSONObject(index)
                add(
                    ShoppingMarket(
                        name = market.getString("name"),
                        description = market.getString("description"),
                        initial = market.getString("initial"),
                        colorResId = colorResIdFor(market.getString("colorKey"))
                    )
                )
            }
        }

        val productsByMarket = buildMap {
            for (marketIndex in 0 until marketItems.length()) {
                val market = marketItems.getJSONObject(marketIndex)
                val productItems = market.getJSONArray("products")
                val products = buildList {
                    for (productIndex in 0 until productItems.length()) {
                        val product = productItems.getJSONObject(productIndex)
                        add(
                            MarketProduct(
                                id = product.getString("id"),
                                name = product.getString("name"),
                                category = product.getString("category"),
                                price = product.getInt("price"),
                                stockCount = product.getInt("stockCount")
                            )
                        )
                    }
                }
                put(market.getString("name"), products)
            }
        }

        return MarketDatabaseSnapshot(
            markets = markets,
            productsByMarket = productsByMarket
        ).also { cache = it }
    }

    @ColorRes
    private fun colorResIdFor(colorKey: String): Int {
        return when (colorKey) {
            "emart" -> R.color.market_emart
            "homeplus" -> R.color.market_homeplus
            "gs25" -> R.color.market_gs25
            "lotte" -> R.color.market_lotte
            "costco" -> R.color.market_costco
            else -> R.color.market_gs25
        }
    }
}

private data class MarketDatabaseSnapshot(
    val markets: List<ShoppingMarket>,
    val productsByMarket: Map<String, List<MarketProduct>>
)
