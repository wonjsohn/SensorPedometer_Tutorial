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
public class StepDetector_ZeroCrossing implements SensorEventListener
{
    private SensorManager sensorManager;

    private final static String TAG = "StepDetector";
    private float   mLimit = 10;
    private float   mLastValues[] = new float[20];
    private float   mScale[] = new float[2];
    private float   mYOffset;

    private float   mLastDirections[] = new float[3*2];
    private float   mLastExtremes[][] = { new float[3*2], new float[3*2] };
    private float   mLastDiff[] = new float[10];
    private int     mLastMatch = -1;
    public float   mAdaptiveDiff = 2.0f; // initial threshold
    public float    mPastAverage20 = 0.0f; // initial
    long mLastTimeStamp = System.currentTimeMillis();


    public double acc_net;  // acc scaled in x, y, z.
    public int step;  // step detection information

    private ArrayList<StepListener> mStepListeners = new ArrayList<StepListener>();

    private float mag_compensation; //magnetic field compensation
    private float rawacc[] = new float[3];

    public StepDetector_ZeroCrossing() {
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
        //   0.5, 1, 1.5, 2, 2.5, 3, 4, 5, 6
        //exthigh vhigh high  higher med   lower  low    vlow  extlow
//        float scaleFactor = 2;
        mLimit = -sensitivity;  // tune the factor
    }

    public void addStepListener(StepListener sl) {
        mStepListeners.add(sl);
    }

