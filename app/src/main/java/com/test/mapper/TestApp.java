package com.test.mapper;

import android.app.Application;

import com.mapper.annotation.Mapper;
import com.mapper.annotation.ViewCreator;

/**
 * Created time : 2022/3/30 21:56.
 *
 * @author 10585
 */
@Mapper
public class TestApp extends Application {

    static {
        ViewCreator creator=new TestViewCreator();
    }
}