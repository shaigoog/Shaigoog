package com.shaigoog.ledger.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

internal data class PartyRecord(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String = "",
    val city: String = "",
    val role: String,
    val notes: String = ""
)

internal data class ProductRecord(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String = "",
    val quality: String = ""
)

internal data class DealRecord(
    val id: String = UUID.randomUUID().toString(),
    val number: String,
    val type: String,
    val partyId: String,
    val productId: String,
    val quantityTons: String,
    val pricePerTon: String,
    val totalMinor: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val notes: String = ""
)

internal class LocalLedgerStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun loadParties(): List<PartyRecord> = parseArray(KEY_PARTIES) { json ->
        PartyRecord(
            id = json.optString("id"),
            name = json.optString("name"),
            phone = json.optString("phone"),
            city = json.optString("city"),
            role = json.optString("role", "SUPPLIER"),
            notes = json.optString("notes")
        )
    }

    fun loadProducts(): List<ProductRecord> = parseArray(KEY_PRODUCTS) { json ->
        ProductRecord(
            id = json.optString("id"),
            name = json.optString("name"),
            category = json.optString("category"),
            quality = json.optString("quality")
        )
    }

    fun loadDeals(): List<DealRecord> = parseArray(KEY_DEALS) { json ->
        DealRecord(
            id = json.optString("id"),
            number = json.optString("number"),
            type = json.optString("type", "PURCHASE"),
            partyId = json.optString("partyId"),
            productId = json.optString("productId"),
            quantityTons = json.optString("quantityTons"),
            pricePerTon = json.optString("pricePerTon"),
            totalMinor = json.optLong("totalMinor"),
            createdAt = json.optLong("createdAt"),
            notes = json.optString("notes")
        )
    }.sortedByDescending { it.createdAt }

    fun addParty(record: PartyRecord) {
        val records = loadParties().toMutableList().apply { add(record) }
        saveArray(KEY_PARTIES, records.map { partyToJson(it) })
    }

    fun addProduct(record: ProductRecord) {
        val records = loadProducts().toMutableList().apply { add(record) }
        saveArray(KEY_PRODUCTS, records.map { productToJson(it) })
    }

    fun addDeal(record: DealRecord) {
        val records = loadDeals().toMutableList().apply { add(record) }
        saveArray(KEY_DEALS, records.map { dealToJson(it) })
    }

    fun deleteParty(id: String) {
        saveArray(KEY_PARTIES, loadParties().filterNot { it.id == id }.map { partyToJson(it) })
    }

    fun deleteProduct(id: String) {
        saveArray(KEY_PRODUCTS, loadProducts().filterNot { it.id == id }.map { productToJson(it) })
    }

    fun deleteDeal(id: String) {
        saveArray(KEY_DEALS, loadDeals().filterNot { it.id == id }.map { dealToJson(it) })
    }

    private fun partyToJson(record: PartyRecord) = JSONObject()
        .put("id", record.id)
        .put("name", record.name)
        .put("phone", record.phone)
        .put("city", record.city)
        .put("role", record.role)
        .put("notes", record.notes)

    private fun productToJson(record: ProductRecord) = JSONObject()
        .put("id", record.id)
        .put("name", record.name)
        .put("category", record.category)
        .put("quality", record.quality)

    private fun dealToJson(record: DealRecord) = JSONObject()
        .put("id", record.id)
        .put("number", record.number)
        .put("type", record.type)
        .put("partyId", record.partyId)
        .put("productId", record.productId)
        .put("quantityTons", record.quantityTons)
        .put("pricePerTon", record.pricePerTon)
        .put("totalMinor", record.totalMinor)
        .put("createdAt", record.createdAt)
        .put("notes", record.notes)

    private fun <T> parseArray(key: String, mapper: (JSONObject) -> T): List<T> {
        val raw = preferences.getString(key, "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val json = array.optJSONObject(index) ?: continue
                    add(mapper(json))
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun saveArray(key: String, values: List<JSONObject>) {
        val array = JSONArray()
        values.forEach(array::put)
        preferences.edit().putString(key, array.toString()).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "shaigoog_ledger_offline_v1"
        const val KEY_PARTIES = "parties"
        const val KEY_PRODUCTS = "products"
        const val KEY_DEALS = "deals"
    }
}
