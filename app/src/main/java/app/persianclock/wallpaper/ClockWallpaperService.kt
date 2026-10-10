package app.persianclock.wallpaper

import android.content.SharedPreferences
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder

class ClockWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = ClockEngine()

    inner class ClockEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private val renderer = ClockRenderer()
        private val prefs: SharedPreferences =
            getSharedPreferences(ClockConfig.PREFS, MODE_PRIVATE)
        private var visible = false

        private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> drawFrame() }
        private val ticker = object : Runnable {
            override fun run() {
                drawFrame()
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            prefs.registerOnSharedPreferenceChangeListener(listener)
        }

        override fun onDestroy() {
            visible = false
            handler.removeCallbacks(ticker)
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
            super.onDestroy()
        }

        override fun onVisibilityChanged(isVisible: Boolean) {
            visible = isVisible
            if (isVisible) drawFrame() else handler.removeCallbacks(ticker)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            drawFrame()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            visible = false
            handler.removeCallbacks(ticker)
            super.onSurfaceDestroyed(holder)
        }

        private fun drawFrame() {
            handler.removeCallbacks(ticker)
            val cfg = ClockConfig.load(prefs)
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) renderer.draw(canvas, canvas.width, canvas.height, cfg)
            } catch (e: Exception) {
                // ignore
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
            if (visible) {
                val now = System.currentTimeMillis()
                val delay = if (cfg.seconds) 1000L - now % 1000L else 60000L - now % 60000L
                handler.postDelayed(ticker, delay + 20L)
            }
        }
    }
}
