package com.xlf.nrl;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.FrameLayout;

/**
 * Created by xiaolifan on 2015/12/21.
 * QQ: 1147904198
 * Email: xiao_lifan@163.com
 */
public class NsRefreshLayout extends FrameLayout {

    private static final int LOADING_VIEW_FINAL_HEIGHT_DP = 80;

    private static final int ACTION_PULL_DOWN_REFRESH = 0;
    private static final int ACTION_PULL_UP_LOAD_MORE = 1;


    private LoadView headerView;
    private LoadView footerView;
    private View mContentView;

    /**
     * 是否支持下拉刷新
     */
    private boolean mPullRefreshEnable = true;
    /**
     * 是否支持上拉加载更多
     */
    private boolean mPullLoadEnable = true;
    /**
     * 是否自动加载更多：false-释放后加载更多，true-到达上拉加载条件后自动触发
     */
    private boolean mAutoLoadMore;

    private NsRefreshLayoutListener refreshLayoutListener;
    private NsRefreshLayoutController refreshLayoutController;

    /**
     * 上一次触摸的Y位置
     */
    private float preY;
    private float preX;

    /**
     * 是否正在进行刷新操作
     */
    private boolean isRefreshing = false;

    /**
     * 加载视图最终展示的高度
     */
    private float loadingViewFinalHeight = 0;
    /**
     * 加载视图回弹的高度
     */
    private float loadingViewOverHeight = 0;

    private boolean actionDetermined = false;
    private int mCurrentAction = -1;

    //控件属性
    /**
     * LoadView背景颜色
     */
    private int mLoadViewBgColor;
    /**
     * 进度条背景颜色
     */
    private int mProgressBgColor;
    /**
     * 进度条颜色
     */
    private int mProgressColor;
    /**
     * LoadView文字颜色
     */
    private int mLoadViewTextColor;
    /**
     * 下拉刷新文字描述
     */
    private String mPullRefreshText;
    /**
     * 上拉加载文字描述
     */
    private String mPullLoadText;

    /**
     * 点击移动距离的误差值（点击操作可能会导致轻微的滑动）
     */
    private static final int CLICK_TOUCH_DEVIATION = 4;

    public NsRefreshLayout(Context context) {
        super(context);
        initAttrs(context, null);
    }

    public NsRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttrs(context, attrs);
    }

    public NsRefreshLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public NsRefreshLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initAttrs(context, attrs);
    }

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
    }

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

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        super.requestDisallowInterceptTouchEvent(disallowIntercept);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if ((!mPullRefreshEnable && !mPullLoadEnable) || isRefreshing) {
            return super.onInterceptTouchEvent(ev);
        }

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                if (refreshLayoutController != null) {
                    mPullRefreshEnable = refreshLayoutController.isPullRefreshEnable();
                    mPullLoadEnable = refreshLayoutController.isPullLoadEnable();
                }
                preY = ev.getY();
                preX = ev.getX();
                actionDetermined = false;
                return super.onInterceptTouchEvent(ev);
            }

            case MotionEvent.ACTION_MOVE: {
                float currentY = ev.getY();
                float currentX = ev.getX();
                float dy = currentY - preY;
                float dx = currentX - preX;
                preY = currentY;
                preX = currentX;
                if (!actionDetermined) {
                    actionDetermined = true;
                    //判断是下拉刷新还是上拉加载更多
                    if (dy > 0 && !canChildScrollUp() && mPullRefreshEnable) {
                        mCurrentAction = ACTION_PULL_DOWN_REFRESH;
                    } else if (dy < 0 && !canChildScrollDown() && mPullLoadEnable) {
                        mCurrentAction = ACTION_PULL_UP_LOAD_MORE;
                    } else {
                        mCurrentAction = -1;
                    }
                }

                if (mCurrentAction != -1) {
                    return true;
                } else {
                    return super.onInterceptTouchEvent(ev);
                }
            }

            default: {
                return super.onInterceptTouchEvent(ev);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if ((!mPullRefreshEnable && !mPullLoadEnable) || isRefreshing) {
            return false;
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_MOVE: {
                float currentY = event.getY();
                float currentX = event.getX();
                float dy = currentY - preY;
                float dx = currentX - preX;
                preY = currentY;
                preX = currentX;
                handleScroll(dy);
                observerArriveBottom();
                return true;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                return releaseTouch();
            }

            default: {
                return super.onTouchEvent(event);
            }
        }

    }

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
            if (lp.height > 0) {
                return true;
            }

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
            if (lp.height > 0) {
                return true;
            }
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

    public interface NsRefreshLayoutListener {
        void onRefresh();

        void onLoadMore();
    }

    public void setRefreshLayoutController(NsRefreshLayoutController nsRefreshLayoutController) {
        this.refreshLayoutController = nsRefreshLayoutController;
    }

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

    /**
     * 完成下拉刷新动作
     */
    public void finishPullRefresh() {
        if (mCurrentAction == ACTION_PULL_DOWN_REFRESH) {
            resetPullDownRefresh(headerView == null ? 0 : headerView.getMeasuredHeight());
        }
    }

    /**
     * 完成上拉加载更多动作
     */
    public void finishPullLoad() {
        if (mCurrentAction == ACTION_PULL_UP_LOAD_MORE) {
            resetPullUpLoadMore(footerView == null ? 0 : footerView.getMeasuredHeight());
        }
    }
}
