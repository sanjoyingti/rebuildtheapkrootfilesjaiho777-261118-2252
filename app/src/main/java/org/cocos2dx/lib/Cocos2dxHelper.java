package org.cocos2dx.lib;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Process;
import android.util.Log;

import java.io.File;
import java.util.Locale;

/**
 * Central helper used by the native engine. Every method name here is called
 * from libcocos2djs.so through JNI (JniHelper) by its plain Java name, so the
 * names must not be changed.
 */
public class Cocos2dxHelper {

    private static final String TAG = "Cocos2dxHelper";

    private static Context sContext = null;
    private static AssetManager sAssetManager = null;
    private static String sPackageName = "";
    private static String sWritablePath = "";
    private static String sApkPath = "";
    private static String sObbPath = "";
    private static Cocos2dxHelperListener sCocos2dxHelperListener = null;

    /** Called once by the activity before the renderer starts. */
    public static void init(final Context pContext, final Cocos2dxHelperListener pListener) {
        sContext = pContext.getApplicationContext();
        sCocos2dxHelperListener = pListener;

        final ApplicationInfo applicationInfo = pContext.getApplicationInfo();
        sPackageName = applicationInfo.packageName;
        sApkPath = applicationInfo.sourceDir;
        sAssetManager = pContext.getAssets();
        sObbPath = pContext.getObbDir() != null ? pContext.getObbDir().getAbsolutePath() : "";

        File filesDir = pContext.getFilesDir();
        if (filesDir != null) {
            sWritablePath = filesDir.getAbsolutePath();
        } else {
            sWritablePath = pContext.getCacheDir().getAbsolutePath();
        }

        // Order matters: the native side stores the context, then the apk path
        // is used to mount the assets from the installed package.
        nativeSetContext(pContext, pListener);
        nativeSetApkPath(sApkPath);

        Log.i(TAG, "initialised, writable path = " + sWritablePath);
    }

    public static AssetManager getAssetManager() {
        return sAssetManager;
    }

    public static String getCocos2dxPackageName() {
        return sPackageName;
    }

    public static String getWritablePath() {
        return sWritablePath != null ? sWritablePath : "";
    }

    public static String getApkPath() {
        return sApkPath != null ? sApkPath : "";
    }

    public static String getObbAssets() {
        return sApkPath != null ? sApkPath : "";
    }

    public static String getObbFilePath() {
        return sObbPath != null ? sObbPath : "";
    }

    public static String getDeviceModel() {
        return Build.MODEL;
    }

    @SuppressWarnings("deprecation")
    public static String getSystemVersion() {
        return Build.VERSION.RELEASE;
    }

    public static String getCurrentLanguage() {
        return Locale.getDefault().getLanguage();
    }

    /** Top / bottom insets of the safe area: 0 on a normal full-screen game. */
    public static int[] getSafeArea() {
        return new int[]{0, 0, 0, 0};
    }

    public static void runOnGLThread(final Runnable pRunnable) {
        if (sCocos2dxHelperListener != null) {
            sCocos2dxHelperListener.runOnGLThread(pRunnable);
        }
    }

    public static void terminateProcess() {
        Process.killProcess(Process.myPid());
        System.exit(0);
    }

    public static void showDialog(final String pTitle, final String pMessage) {
        if (sCocos2dxHelperListener != null) {
            sCocos2dxHelperListener.showDialog(pTitle, pMessage);
        }
    }

    public static void showEditTextDialog(final String pTitle,
                                          final String pMessage,
                                          final int pInputMode,
                                          final int pInputFlag,
                                          final int pReturnType,
                                          final int pMaxLength) {
        if (sCocos2dxHelperListener != null) {
            sCocos2dxHelperListener.showEditTextDialog(pTitle, pMessage, pInputMode, pInputFlag, pReturnType, pMaxLength);
        }
    }

    public static void setEditTextDialogResult(final byte[] pBytes) {
        nativeSetEditTextDialogResult(pBytes);
    }

    public static void onResume() {
        // nothing to do; kept for API compatibility
    }

    public static void onPause() {
        // nothing to do; kept for API compatibility
    }

    public static void onDestroy() {
        // nothing to do; kept for API compatibility
    }

    // ---------------------------------------------------------------- natives

    private static native void nativeSetContext(final Context pContext, final Cocos2dxHelperListener pListener);

    private static native void nativeSetApkPath(final String pApkPath);

    private static native void nativeSetEditTextDialogResult(final byte[] pBytes);
}
