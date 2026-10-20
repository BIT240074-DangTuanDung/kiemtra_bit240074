package com.example.kiemtragiuaky

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kiemtragiuaky.adapter.DishImageAdapter
import com.example.kiemtragiuaky.data.repository.SyncManager
import com.example.kiemtragiuaky.databinding.ActivityDetailBinding
import com.example.kiemtragiuaky.model.Dish
import com.example.kiemtragiuaky.network.RetrofitClient
import com.example.kiemtragiuaky.utils.LocaleHelper
import kotlinx.coroutines.launch
import java.util.Locale

class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding
    private var currentDish: Dish? = null
    private val syncManager by lazy { SyncManager(this) }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.title_detail)

        currentDish = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("EXTRA_DISH", Dish::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("EXTRA_DISH") as? Dish
        }

        currentDish?.let { dish ->
            displayDishDetails(dish)
            fetchCategoryName(dish.BIT240074_DishCategoryId)
        } ?: run {
            Toast.makeText(this, getString(R.string.msg_not_found), Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.btnEditDish.setOnClickListener {
            currentDish?.let { dish ->
                val intent = Intent(this, AddEditDishActivity::class.java).apply {
                    putExtra("EXTRA_DISH", dish)
                }
                startActivity(intent)
            }
        }

        binding.btnDeleteDish.setOnClickListener {
            showDeleteConfirmDialog()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun displayDishDetails(dish: Dish) {
        binding.tvDetailName.text = dish.BIT240074_Name
        binding.tvDetailPrice.text = String.format(Locale.getDefault(), "%.0f VNĐ", dish.BIT240074_Price)

        val prepTimeLabel = if (dish.BIT240074_PreparationTime <= 15) {
            getString(R.string.prep_quick)
        } else {
            getString(R.string.prep_regular)
        }
        binding.tvDetailPrepTime.text = getString(R.string.prep_time_format, dish.BIT240074_PreparationTime, prepTimeLabel)

        val availabilityText = if (dish.BIT240074_IsAvailable) getString(R.string.status_available) else getString(R.string.status_unavailable)
        binding.tvDetailAvailability.text = getString(R.string.status_format, availabilityText)

        binding.tvDetailDescription.text = if (dish.BIT240074_Description.isNotBlank()) {
            dish.BIT240074_Description
        } else {
            getString(R.string.no_description)
        }

        binding.rvDetailImages.apply {
            layoutManager = LinearLayoutManager(this@DetailActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = DishImageAdapter(dish.BIT240074_DishImage)
        }
    }

    private fun fetchCategoryName(categoryId: String) {
        lifecycleScope.launch {
            try {
                val categories = RetrofitClient.apiService.getCategories()
                val categoryName = categories.find { it.id == categoryId }?.BIT240074_Name ?: categoryId
                binding.tvDetailCategory.text = getString(R.string.category_format, categoryName)
            } catch (_: Exception) {
                binding.tvDetailCategory.text = "Loại món ID: $categoryId"
            }
        }
    }

    private fun showDeleteConfirmDialog() {
        val dish = currentDish ?: return
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.confirm_delete_title))
            .setMessage(getString(R.string.confirm_delete_msg, dish.BIT240074_Name))
            .setPositiveButton(getString(R.string.btn_delete)) { _, _ ->
                deleteDishOfflineFirst(dish.id)
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    private fun deleteDishOfflineFirst(dishId: String) {
        binding.progressBarDetail.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                syncManager.deleteDish(dishId)
                Toast.makeText(this@DetailActivity, getString(R.string.msg_delete_success), Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@DetailActivity, "Lỗi khi xóa món: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBarDetail.visibility = View.GONE
            }
        }
    }
}
