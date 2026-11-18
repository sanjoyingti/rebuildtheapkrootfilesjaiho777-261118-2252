package org.cocos2dx.lib;

/**
 * Bridge for jsb.reflection: lets Java evaluate a script inside the running
 * JavaScript context. Must be called from the GL thread.
 */
public class Cocos2dxJavascriptJavaBridge {

    public static native int evalString(final String value);
}
