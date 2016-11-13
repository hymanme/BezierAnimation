package com.github.hymanme.adheisionview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.os.Looper;
import android.support.annotation.ColorInt;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.animation.OvershootInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.github.hymanme.adheisionview.utils.DensityUtils;

/**
 * /**
 * Author   :hyman
 * Email    :hymanme@163.com
 * Create at 2016/10/15
 * Description:
 */

public class AdhesionView extends FrameLayout {
    //触摸滑动容差
    private static final int TOUCH_SLOP = 10;
    //圆点背景圆角
    private static final int DEFAULT_DOT_BG_RADIUS = 25;
    //默认最大拖拽长度
    private final int DEFAULT_MAX_DRAG_LENGTH = 70;
    //默认圆点半径
    private final int DEFAULT_DOT_RADIUS = 10;
    //默认圆点颜色
    private final int DEFAULT_DOT_COLOR = 0xFFFF0000;
    //默认字体颜色
    private final int DEFAULT_DOT_TEXT_COLOR = 0xFFFFFFFF;
    //默认字体大小
    private final int DEFAULT_DOT_TEXT_SIZE = 12;

    //最大消息数
    private int maxMessageCount = 99;
    //反弹系数
    private float mTension = 3.0f;
    //最大拖拽长度
    private float mMaxDragLen;
    //圆点半径
    private float mDotRadius;
    //默认半径
    private float mDefaultDotRadius;
    //圆点颜色
    private int mDotColor;
    //文字字体颜色
    private int mDotTextColor;
    //画笔
    private Paint mPaint;
    //贝塞尔曲线的路径
    private Path mPath;
    //圆点是否被触摸
    private boolean isTouch;
    //曲线是否超过最大距离而断裂
    private boolean isBroken;

    //默认的xy坐标
    private float mStartX;
    private float mStartY;

    //手指触摸的xy坐标
    private float mTouchX;
    private float mTouchY;

    private Context mContext;

    //圆点显示的文字
    private String mDotText;
    //文字字体大小
    private float mDotTextSize;

    //圆点view
    private TextView mTextView;
    private int messageCount = 0;


    public AdhesionView(Context context) {
        this(context, null);
    }

    public AdhesionView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AdhesionView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AdhesionView);
        mMaxDragLen = a.getDimension(R.styleable.AdhesionView_av_maxDragLen, DensityUtils.dp2px(mContext, DEFAULT_MAX_DRAG_LENGTH));
        mDotRadius = a.getDimension(R.styleable.AdhesionView_av_dotRadius, DensityUtils.dp2px(mContext, DEFAULT_DOT_RADIUS));
        mDotColor = a.getColor(R.styleable.AdhesionView_av_dotColor, DEFAULT_DOT_COLOR);
        mDotTextColor = a.getColor(R.styleable.AdhesionView_av_dotTextColor, DEFAULT_DOT_TEXT_COLOR);
        mDotText = a.getString(R.styleable.AdhesionView_av_dotText);
        mDotTextSize = a.getDimension(R.styleable.AdhesionView_av_dotTextSize, DensityUtils.sp2px(mContext, DEFAULT_DOT_TEXT_SIZE));
        a.recycle();
        mDefaultDotRadius = mDotRadius;
        init();
    }

    /**
     * 初始化
     */
    private void init() {
        mPath = new Path();

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setStrokeWidth(0);
        mPaint.setColor(mDotColor);
        setWillNotDraw(false);

        mTextView = new TextView(mContext);
        mTextView.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(mDotColor);
        gd.setCornerRadius(DensityUtils.dp2px(mContext, DEFAULT_DOT_BG_RADIUS));

        mTextView.setBackgroundDrawable(gd);
        final int padding = DensityUtils.dp2px(mContext, 5);
        mTextView.setPadding(padding, 0, padding, 0);
        mTextView.setMinEms(1);
        mTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mDotTextSize);
        mTextView.setGravity(Gravity.CENTER);
        mTextView.setTextColor(mDotTextColor);
        mTextView.setText(mDotText);
        addView(mTextView);
        mTextView.measure(0, 0);
        mStartX = mTextView.getMeasuredWidth() / 2;
        mStartY = mTextView.getMeasuredHeight() / 2;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //计算曲线路径，可能距离超过，曲线已经断裂
        calculatePath();
        if (isTouch && !isBroken) {
            //回执曲线path
            canvas.drawPath(mPath, mPaint);
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.OVERLAY);
            canvas.drawCircle(mStartX, mStartY, mDotRadius, mPaint);
