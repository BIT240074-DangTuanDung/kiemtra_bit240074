package com.example.kiemtragiuaky.data.repository

import android.content.Context
import com.example.kiemtragiuaky.data.local.AppDatabase
import com.example.kiemtragiuaky.data.local.DishEntity
import com.example.kiemtragiuaky.model.Dish
import com.example.kiemtragiuaky.network.RetrofitClient
import com.example.kiemtragiuaky.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncManager(context: Context) {

    private val dishDao = AppDatabase.getDatabase(context).dishDao()
    private val apiService = RetrofitClient.apiService
    private val appContext = context.applicationContext

    // Lấy danh sách món ăn: Tự động đồng bộ khi có mạng, nếu mất mạng lấy từ Room DB local
    suspend fun getDishes(): List<Dish> = withContext(Dispatchers.IO) {
        if (NetworkUtils.isNetworkAvailable(appContext)) {
            try {
                // 1. Đồng bộ các thay đổi offline lên Server
                syncUnsyncedData()

                // 2. Tải danh sách mới nhất từ Server lưu vào Room DB local
                val serverDishes = apiService.getDishes()
                val entities = serverDishes.map { DishEntity.fromDish(it, isSynced = true, isDeleted = false) }
                dishDao.insertAll(entities)
            } catch (_: Exception) {
                // Nếu gọi server lỗi, dùng tạm dữ liệu local
            }
        }
        val localEntities = dishDao.getAllDishes()
        return@withContext localEntities.map { it.toDish() }
    }

    // Tự động quét các bản ghi isSynced = false đẩy lên MockAPI
    suspend fun syncUnsyncedData() = withContext(Dispatchers.IO) {
        if (!NetworkUtils.isNetworkAvailable(appContext)) return@withContext

        val unsyncedEntities = dishDao.getUnsyncedDishes()
        for (entity in unsyncedEntities) {
            try {
                val dish = entity.toDish()
                if (entity.isDeleted) {
                    if (entity.id.isNotBlank() && !entity.id.startsWith("temp_")) {
                        apiService.deleteDish(entity.id)
                    }
                    dishDao.deleteById(entity.id)
                } else {
                    if (entity.id.isBlank() || entity.id.startsWith("temp_") || entity.id.length < 5) {
                        val created = apiService.createDish(dish)
                        dishDao.deleteById(entity.id)
                        dishDao.insertOrUpdate(DishEntity.fromDish(created, isSynced = true, isDeleted = false))
                    } else {
                        try {
                            apiService.updateDish(dish.id, dish)
                            dishDao.markAsSynced(dish.id)
                        } catch (_: Exception) {
                            val created = apiService.createDish(dish)
                            dishDao.deleteById(entity.id)
                            dishDao.insertOrUpdate(DishEntity.fromDish(created, isSynced = true, isDeleted = false))
                        }
                    }
                }
            } catch (_: Exception) {
                // Bỏ qua lỗi item hiện tại, tiếp tục đồng bộ item khác
            }
        }
    }

    // Lưu (Thêm/Sửa) món ăn hỗ trợ Offline First
    suspend fun saveDish(dish: Dish, isEditing: Boolean) = withContext(Dispatchers.IO) {
        if (NetworkUtils.isNetworkAvailable(appContext)) {
            try {
                syncUnsyncedData()
                if (isEditing) {
                    apiService.updateDish(dish.id, dish)
                } else {
                    apiService.createDish(dish)
                }
                dishDao.insertOrUpdate(DishEntity.fromDish(dish, isSynced = true, isDeleted = false))
                return@withContext
            } catch (_: Exception) {
                // Nếu API lỗi, lưu offline
            }
        }

        // Lưu local khi không có mạng (isSynced = false)
        val entity = DishEntity.fromDish(dish, isSynced = false, isDeleted = false)
        dishDao.insertOrUpdate(entity)
    }

    // Xóa món ăn hỗ trợ Offline First
    suspend fun deleteDish(dishId: String) = withContext(Dispatchers.IO) {
        if (NetworkUtils.isNetworkAvailable(appContext)) {
            try {
                syncUnsyncedData()
                apiService.deleteDish(dishId)
                dishDao.deleteById(dishId)
                return@withContext
            } catch (_: Exception) {
                // Nếu API lỗi, đánh dấu xóa offline
            }
        }

        // Đánh dấu xóa local (isDeleted = true, isSynced = false)
        dishDao.markAsDeleted(dishId)
    }
}
