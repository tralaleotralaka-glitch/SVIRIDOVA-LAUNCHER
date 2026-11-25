package com.sviridova.nexus.ui

import android.content.Context
import android.graphics.Color
import android.text.TextWatcher
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sviridova.nexus.R
import com.sviridova.nexus.model.AppEntry
import com.sviridova.nexus.util.withAlpha

/**
 * Полноэкранный поиск: ввод фильтрует список, Enter запускает первый результат.
 */
class SearchOverlay(context: Context) : LinearLayout(context) {

    var onLaunch: ((AppEntry) -> Unit)? = null
    var onDismiss: (() -> Unit)? = null

    private val input = EditText(context)
    private val empty = hudText(context, context.getString(R.string.search_empty), 11f, NexusTheme.TERTIARY)
    private val list = RecyclerView(context)
    private var apps: List<AppEntry> = emptyList()
    private var adapter = ResultAdapter()

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.argb(246, 5, 7, 12))
        isClickable = true
        visibility = GONE
        elevation = 20f
        setPadding(dp(18f), dp(18f), dp(18f), 0)

        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val back = HudIconButton(context).apply {
            glyph = HudIconButton.Glyph.CLOSE
            setOnClickListener { dismiss() }
        }
        header.addView(back, LinearLayout.LayoutParams(dp(44f), dp(44f)))

        input.apply {
            hint = context.getString(R.string.search_title)
            textSize = 16f
            setTextColor(NexusTheme.PRIMARY)
            setHintTextColor(NexusTheme.TERTIARY)
            typeface = NexusTheme.regularTypeface(context)
            background = null
            isSingleLine = true
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            letterSpacing = 0.06f
        }
        header.addView(input, LinearLayout.LayoutParams(0, dp(52f), 1f))
        addView(header, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56f)))

        addView(View(context).apply {
            setBackgroundColor(withAlpha(NexusTheme.accent(context), 60))
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1f)))

        list.apply {
            layoutManager = GridLayoutManager(context, 4)
            adapter = this@SearchOverlay.adapter
            overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            setPadding(0, dp(12f), 0, dp(12f))
            clipToPadding = false
        }
        addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        empty.gravity = Gravity.CENTER
        addView(empty, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40f)))
        empty.visibility = GONE

        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filter(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: android.text.Editable?) = Unit
        })

        input.setOnEditorActionListener { _, actionId, event ->
            val isSearch = actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            if (isSearch) {
                adapter.firstOrNull()?.let { launch(it) }
                true
            } else false
        }
    }

    fun setApps(list: List<AppEntry>) {
        apps = list
        filter(input.text?.toString().orEmpty())
    }

    fun show(prefill: String = "") {
        visibility = VISIBLE
        alpha = 0f
        animate().alpha(1f).setDuration(140).start()
        input.setText(prefill)
        input.setSelection(input.text.length)
        input.requestFocus()
        postDelayed({
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(input, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }, 180)
    }

    fun dismiss() {
        hideKeyboard()
        animate().alpha(0f).setDuration(120).withEndAction {
            visibility = GONE
            input.text.clear()
            onDismiss?.invoke()
        }.start()
    }

    private fun launch(entry: AppEntry) {
        dismiss()
        postDelayed({ onLaunch?.invoke(entry) }, 120)
    }

    private fun filter(query: String) {
        val q = query.trim()
        val result = if (q.isEmpty()) apps else apps.filter { it.label.contains(q, ignoreCase = true) }
        adapter.submit(result)
        empty.visibility = if (result.isEmpty() && q.isNotEmpty()) VISIBLE else GONE
    }

    private fun hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    private inner class ResultAdapter : RecyclerView.Adapter<ResultAdapter.Holder>() {

        private var items: List<AppEntry> = emptyList()

        fun submit(list: List<AppEntry>) {
            items = list
            notifyDataSetChanged()
        }

        fun firstOrNull(): AppEntry? = items.firstOrNull()

        override fun getItemCount(): Int = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = AppIconView(context)
            return Holder(view)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val item = items[position]
            holder.view.bind(item)
            holder.view.setOnClickListener { launch(item) }
        }

        inner class Holder(val view: AppIconView) : RecyclerView.ViewHolder(view)
    }

    private fun dp(value: Float) = (value * resources.displayMetrics.density).toInt()
}
