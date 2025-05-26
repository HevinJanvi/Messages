package com.test.messages.demo.ui.Activity

import android.content.Context
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.test.messages.demo.ui.AlphabticScroll.ItemMoveCallback
import com.test.messages.demo.R
import com.test.messages.demo.databinding.ActivityEditCategoryBinding
import com.test.messages.demo.ui.Adapter.EditCategoryAdapter
import com.test.messages.demo.Util.ViewUtils
import com.test.messages.demo.Util.CategoryUpdateEvent
import com.test.messages.demo.Util.CategoryVisibilityEvent
import com.test.messages.demo.Util.CommanConstants
import com.test.messages.demo.Util.CommanConstants.CATEGORY_ORDER
import com.test.messages.demo.Util.CommanConstants.SHOW_CATEGORIES
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
        applyWindowInsetsToView(binding.rootView)
        categories =
            intent.getStringArrayListExtra("category_list")?.toMutableList() ?: mutableListOf()

        adapter = EditCategoryAdapter(categories) { updatedList ->
            saveCategories(updatedList)
        }
        binding.recyclerViewCategories.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewCategories.adapter = adapter


        val itemTouchHelper = ItemTouchHelper(ItemMoveCallback(adapter))
        itemTouchHelper.attachToRecyclerView(binding.recyclerViewCategories)

        val sharedPrefs = getSharedPreferences(CommanConstants.PREFS_NAME, Context.MODE_PRIVATE)
        binding.categorySwitch.isChecked = sharedPrefs.getBoolean(SHOW_CATEGORIES, true)
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
            add(getString(R.string.inbox))
            addAll(updatedList.filterNot { it == getString(R.string.inbox) })
        }
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        val json = JSONArray(orderedList).toString()
        sharedPrefs.edit().putString(CATEGORY_ORDER, json).apply()
        EventBus.getDefault().post(CategoryUpdateEvent(orderedList))
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }


}
