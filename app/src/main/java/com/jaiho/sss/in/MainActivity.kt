package com.jaiho.sss.in

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import org.cocos2dx.lib.Cocos2dxActivity
import org.cocos2dx.lib.Cocos2dxHelper
import org.cocos2dx.lib.Cocos2dxJavascriptJavaBridge

/**
 * Launcher activity of the game.
 *
 * It is a ComponentActivity (through Cocos2dxActivity, which extends
 * androidx.activity.ComponentActivity) and it puts the engine's GL surface on
 * screen with setContent { AndroidView(...) }. The engine, the GL surface and
 * every Java <-> C++ bridge live in org.cocos2dx.lib.
 *
 * On top of booting the engine this class injects the local JavaScript patch
 * (assets/main_js_patch.js) into the running JS context. The patch is the
 * runtime twin of the block that is appended to assets/main.js: it is pushed in
 * on a short retry timer for 90 seconds so that it lands no matter when the
 * engine finished initialising its script engine.
 *
 * Build / runtime notes:
 *  - getRootLayout() (an explicit accessor, not the Kotlin synthetic property)
 *    is used, and the result is allowed to be null. If the engine could not
 *    build its view the screen shows a plain message instead of crashing.
 *  - The factory detaches the view from an earlier parent, otherwise a second
 *    composition would throw "child already has a parent".
 *  - If Compose itself cannot attach the engine view the activity falls back to
 *    plain setContentView(gameRoot), so the game always reaches the screen.
 *  - If the prebuilt engine library did not load (wrong ABI) the screen says so
 *    instead of staying black.
 */
open class MainActivity : Cocos2dxActivity() {

    private val injectorHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var patchSource: String? = null

    private var attempts = 0
    private var destroyed = false
    private var injectFinished = false

    private val injector = object : Runnable {
        override fun run() {
            if (destroyed) {
                return
            }
            attempts++
            val source = patchSource
            if (source != null) {
                evalOnGLThread(source)
            }
            if (attempts < MAX_ATTEMPTS && !destroyed) {
                injectorHandler.postDelayed(this, RETRY_INTERVAL_MS)
            } else {
                injectFinished = true
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        goFullScreen()

        // super.onCreate() built the GL surface and started the engine.
        // getRootLayout() returns the FrameLayout that holds that surface.
        val gameRoot = getRootLayout()
        val libraryLoaded = Cocos2dxHelper.isNativeLibraryLoaded()

        try {
            setContent {
                MaterialTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Black
                    ) {
                        if (gameRoot == null || !libraryLoaded) {
                            EngineUnavailable(libraryLoaded)
                        } else {
                            EngineHost(gameRoot)
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "compose host failed, using setContentView", t)
            if (gameRoot != null) {
                setContentView(gameRoot)
            }
        }

        requestLegacyStoragePermission()
        loadPatchSource()
        // Give the engine a moment to create the JS context, then retry for 90s.
        injectorHandler.postDelayed(injector, FIRST_DELAY_MS)
    }

    override fun onResume() {
        super.onResume()
        if (!injectFinished && !destroyed) {
            injectorHandler.removeCallbacks(injector)
            injectorHandler.postDelayed(injector, RETRY_INTERVAL_MS)
        }
    }

    override fun onDestroy() {
        destroyed = true
        injectorHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    // ------------------------------------------------------------------ views

    @Composable
    private fun EngineHost(gameRoot: FrameLayout) {
        AndroidView(
            factory = { _ ->
                val parent = gameRoot.parent
                if (parent is ViewGroup) {
                    parent.removeView(gameRoot)
                }
                gameRoot
            },
            modifier = Modifier.fillMaxSize()
        )
    }

    @Composable
    private fun EngineUnavailable(libraryLoaded: Boolean) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Jaiho 777",
                    color = Color(0xFFFFD54F),
                    fontSize = 26.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (libraryLoaded) {
                        "The game engine is starting up.\nIf the screen stays black, close and reopen the app."
                    } else {
                        "The game engine could not be loaded on this device (unsupported CPU type)."
                    },
                    color = Color.White,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }

    // ------------------------------------------------------------------ patch

    private fun evalOnGLThread(code: String) {
        try {
            // Must run on the GL thread: that thread owns the JS context.
            runOnGLThread(Runnable {
                try {
                    Cocos2dxJavascriptJavaBridge.evalString(code)
                } catch (t: Throwable) {
                    // Engine not ready yet - the next tick retries.
                }
            })
        } catch (t: Throwable) {
            Log.w(TAG, "could not queue the patch", t)
        }
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

    // -------------------------------------------------------------- platform

    private fun goFullScreen() {
        try {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } catch (t: Throwable) {
            Log.w(TAG, "full screen setup failed", t)
        }
    }

    private fun requestLegacyStoragePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }
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
            try {
                requestPermissions(needed.toTypedArray(), 1001)
            } catch (t: Throwable) {
                Log.w(TAG, "permission request failed", t)
            }
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
