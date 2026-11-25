package com.sviridova.nexus.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.TextView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.sviridova.nexus.Prefs
import com.sviridova.nexus.model.AppEntry
import com.sviridova.nexus.util.withAlpha
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Горизонтально листаемая сетка приложений с индикатором страниц.
 * Количество строк считается по доступной высоте, столбцов — из настроек.
 */
class AppPagerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    var onLaunch: ((AppEntry) -> Unit)? = null
    var onLongPress: ((AppEntry, android.view.View) -> Unit)? = null

    private val pager = ViewPager2(context).apply {
        orientation = ViewPager2.ORIENTATION_HORIZONTAL
        offscreenPageLimit = 1
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
    }
    private val dots = DotsView(context)
    private val emptyHint = TextView(context).apply {
        text = context.getString(com.sviridova.nexus.R.string.apps_empty).uppercase()
        setTextColor(NexusTheme.TERTIARY)
        textSize = 11f
        gravity = Gravity.CENTER
        letterSpacing = 0.2f
        typeface = NexusTheme.semiboldTypeface(context)
        visibility = View.GONE
    }

    private var apps: List<AppEntry> = emptyList()
    private var rows = 3
    private var pageAdapter: PagerAdapter? = null

    init {
        clipChildren = false
        addView(pager, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(dots, LayoutParams(LayoutParams.WRAP_CONTENT, context.densityInt(22f), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
            bottomMargin = context.densityInt(2f)
        })
        addView(emptyHint, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                dots.setDots(pageAdapter?.pageCount ?: 0, position)
            }
        })
    }

    fun setApps(list: List<AppEntry>) {
        apps = list
        emptyHint.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        rebuild()
    }

    fun refreshLook() = rebuild()

    private fun columnCount(): Int = Prefs.columns.coerceIn(4, 6)

    private fun rebuild() {
        val columns = columnCount()
        val d = context.density(84f)
        val available = height - dots.layoutParams.height
        rows = if (available > 0) floor(available / d).toInt().coerceAtLeast(2) else 3
        val perPage = (rows * columns).coerceAtLeast(columns)

        val pages = if (apps.isEmpty()) 1 else ceil(apps.size / perPage.toFloat()).toInt()
        val oldPosition = pager.currentItem.coerceAtMost(pages - 1)

        val adapter = PagerAdapter(pages, perPage) { page ->
            val start = page * perPage
            apps.subList(start.coerceAtMost(apps.size), (start + perPage).coerceAtMost(apps.size))
        }
        pageAdapter = adapter
        pager.adapter = adapter
        pager.setCurrentItem(oldPosition, false)
        dots.setDots(pages, oldPosition)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w != oldw || h != oldh) post { rebuild() }
    }

    private inner class PagerAdapter(
        val pageCount: Int,
        val perPage: Int,
        val itemsFor: (Int) -> List<AppEntry>
    ) : RecyclerView.Adapter<PagerAdapter.PageHolder>() {

        override fun getItemCount(): Int = pageCount

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder {
            val recycler = RecyclerView(context).apply {
                layoutManager = GridLayoutManager(context, columnCount()).apply {
                    orientation = RecyclerView.VERTICAL
                }
                overScrollMode = RecyclerView.OVER_SCROLL_NEVER
                itemAnimator = null
                setPadding(0, context.densityInt(4f), 0, context.densityInt(18f))
                clipToPadding = false
                adapter = IconAdapter()
            }
            return PageHolder(recycler)
        }

        override fun onBindViewHolder(holder: PageHolder, position: Int) {
            (holder.itemView as RecyclerView).adapter = IconAdapter().also { it.submit(itemsFor(position)) }
        }

        inner class PageHolder(view: RecyclerView) : RecyclerView.ViewHolder(view)
    }

    private inner class IconAdapter : RecyclerView.Adapter<IconAdapter.IconHolder>() {

        private var items: List<AppEntry> = emptyList()

        fun submit(list: List<AppEntry>) {
            items = list
            notifyDataSetChanged()
        }

        override fun getItemCount(): Int = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconHolder {
            val view = AppIconView(context)
            view.layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                context.densityInt(84f)
            )
            return IconHolder(view)
        }

        override fun onBindViewHolder(holder: IconHolder, position: Int) {
            val item = items[position]
            holder.view.bind(item)
            holder.view.setOnClickListener { onLaunch?.invoke(item) }
            holder.view.setOnLongClickListener { v ->
                onLongPress?.invoke(item, v)
                true
            }
        }

        inner class IconHolder(val view: AppIconView) : RecyclerView.ViewHolder(view)
    }
}

/** Точки-индикаторы страниц. */
class DotsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : android.view.View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var count = 0
    private var selected = 0

    fun setDots(count: Int, selected: Int) {
        val countChanged = this.count != count
        this.count = count
        this.selected = selected
        visibility = if (count <= 1) INVISIBLE else VISIBLE
        if (countChanged) requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val d = resources.displayMetrics.density
        setMeasuredDimension((count * 10 * d + 6 * d).toInt(), (22 * d).toInt())
    }

    override fun onDraw(canvas: Canvas) {
        val d = resources.displayMetrics.density
        val accent = NexusTheme.accent(context)
        val cy = height / 2f
        var x = 3 * d
        for (i in 0 until count) {
            paint.color = if (i == selected) withAlpha(accent, 240) else withAlpha(accent, 60)
            val r = if (i == selected) 2.6f * d else 1.8f * d
            canvas.drawCircle(x + 3 * d, cy, r, paint)
            x += 10 * d
        }
    }
}
