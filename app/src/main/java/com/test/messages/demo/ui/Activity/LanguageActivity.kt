package com.test.messages.demo.ui.Activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.test.messages.demo.R
import com.test.messages.demo.databinding.ActivityLanguageBinding
import com.test.messages.demo.Util.DebouncedOnClickListener
import com.test.messages.demo.Util.ViewUtils
import java.util.Locale

class LanguageActivity : BaseActivity() {
    private lateinit var binding: ActivityLanguageBinding
    private var isSelected = false
    private var languageAdapter: LanguageAdapter? = null
    private var selectedLanguage: String? = ""
    private var isFirstVisit: Boolean = false

    private val langArray = arrayOf(
        Locale.ENGLISH.toString(),
        Locale("af").toString(),
        Locale("ar").toString(),
        Locale("bn").toString(),
        Locale("fil").toString(),
        Locale("fr").toString(),
        Locale("de").toString(),
        Locale("hi").toString(),
        Locale("in").toString(),
        Locale("it").toString(),
        Locale("ja").toString(),
        Locale("ko").toString(),
        Locale("pl").toString(),
        Locale("pt").toString(),
        Locale("ru").toString(),
        Locale("es").toString(),
        Locale("th").toString(),
        Locale("tr").toString(),
        Locale("uk").toString(),
        Locale("vi").toString(),
        Locale("zh").toString()
    )
    private val langFlag = intArrayOf(
        R.drawable.flag_en,
        R.drawable.flag_af,
        R.drawable.flag_ar,
        R.drawable.flag_bn,
        R.drawable.flag_fil,
        R.drawable.flag_fr,
        R.drawable.flag_de,
        R.drawable.flag_hi,
        R.drawable.flag_in,
        R.drawable.flag_it,
        R.drawable.flag_ja,
        R.drawable.flag_ko,
        R.drawable.flag_pl,
        R.drawable.flag_pt,
        R.drawable.flag_ru,
        R.drawable.flag_es,
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLanguageBinding.inflate(layoutInflater)
        val view: View = binding.getRoot()
        setContentView(view)

        isFirstVisit = !ViewUtils.isLanguageSelected(this)
        if (isFirstVisit) {
            binding.animatedIndicator.visibility = View.VISIBLE
            setupAnimatedIndicator()
        } else {
            binding.animatedIndicator.visibility = View.INVISIBLE
        }

        selectedLanguage = ViewUtils.getSelectedLanguage(this)
        val pos = langArray.indexOf(selectedLanguage)

        binding.langRecyclerView!!.layoutManager =
            LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        languageAdapter = LanguageAdapter(this, langFlag, langArray, pos)
        binding.langRecyclerView!!.adapter = languageAdapter

        binding.btnDone.setOnClickListener {
            selectedLanguage = languageAdapter?.getSelectedLanguage()
            selectedLanguage?.let { ViewUtils.setSelectedLanguage(this, it) }
            ViewUtils.setLanguageSelected(this)

            if (!ViewUtils.isIntroShown(this)) {
                startActivity(Intent(this,IntroActivity::class.java))
                finish()
            } else {
                startActivity(Intent(this,MainActivity::class.java))
                finish()
            }
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
            holder.languageTextView.isSelected = true
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
                holder.imgCheck.setImageResource(R.drawable.ic_selected);
                holder.lang_ly.setBackgroundResource(R.drawable.lang_item_select_bg);
            } else {
                holder.imgCheck.setImageResource(R.drawable.ic_unselected);
                holder.lang_ly.setBackgroundResource(R.drawable.lang_item_bg);
            }
        }

        override fun getItemCount(): Int {
            return languageOptions.size
        }

        fun getSelectedLanguage(): String = languageOptions[selectedPosition]


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
                val mainText: String
                val subText: String

                when (language) {
                    "en" -> {
                        mainText = resources.getString(R.string.english)
                        subText = getString(R.string.subtext_english)
                    }

                    "af" -> {
                        mainText = resources.getString(R.string.afrikaans)
                        subText = getString(R.string.subtext_afrikaans)
                    }

                    "ar" -> {
                        mainText = resources.getString(R.string.arabic)
                        subText = getString(R.string.subtext_arabic)
                    }

                    "bn" -> {
                        mainText = resources.getString(R.string.bangla)
                        subText = getString(R.string.subtext_bangla)
                    }

                    "fil" -> {
                        mainText = resources.getString(R.string.filipino)
                        subText = getString(R.string.subtext_fillipino)
                    }

                    "fr" -> {
                        mainText = resources.getString(R.string.french)
                        subText = getString(R.string.subtext_French)
                    }

                    "de" -> {
                        mainText = resources.getString(R.string.german)
                        subText = getString(R.string.subtext_German)
                    }

                    "hi" -> {
                        mainText = resources.getString(R.string.hindi)
                        subText = getString(R.string.subtext_Indian)
                    }

                    "in" -> {
                        mainText = resources.getString(R.string.indonesian)
                        subText = getString(R.string.subtext_Indonesia)
                    }

                    "it" -> {
                        mainText = resources.getString(R.string.italian)
                        subText = getString(R.string.subtext_italian)
                    }

                    "ja" -> {
                        mainText = resources.getString(R.string.japanese)
                        subText = getString(R.string.subtext_Japanese)
                    }

                    "ko" -> {
                        mainText = resources.getString(R.string.korean)
                        subText = getString(R.string.subtext_Korean)
                    }

                    "pl" -> {
                        mainText = resources.getString(R.string.polish)
                        subText = getString(R.string.subtext_polish)
                    }

                    "pt" -> {
                        mainText = resources.getString(R.string.portuguese)
                        subText = getString(R.string.subtext_portugal)
                    }

                    "ru" -> {
                        mainText = resources.getString(R.string.russian)
                        subText = getString(R.string.subtext_Russian)
                    }

                    "es" -> {
                        mainText = resources.getString(R.string.spanish)
                        subText = getString(R.string.subtext_Spanish)
                    }

                    "th" -> {
                        mainText = resources.getString(R.string.thai)
                        subText = getString(R.string.subtext_thai)
                    }

                    "tr" -> {
                        mainText = resources.getString(R.string.turkish)
                        subText = getString(R.string.subtext_turkish)
                    }

                    "uk" -> {
                        mainText = resources.getString(R.string.ukrainian)
                        subText = getString(R.string.subtext_ukrainian)
                    }

                    "vi" -> {
                        mainText = resources.getString(R.string.vietnamese)
                        subText = getString(R.string.subtext_Vietnamese)
                    }

                    "zh" -> {
                        mainText = resources.getString(R.string.chinese)
                        subText = getString(R.string.subtext_chinese)
                    }

                    else -> {
                        mainText = language ?: ""
                        subText = ""
                    }
                }

                val spannable = SpannableString("$mainText $subText")
                spannable.setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(context, R.color.subtext_color)),
                    mainText.length, spannable.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                languageTextView.text = spannable
                Glide.with(context).load(flag)
                    .placeholder(R.mipmap.ic_launcher)
                    .into(imgFlag)
            }

        }
    }

}