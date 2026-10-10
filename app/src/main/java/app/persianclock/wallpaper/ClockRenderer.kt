package app.persianclock.wallpaper

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/** رسم همهٔ سبک‌های ساعت؛ هم برای والپیپر و هم برای پیش‌نمایش استفاده می‌شود. */
class ClockRenderer {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bgPaint = Paint()
    private val rect = RectF()
    private var bgKey = ""
    private var hh = ""
    private var mm = ""
    private var ss = ""

    fun draw(c: Canvas, w: Int, h: Int, cfg: ClockConfig) {
        val wf = w.toFloat()
        val hf = h.toFloat()

        val key = "$w,$h,${cfg.bg}"
        if (key != bgKey) {
            bgKey = key
            bgPaint.shader = LinearGradient(
                0f, 0f, 0f, hf, Palette.bgTop[cfg.bg], Palette.bgBottom[cfg.bg], Shader.TileMode.CLAMP
            )
        }
        c.drawRect(0f, 0f, wf, hf, bgPaint)

        val cal = GregorianCalendar()
        val h24 = cal.get(Calendar.HOUR_OF_DAY)
        val min = cal.get(Calendar.MINUTE)
        val sec = cal.get(Calendar.SECOND)
        val hour12 = if (h24 % 12 == 0) 12 else h24 % 12
        val shown = if (cfg.is24) h24 else hour12
        hh = Persian.digits(String.format(Locale.US, "%02d", shown), cfg.persian)
        mm = Persian.digits(String.format(Locale.US, "%02d", min), cfg.persian)
        ss = Persian.digits(String.format(Locale.US, "%02d", sec), cfg.persian)

        val cx = wf / 2f
        val accent = Palette.accents[cfg.accent]
        val frac = when (cfg.style) {
            0 -> 0.38f
            1 -> 0.35f
            2, 3 -> 0.36f
            4 -> 0.38f
            5 -> 0.35f
            else -> 0.37f
        }
        val cy = hf * frac

        val bottom = when (cfg.style) {
            0 -> styleDigital(c, wf, cx, cy, accent)
            1 -> styleStacked(c, wf, cx, cy, accent)
            2 -> styleAnalog(c, wf, cx, cy, cfg, accent, true, h24, min, sec)
            3 -> styleAnalog(c, wf, cx, cy, cfg, accent, false, h24, min, sec)
            4 -> styleNeon(c, wf, cx, cy, accent)
            5 -> styleOutline(c, wf, cx, cy, accent)
            6 -> styleRing(c, wf, cx, cy, cfg, accent, min, sec)
            else -> styleFlip(c, wf, cx, cy, cfg, accent, h24)
        }

        var y = bottom + wf * 0.05f
        val secondsText = cfg.seconds && (cfg.style == 0 || cfg.style == 1 || cfg.style == 4 ||
                cfg.style == 5 || cfg.style == 7)
        if (secondsText) {
            tp(accent, "sans-serif-light", false)
            p.textSize = wf * 0.08f
            drawNum(c, ss, cx, y + wf * 0.04f)
            y += wf * 0.11f
        }
        drawDates(c, wf, cx, y, cfg, accent, cal)
    }

    // ---------- سبک‌ها ----------

    private fun styleDigital(c: Canvas, w: Float, cx: Float, cy: Float, accent: Int): Float {
        val t = "$hh:$mm"
        tp(accent, "sans-serif-light", false)
        fit(t, w * 0.92f, w * 0.4f)
        val s = p.textSize
        drawNum(c, t, cx, cy)
        return cy + s * 0.4f
    }

    private fun styleStacked(c: Canvas, w: Float, cx: Float, cy: Float, accent: Int): Float {
        tp(Color.WHITE, "sans-serif-black", false)
        fit(hh, w * 0.7f, w * 0.5f)
        val s = p.textSize
        val gap = s * 0.43f
        drawNum(c, hh, cx, cy - gap)
        p.color = accent
        drawNum(c, mm, cx, cy + gap)
        return cy + gap + s * 0.4f
    }

    private fun styleNeon(c: Canvas, w: Float, cx: Float, cy: Float, accent: Int): Float {
        val t = "$hh:$mm"
        tp(accent, "sans-serif-medium", false)
        fit(t, w * 0.9f, w * 0.36f)
        val s = p.textSize
        p.setShadowLayer(w * 0.05f, 0f, 0f, accent)
        drawNum(c, t, cx, cy)
        p.setShadowLayer(w * 0.02f, 0f, 0f, accent)
        p.color = mix(accent, Color.WHITE, 0.65f)
        drawNum(c, t, cx, cy)
        return cy + s * 0.4f
    }

