package com.xlf.nrl;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.TypedValue;

/**
 * Created by xiaolifan on 2015/12/22.
 * QQ: 1147904198
 * Email: xiao_lifan@163.com
 */
public abstract class NrlUtils {

    public static float dipToPx(Context context, float value) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, metrics);
    }
}
