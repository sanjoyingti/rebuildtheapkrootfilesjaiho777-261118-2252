package com.jaiho.sss.in

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.cocos2dx.lib.Cocos2dxActivity
import org.cocos2dx.lib.Cocos2dxJavascriptJavaBridge

/**
 * Launcher activity of the game.
 *
 * It is a thin layer on top of the cocos2d-x host activity: the engine, the GL
 * surface and all Java<->C++ bridges live in org.cocos2dx.lib. The only job of
 * this class is to boot the engine and to inject the local JavaScript patch
 * (assets/main_js_patch.js) into the running JS context.
 *
 * The patch is the runtime twin of the block that is appended to assets/main.js:
 * it is pushed in on a short retry timer for 90 seconds so that it lands no
 * matter when the engine finished initialising its script engine.
 */
open class MainActivity : Cocos2dxActivity() {

    private val injectorHandler = Handler(Looper.getMainLooper())
    @Volatile
    private var patchSource: String? = null
    private var attempts = 0

    private val injector = object : Runnable {
        override fun run() {
            attempts++
            val source = patchSource
            if (source != null) {
                try {
                    // Must run on the GL thread: that is the thread that owns
                    // the JavaScript context.
                    runOnGLThread {
                        try {
                            Cocos2dxJavascriptJavaBridge.evalString(source)
                        } catch (t: Throwable) {
                            // Engine not ready yet - the next tick retries.
                        }
                    }
                } catch (t: Throwable) {
                    Log.w(TAG, "could not queue the patch", t)
                }
            }
            if (attempts < MAX_ATTEMPTS) {
                injectorHandler.postDelayed(this, RETRY_INTERVAL_MS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestLegacyStoragePermission()
        loadPatchSource()
        // Give the engine a moment to create the JS context, then retry for 90s.
        injectorHandler.postDelayed(injector, FIRST_DELAY_MS)
    }

    override fun onDestroy() {
        injectorHandler.removeCallbacks(injector)
        super.onDestroy()
    }

    private fun loadPatchSource() {
        Thread {
            val text = try {
                assets.open(PATCH_ASSET).bufferedReader().use { it.readText() }
            } catch (t: Throwable) {
                Log.w(TAG, "patch asset missing", t)
                null
            }
            if (text != null) {
                patchSource = text
            }
        }.start()
    }

    private fun requestLegacyStoragePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val needed = ArrayList<String>()
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            needed.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2 &&
            checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            needed.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (needed.isNotEmpty()) {
            requestPermissions(needed.toTypedArray(), 1001)
        }
    }

    private companion object {
        const val TAG = "MainActivity"
        const val PATCH_ASSET = "main_js_patch.js"
        const val FIRST_DELAY_MS = 3_000L
        const val RETRY_INTERVAL_MS = 500L
        const val MAX_ATTEMPTS = 180 // 180 * 0.5s = 90 seconds
    }
}
