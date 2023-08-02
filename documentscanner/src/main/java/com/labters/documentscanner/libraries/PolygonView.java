/*
 * *
 *  * Created by Ali YÜCE on 3/2/20 11:18 PM
 *  * https://github.com/mayuce/
 *  * Copyright (c) 2020 . All rights reserved.
 *  * Last modified 3/2/20 11:10 PM
 *
 */

package com.labters.documentscanner.libraries;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Magnifier;

import com.labters.documentscanner.R;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

/**
 * Created by jhansi on 28/03/15.
 */
public class PolygonView extends FrameLayout {

    protected Context context;
    private Paint paint;
    private ImageView pointer1;
    private ImageView pointer2;
    private ImageView pointer3;
    private ImageView pointer4;
    private ImageView midPointer13;
    private ImageView midPointer12;
    private ImageView midPointer34;
    private ImageView midPointer24;
    private PolygonView polygonView;
    private Magnifier magnifier;

    public PolygonView(Context context) {
        super(context);
        this.context = context;
        init();
    }

    public PolygonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    public PolygonView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        init();
    }

    private void init() {
        polygonView = this;
        pointer1 = getImageView(0, 0);
        pointer2 = getImageView(getWidth(), 0);
        pointer3 = getImageView(0, getHeight());
        pointer4 = getImageView(getWidth(), getHeight());
        midPointer13 = getImageView(0, getHeight() / 2);
        midPointer13.setOnTouchListener(new MidPointTouchListenerImpl(pointer1, pointer3));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            magnifier = new Magnifier(polygonView);
        midPointer12 = getImageView(0, getWidth() / 2);
        midPointer12.setOnTouchListener(new MidPointTouchListenerImpl(pointer1, pointer2));

        midPointer34 = getImageView(0, getHeight() / 2);
        midPointer34.setOnTouchListener(new MidPointTouchListenerImpl(pointer3, pointer4));

        midPointer24 = getImageView(0, getHeight() / 2);
        midPointer24.setOnTouchListener(new MidPointTouchListenerImpl(pointer2, pointer4));

        addView(pointer1);
        addView(pointer2);
        addView(midPointer13);
        addView(midPointer12);
        addView(midPointer34);
        addView(midPointer24);
        addView(pointer3);
        addView(pointer4);
        initPaint();
    }

    @Override
    protected void attachViewToParent(View child, int index, ViewGroup.LayoutParams params) {
        super.attachViewToParent(child, index, params);
    }

    private void initPaint() {
        paint = new Paint();
        handleImageValid(true);
        paint.setColor(getResources().getColor(R.color.blue));
        paint.setStrokeWidth(2);
        paint.setAntiAlias(true);
    }

    private void handleImageValid(Boolean isValid) {
        if (imageValidListener != null) {
            imageValidListener.onListener(isValid);
        }
    }

    public Map<Integer, PointF> getPoints() {

        List<PointF> points = new ArrayList<>();
        points.add(new PointF(pointer1.getX(), pointer1.getY()));
        points.add(new PointF(pointer2.getX(), pointer2.getY()));
        points.add(new PointF(pointer3.getX(), pointer3.getY()));
        points.add(new PointF(pointer4.getX(), pointer4.getY()));

        return getOrderedPoints(points);
    }

    public Map<Integer, PointF> getOrderedPoints(List<PointF> points) {

        PointF centerPoint = new PointF();
        int size = points.size();
        for (PointF pointF : points) {
            centerPoint.x += pointF.x / size;
            centerPoint.y += pointF.y / size;
        }
        Map<Integer, PointF> orderedPoints = new HashMap<>();
        for (PointF pointF : points) {
            int index = -1;
            if (pointF.x < centerPoint.x && pointF.y < centerPoint.y) {
                index = 0;
            } else if (pointF.x > centerPoint.x && pointF.y < centerPoint.y) {
                index = 1;
            } else if (pointF.x < centerPoint.x && pointF.y > centerPoint.y) {
                index = 2;
            } else if (pointF.x > centerPoint.x && pointF.y > centerPoint.y) {
                index = 3;
            }
            orderedPoints.put(index, pointF);
        }
        return orderedPoints;
    }

    public void setPoints(Map<Integer, PointF> pointFMap) {
        if (pointFMap.size() == 4) {
            setPointsCoordinates(pointFMap);
        }
    }

    public void setPointColor(int color) {
        if (paint != null)
            paint.setColor(color);
    }

    private void setPointsCoordinates(Map<Integer, PointF> pointFMap) {
        pointer1.setX(pointFMap.get(0).x);
        pointer1.setY(pointFMap.get(0).y);

        pointer2.setX(pointFMap.get(1).x);
        pointer2.setY(pointFMap.get(1).y);

        pointer3.setX(pointFMap.get(2).x);
        pointer3.setY(pointFMap.get(2).y);

        pointer4.setX(pointFMap.get(3).x);
        pointer4.setY(pointFMap.get(3).y);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        ImageView tmpPointer1;
        ImageView tmpPointer2;
        ImageView tmpPointer3;
        ImageView tmpPointer4;
        if (isValidShape(getPoints())) {
            List<ImageView> points = new ArrayList<>();
            points.add(pointer1);
            points.add(pointer2);
            points.add(pointer3);
            points.add(pointer4);
            Collections.sort(points, (obj1, obj2) -> Float.compare(obj1.getY(), obj2.getY()));
            ImageView imageViewSort1 = points.get(0);
            ImageView imageViewSort2 = points.get(1);
            ImageView imageViewSort3 = points.get(2);
            ImageView imageViewSort4 = points.get(3);

            if (imageViewSort1.getX() > imageViewSort2.getX()) {
                tmpPointer1 = imageViewSort2;
                tmpPointer2 = imageViewSort1;
            } else {
                tmpPointer1 = imageViewSort1;
                tmpPointer2 = imageViewSort2;
            }
            if (imageViewSort3.getX() > imageViewSort4.getX()) {
                tmpPointer3 = imageViewSort4;
                tmpPointer4 = imageViewSort3;
            } else {
                tmpPointer3 = imageViewSort3;
                tmpPointer4 = imageViewSort4;
            }
        } else {
            tmpPointer1 = pointer1;
            tmpPointer2 = pointer2;
            tmpPointer3 = pointer3;
            tmpPointer4 = pointer4;
        }

        int haftWidthPointer1 = tmpPointer1.getWidth() / 2;
        int haftHeightPointer1 = tmpPointer1.getHeight() / 2;
        int haftWidthPointer2 = tmpPointer2.getWidth() / 2;
        int haftHeightPointer2 = tmpPointer2.getHeight() / 2;
        int haftWidthPointer3 = tmpPointer3.getWidth() / 2;
        int haftHeightPointer3 = tmpPointer3.getHeight() / 2;
        int haftWidthPointer4 = tmpPointer4.getWidth() / 2;
        int haftHeightPointer4 = tmpPointer4.getHeight() / 2;
        canvas.drawLine(tmpPointer1.getX() + haftWidthPointer1, tmpPointer1.getY() + haftHeightPointer1, tmpPointer3.getX() + haftWidthPointer3, tmpPointer3.getY() + haftHeightPointer3, paint);
        canvas.drawLine(tmpPointer1.getX() + haftWidthPointer1, tmpPointer1.getY() + haftHeightPointer1, tmpPointer2.getX() + haftWidthPointer2, tmpPointer2.getY() + haftHeightPointer2, paint);
        canvas.drawLine(tmpPointer2.getX() + haftWidthPointer2, tmpPointer2.getY() + haftHeightPointer2, tmpPointer4.getX() + haftWidthPointer4, tmpPointer4.getY() + haftHeightPointer4, paint);
        canvas.drawLine(tmpPointer3.getX() + haftWidthPointer3, tmpPointer3.getY() + haftHeightPointer3, tmpPointer4.getX() + haftWidthPointer4, tmpPointer4.getY() + haftHeightPointer4, paint);
        midPointer13.setX(tmpPointer3.getX() - ((tmpPointer3.getX() - tmpPointer1.getX()) / 2));
        midPointer13.setY(tmpPointer3.getY() - ((tmpPointer3.getY() - tmpPointer1.getY()) / 2));
        midPointer24.setX(tmpPointer4.getX() - ((tmpPointer4.getX() - tmpPointer2.getX()) / 2));
        midPointer24.setY(tmpPointer4.getY() - ((tmpPointer4.getY() - tmpPointer2.getY()) / 2));
        midPointer34.setX(tmpPointer4.getX() - ((tmpPointer4.getX() - tmpPointer3.getX()) / 2));
        midPointer34.setY(tmpPointer4.getY() - ((tmpPointer4.getY() - tmpPointer3.getY()) / 2));
        midPointer12.setX(tmpPointer2.getX() - ((tmpPointer2.getX() - tmpPointer1.getX()) / 2));
        midPointer12.setY(tmpPointer2.getY() - ((tmpPointer2.getY() - tmpPointer1.getY()) / 2));
    }

    private void drawMag(float x, float y) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && magnifier != null) {
            magnifier.show(x, y);
        }
    }

    private void dismissMag() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && magnifier != null) {
            magnifier.dismiss();
        }
    }

    private ImageView getImageView(int x, int y) {
        ImageView imageView = new ImageView(context);
        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        imageView.setLayoutParams(layoutParams);
        imageView.setImageResource(R.drawable.circle);
        imageView.setX(x);
        imageView.setY(y);
        imageView.setOnTouchListener(new TouchListenerImpl());
        return imageView;
    }

    public void setImageValidListener(@Nullable Function1<? super Boolean, Unit> listener) {

    }

    private OnImageValidListener imageValidListener;

    public void setImageValidListener(OnImageValidListener l) {
        imageValidListener = l;
    }

    public interface OnImageValidListener {
        void onListener(Boolean isValid);
    }

    private class MidPointTouchListenerImpl implements OnTouchListener {

        PointF DownPT = new PointF(); // Record Mouse Position When Pressed Down
        PointF StartPT = new PointF(); // Record Start Position of 'img'

        private ImageView mainPointer1;
        private ImageView mainPointer2;

        public MidPointTouchListenerImpl(ImageView mainPointer1, ImageView mainPointer2) {
            this.mainPointer1 = mainPointer1;
            this.mainPointer2 = mainPointer2;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int eid = event.getAction();
            switch (eid) {
                case MotionEvent.ACTION_MOVE:
                    PointF mv = new PointF(event.getX() - DownPT.x, event.getY() - DownPT.y);

                    if (Math.abs(mainPointer1.getX() - mainPointer2.getX()) > Math.abs(mainPointer1.getY() - mainPointer2.getY())) {
                        if (((mainPointer2.getY() + mv.y + v.getHeight() < polygonView.getHeight()) && (mainPointer2.getY() + mv.y > 0))) {
                            v.setX((int) (StartPT.y + mv.y));
                            StartPT = new PointF(v.getX(), v.getY());
                            mainPointer2.setY((int) (mainPointer2.getY() + mv.y));
                        }
                        if (((mainPointer1.getY() + mv.y + v.getHeight() < polygonView.getHeight()) && (mainPointer1.getY() + mv.y > 0))) {
                            v.setX((int) (StartPT.y + mv.y));
                            StartPT = new PointF(v.getX(), v.getY());
                            mainPointer1.setY((int) (mainPointer1.getY() + mv.y));
                        }
                    } else {
                        if ((mainPointer2.getX() + mv.x + v.getWidth() < polygonView.getWidth()) && (mainPointer2.getX() + mv.x > 0)) {
                            v.setX((int) (StartPT.x + mv.x));
                            StartPT = new PointF(v.getX(), v.getY());
                            mainPointer2.setX((int) (mainPointer2.getX() + mv.x));
                        }
                        if ((mainPointer1.getX() + mv.x + v.getWidth() < polygonView.getWidth()) && (mainPointer1.getX() + mv.x > 0)) {
                            v.setX((int) (StartPT.x + mv.x));
                            StartPT = new PointF(v.getX(), v.getY());
                            mainPointer1.setX((int) (mainPointer1.getX() + mv.x));
                        }
                    }
                    drawMag(StartPT.x + 50, StartPT.y + 50);
                    break;
                case MotionEvent.ACTION_DOWN:
                    DownPT.x = event.getX();
                    DownPT.y = event.getY();
                    StartPT = new PointF(v.getX(), v.getY());
                    break;
                case MotionEvent.ACTION_UP:
                    int color = 0;
                    boolean validShape = isValidShape(getPoints());
                    handleImageValid(validShape);
                    if (validShape) {
                        color = getResources().getColor(R.color.blue);
                    } else {
                        color = getResources().getColor(R.color.orange);
                    }
                    paint.setColor(color);
                    dismissMag();
                    break;
                default:
                    break;
            }
            polygonView.invalidate();
            return true;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

    public boolean isValidShape(Map<Integer, PointF> pointFMap) {
        return pointFMap.size() == 4;
    }

    private class TouchListenerImpl implements OnTouchListener {

        PointF DownPT = new PointF(); // Record Mouse Position When Pressed Down
        PointF StartPT = new PointF(); // Record Start Position of 'img'

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int eid = event.getAction();
            switch (eid) {
                case MotionEvent.ACTION_MOVE:
                    PointF mv = new PointF(event.getX() - DownPT.x, event.getY() - DownPT.y);
                    if (((StartPT.x + mv.x + v.getWidth()) < polygonView.getWidth() && (StartPT.y + mv.y + v.getHeight() < polygonView.getHeight())) && ((StartPT.x + mv.x) > 0 && StartPT.y + mv.y > 0)) {
                        v.setX((int) (StartPT.x + mv.x));
                        v.setY((int) (StartPT.y + mv.y));
                        StartPT = new PointF(v.getX(), v.getY());
                        drawMag(StartPT.x + 50, StartPT.y + 50);
                    }
                    break;
                case MotionEvent.ACTION_DOWN:
                    DownPT.x = event.getX();
                    DownPT.y = event.getY();
                    StartPT = new PointF(v.getX(), v.getY());
                    break;
                case MotionEvent.ACTION_UP:
                    int color = 0;
                    boolean validShape = isValidShape(getPoints());
                    handleImageValid(validShape);
                    if (validShape) {
                        color = getResources().getColor(R.color.blue);
                    } else {
                        color = getResources().getColor(R.color.orange);
                    }
                    paint.setColor(color);
                    dismissMag();
                    break;
                default:
                    break;
            }
            polygonView.invalidate();
            return true;
        }

    }

}
