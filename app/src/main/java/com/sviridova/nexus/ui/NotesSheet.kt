package com.sviridova.nexus.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import com.sviridova.nexus.R
import com.sviridova.nexus.data.NotesStore
import com.sviridova.nexus.util.withAlpha
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Выдвижная панель заметок: добавление и удаление прямо с рабочего стола.
 */
class NotesSheet(context: Context) : FrameLayout(context) {

    var onAdd: ((String) -> Unit)? = null
    var onDelete: ((String) -> Unit)? = null
    var onDismiss: (() -> Unit)? = null

    private val listColumn = LinearLayout(context)
    private val input = EditText(context)
    private val emptyLabel = hudText(context, context.getString(R.string.notes_panel_empty), 11f, NexusTheme.TERTIARY)

    init {
        setBackgroundColor(Color.argb(220, 3, 5, 9))
        isClickable = true
        alpha = 0f
        val sheet = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18f), dp(18f), dp(18f), dp(18f))
        }
        addView(sheet, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(
            hudText(context, context.getString(R.string.notes_title).uppercase(), 15f, NexusTheme.PRIMARY, R.font.orbitron, 0.2f),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )
        header.addView(HudIconButton(context).apply {
            glyph = HudIconButton.Glyph.CLOSE
            setOnClickListener { dismiss() }
        }, LinearLayout.LayoutParams(dp(44f), dp(44f)))
        sheet.addView(header, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56f)))

        sheet.addView(
            View(context).apply { setBackgroundColor(withAlpha(accentColor(), 70)) },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1f))
        )

        input.apply {
            hint = context.getString(R.string.notes_hint)
            textSize = 15f
            setTextColor(NexusTheme.PRIMARY)
            setHintTextColor(NexusTheme.TERTIARY)
            typeface = NexusTheme.regularTypeface(context)
            background = GradientDrawable().apply {
                cornerRadius = dp(12f).toFloat()
                setColor(Color.argb(30, 255, 255, 255))
                setStroke(dp(1f), withAlpha(accentColor(), 90))
            }
            setPadding(dp(14f), dp(10f), dp(14f), dp(10f))
            isSingleLine = true
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            imeOptions = EditorInfo.IME_ACTION_DONE
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    submit()
                    true
                } else false
            }
        }
        sheet.addView(input, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48f)).apply {
            topMargin = dp(14f)
        })

        val scroll = ScrollView(context)
        listColumn.orientation = LinearLayout.VERTICAL
        scroll.addView(listColumn)
        sheet.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).apply {
            topMargin = dp(12f)
        })

        emptyLabel.gravity = Gravity.CENTER
        sheet.addView(emptyLabel, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40f)))
    }

    private fun accentColor(): Int = NexusTheme.accent(context)

    fun animateIn() {
        translationY = dp(24f).toFloat()
        animate().alpha(1f).translationY(0f).setDuration(180).start()
        input.requestFocus()
        postDelayed({
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
        }, 180)
    }

    fun setNotes(notes: List<NotesStore.Note>) {
        listColumn.removeAllViews()
        emptyLabel.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE
        val formatter = SimpleDateFormat("d MMM, HH:mm", Locale("ru"))
        for (note in notes) {
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(14f), dp(12f), dp(8f), dp(12f))
                background = GradientDrawable().apply {
                    cornerRadius = dp(14f).toFloat()
                    setColor(Color.argb(28, 255, 255, 255))
                    setStroke(dp(1f), withAlpha(NexusTheme.accent(context), 40))
                }
            }
            val column = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
            column.addView(
                hudText(context, note.text, 14f, NexusTheme.PRIMARY, R.font.rajdhani_regular, 0.02f, upper = false)
                    .apply { maxLines = 4 }
            )
            column.addView(
                hudText(
                    context,
                    formatter.format(Date(note.at)).uppercase(Locale("ru")),
                    9.5f,
                    NexusTheme.TERTIARY,
                    R.font.rajdhani_semibold,
                    0.14f
                ),
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    .apply { topMargin = dp(2f) }
            )
            row.addView(column, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(HudIconButton(context).apply {
                glyph = HudIconButton.Glyph.CLOSE
                setOnClickListener { onDelete?.invoke(note.id) }
            }, LinearLayout.LayoutParams(dp(38f), dp(38f)))
            listColumn.addView(row, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8f) })
        }
    }

    private fun submit() {
        val text = input.text?.toString().orEmpty()
        if (text.isBlank()) return
        input.setText("")
        onAdd?.invoke(text)
    }

    fun dismiss() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
        animate().alpha(0f).setDuration(140).withEndAction {
            (parent as? ViewGroup)?.removeView(this)
            onDismiss?.invoke()
        }.start()
    }

    private fun dp(value: Float): Int = (value * resources.displayMetrics.density).toInt()
}
