package com.example.kiemtragiuaky

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.kiemtragiuaky.data.repository.SyncManager
import com.example.kiemtragiuaky.databinding.ActivityAddEditDishBinding
import com.example.kiemtragiuaky.model.Dish
import com.example.kiemtragiuaky.model.DishCategory
import com.example.kiemtragiuaky.model.DishImage
import com.example.kiemtragiuaky.network.RetrofitClient
import com.example.kiemtragiuaky.utils.LocaleHelper
import kotlinx.coroutines.launch

class AddEditDishActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditDishBinding
    private val syncManager by lazy { SyncManager(this) }

    private var editingDish: Dish? = null
    private var categoryList: List<DishCategory> = emptyList()
    private var existingDishList: List<Dish> = emptyList()
    private var selectedCategoryId: String? = null

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditDishBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Bật nút mũi tên Back trên Toolbar/ActionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Kiểm tra xem là Thêm mới hay Chỉnh sửa
        editingDish = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("EXTRA_DISH", Dish::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("EXTRA_DISH") as? Dish
        }

        // Đặt tiêu đề tương ứng
        if (editingDish != null) {
            supportActionBar?.title = getString(R.string.title_edit_dish)
        } else {
            supportActionBar?.title = getString(R.string.title_add_dish)
        }

        loadInitialData()

        // Sự kiện lưu dữ liệu
        binding.btnSaveDish.setOnClickListener {
            validateAndSaveDish()
        }
    }

    // 1. Đăng ký sự kiện nút Back (onOptionsItemSelected) để quay về màn hình trước
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    // Tải danh sách loại món và các món ăn đã có để kiểm tra ràng buộc
    private fun loadInitialData() {
        binding.progressBarAddEdit.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                categoryList = RetrofitClient.apiService.getCategories()
                existingDishList = syncManager.getDishes() // Lấy qua SyncManager hỗ trợ offline

                setupCategorySpinner()

                // 3. Nếu là Chỉnh sửa: Điền sẵn thông tin cũ vào form
                editingDish?.let { dish ->
                    populateDishData(dish)
                }
            } catch (e: Exception) {
                Toast.makeText(this@AddEditDishActivity, "Lỗi tải dữ liệu: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBarAddEdit.visibility = View.GONE
            }
        }
    }

    // Load danh sách loại món vào Spinner
    private fun setupCategorySpinner() {
        val categoryNames = mutableListOf(getString(R.string.hint_select_category))
        categoryNames.addAll(categoryList.map { it.BIT240074_Name })

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDishCategory.adapter = adapter

        binding.spinnerDishCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedCategoryId = if (position == 0) null else categoryList.getOrNull(position - 1)?.id
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Nếu đang sửa món, chọn đúng vị trí loại món cũ
        editingDish?.let { dish ->
            val index = categoryList.indexOfFirst { it.id == dish.BIT240074_DishCategoryId }
            if (index != -1) {
                binding.spinnerDishCategory.setSelection(index + 1)
                selectedCategoryId = dish.BIT240074_DishCategoryId
            }
        }
    }

    // Điền thông tin cũ vào các ô nhập liệu
    private fun populateDishData(dish: Dish) {
        binding.etDishName.setText(dish.BIT240074_Name)
        binding.etDishPrice.setText(dish.BIT240074_Price.toString())
        binding.etDishPrepTime.setText(dish.BIT240074_PreparationTime.toString())
        binding.cbDishAvailable.isChecked = dish.BIT240074_IsAvailable
        binding.etDishDescription.setText(dish.BIT240074_Description)

        val firstImageUrl = dish.BIT240074_DishImage.firstOrNull()?.imageUrl ?: ""
        binding.etDishImageUrl.setText(firstImageUrl)
    }

    // 2. Kiểm tra ràng buộc dữ liệu & xử lý lưu
    private fun validateAndSaveDish() {
        val name = binding.etDishName.text.toString().trim()
        val priceStr = binding.etDishPrice.text.toString().trim()
        val prepTimeStr = binding.etDishPrepTime.text.toString().trim()
        val description = binding.etDishDescription.text.toString().trim()
        val imageUrl = binding.etDishImageUrl.text.toString().trim()
        val isAvailable = binding.cbDishAvailable.isChecked

        // Ràng buộc 1: BIT240074_Name không được để trống
        if (name.isBlank()) {
            binding.etDishName.error = getString(R.string.err_name_empty)
            binding.etDishName.requestFocus()
            return
        }

        // Ràng buộc 5: Phải chọn loại món hợp lệ
        val categoryId = selectedCategoryId
        if (categoryId == null) {
            Toast.makeText(this, getString(R.string.err_category_invalid), Toast.LENGTH_SHORT).show()
            return
        }

        // Ràng buộc 2: Tên món không trùng với món khác trong cùng loại món
        val isDuplicate = existingDishList.any { dish ->
            val isSameCategory = dish.BIT240074_DishCategoryId == categoryId
            val isSameName = dish.BIT240074_Name.equals(name, ignoreCase = true)
            val isDifferentDish = editingDish == null || dish.id != editingDish?.id

            isSameCategory && isSameName && isDifferentDish
        }

        if (isDuplicate) {
            binding.etDishName.error = getString(R.string.err_name_duplicate)
            binding.etDishName.requestFocus()
            return
        }

        // Ràng buộc 3: BIT240074_Price phải > 0
        val price = priceStr.toDoubleOrNull()
        if (price == null || price <= 0) {
            binding.etDishPrice.error = getString(R.string.err_price_invalid)
            binding.etDishPrice.requestFocus()
            return
        }

        // Ràng buộc 4: BIT240074_PreparationTime phải > 0
        val prepTime = prepTimeStr.toIntOrNull()
        if (prepTime == null || prepTime <= 0) {
            binding.etDishPrepTime.error = getString(R.string.err_prep_time_invalid)
            binding.etDishPrepTime.requestFocus()
            return
        }

        val images = if (imageUrl.isNotBlank()) {
            listOf(DishImage(imageUrl = imageUrl, isThumbnail = true))
        } else {
            editingDish?.BIT240074_DishImage ?: emptyList()
        }

        val dishId = editingDish?.id?.takeIf { it.isNotBlank() } ?: "temp_${System.currentTimeMillis()}"

        val dishToSave = Dish(
            id = dishId,
            BIT240074_Name = name,
            BIT240074_Price = price,
            BIT240074_PreparationTime = prepTime,
            BIT240074_IsAvailable = isAvailable,
            BIT240074_Description = description,
            BIT240074_DishCategoryId = categoryId,
            BIT240074_DishImage = images,
        )

        // 3. Gọi SyncManager Lưu dữ liệu (Hỗ trợ Offline-first & tự động đồng bộ)
        saveDishOfflineFirst(dishToSave, editingDish != null)
    }

    private fun saveDishOfflineFirst(dish: Dish, isEditing: Boolean) {
        binding.progressBarAddEdit.visibility = View.VISIBLE
        binding.btnSaveDish.isEnabled = false

        lifecycleScope.launch {
            try {
                syncManager.saveDish(dish, isEditing)
                Toast.makeText(this@AddEditDishActivity, getString(R.string.msg_save_success), Toast.LENGTH_SHORT).show()
                finish() // Sau khi lưu thành công, thông báo Toast và gọi finish() để quay lại
            } catch (e: Exception) {
                Toast.makeText(this@AddEditDishActivity, "Lỗi khi lưu món ăn: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBarAddEdit.visibility = View.GONE
                binding.btnSaveDish.isEnabled = true
            }
        }
    }
}
