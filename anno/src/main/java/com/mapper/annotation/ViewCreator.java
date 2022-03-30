package com.mapper.annotation;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created time : 2022/3/30 21:12.
 *
 * @author 10585
 */
public interface ViewCreator {
    View createView(Context context, String name, AttributeSet attrs);
}
