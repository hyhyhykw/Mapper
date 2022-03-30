package com.mapper.mapper;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.mapper.annotation.ViewCreator;

import java.util.Set;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatViewInflater;
import androidx.collection.ArraySet;

/**
 * Created time : 2022/3/30 22:18.
 *
 * @author 10585
 */
public class ViewInflater extends AppCompatViewInflater {

    private static Set<ViewCreator> mCreators;

    public static void addCreator(ViewCreator viewCreator) {
        synchronized (ViewInflater.class) {
            if (null == mCreators) {
                mCreators = new ArraySet<>();
            }
        }
        mCreators.add(viewCreator);
    }

    @Nullable
    @Override
    protected View createView(Context context, String name, AttributeSet attrs) {
        if (null != mCreators) {
            for (ViewCreator creator : mCreators) {
                View view = creator.createView(context, name, attrs);
                if (view != null) return view;
            }
        }
        return super.createView(context, name, attrs);
    }
}