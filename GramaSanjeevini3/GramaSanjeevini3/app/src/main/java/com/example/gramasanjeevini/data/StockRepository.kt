package com.example.gramasanjeevini.data

import com.example.gramasanjeevini.models.MOCK_SHOPS
import com.example.gramasanjeevini.models.MOCK_STOCK
import com.example.gramasanjeevini.models.Shop
import com.example.gramasanjeevini.models.StockItem
import com.example.gramasanjeevini.models.haversineKm
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

private const val STOCK_COL = "stock"
private const val SHOPS_COL = "shops"

object StockRepository {

    private val db: FirebaseFirestore get() = Firebase.firestore

    // ── Seed stock if needed ──────────────────────────────────────────────────
    suspend fun seedIfEmpty() {
        try {
            val snap = db.collection(STOCK_COL).limit(1).get().await()
            val needsSeed = if (snap.isEmpty) {
                true
            } else {
                val firstDoc = snap.documents.first()
                val firstId  = firstDoc.id
                val dist     = firstDoc.getDouble("distanceKm") ?: 0.0
                // Re-seed if: old ID format OR distance is 0.0 (stale data)
                !firstId.contains("_") || dist == 0.0
            }
            if (needsSeed) {
                val batch = db.batch()
                MOCK_STOCK.forEach { item ->
                    batch.set(db.collection(STOCK_COL).document(item.id), item.toMap())
                }
                batch.commit().await()
            }
        } catch (_: Exception) {}
    }

