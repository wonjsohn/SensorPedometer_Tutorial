/*
 *  Pedometer - Android App
 *  Copyright (C) 2009 Levente Bagi
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package name.wonjoon.sohn.pedometer;

import java.util.ArrayList;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.widget.Toast;


/**
 * Detects steps and notifies all listeners (that implement StepListener).
 * @author Levente Bagi
 * @todo REFACTOR: SensorListener is deprecated
 *
 * @author2 Won Joon Sohn, 2015
 * @modification acceleration in xyz quantified in different way: sqrt(x2 + y2 + z2) 
 * 				various filters e.g. Hanning recursive filter added.
 * 			in StepDetector3,  modification attempts to prevent double count per leg .
 *
 */
public class StepDetector3 implements SensorEventListener
{
    private SensorManager sensorManager;

    private final static String TAG = "StepDetector";
    private float   mLimit = 10;
    private float   mLastValues[] = new float[3*2];
    private float   mScale[] = new float[2];
    private float   mYOffset;

    private float   mLastDirections[] = new float[3*2];
    private float   mLastExtremes[][] = { new float[3*2], new float[3*2] };
    private float   mLastDiff[] = new float[3*2];
    private int     mLastMatch = -1;
    long mLastTimeStamp = System.currentTimeMillis();


    public double acc_net;  // acc scaled in x, y, z.
    public int step;  // step detection information 

    private ArrayList<StepListener> mStepListeners = new ArrayList<StepListener>();

    private float mag_compensation; //magnetic field compensation
    private float rawacc[] = new float[3];

    public StepDetector3() {
        int h = 480; // TODO: remove this constant
        mYOffset = h * 0.5f;
        mScale[0] = - (h * 0.5f * (1.0f / (SensorManager.STANDARD_GRAVITY * 2)));
        mScale[1] = - (h * 0.5f * (1.0f / (SensorManager.MAGNETIC_FIELD_EARTH_MAX)));
        step = 0; //initial value
        //Log.d("Sohn", "Mag field max =" + SensorManager.MAGNETIC_FIELD_EARTH_MAX ); //60
//        mag_compensation = 1.0f; //default.
    }

    public void setSensitivity(float sensitivity) {
        //mLimit = sensitivity; // 1.97  2.96  4.44  6.66  10.00  15.00  22.50  33.75  50.62
        //exthigh vhigh high  higher med   lower  low    vlow  extlow
        float scaleFactor = 2;
        mLimit = sensitivity / scaleFactor;  // tune the factor
    }

    public void addStepListener(StepListener sl) {
        mStepListeners.add(sl);
    }

