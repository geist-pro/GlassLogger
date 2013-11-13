package com.mrk1869.glasslogger;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

public class MonitoringActivity extends Activity{

    private IRSensorLogger irSensorLogger;
    private Thread mThread;
    private GraphView mGraphView;
    private boolean isRunning;
    
    private SensorManager sensorManager;
    private Sensor accSensor;
    private Float irValue;

    private class GraphView extends View implements SensorEventListener{

        private Bitmap  mBitmap;
        private Paint   mPaint = new Paint();
        private Canvas  mCanvas = new Canvas();
        private float   mLastValue;
        private int     mColors[] = new int[3*2];
        private float   mLastX;
        private float   mScale;
        private float   mYOffset;
        private float   mMaxX;
        private float   mSpeed = 0.5f;
        private float   mWidth;
        private float   mHeight;
        private float   defaultValue;

        public GraphView(Context context) {
            super(context);
            mColors[0] = Color.argb(192, 255, 64, 64);
            mColors[1] = Color.argb(192, 64, 128, 64);
            mColors[2] = Color.argb(192, 64, 64, 255);
            mColors[3] = Color.argb(192, 64, 255, 255);
            mColors[4] = Color.argb(192, 128, 64, 128);
            mColors[5] = Color.argb(192, 255, 255, 64);

            mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
            mPaint.setStrokeWidth(3.0f);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
            mCanvas.setBitmap(mBitmap);
            mCanvas.drawColor(0xFF000000);
            mYOffset = h * 0.5f;
            defaultValue = irValue;
            mScale = h * 0.5f * (1.0f/100.0f);
            mWidth = w;
            mHeight = h;
            if (mWidth < mHeight) {
                mMaxX = w;
            } else {
                mMaxX = w-50;
            }
            mLastX = mMaxX;
            super.onSizeChanged(w, h, oldw, oldh);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            synchronized (this) {
                if (mBitmap != null) {
                    final Paint paint = mPaint;
                    if (mLastX >= mMaxX) {
                        mLastX = 0;
                        defaultValue = irValue;
                        Toast toast = Toast.makeText(getBaseContext(), String.valueOf(irValue), Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.RIGHT, 0, 0);
                        toast.show();
                        final Canvas cavas = mCanvas;
                        paint.setColor(0xFFAAAAAA);
                        cavas.drawColor(0xFF000000);
                    }
                    canvas.drawBitmap(mBitmap, 0, 0, null);
                }
            }
        }

        public void updateView(float value) {
            synchronized (this) {
                if (mBitmap != null) {
                    final Canvas canvas = mCanvas;
                    final Paint paint = mPaint;
                    float newX = mLastX + mSpeed;
                    final float v = mYOffset + (value - defaultValue) * mScale;
                    paint.setColor(mColors[0]);
                    canvas.drawLine(mLastX, mLastValue, newX, v, paint);
                    mLastValue = v;
                    mLastX += mSpeed;
                    Log.v("ir-data", String.valueOf(v));
                    invalidate();
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            // TODO Auto-generated method stub
            updateView(irValue);
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);        
        irSensorLogger = new IRSensorLogger();
        mGraphView = new GraphView(this);
        setContentView(mGraphView);
        
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        accSensor = (Sensor)sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);
        irValue = 0.0f;
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(mGraphView, accSensor, SensorManager.SENSOR_DELAY_FASTEST);
        
        mThread = new Thread(){
            @Override
            public void run(){
                while (isRunning) {
                    try {
                        String logData = irSensorLogger.getIRSensorData();
                        irValue = Float.valueOf(logData);
                    } catch (Exception e) {
                        Log.v("IRSensorLogger", "stopped");
                    }
                }
            }
        };
        mThread.start();
        isRunning = true;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onPause() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        sensorManager.unregisterListener(mGraphView);
        isRunning = false;
        mThread.interrupt();
        super.onPause();
    }
}
