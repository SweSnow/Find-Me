package com.simon.findme;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;

import java.util.HashMap;

public class FontCache {

    public final static String RobotoMedium = "robotomedium.ttf";

    private static HashMap<String, Typeface> cache = new HashMap<String, Typeface>();

    /* UnsupportedOperationException WTF? */
    public static Typeface get(Context ctx, String fontName) throws UnsupportedOperationException {

        if (cache.containsKey(fontName)) {
            return cache.get(fontName);
        } else {

            Typeface face;

            if (fontName.equals(RobotoMedium) && Build.VERSION.SDK_INT >= 21) {
                face = Typeface.create("sans-serif-medium", Typeface.NORMAL);
            } else {
                face = Typeface.createFromAsset(ctx.getAssets(), fontName);
            }
            cache.put(fontName, face);
            return face;
        }
    }
}
