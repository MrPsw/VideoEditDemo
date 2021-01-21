package com.oaks.golf.utils;

import android.content.Context;

import com.oaks.golf.R;


/**
 * @author liyuqing
 * @date 2018/11/6.
 * @description 写自己的代码，让别人说去吧
 */
public class ScreenFilter extends AbstractFilter {


    public ScreenFilter(Context context) {
        super(context, R.raw.base_vertex, R.raw.base_frag);
    }
}
