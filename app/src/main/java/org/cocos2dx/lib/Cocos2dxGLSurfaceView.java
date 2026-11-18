package org.cocos2dx.lib;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;

/**
 * The GL surface plus input. Some engine builds keep the touch natives on this
 * class, newer ones on the renderer, so both spellings are declared and the
 * first working one is used for the whole session.
 */
public class Cocos2dxGLSurfaceView extends GLSurfaceView {

    private Cocos2dxRenderer mRenderer;
    private Cocos2dxEditBox mEditBox;
    private boolean mRendererOwnsTouches = true;

    public Cocos2dxGLSurfaceView(final Context pContext) {
        super(pContext);
        initView();
    }

    public Cocos2dxGLSurfaceView(final Context pContext, final AttributeSet pAttrs) {
        super(pContext, pAttrs);
        initView();
    }

    private void initView() {
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        setZOrderMediaOverlay(true);
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    public void setCocos2dxRenderer(final Cocos2dxRenderer pRenderer) {
        this.mRenderer = pRenderer;
        setRenderer(pRenderer);
    }

    public void setEditBox(final Cocos2dxEditBox pEditBox) {
        this.mEditBox = pEditBox;
    }

    public Cocos2dxEditBox getEditBox() {
        return mEditBox;
    }

    @Override
    public void onResume() {
        super.onResume();
        queueEvent(new Runnable() {
            @Override
            public void run() {
                if (mRenderer != null) {
                    mRenderer.setScreenWidthAndHeight(getWidth(), getHeight());
                    mRenderer.handleOnResume();
                }
            }
        });
    }

    @Override
    public void onPause() {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                if (mRenderer != null) {
                    mRenderer.handleOnPause();
                }
            }
        });
        super.onPause();
    }

    // ------------------------------------------------------------------ touch

    @Override
    public boolean onTouchEvent(final MotionEvent pMotionEvent) {
        final int action = pMotionEvent.getAction() & MotionEvent.ACTION_MASK;
        final int count = pMotionEvent.getPointerCount();
        final int[] ids = new int[count];
        final float[] xs = new float[count];
        final float[] ys = new float[count];
        for (int i = 0; i < count; i++) {
            ids[i] = pMotionEvent.getPointerId(i);
            xs[i] = pMotionEvent.getX(i);
            ys[i] = pMotionEvent.getY(i);
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                final int index = pMotionEvent.getActionIndex();
                dispatchDown(pMotionEvent.getPointerId(index),
                        pMotionEvent.getX(index), pMotionEvent.getY(index));
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP: {
                final int index = pMotionEvent.getActionIndex();
                dispatchUp(pMotionEvent.getPointerId(index),
                        pMotionEvent.getX(index), pMotionEvent.getY(index));
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                dispatchMove(ids, xs, ys);
                break;
            }
            case MotionEvent.ACTION_CANCEL: {
                dispatchCancel(ids, xs, ys);
                break;
            }
            default:
                break;
        }
        return true;
    }

    private void dispatchDown(final int id, final float x, final float y) {
        if (mRendererOwnsTouches && mRenderer != null) {
            try {
                mRenderer.handleActionDown(id, x, y);
                return;
            } catch (final UnsatisfiedLinkError error) {
                mRendererOwnsTouches = false;
            }
        }
        try {
            nativeTouchesBegin(id, x, y);
        } catch (final UnsatisfiedLinkError ignored) {
        }
    }

    private void dispatchUp(final int id, final float x, final float y) {
        if (mRendererOwnsTouches && mRenderer != null) {
            try {
                mRenderer.handleActionUp(id, x, y);
                return;
            } catch (final UnsatisfiedLinkError error) {
                mRendererOwnsTouches = false;
            }
        }
        try {
            nativeTouchesEnd(id, x, y);
        } catch (final UnsatisfiedLinkError ignored) {
        }
    }

    private void dispatchMove(final int[] ids, final float[] xs, final float[] ys) {
        if (mRendererOwnsTouches && mRenderer != null) {
            try {
                mRenderer.handleActionMove(ids, xs, ys);
                return;
            } catch (final UnsatisfiedLinkError error) {
                mRendererOwnsTouches = false;
            }
        }
        try {
            nativeTouchesMove(ids, xs, ys);
        } catch (final UnsatisfiedLinkError ignored) {
        }
    }

    private void dispatchCancel(final int[] ids, final float[] xs, final float[] ys) {
        if (mRendererOwnsTouches && mRenderer != null) {
            try {
                mRenderer.handleActionCancel(ids, xs, ys);
                return;
            } catch (final UnsatisfiedLinkError error) {
                mRendererOwnsTouches = false;
            }
        }
        try {
            nativeTouchesCancel(ids, xs, ys);
        } catch (final UnsatisfiedLinkError ignored) {
        }
    }

    // -------------------------------------------------------------------- keys

    @Override
    public boolean onKeyDown(final int pKeyCode, final KeyEvent pKeyEvent) {
        try {
            nativeKeyDown(pKeyCode);
        } catch (final UnsatisfiedLinkError ignored) {
        }
        return super.onKeyDown(pKeyCode, pKeyEvent);
    }

    @Override
    public boolean onKeyUp(final int pKeyCode, final KeyEvent pKeyEvent) {
        try {
            nativeKeyUp(pKeyCode);
        } catch (final UnsatisfiedLinkError ignored) {
        }
        return super.onKeyUp(pKeyCode, pKeyEvent);
    }

    public void insertText(final String pText) {
        try {
            nativeInsertText(pText);
        } catch (final UnsatisfiedLinkError ignored) {
        }
    }

    public void deleteBackward() {
        try {
            nativeDeleteBackward();
        } catch (final UnsatisfiedLinkError ignored) {
        }
    }

    // ---------------------------------------------------------------- natives

    private static native void nativeTouchesBegin(final int pID, final float pX, final float pY);

    private static native void nativeTouchesEnd(final int pID, final float pX, final float pY);

    private static native void nativeTouchesMove(final int[] pIDs, final float[] pXs, final float[] pYs);

    private static native void nativeTouchesCancel(final int[] pIDs, final float[] pXs, final float[] pYs);

    private static native void nativeKeyDown(final int pKeyCode);

    private static native void nativeKeyUp(final int pKeyCode);

    private static native void nativeInsertText(final String pText);

    private static native void nativeDeleteBackward();
}