    //public void onSensorChanged(int sensor, float[] values) {
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        synchronized (this) {
            if (sensor.getType() == Sensor.TYPE_ORIENTATION) {
            } else if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                float Gyrox = event.values[0]; // Gyrox
                float Gyroy = event.values[1]; // Gyroy
                float Gyroz = event.values[2]; // Gyroz


                float v = Gyrox;
                int k = 0;

                //**  Hanning recursive smoohting technique (filtering). This is better than other filters. (IIR) -added by Eric Sohn
                //**  recursion coefficients and the filter's response  as Z-transform.

                // y(t) = 1/4*[x(t) + 2y(t-1) + y(t-2)]
                float v_hr = (float) 0.25*(v + 2* mLastValues[0]  + mLastValues[1]); //mLastValues: previous filtered values
                //mLastValues[2] = mLastValues[1]; // shift left


                float direction = ((v_hr > mLastValues[0]) ? 1 : ((v_hr < mLastValues[0]) ? -1 : 0));


//                // A. Direction change + sensitivity + minimum threshold
//                if (direction == - mLastDirections[0]) {
//                    // Direction changed
//                    int extType = (direction > 0 ? 0 : 1); // minumum or maximum?
//                    mLastExtremes[extType][0] = mLastValues[0];
//                    float diff = Math.abs(mLastExtremes[extType][0] - mLastExtremes[1 - extType][0]);
////                    float diff = mLastExtremes[extType][k] - mLastExtremes[1 - extType][k];  // notice it's not math.abs
//
//                    if (diff > mLimit) { // + minimum sensitivity requirement. (removes small noise fluctuation)
//                        if ((diff > mAdaptiveDiff * 0.6)) { // + adaptive sensitivity
//
//                            boolean isAlmostAsLargeAsPrevious = diff > (mLastDiff[0] * 2 / 3);
//                            boolean isPreviousLargeEnough = mLastDiff[0] > (diff / 3);
//                            boolean isNotContra = (mLastMatch != 1 - extType);
//                            boolean isMin = (extType == 1); // catch the forward swing after toe-off.
//
//                            if (isAlmostAsLargeAsPrevious && isPreviousLargeEnough && isNotContra && isMin) {
//                                Log.i(TAG, "step");
//                                step = step + 1;
//                                for (StepListener stepListener : mStepListeners) { // for all stepListeners, onStep()?
//                                    stepListener.onStep();  // updates UI text values.
//                                }
//                                // update threshold in threshold notifier -> activity
//
//                                mLastMatch = extType;
//                            } else {
//                                mLastMatch = -1;
//                            }
//                        }
//
//                        // adaptive threshold
//                        mLastDiff[9] = mLastDiff[8];
//                        mLastDiff[8] = mLastDiff[7];
//                        mLastDiff[7] = mLastDiff[6];
//                        mLastDiff[6] = mLastDiff[5];
//                        mLastDiff[5] = mLastDiff[4];
//                        mLastDiff[4] = mLastDiff[3];
//                        mLastDiff[3] = mLastDiff[2];
//                        mLastDiff[2] = mLastDiff[1];
//                        mLastDiff[1] = mLastDiff[0];
//                        mLastDiff[0] = diff;
//
//                        float mAdaptiveDiffSum = 0;
//                        for (int i = 0; i < 9; i++) {
//                            mAdaptiveDiffSum = mAdaptiveDiffSum + mLastDiff[i];
//                        }
//                        mAdaptiveDiff = mAdaptiveDiffSum * 0.1f;
////                        mAdaptiveDiff = diff;
//                    }
//
//
//                }
//                mLastDirections[0] = direction;
//
//                // for the smoothing hr filter
//                mLastValues[1] = mLastValues[0]; // shift left
//                mLastValues[0] = v_hr;  // k = 0 here


                //*** B. Zero crossing.  (in Jayalath et al 2013)
                if ( direction >0 & (v_hr > 0 & mLastValues[0] <= 0)) { // rising direction & zero-crossing time
                    if (mPastAverage20 < mLimit ) { // mlimit is threshold
                        mPastAverage20 = 0; // reset past integral to zero.
                        Log.i(TAG, "step");
                        step = step + 1;
                        for (StepListener stepListener : mStepListeners) { // for all stepListeners, onStep()?
                            stepListener.onStep();  // updates UI text values.
                        }
                        mAdaptiveDiff = mPastAverage20; // temporary output (reuse output variable)

                    }


                }
                mLastDirections[0] = direction;

//                float pastIntegral = 0;
//                for (int i = 0; i < 9; i++) {
//                    pastIntegral = pastIntegral + mLastValues[i];
//                }

                mPastAverage20 = mPastAverage20*0.95f + v_hr*0.05f; //update past integral mean of past 20 points

//                mPastIntegral_ten =pastIntegral;

                // for past-integral
//                mLastValues[9] = mLastValues[8]; // shift left
//                mLastValues[8] = mLastValues[7]; // shift left
//                mLastValues[7] = mLastValues[6]; // shift left
//                mLastValues[6] = mLastValues[5]; // shift left
//                mLastValues[5] = mLastValues[4]; // shift left
//                mLastValues[4] = mLastValues[3]; // shift left
//                mLastValues[3] = mLastValues[2]; // shift left
//                mLastValues[2] = mLastValues[1]; // shift left
                // for the smoothing hr filter
                mLastValues[1] = mLastValues[0]; // shift left
                mLastValues[0] = v_hr;  // k = 0 here






                // C. HS described by 4th order Daubechies mother Wavelet (as in Lim 2008)





            }else {
//                int mag = (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) ? 1 : 0;
//                if (mag == 1) {
//                   mag_compensation =  event.values[1]; // y-direction. upright ~ -40. flat with earth ~ 15 or -15.
//
//                }

                int j = (sensor.getType() == Sensor.TYPE_ACCELEROMETER) ? 1 : 0;

                if ( j == 1) {
//                    double vvSum = 0;
////                    for (int i=0 ; i<3 ; i++) {
//                    //final float v = mYOffset + event.values[i] * mScale[j]; // why not squared?  j not 0?
////                        if (i == 1) {// y-axis (vertical)? need to compensate gravity (approx)
////                            //Log.d(TAG, "mag compensation: " + Float.toString(mag_compensation));
////
////                            rawacc[i] = event.values[i] - 6.0f; //a really bad way to compensate for gravity. (empirical value to subtract to minimize gravity effect)
////                        }
////                        else {
////                            rawacc[i] = event.values[i];
////                        }
////                        final float vv = event.values[i] * event.values[i]; // square
//
//                    float Ax = event.values[0]; // Ax
//                    float Ay = event.values[1]; // Ay
//                    float Az = event.values[2]; // Az
//
////
////                    final float vv = event.values[1]; // vertical
////                    vvSum += vv;
//
////                    }
//                    int k = 0;
////                    float v = (float) (Math.sqrt(vvSum));
//
//
//                    //**  Hanning recursive smoohting technique (filtering). This is better than other filters. -added by Eric Sohn
//                    // y(t) = 1/4*[x(t) + 2y(t-1) + y(t-2)]
//                    //float v_hr = (float) 0.25*(v + 2* mLastValues[0]  + mLastValues[1]); //mLastValues: previous filtered values
//                    //mLastValues[2] = mLastValues[1]; // shift left
//
//
//                    acc_net = Az;  // to access acc from outside
//
//                    //** new algorithm 20160503
//                    //** No peak detection by rate and magnitude of acc change && direction && adaptive threshold
//
//                    float direction = ((Az > mLastValues[k]) ? 1 : ((Az < mLastValues[k]) ? -1 : 0));
//                    float th_rate = -10;// sagittal magnitude threshold
//                    float th_mag = -7;
////                    float th_rate =  10;// sagittal  rate threshold
////                    if (direction >= 0 ) { // rising phase
//                    float diff = Az - mLastValues[5];  //  mag change / 6 time sample interval)
//
//                    boolean isFastRisingEnough = diff < th_rate;
//                    boolean isLargeEnough = Az < th_mag;
//                    boolean safePeriod = ((System.currentTimeMillis()-mLastTimeStamp) > 100); //at least 100ms apart from last step
//
//
//                    if (isFastRisingEnough && isLargeEnough && safePeriod) {  //
//                        Log.i(TAG, "step");
//                        step = step + 1;
//                        for (StepListener stepListener : mStepListeners) { // for all stepListeners, onStep()?
//                            stepListener.onStep();  // updates UI text values.
//                        }
//                        mLastTimeStamp = System.currentTimeMillis();
//                    }
//
//
//
//                    mLastDirections[k] = direction;
//
//                    mLastValues[1] = mLastValues[0]; // shift left
//                    mLastValues[2] = mLastValues[1]; // shift left
//                    mLastValues[3] = mLastValues[2]; // shift left
//                    mLastValues[4] = mLastValues[3]; // shift left
//                    mLastValues[5] = mLastValues[4]; // shift left
//                    mLastValues[k] = Az;  // k = 0 here

                }
            }
        }
    }


    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub
    }

}