    private fun styleOutline(c: Canvas, w: Float, cx: Float, cy: Float, accent: Int): Float {
        tp(accent, "sans-serif-black", false)
        fit(hh, w * 0.7f, w * 0.5f)
        val s = p.textSize
        val gap = s * 0.43f
        p.style = Paint.Style.STROKE
        p.strokeWidth = w * 0.008f
        p.strokeJoin = Paint.Join.ROUND
        drawNum(c, hh, cx, cy - gap)
        p.style = Paint.Style.FILL
        drawNum(c, mm, cx, cy + gap)
        return cy + gap + s * 0.4f
    }

    private fun styleRing(
        c: Canvas, w: Float, cx: Float, cy: Float, cfg: ClockConfig, accent: Int, min: Int, sec: Int
    ): Float {
        val r = w * 0.34f
        val stroke = w * 0.03f
        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.STROKE
        p.strokeWidth = stroke
        p.color = Color.argb(45, 255, 255, 255)
        c.drawCircle(cx, cy, r, p)
        p.color = accent
        p.strokeCap = Paint.Cap.ROUND
        val sweep = if (cfg.seconds) sec * 6f else (min + sec / 60f) * 6f
        rect.set(cx - r, cy - r, cx + r, cy + r)
        c.drawArc(rect, -90f, max(sweep, 0.5f), false, p)
        tp(Color.WHITE, "sans-serif-light", false)
        fit("$hh:$mm", r * 1.5f, r * 0.9f)
        drawNum(c, "$hh:$mm", cx, cy)
        return cy + r + stroke
    }

    private fun styleFlip(
        c: Canvas, w: Float, cx: Float, cy: Float, cfg: ClockConfig, accent: Int, h24: Int
    ): Float {
        val cw = w * 0.4f
        val ch = w * 0.46f
        val gap = w * 0.04f
        val rad = w * 0.04f
        val lefts = floatArrayOf(cx - gap / 2f - cw, cx + gap / 2f)
        val texts = arrayOf(hh, mm)
        for (i in 0..1) {
            val l = lefts[i]
            rect.set(l, cy - ch / 2f, l + cw, cy + ch / 2f)
            p.reset()
            p.isAntiAlias = true
            p.style = Paint.Style.FILL
            p.color = Color.argb(215, 24, 24, 30)
            c.drawRoundRect(rect, rad, rad, p)
            tp(accent, "sans-serif", true)
            fit(texts[i], cw * 0.8f, cw)
            drawNum(c, texts[i], l + cw / 2f, cy)
            p.reset()
            p.isAntiAlias = true
            p.style = Paint.Style.STROKE
            p.strokeWidth = w * 0.006f
            p.color = Color.argb(220, 0, 0, 0)
            c.drawLine(l, cy, l + cw, cy, p)
        }
        var bottom = cy + ch / 2f
        if (!cfg.is24) {
            tp(Color.argb(190, 255, 255, 255), "sans-serif", false)
            p.textSize = w * 0.045f
            drawCentered(c, if (h24 < 12) "ق.ظ" else "ب.ظ", cx, bottom + w * 0.045f)
            bottom += w * 0.07f
        }
        return bottom
    }

    private fun styleAnalog(
        c: Canvas, w: Float, cx: Float, cy: Float, cfg: ClockConfig, accent: Int, classic: Boolean,
        h24: Int, min: Int, sec: Int
    ): Float {
        val r = w * 0.4f
        p.reset()
        p.isAntiAlias = true
        if (classic) {
            p.style = Paint.Style.FILL
            p.color = Color.argb(30, 255, 255, 255)
            c.drawCircle(cx, cy, r, p)
            p.style = Paint.Style.STROKE
            p.strokeWidth = w * 0.008f
            p.color = accent
            c.drawCircle(cx, cy, r, p)
            for (i in 0 until 60) {
                val major = i % 5 == 0
                val a = Math.toRadians(i * 6.0)
                val s = sin(a).toFloat()
                val co = cos(a).toFloat()
                val r1 = r * 0.93f
                val r0 = r1 - (if (major) r * 0.09f else r * 0.045f)
                p.style = Paint.Style.STROKE
                p.strokeWidth = if (major) w * 0.007f else w * 0.003f
                p.color = if (major) Color.WHITE else Color.argb(130, 255, 255, 255)
                c.drawLine(cx + s * r0, cy - co * r0, cx + s * r1, cy - co * r1, p)
            }
            tp(Color.WHITE, "sans-serif", false)
            p.textSize = r * 0.17f
            for (n in 1..12) {
                val a = Math.toRadians(n * 30.0)
                drawNum(
                    c, Persian.digits(n.toString(), cfg.persian),
                    cx + sin(a).toFloat() * r * 0.74f, cy - cos(a).toFloat() * r * 0.74f
                )
            }
        } else {
            for (i in 0 until 12) {
                val big = i % 3 == 0
                val a = Math.toRadians(i * 30.0)
                p.style = Paint.Style.FILL
                p.color = if (big) accent else Color.argb(150, 255, 255, 255)
                c.drawCircle(
                    cx + sin(a).toFloat() * r, cy - cos(a).toFloat() * r,
                    if (big) w * 0.013f else w * 0.006f, p
                )
            }
        }

        val hourAngle = ((h24 % 12) + min / 60f) * 30f
        val minAngle = (min + sec / 60f) * 6f
        if (classic) {
            hand(c, cx, cy, hourAngle, r * 0.5f, w * 0.016f, Color.WHITE, r * 0.08f)
            hand(c, cx, cy, minAngle, r * 0.78f, w * 0.011f, Color.WHITE, r * 0.1f)
        } else {
            hand(c, cx, cy, hourAngle, r * 0.5f, w * 0.03f, Color.WHITE, 0f)
            hand(c, cx, cy, minAngle, r * 0.82f, w * 0.02f, accent, 0f)
        }
        if (cfg.seconds) {
            hand(c, cx, cy, sec * 6f, r * 0.88f, w * 0.004f, accent, r * 0.14f)
        }
        p.style = Paint.Style.FILL
        p.color = accent
        c.drawCircle(cx, cy, w * 0.016f, p)
        p.color = Color.argb(255, 20, 20, 25)
        c.drawCircle(cx, cy, w * 0.006f, p)
        return cy + r + w * 0.02f
    }

