package com.jaiho.sss.in

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.FrameLayout
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
 * There are two stages here and they are deliberately kept apart:
 *
 *  1. The engine. [Cocos2dxActivity] (the class this one derives from) builds
 *     the GL surface in onCreate() and keeps it in a FrameLayout. This class
 *     never touches the renderer, the library or the JNI names.
 *
 *  2. The screen. setContent { AndroidView(...) } puts an empty FrameLayout on
 *     screen and the engine's layout is attached into it once with
 *     attachGameView(). Mounting a plain container (instead of handing the
 *     whole engine tree to Compose) means a re-composition can never end in
 *     "child already has a parent" and the engine view is never detached again.
 *
 * On top of that the local JavaScript patch (assets/main_js_patch.js) is
 * evaluated inside the running JS context on a retry timer for 90 seconds, so
 * the clone-detector bypass and the always-win hook land no matter when the
 * engine finished initialising.
 *
 * Every step is guarded: if Compose fails the activity falls back to plain
 * setContentView(gameRoot); if the prebuilt engine library did not load (wrong
 * CPU type) the screen says so instead of staying black.
 */
class MainActivity : Cocos2dxActivity() {

    private val injectorHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var patchSource: String? = null

    private var attempts = 0
    private var destroyed = false
    private var injectFinished = false

    /** Guards against handing the same engine layout to Compose twice. */
    private var gameViewMounted = false

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
        // super.onCreate() builds the GL surface and starts the engine.
        super.onCreate(savedInstanceState)

        goFullScreen()

        val gameRoot = getRootLayout()
        val libraryLoaded = Cocos2dxHelper.isNativeLibraryLoaded()

        if (gameRoot == null || !libraryLoaded) {
            // Nothing to host - keep the engine view off screen and explain it.
            try {
                setContent {
                    MaterialTheme {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black)
                        ) {
                            EngineUnavailable(libraryLoaded)
                        }
                    }
                }
            } catch (t: Throwable) {
                Log.w(TAG, "compose fallback screen failed", t)
            }
        } else {
            try {
                setContent {
                    MaterialTheme {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black)
                        ) {
                            GameHost(gameRoot)
                        }
                    }
                }
            } catch (t: Throwable) {
                Log.w(TAG, "compose host failed, using setContentView", t)
                if (gameRoot.parent == null) {
                    setContentView(gameRoot)
                }
            }
        }

        requestLegacyStoragePermission()
        loadPatchSource()
        // Give the engine time to create the JS context, then retry for 90s.
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
    private fun GameHost(gameRoot: FrameLayout) {
        AndroidView(
            factory = { context ->
                val host = FrameLayout(context)
                if (!gameViewMounted) {
                    gameViewMounted = true
                    // Moves the engine layout into this container exactly once.
                    attachGameView(host)
                }
                host
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
        const val FIRST_DELAY_MS = 4_000L
        const val RETRY_INTERVAL_MS = 1_000L
        const val MAX_ATTEMPTS = 90 // 90 * 1s = 90 seconds
    }
}
