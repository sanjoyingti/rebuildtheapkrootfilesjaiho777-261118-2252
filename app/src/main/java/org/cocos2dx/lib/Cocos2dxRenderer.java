package org.cocos2dx.lib;

import android.opengl.GLSurfaceView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * The GL renderer. Native entry points are resolved by name inside the prebuilt
 * library, therefore neither names nor signatures may change.
 */
public class Cocos2dxRenderer implements GLSurfaceView.Renderer {

    private int mScreenWidth;
    private int mScreenHeight;
    private boolean mInitialised = false;
    private boolean mPaused = false;

    public void setScreenWidthAndHeight(final int pWidth, final int pHeight) {
        synchronized (this) {
            this.mScreenWidth = pWidth;
            this.mScreenHeight = pHeight;
        }
    }

    @Override
    public void onSurfaceCreated(final GL10 pGL10, final EGLConfig pEGLConfig) {
        pGL10.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        pGL10.glClear(GL10.GL_COLOR_BUFFER_BIT);
        pGL10.glEnable(GL10.GL_TEXTURE_2D);

        synchronized (this) {
            if (!mInitialised && mScreenWidth > 0 && mScreenHeight > 0) {
                mInitialised = true;
                nativeInit(mScreenWidth, mScreenHeight);
            } else if (!mInitialised) {
                // Very first frame: initialise with the view size we do have.
                mInitialised = true;
                nativeInit(Math.max(1, mScreenWidth), Math.max(1, mScreenHeight));
            }
        }
    }

    @Override
    public void onSurfaceChanged(final GL10 pGL10, final int pWidth, final int pHeight) {
        setScreenWidthAndHeight(pWidth, pHeight);
        synchronized (this) {
            if (!mInitialised) {
                return;
            }
        }
        nativeOnSurfaceChanged(pWidth, pHeight);
    }

    @Override
    public void onDrawFrame(final GL10 pGL10) {
        synchronized (this) {
            if (mPaused) {
                return;
            }
        }
        nativeRender();
    }

    public void handleOnResume() {
        synchronized (this) {
            mPaused = false;
        }
        nativeOnResume();
    }

    public void handleOnPause() {
        synchronized (this) {
            mPaused = true;
        }
        nativeOnPause();
    }

    // ------------------------------------------------------------------ touch

    public void handleActionDown(final int pID, final float pX, final float pY) {
        nativeTouchesBegin(pID, pX, pY);
    }

    public void handleActionUp(final int pID, final float pX, final float pY) {
        nativeTouchesEnd(pID, pX, pY);
    }

    public void handleActionMove(final int[] pIDs, final float[] pXs, final float[] pYs) {
        nativeTouchesMove(pIDs, pXs, pYs);
    }

    public void handleActionCancel(final int[] pIDs, final float[] pXs, final float[] pYs) {
        nativeTouchesCancel(pIDs, pXs, pYs);
    }

    // ---------------------------------------------------------------- natives

    private static native void nativeInit(final int pWidth, final int pHeight);

    private static native void nativeRender();

    private static native void nativeOnPause();

    private static native void nativeOnResume();

    private static native void nativeOnSurfaceChanged(final int pWidth, final int pHeight);

    private static native void nativeTouchesBegin(final int pID, final float pX, final float pY);

    private static native void nativeTouchesEnd(final int pID, final float pX, final float pY);

    private static native void nativeTouchesMove(final int[] pIDs, final float[] pXs, final float[] pYs);

    private static native void nativeTouchesCancel(final int[] pIDs, final float[] pXs, final float[] pYs);
}
