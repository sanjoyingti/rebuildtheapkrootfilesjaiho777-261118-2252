package org.cocos2dx.lib;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.activity.ComponentActivity;

/**
 * Hosts the GL surface and the engine.
 *
 * It extends androidx.activity.ComponentActivity so the launcher screen can put
 * the game view on screen with setContent { AndroidView(...) }. onCreate() only
 * builds the view hierarchy - it does not call setContentView() - and the
 * launcher activity picks up that view through getRootLayout().
 */
public class Cocos2dxActivity extends ComponentActivity implements Cocos2dxHelperListener {

    private Cocos2dxGLSurfaceView mGLSurfaceView;
    private FrameLayout mFrameLayout;
    private static Cocos2dxActivity sContext;

    public static Cocos2dxActivity getActivity() {
        return sContext;
    }

    protected Cocos2dxGLSurfaceView onCreateView() {
        return new Cocos2dxGLSurfaceView(this);
    }

    /** The view the launcher activity has to show. */
    public FrameLayout getRootLayout() {
        return mFrameLayout;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sContext = this;

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mFrameLayout = new FrameLayout(this);
        mFrameLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        mGLSurfaceView = onCreateView();
        mGLSurfaceView.setCocos2dxRenderer(new Cocos2dxRenderer());
        mFrameLayout.addView(mGLSurfaceView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        Cocos2dxHelper.init(this, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Cocos2dxHelper.onResume();
        if (mGLSurfaceView != null) {
            mGLSurfaceView.onResume();
        }
    }

    @Override
    protected void onPause() {
        if (mGLSurfaceView != null) {
            mGLSurfaceView.onPause();
        }
        Cocos2dxHelper.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Cocos2dxHelper.onDestroy();
        sContext = null;
        super.onDestroy();
    }

    // ------------------------------------------------------ helper listener

    @Override
    public void runOnGLThread(final Runnable pRunnable) {
        if (mGLSurfaceView != null) {
            mGLSurfaceView.queueEvent(pRunnable);
        }
    }

    @Override
    public void showDialog(final String pTitle, final String pMessage) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(Cocos2dxActivity.this)
                        .setTitle(pTitle)
                        .setMessage(pMessage)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }
        });
    }

    @Override
    public void showEditTextDialog(final String pTitle,
                                   final String pMessage,
                                   final int pInputMode,
                                   final int pInputFlag,
                                   final int pReturnType,
                                   final int pMaxLength) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final EditText input = new EditText(Cocos2dxActivity.this);
                input.setSingleLine(true);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                if (pMaxLength > 0) {
                    input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(pMaxLength)});
                }
                if (pMessage != null && pMessage.length() > 0) {
                    input.setHint(pMessage);
                } else if (pTitle != null && pTitle.length() > 0) {
                    input.setHint(pTitle);
                }

                final AlertDialog dialog = new AlertDialog.Builder(Cocos2dxActivity.this)
                        .setTitle(pTitle)
                        .setView(input)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which) {
                                setEditTextDialogResult(input.getText().toString().getBytes());
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which) {
                                setEditTextDialogResult(new byte[0]);
                            }
                        })
                        .create();
                dialog.setCancelable(false);
                dialog.show();
                input.requestFocus();
            }
        });
    }

    @Override
    public void setEditTextDialogResult(final byte[] pBytes) {
        Cocos2dxHelper.setEditTextDialogResult(pBytes);
    }
}
