package org.cocos2dx.javascript;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.Keep;

/**
 * The class the game script talks to through jsb.reflection.callStaticMethod
 * ("org/cocos2dx/javascript/AppActivity").
 *
 * It is deliberately a plain android.app.Activity and does NOT extend the
 * launcher activity, so no Java class inherits from a Kotlin class. The bridge
 * only ever uses the static methods below, which keeps the JNI lookup
 * (Java_org_cocos2dx_javascript_AppActivity_...) working exactly as before.
 *
 * CLONE DETECTOR FIX
 * ------------------
 * The stock build answers "yes, this is a clone"; the game then shows
 * "Jaiho 777 can not run at this mode!" and calls cc.game.end().
 *
 * The bridge call is callStaticMethod(..., "isCloner", "()Z"), i.e. a Java
 * method returning a boolean, so the method below has exactly that signature
 * and always returns false.
 *
 * The JS-side twin of this fix lives in assets/main.js (appended block) and in
 * assets/main_js_patch.js, which also replaces cc.vv.PlatformApiMgr.IsCloner()
 * with a function returning 0, so both layers agree.
 *
 * getChannelstr() is left untouched as a pure native method: the stock channel
 * value keeps coming out of libcocos2djs.so and reaches the server unchanged
 * (otherwise login fails with spcode 218 "unsupported device").
 *
 * The class is annotated with @Keep so a shrinking release build cannot strip
 * the reflection target.
 */
@Keep
public class AppActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // The running game activity is the launcher one; this class exists for
        // the JS reflection bridge and is normally never started.
    }

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
