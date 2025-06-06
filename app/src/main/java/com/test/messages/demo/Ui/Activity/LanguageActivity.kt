package com.test.messages.demo.Ui.Activity

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.test.messages.demo.R
import com.test.messages.demo.databinding.ActivityLanguageBinding
import com.test.messages.demo.Helper.DebouncedOnClickListener
import com.test.messages.demo.Helper.LanguageChangeEvent
import com.test.messages.demo.Utils.ViewUtils
import com.test.messages.demo.Utils.ViewUtils.getLanguageName
import org.greenrobot.eventbus.EventBus
import java.util.Locale

class LanguageActivity : BaseActivity() {
    private lateinit var binding: ActivityLanguageBinding
    private var isSelected = false
    private var launchedFrom = false
    private var languageAdapter: LanguageAdapter? = null
    private var selectedLanguage: String? = ""
    private var isFirstVisit: Boolean = false
    private var pos: Int = -1

    private val langArray = arrayOf(
        Locale.ENGLISH.toString(),
        Locale("ar").toString(),
        Locale("de").toString(),
        Locale("es").toString(),
        Locale("fr").toString(),
        Locale("hi").toString(),
        Locale("af").toString(),
        Locale("bn").toString(),
        Locale("fil").toString(),
        Locale("in").toString(),
        Locale("it").toString(),
        Locale("ja").toString(),
        Locale("ko").toString(),
        Locale("pl").toString(),
        Locale("pt").toString(),
        Locale("ru").toString(),
        Locale("th").toString(),
        Locale("tr").toString(),
        Locale("uk").toString(),
        Locale("vi").toString(),
        Locale("zh").toString()
    )
    private val langFlag = intArrayOf(
        R.drawable.flag_en,
        R.drawable.flag_ar,
        R.drawable.flag_de,
        R.drawable.flag_es,
        R.drawable.flag_fr,
        R.drawable.flag_hi,
        R.drawable.flag_af,
        R.drawable.flag_bn,
        R.drawable.flag_fil,
        R.drawable.flag_in,
        R.drawable.flag_it,
        R.drawable.flag_ja,
        R.drawable.flag_ko,
        R.drawable.flag_pl,
        R.drawable.flag_pt,
        R.drawable.flag_ru,
        R.drawable.flag_th,
        R.drawable.flag_tr,
        R.drawable.flag_uk,
        R.drawable.flag_vi,
        R.drawable.flag_zh
    )

    private fun setupAnimatedIndicator() {
        binding.animatedIndicator.startAnimation(
            AnimationUtils.loadAnimation(
                this,
                R.anim.pulsing_animation
            )
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("launched_from", launchedFrom.toString())
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLanguageBinding.inflate(layoutInflater)
        val view: View = binding.getRoot()
        setContentView(view)
        applyWindowInsetsToView(binding.rootView)
        val launchedFrom = intent.getStringExtra("launched_from") ?: "first_time"

        isFirstVisit = !ViewUtils.isLanguageSelected(this)
        if (isFirstVisit) {
            binding.title.text = getString(R.string.select_language)
            binding.icBack.visibility = View.GONE
            binding.titleMain.visibility = View.VISIBLE
            binding.title.visibility = View.GONE
            binding.animatedIndicator.visibility = View.VISIBLE
            setupAnimatedIndicator()
        } else {
            selectedLanguage = ViewUtils.getSelectedLanguage(this)
            pos = langArray.indexOf(selectedLanguage)
            binding.title.text = getString(R.string.language)
            binding.icBack.visibility = View.VISIBLE
            binding.titleMain.visibility = View.GONE
            binding.title.visibility = View.VISIBLE
            binding.animatedIndicator.visibility = View.INVISIBLE
        }

        binding.icBack.setOnClickListener {
            onBackPressed()
        }

        binding.langRecyclerView!!.layoutManager =
            LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        languageAdapter = LanguageAdapter(this, langFlag, langArray, pos)
        binding.langRecyclerView!!.adapter = languageAdapter

        binding.btnDone.setOnClickListener {
            val selectedLanguage = languageAdapter?.getSelectedLanguage()
            if (selectedLanguage == null) {
                Toast.makeText(
                    this,
                    getString(R.string.please_select_a_language),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            ViewUtils.setSelectedLanguage(this, selectedLanguage)
            ViewUtils.setLanguageSelected(this)
            EventBus.getDefault().post(LanguageChangeEvent(selectedLanguage))

            if (launchedFrom.equals("settings")) {
                setResult(RESULT_OK)
                finish()
            } else {
                if (!ViewUtils.isIntroShown(this)) {
                    startActivity(Intent(this, IntroActivity::class.java))
                    finish()
                }

            }
        }

    }


    override fun onBackPressed() {
        if (launchedFrom.equals("settings")) {
            finish()
        } else {
            super.onBackPressed()
        }
    }

    internal inner class LanguageAdapter(
        private val context: Context,
        private val languageFlag: IntArray,
        private val languageOptions: Array<String>,
        private var selectedPosition: Int
    ) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_language, parent, false)
            return LanguageViewHolder(view)
        }

        override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
            holder.bind(languageOptions[position], languageFlag[position])
            holder.itemView.setOnClickListener(object :
                DebouncedOnClickListener(500L) {
                override fun onDebouncedClick(v: View) {
                    isSelected = true
                    selectedPosition = position
                    notifyDataSetChanged()
                }
            })

            if (selectedPosition == position) {
                holder.imgCheck.setImageResource(R.drawable.ic_selected2);
                holder.lang_ly.setBackgroundResource(R.drawable.lang_item_select_bg);
            } else {
                holder.imgCheck.setImageResource(R.drawable.ic_unselected);
                holder.lang_ly.setBackgroundResource(R.drawable.lang_item_bg);
            }
        }

        override fun getItemCount(): Int {
            return languageOptions.size
        }

        fun getSelectedLanguage(): String? {
            return if (selectedPosition in languageOptions.indices) {
                languageOptions[selectedPosition]
            } else {
                null
            }
        }


        internal inner class LanguageViewHolder(itemView: View) :
            RecyclerView.ViewHolder(itemView) {
            val languageTextView: TextView
            private val imgFlag: ImageView
            val imgCheck: ImageView
            val lang_ly: ConstraintLayout

            init {
                languageTextView = itemView.findViewById(R.id.languageTextView)
                imgFlag = itemView.findViewById(R.id.img_flag)
                imgCheck = itemView.findViewById(R.id.icSelect)
                lang_ly = itemView.findViewById(R.id.lang_ly)
            }

            fun bind(language: String?, flag: Int) {
                languageTextView.text = getLanguageName(itemView.context,language)
                imgFlag.setImageResource(flag)

            }

        }
    }

    override fun attachBaseContext(newBase: Context?) {
        val language = ViewUtils.getSelectedLanguage(newBase ?: return)
        val context = newBase.let {
            val locale = Locale(language ?: "en")
            Locale.setDefault(locale)

            val config = Configuration()
            config.setLocale(locale)
            it.createConfigurationContext(config)
        }
        super.attachBaseContext(context)
    }

}