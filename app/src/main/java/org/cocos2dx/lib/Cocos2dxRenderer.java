package org.cocos2dx.lib;

import android.opengl.GLSurfaceView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * The GL renderer. Native entry points are resolved by name inside the prebuilt
 * library, therefore neither names nor signatures may change.
 *
 * Every native call is wrapped: a device that cannot load the engine library
 * keeps a black but stable screen.
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
            if (mInitialised) {
                return;
            }
            mInitialised = true;
            try {
                nativeInit(Math.max(1, mScreenWidth), Math.max(1, mScreenHeight));
            } catch (Throwable t) {
                // library missing - nothing to initialise
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
        try {
            nativeOnSurfaceChanged(pWidth, pHeight);
        } catch (Throwable t) {
            // library missing
        }
    }

    @Override
    public void onDrawFrame(final GL10 pGL10) {
        synchronized (this) {
            if (mPaused) {
                return;
            }
        }
        try {
            nativeRender();
        } catch (Throwable t) {
            // library missing
        }
    }

    public void handleOnResume() {
        synchronized (this) {
            mPaused = false;
        }
        try {
            nativeOnResume();
        } catch (Throwable t) {
            // library missing
        }
    }

    public void handleOnPause() {
        synchronized (this) {
            mPaused = true;
        }
        try {
            nativeOnPause();
        } catch (Throwable t) {
            // library missing
        }
    }

    // ------------------------------------------------------------------ touch

    public void handleActionDown(final int pID, final float pX, final float pY) {
        try {
            nativeTouchesBegin(pID, pX, pY);
        } catch (Throwable t) {
            // library missing
        }
    }

    public void handleActionUp(final int pID, final float pX, final float pY) {
        try {
            nativeTouchesEnd(pID, pX, pY);
        } catch (Throwable t) {
            // library missing
        }
    }

    public void handleActionMove(final int[] pIDs, final float[] pXs, final float[] pYs) {
        try {
            nativeTouchesMove(pIDs, pXs, pYs);
        } catch (Throwable t) {
            // library missing
        }
    }

    public void handleActionCancel(final int[] pIDs, final float[] pXs, final float[] pYs) {
        try {
            nativeTouchesCancel(pIDs, pXs, pYs);
        } catch (Throwable t) {
            // library missing
        }
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
