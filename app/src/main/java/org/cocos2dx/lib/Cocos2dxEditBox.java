package org.cocos2dx.lib;

import android.content.Context;
import android.graphics.Color;
import android.text.InputFilter;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

/**
 * Text box used by the engine when JavaScript calls editBox.show().
 * The native side only speaks plain method names, so keep them.
 */
public class Cocos2dxEditBox extends EditText {

    private int mMaxLength = -1;

    public Cocos2dxEditBox(final Context pContext) {
        super(pContext);
        init();
    }

    private void init() {
        setSingleLine(true);
        setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        setBackgroundColor(Color.WHITE);
        setTextColor(Color.BLACK);
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        setPadding(12, 8, 12, 8);
        setImeOptions(EditorInfo.IME_ACTION_DONE);
        setVisibility(View.GONE);
    }

    public void setFont(final String pFontName, final float pFontSize) {
        if (pFontSize > 0) {
            setTextSize(TypedValue.COMPLEX_UNIT_PX, pFontSize);
        }
    }

    public void setMaxLength(final int pMaxLength) {
        mMaxLength = pMaxLength;
        if (pMaxLength > 0) {
            setFilters(new InputFilter[]{new InputFilter.LengthFilter(pMaxLength)});
        } else {
            setFilters(new InputFilter[0]);
        }
    }

    public int getMaxLength() {
        return mMaxLength;
    }

    public void setPlaceHolderText(final String pText) {
        setHint(pText == null ? "" : pText);
    }

    public void setTextHorizontalAlignment(final int pAlignment) {
        switch (pAlignment) {
            case 0:
                setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
                break;
            case 1:
                setGravity(Gravity.CENTER);
                break;
            case 2:
                setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
                break;
            default:
                break;
        }
    }

    public void openKeyboard() {
        setVisibility(View.VISIBLE);
        requestFocus();
        setFocusableInTouchMode(true);
        Cocos2dxActivity activity = Cocos2dxActivity.getActivity();
        if (activity != null) {
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager)
                            activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(this, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
        }
    }

    public void closeKeyboard() {
        Cocos2dxActivity activity = Cocos2dxActivity.getActivity();
        if (activity != null) {
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager)
                            activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(getWindowToken(), 0);
            }
        }
        setVisibility(View.GONE);
    }
}
