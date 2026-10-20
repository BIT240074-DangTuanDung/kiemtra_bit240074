package com.example.kiemtragiuaky.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DishDao {

    // Lấy tất cả món ăn chưa bị xóa ở Local
    @Query("SELECT * FROM dishes WHERE isDeleted = 0")
    suspend fun getAllDishes(): List<DishEntity>

    // Lấy các bản ghi chưa được đồng bộ với MockAPI (isSynced = false)
    @Query("SELECT * FROM dishes WHERE isSynced = 0")
    suspend fun getUnsyncedDishes(): List<DishEntity>

    // Chèn hoặc cập nhật một món ăn vào Room DB
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(dish: DishEntity)

    // Chèn danh sách món ăn từ Server vào Local DB
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(dishes: List<DishEntity>)

    // Xóa hoàn toàn một món ăn khỏi Room DB theo ID
    @Query("DELETE FROM dishes WHERE id = :dishId")
    suspend fun deleteById(dishId: String)

    // Đánh dấu xóa tạm thời ở Local để chờ đồng bộ DELETE lên Server
    @Query("UPDATE dishes SET isDeleted = 1, isSynced = 0 WHERE id = :dishId")
    suspend fun markAsDeleted(dishId: String)

    // Cập nhật trạng thái đồng bộ thành công
    @Query("UPDATE dishes SET isSynced = 1 WHERE id = :dishId")
    suspend fun markAsSynced(dishId: String)
}
