package kr.ac.dankook.smartshoppingcart.data

import android.content.Context

private const val PreferencesName = "smart_shopping_cart_preferences"
private const val SelectedMarketKey = "selected_market"

fun Context.getSavedMarket(): String? {
    return getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        .getString(SelectedMarketKey, null)
}

fun Context.saveMarket(marketName: String) {
    getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        .edit()
        .putString(SelectedMarketKey, marketName)
        .apply()
}

fun Context.clearSavedMarket() {
    getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        .edit()
        .remove(SelectedMarketKey)
        .apply()
}
