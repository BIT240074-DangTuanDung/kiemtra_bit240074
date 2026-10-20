package com.example.kiemtragiuaky

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kiemtragiuaky.adapter.DishAdapter
import com.example.kiemtragiuaky.data.repository.SyncManager
import com.example.kiemtragiuaky.databinding.ActivityMainBinding
import com.example.kiemtragiuaky.model.Dish
import com.example.kiemtragiuaky.model.DishCategory
import com.example.kiemtragiuaky.network.RetrofitClient
import com.example.kiemtragiuaky.utils.LocaleHelper
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var dishAdapter: DishAdapter
    private val syncManager by lazy { SyncManager(this) }

    // Cache dữ liệu gốc tải từ API hoặc Room DB (Hỗ trợ offline)
    private var originalDishList: List<Dish> = emptyList()
    private var categoryList: List<DishCategory> = emptyList()

    // Lưu trạng thái điều kiện lọc & sắp xếp hiện tại
    private var searchQuery: String = ""
    private var selectedCategoryId: String? = null // null tương ứng chọn "Tất cả"
    private var isAvailableOnly: Boolean = false
    private var minPrice: Double? = null
    private var maxPrice: Double? = null
    private var selectedSortType: Int = 0 // 0: Mặc định, 1: Giá tăng, 2: Giá giảm, 3: Thời gian chế biến tăng

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupFilterListeners()
        setupSortSpinner()

        // Nút thêm món để chuyển sang AddEditDishActivity
        binding.fabAddDish.setOnClickListener {
            val intent = Intent(this, AddEditDishActivity::class.java)
            startActivity(intent)
        }

        // Tải dữ liệu (tự động đồng bộ ngầm khi có mạng hoặc lấy từ Room DB local)
        fetchDataAndSync()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_change_language -> {
                showLanguageDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showLanguageDialog() {
        val currentLang = LocaleHelper.getSavedLanguage(this)
        val options = arrayOf(getString(R.string.lang_english), getString(R.string.lang_vietnamese))
        val checkedItem = if (currentLang == LocaleHelper.LANG_ENGLISH) 0 else 1

        AlertDialog.Builder(this)
            .setTitle(R.string.change_language)
            .setSingleChoiceItems(options, checkedItem) { dialog, which ->
                val newLang = if (which == 0) LocaleHelper.LANG_ENGLISH else LocaleHelper.LANG_VIETNAMESE
                if (newLang != currentLang) {
                    LocaleHelper.setLocale(this, newLang)
                    recreate()
                }
                dialog.dismiss()
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        fetchDataAndSync()
    }

    private fun setupRecyclerView() {
        dishAdapter = DishAdapter()
        binding.rvDishes.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = dishAdapter
        }
    }

    // Tải dữ liệu và đồng bộ offline qua SyncManager
    private fun fetchDataAndSync() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                // Tải danh mục (có thể bắt lỗi nếu offline hoàn toàn)
                try {
                    categoryList = RetrofitClient.apiService.getCategories()
                } catch (_: Exception) {}

                // Lấy danh sách món ăn từ SyncManager (Tự động đồng bộ ngầm hoặc lấy từ Room DB local)
                originalDishList = syncManager.getDishes()

                setupCategorySpinner()
                applyFilterAndSort()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Lỗi tải dữ liệu: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    // Cấu hình Spinner chọn Loại món
    private fun setupCategorySpinner() {
        val categoryNames = mutableListOf(getString(R.string.category_all))
        categoryNames.addAll(categoryList.map { it.BIT240074_Name })

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = adapter

        val currentSelectionIndex = if (selectedCategoryId == null) 0 else {
            categoryList.indexOfFirst { it.id == selectedCategoryId }.let { if (it != -1) it + 1 else 0 }
        }
        binding.spinnerCategory.setSelection(currentSelectionIndex, false)

        binding.spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedCategoryId = if (position == 0) null else categoryList.getOrNull(position - 1)?.id
                applyFilterAndSort()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    // Cấu hình Spinner Sắp xếp
    private fun setupSortSpinner() {
        val sortOptions = listOf(
            getString(R.string.sort_default),
            getString(R.string.sort_price_asc),
            getString(R.string.sort_price_desc),
            getString(R.string.sort_prep_asc),
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sortOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSort.adapter = adapter
        binding.spinnerSort.setSelection(selectedSortType, false)

        binding.spinnerSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedSortType = position
                applyFilterAndSort()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupFilterListeners() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s.toString().trim()
                applyFilterAndSort()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.cbAvailableOnly.setOnCheckedChangeListener { _, isChecked ->
            isAvailableOnly = isChecked
            applyFilterAndSort()
        }

        binding.etMinPrice.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                minPrice = s.toString().toDoubleOrNull()
                applyFilterAndSort()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.etMaxPrice.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                maxPrice = s.toString().toDoubleOrNull()
                applyFilterAndSort()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun applyFilterAndSort() {
        var filteredList = originalDishList.filter { dish ->
            val matchesName = searchQuery.isEmpty() ||
                    dish.BIT240074_Name.contains(searchQuery, ignoreCase = true)

            val matchesCategory = selectedCategoryId == null ||
                    dish.BIT240074_DishCategoryId == selectedCategoryId

            val matchesAvailable = !isAvailableOnly || dish.BIT240074_IsAvailable

            val matchesMinPrice = minPrice == null || dish.BIT240074_Price >= minPrice!!
            val matchesMaxPrice = maxPrice == null || dish.BIT240074_Price <= maxPrice!!

            matchesName && matchesCategory && matchesAvailable && matchesMinPrice && matchesMaxPrice
        }

        filteredList = when (selectedSortType) {
            1 -> filteredList.sortedBy { it.BIT240074_Price }
            2 -> filteredList.sortedByDescending { it.BIT240074_Price }
            3 -> filteredList.sortedBy { it.BIT240074_PreparationTime }
            else -> filteredList
        }

        dishAdapter.updateData(filteredList)
    }
}