    // ── Real-time stock stream ────────────────────────────────────────────────
    fun stockFlow(): Flow<List<StockItem>> = callbackFlow {
        val listener = db.collection(STOCK_COL)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) { trySend(MOCK_STOCK); return@addSnapshotListener }
                val items = snap.documents.mapNotNull { it.toStockItem() }
                trySend(if (items.isEmpty()) MOCK_STOCK else items)
            }
        awaitClose { listener.remove() }
    }

    // ── Stock stream with real GPS distances ──────────────────────────────────
    fun stockFlowWithDistance(userLat: Double, userLng: Double): Flow<List<StockItem>> =
        callbackFlow {
            val listener = db.collection(STOCK_COL)
                .addSnapshotListener { snap, err ->
                    if (err != null || snap == null) {
                        trySend(MOCK_STOCK.withRealDistance(userLat, userLng))
                        return@addSnapshotListener
                    }
                    val items = snap.documents.mapNotNull { it.toStockItem() }
                    val base  = if (items.isEmpty()) MOCK_STOCK else items
                    trySend(base.withRealDistance(userLat, userLng))
                }
            awaitClose { listener.remove() }
        }

    // ── Add stock item ────────────────────────────────────────────────────────
    suspend fun addStockItem(item: StockItem): Result<Unit> = try {
        db.collection(STOCK_COL).document(item.id).set(item.toMap()).await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    // ── Update quantity ───────────────────────────────────────────────────────
    suspend fun updateQuantity(itemId: String, newQty: Int): Result<Unit> = try {
        db.collection(STOCK_COL).document(itemId).update("quantity", newQty).await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    // ── Update discount + mark near-expiry ────────────────────────────────────
    suspend fun updateDiscount(itemId: String, discountPercent: Int): Result<Unit> = try {
        db.collection(STOCK_COL).document(itemId).update(
            mapOf("discountPercent" to discountPercent, "isNearExpiry" to true)
        ).await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    // ── Delete stock item ─────────────────────────────────────────────────────
    suspend fun deleteStockItem(itemId: String): Result<Unit> = try {
        db.collection(STOCK_COL).document(itemId).delete().await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    // ── Register a new pharmacist shop ────────────────────────────────────────
    suspend fun registerShop(shop: Shop): Result<Unit> = try {
        db.collection(SHOPS_COL).document(shop.id).set(shop.toMap()).await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    // ── Real-time stream of all registered shops (seed + pharmacist-added) ────
    fun shopsFlow(): Flow<List<Shop>> = callbackFlow {
        val listener = db.collection(SHOPS_COL)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) { trySend(MOCK_SHOPS); return@addSnapshotListener }
                val shops = snap.documents.mapNotNull { it.toShop() }
                trySend(if (shops.isEmpty()) MOCK_SHOPS else shops)
            }
        awaitClose { listener.remove() }
    }

    // ── Seed shops collection if empty ────────────────────────────────────────
    suspend fun seedShopsIfEmpty() {
        try {
            val snap = db.collection(SHOPS_COL).limit(1).get().await()
            if (snap.isEmpty) {
                val batch = db.batch()
                MOCK_SHOPS.forEach { shop ->
                    batch.set(db.collection(SHOPS_COL).document(shop.id), shop.toMap())
                }
                batch.commit().await()
            }
        } catch (_: Exception) {}
    }
}

// ── Recalculate distances from user GPS ───────────────────────────────────────
fun List<StockItem>.withRealDistance(userLat: Double, userLng: Double): List<StockItem> =
    map { item ->
        if (item.shopLat != 0.0 && item.shopLng != 0.0) {
            val km = haversineKm(userLat, userLng, item.shopLat, item.shopLng)
            item.copy(distanceKm = Math.round(km * 10) / 10.0)
        } else item
    }

// ── StockItem → Firestore map ─────────────────────────────────────────────────
fun StockItem.toMap(): Map<String, Any> = mapOf(
    "id" to id, "medicineId" to medicineId, "medicineName" to medicineName,
    "medicineCategory" to medicineCategory, "shopId" to shopId,
    "shopName" to shopName, "shopVillage" to shopVillage,
    "shopDistrict" to shopDistrict, "shopLat" to shopLat, "shopLng" to shopLng,
    "distanceKm" to distanceKm, "quantity" to quantity, "expiryDate" to expiryDate,
    "isNearExpiry" to isNearExpiry, "discountPercent" to discountPercent
)

// ── Shop → Firestore map ──────────────────────────────────────────────────────
fun Shop.toMap(): Map<String, Any> = mapOf(
    "id" to id, "name" to name, "village" to village, "district" to district,
    "distanceKm" to distanceKm, "phone" to phone, "ownerName" to ownerName,
    "lat" to lat, "lng" to lng, "isRegisteredByPharmacist" to isRegisteredByPharmacist
)

// ── Firestore document → StockItem ────────────────────────────────────────────
fun com.google.firebase.firestore.DocumentSnapshot.toStockItem(): StockItem? = try {
    StockItem(
        id = getString("id") ?: id, medicineId = getString("medicineId") ?: "",
        medicineName = getString("medicineName") ?: "",
        medicineCategory = getString("medicineCategory") ?: "General",
        shopId = getString("shopId") ?: "", shopName = getString("shopName") ?: "",
        shopVillage = getString("shopVillage") ?: "",
        shopDistrict = getString("shopDistrict") ?: "",
        shopLat = getDouble("shopLat") ?: 0.0, shopLng = getDouble("shopLng") ?: 0.0,
        distanceKm = getDouble("distanceKm") ?: 0.0,
        quantity = getLong("quantity")?.toInt() ?: 0,
        expiryDate = getString("expiryDate") ?: "",
        isNearExpiry = getBoolean("isNearExpiry") ?: false,
        discountPercent = getLong("discountPercent")?.toInt() ?: 0
    )
} catch (_: Exception) { null }

// ── Firestore document → Shop ─────────────────────────────────────────────────
fun com.google.firebase.firestore.DocumentSnapshot.toShop(): Shop? = try {
    Shop(
        id = getString("id") ?: id, name = getString("name") ?: "",
        village = getString("village") ?: "", district = getString("district") ?: "",
        distanceKm = getDouble("distanceKm") ?: 0.0,
        phone = getString("phone") ?: "", ownerName = getString("ownerName") ?: "",
        lat = getDouble("lat") ?: 0.0, lng = getDouble("lng") ?: 0.0,
        isRegisteredByPharmacist = getBoolean("isRegisteredByPharmacist") ?: false
    )
} catch (_: Exception) { null }