    //public void onSensorChanged(int sensor, float[] values) {
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        synchronized (this) {
            if (sensor.getType() == Sensor.TYPE_ORIENTATION) {
//            } else if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
//                float Gyrox = event.values[0]; // Gyrox
//                float Gyroy = event.values[1]; // Gyroy
//                float Gyroz = event.values[2]; // Gyroz

            }else {
//                int mag = (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) ? 1 : 0;
//                if (mag == 1) {
//                   mag_compensation =  event.values[1]; // y-direction. upright ~ -40. flat with earth ~ 15 or -15.
//
//                }

                int j = (sensor.getType() == Sensor.TYPE_ACCELEROMETER) ? 1 : 0;

                if ( j == 1) {
                    double vvSum = 0;
//                    for (int i=0 ; i<3 ; i++) {
                    //final float v = mYOffset + event.values[i] * mScale[j]; // why not squared?  j not 0?
//                        if (i == 1) {// y-axis (vertical)? need to compensate gravity (approx)
//                            //Log.d(TAG, "mag compensation: " + Float.toString(mag_compensation));
//
//                            rawacc[i] = event.values[i] - 6.0f; //a really bad way to compensate for gravity. (empirical value to subtract to minimize gravity effect)
//                        }
//                        else {
//                            rawacc[i] = event.values[i];
//                        }
//                        final float vv = event.values[i] * event.values[i]; // square

                    float Ax = event.values[0]; // Ax
                    float Ay = event.values[1]; // Ay
                    float Az = event.values[2]; // Az

//
//                    final float vv = event.values[1]; // vertical
//                    vvSum += vv;

//                    }
                    int k = 0;
//                    float v = (float) (Math.sqrt(vvSum));


                    //**  Hanning recursive smoohting technique (filtering). This is better than other filters. -added by Eric Sohn
                    // y(t) = 1/4*[x(t) + 2y(t-1) + y(t-2)]
                    //float v_hr = (float) 0.25*(v + 2* mLastValues[0]  + mLastValues[1]); //mLastValues: previous filtered values
                    //mLastValues[2] = mLastValues[1]; // shift left


                    acc_net = Az;  // to access acc from outside

                    //** new algorithm 20160503
                    //** No peak detection by rate and magnitude of acc change && direction && adaptive threshold

                    float direction = ((Az > mLastValues[k]) ? 1 : ((Az < mLastValues[k]) ? -1 : 0));
                    float th_rate = -10;// sagittal magnitude threshold
                    float th_mag = -7;
//                    float th_rate =  10;// sagittal  rate threshold
//                    if (direction >= 0 ) { // rising phase
                    float diff = Az - mLastValues[5];  //  mag change / 6 time sample interval)

                    boolean isFastRisingEnough = diff < th_rate;
                    boolean isLargeEnough = Az < th_mag;
                    boolean safePeriod = ((System.currentTimeMillis()-mLastTimeStamp) > 100); //at least 100ms apart from last step


                    if (isFastRisingEnough && isLargeEnough && safePeriod) {  //
                        Log.i(TAG, "step");
                        step = step + 1;
                        for (StepListener stepListener : mStepListeners) { // for all stepListeners, onStep()?
                            stepListener.onStep();  // updates UI text values.
                        }
                        mLastTimeStamp = System.currentTimeMillis();
                    }



//                    float direction = ((v_hr > mLastValues[k]) ? 1 : ((v_hr < mLastValues[k]) ? -1 : 0));
//                    if (direction == - mLastDirections[k]) {
//                        // Direction changed
//                        int extType = (direction > 0 ? 0 : 1); // minumum or maximum?
//                        mLastExtremes[extType][k] = mLastValues[k];
//                        float diff = Math.abs(mLastExtremes[extType][k] - mLastExtremes[1 - extType][k]);
//
//                        if (diff > mLimit) {
//
//                            boolean isAlmostAsLargeAsPrevious = diff > (mLastDiff[k]*2/3);
//                            boolean isPreviousLargeEnough = mLastDiff[k] > (diff/3);
//                            boolean isNotContra = (mLastMatch != 1 - extType); // direction should change
////                            boolean isNotVibration = (System.currentTimeMillis()-mLastTimeStamp > 500); //at least 0.5s apart from last step
//
//                            if (isAlmostAsLargeAsPrevious && isPreviousLargeEnough && isNotContra ) {
//                                Log.i(TAG, "step");
//                                step = step +  1;
//                                for (StepListener stepListener : mStepListeners) { // for all stepListeners, onStep()?
//                                    stepListener.onStep();  // updates UI text values.
//                                }
//                                mLastMatch = extType;
//                                mLastTimeStamp = System.currentTimeMillis();
//                            }
//                            else {
//                                mLastMatch = -1;
//                            }
//                        }
//                        mLastDiff[k] = diff;
//                    }
                    mLastDirections[k] = direction;

                    mLastValues[1] = mLastValues[0]; // shift left
                    mLastValues[2] = mLastValues[1]; // shift left
                    mLastValues[3] = mLastValues[2]; // shift left
                    mLastValues[4] = mLastValues[3]; // shift left
                    mLastValues[5] = mLastValues[4]; // shift left
                    mLastValues[k] = Az;  // k = 0 here

                }
            }
        }
    }


    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub
    }

}