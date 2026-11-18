package org.cocos2dx.javascript;

import com.jaiho.sss.in.MainActivity;

/**
 * The class the game script talks to through jsb.reflection.callStaticMethod
 * ("org/cocos2dx/javascript/AppActivity").
 *
 * CLONE DETECTOR FIX
 * ------------------
 * The stock build answers "yes, this is a clone" and the game then shows
 * "Jaiho 777 can not run at this mode!" and calls cc.game.end().
 *
 * The bridge call is callStaticMethod(..., "isCloner", "()Z"), i.e. a Java
 * method returning a boolean, so the method below has exactly that signature
 * (the earlier int version could not be resolved by the "()Z" lookup) and it
 * always returns false.
 *
 * The JS-side twin of this fix lives in assets/main_js_patch.js, which also
 * replaces cc.vv.PlatformApiMgr.IsCloner() with a function returning 0, so both
 * layers agree.
 *
 * getChannelstr() is left untouched as a pure native method: the stock channel
 * value keeps coming out of libcocos2djs.so and reaches the server unchanged
 * (otherwise login fails with spcode 218 "unsupported device").
 */
public class AppActivity extends MainActivity {

    /** Native symbol: Java_org_cocos2dx_javascript_AppActivity_isCloner. */
    public static boolean isCloner() {
        return false;
    }

    /** Extra spelling used by some builds: "()I". */
    public static int isClonerInt() {
        return 0;
    }

    /** Stock channel string, implemented inside libcocos2djs.so. */
    public static native String getChannelstr();
}
