package com.example.myapplication.view;

import android.content.Context;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import static android.os.SystemClock.sleep;

public class PullDownDumperLayout extends LinearLayout implements View.OnTouchListener {

    /**
     * 取布局中的第一个子元素为下拉隐藏头部
     */
    private View mHeadLayout;

    /**
     * 隐藏头部布局的高的负值
     */
    private int mHeadLayoutHeight;

    /**
     * 隐藏头部的布局参数
     */
    private MarginLayoutParams mHeadLayoutParams;

    /**
     * 判断是否为第一次初始化，第一次初始化需要把headView移出界面外
     */
    private boolean mOnLayoutIsInit=false;
    /**
     * 移动时，前一个坐标
     */
    private float mMoveY;

    /**
     * 如果为false，会退出头部展开或隐藏动画
     */
    private boolean mChangeHeadLayoutTopMargin;

    /**
     * 触发动画的分界线，由mRatio计算得到
     */
    private int mBoundary;

    /**
     * 头部布局的隐藏和展开速度，以及单次执行时间
     */
    private int mHeadLayoutHideSpeed;
    private int mHeadLayoutUnfoldSpeed;
    private long mSleepTime;

    /**
     * 触发动画的分界线，头部布局上半部分和整体高度的比例
     */
    private double mRatio;

    public PullDownDumperLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        //初始化参数，根据自己的需求调整
        mHeadLayoutHideSpeed=-20;
        mHeadLayoutUnfoldSpeed=20;
        mSleepTime=10;
        mRatio=0.5;
    }

    /**
     * 布局开始设置每一个控件
     * 在activity的onCreate执行之后才会执行
     * 因此可以在onCreate中调用set方法设置参数
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if(!mOnLayoutIsInit && changed) {
            //将第一个子元素作为头部移出界面外
            mHeadLayout = this.getChildAt(0);
            mHeadLayoutHeight=-mHeadLayout.getHeight();
            mBoundary=(int)(mRatio*mHeadLayoutHeight);//计算触发动画分界线
            mHeadLayoutParams=(MarginLayoutParams) mHeadLayout.getLayoutParams();
            mHeadLayoutParams.topMargin=mHeadLayoutHeight;
            mHeadLayout.setLayoutParams(mHeadLayoutParams);
            //TODO 设置手势监听器，不能触碰的控件需要添加android:clickable="true"
            getChildAt(1).setOnTouchListener(this);
            mHeadLayout.setOnTouchListener(this);
            //标记已被初始化
            mOnLayoutIsInit=true;
        }
    }

    /**
     * 屏幕触摸操作监听器
     * @return false则注册本监听器的控件将不会对事件做出响应，true则相反
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mMoveY=event.getRawY();//捕获按下时的坐标，初始化mMoveY
                mChangeHeadLayoutTopMargin=false;
                break;
            case MotionEvent.ACTION_MOVE:
                float currY=event.getRawY();
                int vector=(int)(currY-mMoveY);//向量，用于判断手势的上滑和下滑
                mMoveY=currY;
                //判断是否为滑动
                if(Math.abs(vector)==0){
                    return false;
                }
                //头部完全隐藏时不再向上滑动
                if (vector < 0 && mHeadLayoutParams.topMargin <= mHeadLayoutHeight) {
                    return false;
                }
                //头部完全展开时不再向下滑动
                if (vector > 0 && mHeadLayoutParams.topMargin >= 0) {
                    return false;
                }

                //对增量进行修正，对滑动距离进行减半
                int topMargin = mHeadLayoutParams.topMargin + (vector/2);//阻尼值
                if(topMargin>0){
                    // 瞬间拉动的距离超过了头部高度，因为这一瞬间很短，这里采用直接赋值的方式
                    // 如需平滑过渡，要另开线程，并且监听到ACTION_DOWN时线程可被打断
                    topMargin = 0;
                }
                else if(topMargin<mHeadLayoutHeight){
                    // 瞬间拉动的距离超过了头部高度，因为这一瞬间很短，这里采用直接赋值的方式
                    // 如需平滑过渡，要另开线程，并且监听ACTION_DOWN时线程可被打断
                    topMargin = mHeadLayoutHeight;
                }
                //用户对屏幕的滑动将会改变控件的TopMargin
                mHeadLayoutParams.topMargin = topMargin ;
                mHeadLayout.setLayoutParams(mHeadLayoutParams);
                break;
            default:
                //TODO 出现其他触碰事件，如MotionEvent.ACTION_UP时，根据阈值判断此时头部应该弹出还是隐藏
                mChangeHeadLayoutTopMargin=true;
                if(mHeadLayoutParams.topMargin<=mBoundary){
                    //隐藏
                    new MoveHeaderTask().execute(true);
                }
                else{
                    //展开
                    new MoveHeaderTask().execute(false);
                }
                break;
        }
        return false;
    }

    /**
     * 新线程，隐藏或者展开头部布局，线程可被ACTION_DOWN打断
     */
    class MoveHeaderTask extends AsyncTask<Boolean, Integer, Integer> {

        /**
         *
         * @param opt true为隐藏动画，false为展开动画
         * @return
         */
        @Override
        protected Integer doInBackground(Boolean... opt) {
            int topMargin=mHeadLayoutParams.topMargin;
            //true为隐藏，false为展开
            int speed=(opt[0])?mHeadLayoutHideSpeed:mHeadLayoutUnfoldSpeed;
            while(mChangeHeadLayoutTopMargin){
                topMargin += speed;
                if (topMargin <= mHeadLayoutHeight||topMargin>=0) {
                    topMargin=(opt[0])?mHeadLayoutHeight:0;
                    publishProgress(topMargin);
                    break;
                }
                publishProgress(topMargin);
                sleep(mSleepTime);
            }
            return null;
        }

        //调用publishProg
        // ress后会执行
        @Override
        protected void onProgressUpdate(Integer... topMargin) {
            mHeadLayoutParams.topMargin=topMargin[0];
            mHeadLayout.setLayoutParams(mHeadLayoutParams);
        }

    }

    //调整参数
    public void setHeadLayoutHideSpeed(int speed){
        this.mHeadLayoutHideSpeed=speed;
    }
    public void setHeadLayoutUnfoldSpeed(int speed){
        this.mHeadLayoutUnfoldSpeed=speed;
    }
    public void setSleepTime(long time){
        this.mSleepTime=time;
    }
    public void setRatio(double ratio){
        this.mRatio=ratio;
    }
}
