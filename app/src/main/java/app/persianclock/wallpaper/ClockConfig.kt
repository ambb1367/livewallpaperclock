package app.persianclock.wallpaper

import android.content.SharedPreferences
import android.graphics.Color

object Palette {
    val styleNames = arrayOf(
        "دیجیتال بزرگ", "دو خطی ضخیم", "آنالوگ کلاسیک", "آنالوگ مینیمال",
        "نئونی", "توخالی", "حلقه‌ای", "کارتی (فلیپ)"
    )
    val accents = intArrayOf(
        Color.parseColor("#FFFFFF"), Color.parseColor("#00E5FF"), Color.parseColor("#FF4081"),
        Color.parseColor("#FFD740"), Color.parseColor("#69F0AE"), Color.parseColor("#B388FF"),
        Color.parseColor("#FF6E40"), Color.parseColor("#40C4FF")
    )
    val bgTop = intArrayOf(
        Color.parseColor("#000000"), Color.parseColor("#0F2027"), Color.parseColor("#0D0D1A"),
        Color.parseColor("#42275A"), Color.parseColor("#141E30"), Color.parseColor("#0B3D2E"),
        Color.parseColor("#3A1C71")
    )
    val bgBottom = intArrayOf(
        Color.parseColor("#000000"), Color.parseColor("#2C5364"), Color.parseColor("#1F2A5A"),
        Color.parseColor("#734B6D"), Color.parseColor("#243B55"), Color.parseColor("#1B5E20"),
        Color.parseColor("#D76D77")
    )
}

data class ClockConfig(
    val style: Int,
    val accent: Int,
    val bg: Int,
    val is24: Boolean,
    val seconds: Boolean,
    val persian: Boolean,
    val jalali: Boolean,
    val gregorian: Boolean
) {
    companion object {
        const val PREFS = "clock_prefs"

        fun load(p: SharedPreferences) = ClockConfig(
            style = p.getInt("style", 0).coerceIn(0, Palette.styleNames.size - 1),
            accent = p.getInt("accent", 1).coerceIn(0, Palette.accents.size - 1),
            bg = p.getInt("bg", 2).coerceIn(0, Palette.bgTop.size - 1),
            is24 = p.getBoolean("is24", true),
            seconds = p.getBoolean("seconds", false),
            persian = p.getBoolean("persian", true),
            jalali = p.getBoolean("jalali", true),
            gregorian = p.getBoolean("gregorian", false)
        )
    }
}