    private fun hand(
        c: Canvas, cx: Float, cy: Float, angleDeg: Float, len: Float, width: Float, color: Int, tail: Float
    ) {
        val a = Math.toRadians(angleDeg.toDouble())
        val sx = sin(a).toFloat()
        val sy = -cos(a).toFloat()
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = width
        p.color = color
        c.drawLine(cx - sx * tail, cy - sy * tail, cx + sx * len, cy + sy * len, p)
    }

    // ---------- تاریخ ----------

    private fun drawDates(
        c: Canvas, w: Float, cx: Float, top: Float, cfg: ClockConfig, accent: Int, cal: Calendar
    ) {
        var y = top
        if (cfg.jalali) {
            val j = Persian.toJalali(
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH)
            )
            val dow = Persian.weekdays[cal.get(Calendar.DAY_OF_WEEK) - 1]
            val text = dow + " " + Persian.digits(j[2].toString(), cfg.persian) + " " +
                    Persian.months[j[1] - 1] + " " + Persian.digits(j[0].toString(), cfg.persian)
            tp(Color.WHITE, "sans-serif", false)
            fit(text, w * 0.9f, w * 0.065f)
            if (cfg.style == 4) p.setShadowLayer(w * 0.02f, 0f, 0f, accent)
            drawCentered(c, text, cx, y + p.textSize * 0.6f)
            y += p.textSize * 1.45f
        }
        if (cfg.gregorian) {
            val text = SimpleDateFormat("EEE, d MMM yyyy", Locale.ENGLISH).format(cal.time)
            tp(Color.argb(170, 255, 255, 255), "sans-serif-light", false)
            fit(text, w * 0.9f, w * 0.048f)
            drawCentered(c, text, cx, y + p.textSize * 0.6f)
        }
    }

    // ---------- ابزارهای کمکی ----------

    private fun tp(color: Int, family: String, bold: Boolean) {
        p.reset()
        p.isAntiAlias = true
        p.style = Paint.Style.FILL
        p.textAlign = Paint.Align.CENTER
        p.typeface = Typeface.create(family, if (bold) Typeface.BOLD else Typeface.NORMAL)
        p.color = color
    }

    private fun fit(t: String, maxW: Float, maxSize: Float) {
        p.textSize = maxSize
        val tw = p.measureText(t)
        if (tw > maxW) p.textSize = maxSize * maxW / tw
    }

    /** وسط‌چین عمودی مخصوص ارقام */
    private fun drawNum(c: Canvas, t: String, x: Float, y: Float) {
        c.drawText(t, x, y + p.textSize * 0.36f, p)
    }

    /** وسط‌چین عمودی برای متن‌های فارسی/انگلیسی */
    private fun drawCentered(c: Canvas, t: String, x: Float, y: Float) {
        val base = y - (p.descent() + p.ascent()) / 2f
        c.drawText(t, x, base, p)
    }

    private fun mix(a: Int, b: Int, t: Float): Int {
        val r = (Color.red(a) * (1 - t) + Color.red(b) * t).toInt()
        val g = (Color.green(a) * (1 - t) + Color.green(b) * t).toInt()
        val bl = (Color.blue(a) * (1 - t) + Color.blue(b) * t).toInt()
        return Color.rgb(r, g, bl)
    }
}
