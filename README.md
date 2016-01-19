# NsRefreshLayout 
支持任意View的下拉刷新控件，同时支持上拉加载更多。实现原理：http://blog.csdn.net/xiaomoit/article/details/50469810

## 效果预览

![demo](https://github.com/xiaolifan/NsRefreshLayout/blob/master/art/demo.gif?raw=true)

## 属性说明

```xml
<declare-styleable name="NsRefreshLayout">
    <!--Loading视图背景颜色-->
    <attr name="load_view_bg_color" format="color|reference"/>
    <!--进度条颜色-->
    <attr name="progress_bar_color" format="color|reference"/>
    <!--进度条背景色-->
    <attr name="progress_bg_color" format="color|reference"/>
    <!--Loading视图中文字颜色-->
    <attr name="load_text_color" format="color|reference"/>
    <!--下拉刷新问题描述-->
    <attr name="pull_refresh_text" format="string|reference"/>
    <!--上拉加载文字描述-->
    <attr name="pull_load_text" format="string|reference"/>
    <!--是否自动触发加载更多-->
    <attr name="auto_load_more" format="boolean"/>
    <!--下拉刷新是否可用-->
    <attr name="pull_refresh_enable" format="boolean"/>
    <!--上拉加载是否可用-->
    <attr name="pull_load_enable" format="boolean"/>
</declare-styleable>
```

## 举例

```xml
<?xml version="1.0" encoding="utf-8"?>
<com.xlf.nrl.NsRefreshLayout
    android:id="@+id/nrl_test"
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:auto_load_more="false"
    tools:context="com.xlf.nrl.demo.MainActivity">

    <android.support.v7.widget.RecyclerView
        android:id="@+id/rv_test"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:layoutManager="LinearLayoutManager"
        tools:listitem="@layout/item_test"/>
</com.xlf.nrl.NsRefreshLayout>
```

```java
package com.xlf.nrl.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;

import com.xlf.nrl.NsRefreshLayout;

public class MainActivity extends AppCompatActivity implements
        NsRefreshLayout.NsRefreshLayoutController, NsRefreshLayout.NsRefreshLayoutListener {

    private boolean loadMoreEnable = true;
    private NsRefreshLayout refreshLayout;
    private RecyclerView rvTest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        refreshLayout = (NsRefreshLayout) findViewById(R.id.nrl_test);
        refreshLayout.setRefreshLayoutController(this);
        refreshLayout.setRefreshLayoutListener(this);

        rvTest = (RecyclerView) findViewById(R.id.rv_test);
        TestRecyclerAdapter adapter = new TestRecyclerAdapter(this);
        rvTest.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem item = menu.add(0, 0, 0, "禁用上拉加载功能");
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        loadMoreEnable = false;
        return true;
    }

    @Override
    public boolean isPullRefreshEnable() {
        return true;
    }

    @Override
    public boolean isPullLoadEnable() {
        return loadMoreEnable;
    }

    @Override
    public void onRefresh() {
        refreshLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                refreshLayout.finishPullRefresh();
            }
        }, 1000);
    }

    @Override
    public void onLoadMore() {
        refreshLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                refreshLayout.finishPullLoad();
            }
        }, 1000);
    }

}
```

## License
-------

    Mozilla Public License, version 2.0
