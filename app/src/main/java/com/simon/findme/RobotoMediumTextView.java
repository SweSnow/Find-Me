package com.simon.findme;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

public class RobotoMediumTextView extends TextView {

    public RobotoMediumTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setTypeface(FontCache.get(getContext(), FontCache.RobotoMedium));
    }

    public RobotoMediumTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RobotoMediumTextView(Context context) {
        super(context);
    }

}