package org.cocos2dx.lib;

/**
 * Callbacks the native engine needs from the hosting activity.
 * Names and signatures follow the stock cocos2d-x 3.x helper listener so the
 * prebuilt libcocos2djs.so can resolve them.
 */
public interface Cocos2dxHelperListener {

    void showDialog(final String pTitle, final String pMessage);

    void showEditTextDialog(final String pTitle,
                            final String pMessage,
                            final int pInputMode,
                            final int pInputFlag,
                            final int pReturnType,
                            final int pMaxLength);

    void runOnGLThread(final Runnable pRunnable);

    void setEditTextDialogResult(final byte[] pBytes);
}