//            canvas.drawCircle(mTextView.getX() + mTextView.getWidth() / 2, mTextView.getY() + mTextView.getHeight() / 2, mDefaultDotRadius, mPaint);
        } else {
            //曲线断裂，清楚曲线
            canvas.drawCircle(mStartX, mStartY, 0, mPaint);
            canvas.drawCircle(mTouchX, mTouchY, 0, mPaint);
            canvas.drawLine(0, 0, 0, 0, mPaint);
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.OVERLAY);
        }
        super.onDraw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mTouchX = (int) event.getX();
        mTouchY = (int) event.getY();
        Log.d("mTouchX=", mTouchX + "");
        Log.d("mTouchY=", mTouchY + "");
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isBroken = false;
                mDotRadius = mDefaultDotRadius;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                //清楚消息动画或者返回远处反弹动画
                isTouch = false;
                if (isBroken) {
                    disappearAnim();
                } else {
                    returnBackAnim(mTextView.getX(), mTextView.getY());
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (Math.abs(mTouchX - mStartX) < TOUCH_SLOP && Math.abs(mTouchY - mStartY) < TOUCH_SLOP) {
                    isTouch = false;
                } else {
                    mTextView.setX(mTouchX - mTextView.getWidth() / 2);
                    mTextView.setY(mTouchY - mTextView.getHeight() / 2);
                    isTouch = true;
                }
                break;
        }
        invalidateView();
        return true;
    }

    public void invalidateView() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            invalidate();
        } else {
            postInvalidate();
        }
    }

    private void returnBackAnim(float mTouchX, float mTouchY) {
        TranslateAnimation back = new TranslateAnimation(mTouchX, 0, mTouchY, 0);
        back.setInterpolator(new OvershootInterpolator(mTension));
        back.setDuration(150);
        mTextView.setAnimation(back);
        back.startNow();
        mTextView.setX(mStartX - mTextView.getWidth() / 2);
        mTextView.setY(mStartY - mTextView.getHeight() / 2);
    }

    private void disappearAnim() {
        messageCount = 0;
        mTextView.setX(0);
        mTextView.setY(0);
        mTextView.setVisibility(GONE);
    }

    private void calculatePath() {
        int dragLen = (int) Math.sqrt(Math.pow(mTouchX - mStartX, 2) + Math.pow(mStartY - mTouchY, 2));
        if (dragLen > mMaxDragLen) {
            isBroken = true;
        } else {
            //未达到最大断裂距离，计算path: 四个顶点p1,p2,p3,p4 => 控制点 anchor1,anchor2
            //得到绘制贝塞尔曲线需要的四个点
            mDotRadius = Math.max(mDefaultDotRadius * (1.0f - dragLen / mMaxDragLen), 12);

            float r = (float) Math.asin((mDefaultDotRadius - mDotRadius) / dragLen);
            float a = (float) Math.atan((mStartY - mTouchY) / (mTouchX - mStartX));

            float offset1X = (float) Math.cos(Math.PI / 2 - r - a);
            float offset1Y = (float) Math.sin(Math.PI / 2 - r - a);

            float offset2X = (float) Math.cos(Math.PI / 2 + r - a);
            float offset2Y = (float) Math.sin(Math.PI / 2 + r - a);

            //第一条曲线
            float x1 = mStartX - offset1X * mDotRadius;
            float y1 = mStartY - offset1Y * mDotRadius;

            float x2 = mTouchX - offset1X * mDefaultDotRadius;
            float y2 = mTouchY - offset1Y * mDefaultDotRadius;

            //第二条曲线
            float x3 = mStartX + offset2X * mDotRadius;
            float y3 = mStartY + offset2Y * mDotRadius;

            float x4 = mTouchX + offset2X * mDefaultDotRadius;
            float y4 = mTouchY + offset2Y * mDefaultDotRadius;

            //控制点1,2
            float mAnchor1X = (x1 + x4) / 2;
            float mAnchor1Y = (y1 + y4) / 2;
            float mAnchor2X = (x2 + x3) / 2;
            float mAnchor2Y = (y2 + y3) / 2;


            mPath.reset();
            mPath.moveTo(x1, y1);
            mPath.quadTo(mAnchor1X, mAnchor1Y, x2, y2);
            mPath.lineTo(x4, y4);
            mPath.quadTo(mAnchor2X, mAnchor2Y, x3, y3);
            mPath.lineTo(x1, y1);
        }
    }

    public float getTension() {
        return mTension;
    }

    public void setTension(float mTension) {
        this.mTension = mTension;
    }

    public float getMaxDragLen() {
        return mMaxDragLen;
    }

    public void setMaxDragLen(float mMaxDragLen) {
        this.mMaxDragLen = DensityUtils.dp2px(mContext, mMaxDragLen);
        invalidateView();
    }

    public float getDefaultDotRadius() {
        return mDefaultDotRadius;
    }

    public void setDefaultDotRadius(float mDefaultDotRadius) {
        this.mDefaultDotRadius = DensityUtils.dp2px(mContext, mDefaultDotRadius);
        invalidateView();
    }

    public int getDotColor() {
        return mDotColor;
    }

    public void setDotColor(int mDotColor) {
        this.mDotColor = mDotColor;
        invalidateView();
    }

    public int getDotTextColor() {
        return mDotTextColor;
    }

    public void setDotTextColor(@ColorInt int dotTextColor) {
        this.mDotTextColor = dotTextColor;
        mTextView.setTextColor(mDotTextColor);
    }

    public String getDotText() {
        return mDotText;
    }

    public void setDotText(String dotText) {
        this.mDotText = dotText;
        mTextView.setText(mDotText);
    }

    public float getDotTextSize() {
        return mDotTextSize;
    }

    public void setDotTextSize(float dotTextSize) {
        this.mDotTextSize = DensityUtils.sp2px(mContext, dotTextSize);
        mTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mDotTextSize);
    }

    public int getMaxMessageCount() {
        return maxMessageCount;
    }

    public void setMaxMessageCount(int maxMessageCount) {
        this.maxMessageCount = maxMessageCount;
        updateText();
    }

    public void increment() {
        messageCount++;
        updateText();
    }

    public void setMessageCount(int count) {
        messageCount = count;
        updateText();
    }

    public void increment(int count) {
        messageCount += count;
        updateText();
    }

    public void decrement() {
        messageCount--;
        updateText();
    }

    public void decrement(int count) {
        messageCount -= count;
        updateText();
    }

    private void updateText() {
        mTextView.setVisibility(VISIBLE);
        if (messageCount > maxMessageCount) {
            setDotText(maxMessageCount + "+");
        } else if (messageCount < 1) {
            mTextView.setVisibility(GONE);
        } else {
            setDotText(messageCount + "");
        }
    }
}
