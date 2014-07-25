package com.onegoal.flashlight.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;

/**
 * Created by rutul on 5/8/14.
 */
public class Compass extends android.view.View {

    private float direction;

    public Compass(Context context) {
        super(context);
    }

    public Compass(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Compass(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }



    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int h = getMeasuredHeight();
        int w = getMeasuredWidth();
        int r;

        if (h > w) {
            r = w/2;
        } else {
            r = h/2;
        }

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        paint.setColor(Color.WHITE);

        canvas.drawCircle(w/2, h/2, r, paint);

        paint.setColor(Color.RED);
        canvas.drawLine(
                w/2,
                h/2,
                (float) (w/2 + r * Math.sin((double)(-direction)/ 180 * 3.143)),
                (float) (h/2 - r * Math.cos((double)(-direction)/ 180 * 3.143)),
                paint);

    }

    public void update(float dir) {
        this.direction = dir;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(
                MeasureSpec.getSize(widthMeasureSpec),
                MeasureSpec.getSize(heightMeasureSpec));
    }


}
