## 动机

项目中，需要一个支持任意View的下拉刷新+上拉加载控件，GitHub上有很多现成的实现，如[Android-PullToRefresh](https://github.com/chrisbanes/Android-PullToRefresh), [android-Ultra-Pull-To-Refresh](https://github.com/liaohuqiu/android-Ultra-Pull-To-Refresh)等，这些Library都非常优秀，但是[Android-PullToRefresh](https://github.com/chrisbanes/Android-PullToRefresh)
已经不在维护了，[android-Ultra-Pull-To-Refresh](https://github.com/liaohuqiu/android-Ultra-Pull-To-Refresh)本身并不支持上拉加载更多，经过一番纠结后决定自己写一个。

## 原理

&nbsp;&nbsp;&nbsp;&nbsp;无论是下拉刷新还是上拉加载更多，原理都是在内容View（ListView、RecyclerView...）不能下拉或者上划时响应用户的触摸事件，在顶部或者底部显示一个刷新视图，在程序刷新操作完成后再隐藏掉。

## 实现

&nbsp;&nbsp;&nbsp;&nbsp;既然要在头部和顶部添加刷新视图，我们的控件应该是个ViewGroup，我是直接继承FrameLayout，这个控件的名字叫[NsRefreshLayoutController](https://github.com/xiaolifan/NsRefreshLayout)。然后我们需要定义一些属性，如是否自动触发上拉加载更多、刷新视图中的文字颜色等。

### 属性定义

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

### 属性读取

```java
/**
 * 初始化控件属性
 */
private void initAttrs(Context context, AttributeSet attrs) {
    if (getChildCount() > 1) {
        throw new RuntimeException("can only have one child");
    }
    loadingViewFinalHeight = NrlUtils.dipToPx(context, LOADING_VIEW_FINAL_HEIGHT_DP);
    loadingViewOverHeight = loadingViewFinalHeight * 2;

    if (isInEditMode() && attrs == null) {
        return;
    }

    int resId;
    TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.NsRefreshLayout);
    Resources resources = context.getResources();

    //LoadView背景颜色
    resId = ta.getResourceId(R.styleable.NsRefreshLayout_load_view_bg_color, -1);
    if (resId == -1) {
        mLoadViewBgColor = ta.getColor(R.styleable.NsRefreshLayout_load_view_bg_color,
                Color.WHITE);
    } else {
        mLoadViewBgColor = resources.getColor(resId);
    }

    //加载文字颜色
    resId = ta.getResourceId(R.styleable.NsRefreshLayout_load_text_color, -1);
    if (resId == -1) {
        mLoadViewTextColor = ta.getColor(R.styleable.NsRefreshLayout_load_text_color,
                Color.BLACK);
    } else {
        mLoadViewTextColor = resources.getColor(resId);
    }

    //进度条背景颜色
    resId = ta.getResourceId(R.styleable.NsRefreshLayout_progress_bg_color, -1);
    if (resId == -1) {
        mProgressBgColor = ta.getColor(R.styleable.NsRefreshLayout_progress_bg_color,
                Color.WHITE);
    } else {
        mProgressBgColor = resources.getColor(resId);
    }

    //进度条颜色
    resId = ta.getResourceId(R.styleable.NsRefreshLayout_progress_bar_color, -1);
    if (resId == -1) {
        mProgressColor = ta.getColor(R.styleable.NsRefreshLayout_progress_bar_color,
                Color.RED);
    } else {
        mProgressColor = resources.getColor(resId);
    }

    //下拉刷新文字描述
    resId = ta.getResourceId(R.styleable.NsRefreshLayout_pull_refresh_text, -1);
    if (resId == -1) {
        mPullRefreshText = ta.getString(R.styleable.NsRefreshLayout_pull_refresh_text);
    } else {
        mPullRefreshText = resources.getString(resId);
    }

    //上拉加载文字描述
    resId = ta.getResourceId(R.styleable.NsRefreshLayout_pull_load_text, -1);
    if (resId == -1) {
        mPullLoadText = ta.getString(R.styleable.NsRefreshLayout_pull_load_text);
    } else {
        mPullLoadText = resources.getString(resId);
    }

    mAutoLoadMore = ta.getBoolean(R.styleable.NsRefreshLayout_auto_load_more, false);
    mPullRefreshEnable = ta.getBoolean(R.styleable.NsRefreshLayout_pull_refresh_enable, true);
    mPullLoadEnable = ta.getBoolean(R.styleable.NsRefreshLayout_pull_load_enable, true);

    ta.recycle();
}
```

### 属性使用

&nbsp;&nbsp;&nbsp;&nbsp;在内容View布局完成后(onFinishInflate)，根据设置的属性，来确定是否需要添加下拉刷新视图、上拉加载更多视图，以及视图中的文字颜色、进度条颜色等。

```java
@Override
protected void onFinishInflate() {
    super.onFinishInflate();
    mContentView = getChildAt(0);
    setupViews();
}

private void setupViews() {
    //下拉刷新视图
    LayoutParams lp;
    if (mPullRefreshEnable) {
        lp = new LayoutParams(LayoutParams.MATCH_PARENT, 0);
        headerView = new LoadView(getContext());
        headerView.setLoadText(TextUtils.isEmpty(mPullRefreshText) ?
                getContext().getString(R.string.default_pull_refresh_text) : mPullRefreshText);
        headerView.setStartEndTrim(0, 0.75f);
        headerView.setBackgroundColor(mLoadViewBgColor);
        headerView.setLoadTextColor(mLoadViewTextColor);
        headerView.setProgressBgColor(mProgressBgColor);
        headerView.setProgressColor(mProgressColor);
        addView(headerView, lp);
    }

    if (mPullLoadEnable) {
        //上拉加载更多视图
        lp = new LayoutParams(LayoutParams.MATCH_PARENT, 0);
        lp.gravity = Gravity.BOTTOM;
        footerView = new LoadView(getContext());
        footerView.setLoadText(TextUtils.isEmpty(mPullLoadText) ?
                getContext().getString(R.string.default_pull_load_text) : mPullLoadText);
        footerView.setStartEndTrim(0.5f, 1.25f);
        footerView.setBackgroundColor(mLoadViewBgColor);
        footerView.setLoadTextColor(mLoadViewTextColor);
        footerView.setProgressBgColor(mProgressBgColor);
        footerView.setProgressColor(mProgressColor);
        addView(footerView, lp);
    }
}
```

### 动态响应用户配置变化

&nbsp;&nbsp;&nbsp;&nbsp;有这样一种需求，一个列表分页加载，每一页10条，如果在上拉加载更多后只返回8条，说明已经没有更多数据了，所以在列表达到底部，用户再次上划时就不需要触发上拉加载更多了。基于这种需求，我设计了一个接口NsRefreshLayoutController。

```java
public interface NsRefreshLayoutController {
    /**
     * 当前下拉刷新是否可用
     */
    boolean isPullRefreshEnable();

    /**
     * 当前上拉加载是否可用，比如列表已无更多数据，可禁用上拉加载功能
     */
    boolean isPullLoadEnable();
}
```
使用时，实现这个接口，根据当前数据的情况返回True或者False启用或者禁用两个功能了。控件内部，我们在用户每次触发触摸事件的时候获取接口返回值。

```java
@Override
public boolean onInterceptTouchEvent(MotionEvent ev) {
    if (refreshLayoutController != null) {
        mPullRefreshEnable = refreshLayoutController.isPullRefreshEnable();
        mPullLoadEnable = refreshLayoutController.isPullLoadEnable();
    }
    return super.onInterceptTouchEvent(ev);
}
```

### 处理Touch事件

&nbsp;&nbsp;&nbsp;&nbsp;我们需要做到对Touch事件的处理不影响内容视图的功能，所以我们只处理Touch事件，不消耗Touch事件，一个合适的回调很重要，找来找去我选择了dispatchTouchEvent，官方文档对这个函数的描述如下：



&nbsp;&nbsp;&nbsp;&nbsp;处理Touch事件的流程如下，ACTION\_DOWN、ACTION\_MOVE时记录Touch的位置，ACTION\_MOVE时用当前Touch的位置减去上次DOWN或者MOVE的位置，得到手指滑动的距离，用这个距离来控制内容视图、刷新视图的显示位置，当达到触发刷新的位置后，提示用户松手触发刷新，用户松手后开始刷新动画并通知程序开始刷新。代码如下：

```java
@Override
public boolean dispatchTouchEvent(MotionEvent event) {
    if (!mPullRefreshEnable && !mPullLoadEnable) {
        return super.dispatchTouchEvent(event);
    }

    if (isRefreshing) {
        return super.dispatchTouchEvent(event);
    }

    switch (event.getActionMasked()) {
        case MotionEvent.ACTION_DOWN: {
            preY = event.getY();
            preX = event.getX();
            break;
        }

        case MotionEvent.ACTION_MOVE: {
            float currentY = event.getY();
            float currentX = event.getX();
            float dy = currentY - preY;
            float dx = currentX - preX;
            preY = currentY;
            preX = currentX;
            if (!actionDetermined) {
                //判断是下拉刷新还是上拉加载更多
                if (dy > 0 && !canChildScrollUp() && mPullRefreshEnable) {
                    mCurrentAction = ACTION_PULL_DOWN_REFRESH;
                    actionDetermined = true;
                } else if (dy < 0 && !canChildScrollDown() && mPullLoadEnable) {
                    mCurrentAction = ACTION_PULL_UP_LOAD_MORE;
                    actionDetermined = true;
                }
            }
            handleScroll(dy);
            observerArriveBottom();
            break;
        }

        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL: {
            //用户松手后需要判断当前的滑动距离是否满足触发刷新的条件
            if (releaseTouch()) {
                MotionEvent cancelEvent = MotionEvent.obtain(event);
                cancelEvent.setAction(MotionEvent.ACTION_CANCEL);
                return super.dispatchTouchEvent(cancelEvent);
            }
            break;
        }
    }

    return super.dispatchTouchEvent(event);
}

/**
 * 处理滚动
 */
private boolean handleScroll(float distanceY) {
    if (!canChildScrollUp() && mCurrentAction == ACTION_PULL_DOWN_REFRESH &&
            mPullRefreshEnable) {
        //下拉刷新
        LayoutParams lp = (LayoutParams) headerView.getLayoutParams();
        lp.height += distanceY;
        if (lp.height < 0) {
            lp.height = 0;
        } else if (lp.height > loadingViewOverHeight) {
            lp.height = (int) loadingViewOverHeight;
        }
        headerView.setLayoutParams(lp);
        if (lp.height < loadingViewOverHeight) {
            headerView.setLoadText(TextUtils.isEmpty(mPullRefreshText) ?
                    getContext().getString(R.string.default_pull_refresh_text) : mPullRefreshText);
        } else {
            headerView.setLoadText(getContext().getString(R.string.release_to_refresh));
        }
        headerView.setProgressRotation(lp.height / loadingViewOverHeight);
        adjustContentViewHeight(lp.height);
        return true;

    } else if (!canChildScrollDown() && mCurrentAction == ACTION_PULL_UP_LOAD_MORE && mPullLoadEnable) {
        //上拉加载更多
        LayoutParams lp = (LayoutParams) footerView.getLayoutParams();
        lp.height -= distanceY;
        if (lp.height < 0) {
            lp.height = 0;
        } else if (lp.height > loadingViewOverHeight) {
            lp.height = (int) loadingViewOverHeight;
        }
        footerView.setLayoutParams(lp);
        if (lp.height < loadingViewOverHeight) {
            footerView.setLoadText(TextUtils.isEmpty(mPullLoadText) ?
                    getContext().getString(R.string.default_pull_load_text) : mPullLoadText);
        } else {
            footerView.setLoadText(getContext().getString(R.string.release_to_load));
        }
        footerView.setProgressRotation(lp.height / loadingViewOverHeight);
        adjustContentViewHeight(-lp.height);
        return true;
    }
    return false;
}

private void adjustContentViewHeight(float h) {
    mContentView.setTranslationY(h);
    //下面的方式可以看到完整内容，但是有掉帧现象
    /*if (mCurrentAction == ACTION_PULL_DOWN_REFRESH) {
        mContentView.setTranslationY(h);
    }
    LayoutParams lp = (LayoutParams) mContentView.getLayoutParams();
    lp.height = (int) (getMeasuredHeight() - Math.abs(h));
    mContentView.setLayoutParams(lp);*/

}

private boolean releaseTouch() {
    boolean result = false;
    LayoutParams lp;
    if (mPullRefreshEnable && mCurrentAction == ACTION_PULL_DOWN_REFRESH) {
        lp = (LayoutParams) headerView.getLayoutParams();
        if (lp.height >= loadingViewOverHeight) {
            //触发下拉刷新
            startPullDownRefresh(lp.height);
            result = true;
        } else if (lp.height > 0) {
            //未满足下拉刷新触发条件，重置状态
            resetPullDownRefresh(lp.height);
            result = lp.height >= CLICK_TOUCH_DEVIATION;
        } else {
            resetPullRefreshState();
        }
    }

    if (mPullLoadEnable && mCurrentAction == ACTION_PULL_UP_LOAD_MORE) {
        lp = (LayoutParams) footerView.getLayoutParams();
        if (lp.height >= loadingViewOverHeight) {
            //触发上拉加载更多
            startPullUpLoadMore(lp.height);
            result = true;
        } else if (lp.height > 0) {
            //未满足上拉加载更多触发条件，重置状态
            resetPullUpLoadMore(lp.height);
            result = lp.height >= CLICK_TOUCH_DEVIATION;
        } else {
            resetPullLoadState();
        }
    }
    return result;
}

private void startPullDownRefresh(int headerViewHeight) {
    isRefreshing = true;
    ValueAnimator animator = ValueAnimator.ofFloat(headerViewHeight, loadingViewFinalHeight);
    animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            LayoutParams lp = (LayoutParams) headerView.getLayoutParams();
            lp.height = (int) ((Float) animation.getAnimatedValue()).floatValue();
            headerView.setLayoutParams(lp);
            adjustContentViewHeight(lp.height);
        }
    });
    animator.addListener(new SimpleAnimatorListener() {
        @Override
        public void onAnimationEnd(Animator animation) {
            headerView.start();
            headerView.setLoadText(getContext().getString(R.string.refresh_text));

            if (refreshLayoutListener != null) {
                refreshLayoutListener.onRefresh();
            }
        }
    });
    animator.setDuration(300);
    animator.start();
}

/**
 * 重置下拉刷新状态
 *
 * @param headerViewHeight 当前下拉刷新视图的高度
 */
private void resetPullDownRefresh(int headerViewHeight) {
    headerView.stop();
    //headerView.setStartEndTrim(0, 0.75f);
    ValueAnimator animator = ValueAnimator.ofFloat(headerViewHeight, 0);
    animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            LayoutParams lp = (LayoutParams) headerView.getLayoutParams();
            lp.height = (int) ((Float) animation.getAnimatedValue()).floatValue();
            headerView.setLayoutParams(lp);
            adjustContentViewHeight(lp.height);
        }
    });
    animator.addListener(new SimpleAnimatorListener() {
        @Override
        public void onAnimationEnd(Animator animation) {
            resetPullRefreshState();

        }
    });
    animator.setDuration(300);
    animator.start();
}

private void resetPullRefreshState() {
    //重置动画结束才算完全完成刷新动作
    isRefreshing = false;
    actionDetermined = false;
    mCurrentAction = -1;
    headerView.setLoadText(TextUtils.isEmpty(mPullRefreshText) ?
            getContext().getString(R.string.default_pull_refresh_text) : mPullRefreshText);
}

private void startPullUpLoadMore(int headerViewHeight) {
    isRefreshing = true;
    ValueAnimator animator = ValueAnimator.ofFloat(headerViewHeight, loadingViewFinalHeight);
    animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            LayoutParams lp = (LayoutParams) footerView.getLayoutParams();
            lp.height = (int) ((Float) animation.getAnimatedValue()).floatValue();
            footerView.setLayoutParams(lp);
            adjustContentViewHeight(-lp.height);
        }
    });
    animator.addListener(new SimpleAnimatorListener() {
        @Override
        public void onAnimationEnd(Animator animation) {
            footerView.start();
            footerView.setLoadText(getContext().getString(R.string.load_text));

            if (refreshLayoutListener != null) {
                refreshLayoutListener.onLoadMore();
            }
        }
    });
    animator.setDuration(300);
    animator.start();
}

/**
 * 重置下拉刷新状态
 *
 * @param headerViewHeight 当前下拉刷新视图的高度
 */
private void resetPullUpLoadMore(int headerViewHeight) {
    footerView.stop();
    //footerView.setStartEndTrim(0.5f, 1.25f);
    ValueAnimator animator = ValueAnimator.ofFloat(headerViewHeight, 0);
    animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            LayoutParams lp = (LayoutParams) footerView.getLayoutParams();
            lp.height = (int) ((Float) animation.getAnimatedValue()).floatValue();
            footerView.setLayoutParams(lp);
            adjustContentViewHeight(-lp.height);
        }
    });
    animator.addListener(new SimpleAnimatorListener() {
        @Override
        public void onAnimationEnd(Animator animation) {
            resetPullLoadState();

        }
    });
    animator.setDuration(300);
    animator.start();
}

private void resetPullLoadState() {
    //重置动画结束才算完全完成刷新动作
    isRefreshing = false;
    actionDetermined = false;
    mCurrentAction = -1;
    footerView.setLoadText(TextUtils.isEmpty(mPullLoadText) ?
            getContext().getString(R.string.default_pull_load_text) : mPullLoadText);
}

/**
 * @return 子视图是否可以下拉
 */
public boolean canChildScrollUp() {
    if (mContentView == null) {
        return false;
    }
    if (Build.VERSION.SDK_INT < 14) {
        if (mContentView instanceof AbsListView) {
            final AbsListView absListView = (AbsListView) mContentView;
            return absListView.getChildCount() > 0
                    && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0)
                    .getTop() < absListView.getPaddingTop());
        } else {
            return ViewCompat.canScrollVertically(mContentView, -1) || mContentView.getScrollY() > 0;
        }
    } else {
        return ViewCompat.canScrollVertically(mContentView, -1);
    }
}

/**
 * @return 子视图是否可以上划
 */
public boolean canChildScrollDown() {
    if (mContentView == null) {
        return false;
    }
    if (Build.VERSION.SDK_INT < 14) {
        if (mContentView instanceof AbsListView) {
            final AbsListView absListView = (AbsListView) mContentView;
            if (absListView.getChildCount() > 0) {
                int lastChildBottom = absListView.getChildAt(absListView.getChildCount() - 1)
                        .getBottom();
                return absListView.getLastVisiblePosition() == absListView.getAdapter().getCount() - 1
                        && lastChildBottom <= absListView.getMeasuredHeight();
            } else {
                return false;
            }

        } else {
            return ViewCompat.canScrollVertically(mContentView, 1) || mContentView.getScrollY() > 0;
        }
    } else {
        return ViewCompat.canScrollVertically(mContentView, 1);
    }
}

public void setRefreshLayoutListener(NsRefreshLayoutListener refreshLayoutListener) {
    this.refreshLayoutListener = refreshLayoutListener;
}
```

&nbsp;&nbsp;&nbsp;&nbsp;上面代码中有一个变量CLICK\_TOUCH\_DEVIATION，这个变量表示对用户点击事件的容错值，用户进行点击动作时，会产生很小的滑动距离，如果不做容错处理会出现刷新视图抖动出现的问题。

&nbsp;&nbsp;&nbsp;&nbsp;另外还有一个observerArriveBottom(); 这个函数就是处理自动加载更多的关键。该函数在Touch事件产生滑动距离后，采取类似轮询的机制，判断滑动是否已经停止，滑动事件停止后，根据内容控件当前状态、用户配置来确定是否触发加载更多事件。代码如下：

```java
private void observerArriveBottom() {
    if (isRefreshing || !mAutoLoadMore || !mPullLoadEnable) {
        return;
    }
    mContentView.getViewTreeObserver().addOnScrollChangedListener(
            new ViewTreeObserver.OnScrollChangedListener() {

                @Override
                public void onScrollChanged() {
                    mContentView.removeCallbacks(flingRunnable);
                    mContentView.postDelayed(flingRunnable, 6);
                }
            });
}

private Runnable flingRunnable = new Runnable() {
    @Override
    public void run() {
        if (isRefreshing || !mAutoLoadMore || !mPullLoadEnable) {
            return;
        }

        if (!canChildScrollDown()) {
            mCurrentAction = ACTION_PULL_UP_LOAD_MORE;
            isRefreshing = true;
            startPullUpLoadMore(0);
        }
    }
};
```

### 对外接口

```java
public interface NsRefreshLayoutListener {
    void onRefresh();

    void onLoadMore();
}
```

## 搞定

&nbsp;&nbsp;&nbsp;&nbsp;整个控件实现就是这样的，代码我已经放到GitHub上了，欢迎大家拍砖。
https://github.com/xiaolifan/NsRefreshLayout
