package com.test.messages.demo.ui.Activity

import android.content.Context
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.test.messages.demo.AlphabticScroll.ItemMoveCallback
import com.test.messages.demo.R
import com.test.messages.demo.databinding.ActivityEditCategoryBinding
import com.test.messages.demo.ui.Adapter.EditCategoryAdapter
import com.test.messages.demo.ui.Utils.ViewUtils
import com.test.messages.demo.ui.reciever.CategoryUpdateEvent
import com.test.messages.demo.ui.reciever.CategoryVisibilityEvent
import org.greenrobot.eventbus.EventBus
import org.json.JSONArray

class EditCategoryActivity : BaseActivity() {
    private lateinit var binding: ActivityEditCategoryBinding
    private lateinit var adapter: EditCategoryAdapter
    private var categories = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditCategoryBinding.inflate(layoutInflater)
        val view: View = binding.getRoot()
        setContentView(view)

        categories =
            intent.getStringArrayListExtra("category_list")?.toMutableList() ?: mutableListOf()


//        adapter = EditCategoryAdapter(categories)
        adapter = EditCategoryAdapter(categories) { updatedList ->
            saveCategories(updatedList)
        }
        binding.recyclerViewCategories.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewCategories.adapter = adapter


        val itemTouchHelper = ItemTouchHelper(ItemMoveCallback(adapter))
        itemTouchHelper.attachToRecyclerView(binding.recyclerViewCategories)

        val sharedPrefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        binding.categorySwitch.isChecked = sharedPrefs.getBoolean("SHOW_CATEGORIES", true)
        binding.categorySwitch.setOnCheckedChangeListener { _, isChecked ->
            ViewUtils.setCategoryEnabled(this,isChecked)
            EventBus.getDefault().post(CategoryVisibilityEvent(isChecked))
        }
        binding.icBack.setOnClickListener {
            onBackPressed()
        }

    }

    private fun saveCategories(updatedList: List<String>) {
        val orderedList = mutableListOf<String>().apply {
            add(getString(R.string.inbox)) // Ensure Inbox is always first
            addAll(updatedList.filterNot { it == getString(R.string.inbox) }) // Add others
        }

        Log.d("EditCategoryActivity", "Saving Categories: $orderedList")

        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        val json = JSONArray(orderedList).toString()
        sharedPrefs.edit().putString("CATEGORY_ORDER", json).apply()

        Log.d("EditCategoryActivity", "Saved CATEGORY_ORDER JSON: $json")

        EventBus.getDefault().post(CategoryUpdateEvent(orderedList))
    }



}
