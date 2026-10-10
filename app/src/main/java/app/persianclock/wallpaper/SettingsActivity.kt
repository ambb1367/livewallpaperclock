package app.persianclock.wallpaper

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class SettingsActivity : Activity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var preview: PreviewView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(ClockConfig.PREFS, MODE_PRIVATE)
        val density = resources.displayMetrics.density
        fun dp(v: Int): Int = (v * density).toInt()
        val cfg0 = ClockConfig.load(prefs)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(16), dp(20), dp(16), dp(32))
        }
        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#12141A"))
            addView(root)
        }
        setContentView(scroll)

        fun title(t: String) = TextView(this).apply {
            text = t
            textSize = 15f
            setTextColor(Color.parseColor("#9AA4B2"))
            setPadding(0, dp(20), 0, dp(6))
        }

        preview = PreviewView(this, prefs)
        root.addView(preview, LinearLayout.LayoutParams(dp(160), dp(340)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            bottomMargin = dp(8)
        })

        val setBtn = Button(this).apply {
            text = "تنظیم به‌عنوان والپیپر"
            setOnClickListener { openPicker() }
        }
        root.addView(setBtn)

        // سبک ساعت
        root.addView(title("سبک ساعت"))
        val ids = ArrayList<Int>()
        val rg = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }
        for (name in Palette.styleNames) {
            val rb = RadioButton(this).apply {
                id = View.generateViewId()
                text = name
                textSize = 16f
            }
            ids.add(rb.id)
            rg.addView(rb)
        }
        rg.check(ids[cfg0.style])
        rg.setOnCheckedChangeListener { _, checkedId ->
            val idx = ids.indexOf(checkedId)
            if (idx >= 0) {
                prefs.edit().putInt("style", idx).apply()
                preview.invalidate()
            }
        }
        root.addView(rg)

        // رنگ
        root.addView(title("رنگ ساعت"))
        root.addView(swatchGrid("accent", cfg0.accent, Palette.accents, Palette.accents) { dp(it) })

        root.addView(title("پس‌زمینه"))
        root.addView(swatchGrid("bg", cfg0.bg, Palette.bgTop, Palette.bgBottom) { dp(it) })

        // گزینه‌ها
        root.addView(title("گزینه‌ها"))
        root.addView(switchRow("نمایش ساعت ۲۴ ساعته", "is24", cfg0.is24) { dp(it) })
        root.addView(switchRow("نمایش ثانیه", "seconds", cfg0.seconds) { dp(it) })
        root.addView(switchRow("ارقام فارسی", "persian", cfg0.persian) { dp(it) })
        root.addView(switchRow("نمایش تاریخ شمسی", "jalali", cfg0.jalali) { dp(it) })
        root.addView(switchRow("نمایش تاریخ میلادی", "gregorian", cfg0.gregorian) { dp(it) })
    }

    private fun switchRow(label: String, key: String, initial: Boolean, dp: (Int) -> Int): Switch {
        return Switch(this).apply {
            text = label
            textSize = 16f
            isChecked = initial
            setPadding(0, dp(8), 0, dp(8))
            setOnCheckedChangeListener { _, v ->
                prefs.edit().putBoolean(key, v).apply()
                preview.invalidate()
            }
        }
    }

    private fun swatchGrid(
        key: String, initial: Int, tops: IntArray, bottoms: IntArray, dp: (Int) -> Int
    ): View {
        val grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val drawables = ArrayList<GradientDrawable>()
        var current = initial

        fun refresh() {
            for (i in drawables.indices) {
                drawables[i].setStroke(dp(3), if (i == current) Color.WHITE else Color.TRANSPARENT)
            }
        }

        var row: LinearLayout? = null
        for (i in tops.indices) {
            if (i % 4 == 0) {
                row = LinearLayout(this)
                grid.addView(row)
            }
            val g = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(tops[i], bottoms[i])
            ).apply { shape = GradientDrawable.OVAL }
            drawables.add(g)
            val v = View(this).apply {
                background = g
                setOnClickListener {
                    current = i
                    prefs.edit().putInt(key, i).apply()
                    refresh()
                    preview.invalidate()
                }
            }
            row!!.addView(v, LinearLayout.LayoutParams(dp(48), dp(48)).apply {
                setMargins(dp(6), dp(6), dp(6), dp(6))
            })
        }
        refresh()
        return grid
    }

    private fun openPicker() {
        try {
            val i = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            i.putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this, ClockWallpaperService::class.java)
            )
            startActivity(i)
        } catch (e: Exception) {
            try {
                startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
            } catch (e2: Exception) {
                Toast.makeText(
                    this, "از تنظیمات گوشی > والپیپر، «ساعت زنده» را انتخاب کنید", Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /** پیش‌نمایش زنده با همان رندرر والپیپر */
    private class PreviewView(ctx: Context, private val prefs: SharedPreferences) : View(ctx) {
        private val renderer = ClockRenderer()
        private val clip = Path()
        private val box = RectF()
        private val tick = object : Runnable {
            override fun run() {
                invalidate()
                postDelayed(this, 1000)
            }
        }

        init {
            setLayerType(LAYER_TYPE_SOFTWARE, null)
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            post(tick)
        }

        override fun onDetachedFromWindow() {
            removeCallbacks(tick)
            super.onDetachedFromWindow()
        }

        override fun onDraw(canvas: Canvas) {
            box.set(0f, 0f, width.toFloat(), height.toFloat())
            clip.reset()
            clip.addRoundRect(box, width * 0.08f, width * 0.08f, Path.Direction.CW)
            canvas.clipPath(clip)
            renderer.draw(canvas, width, height, ClockConfig.load(prefs))
        }
    }
}
