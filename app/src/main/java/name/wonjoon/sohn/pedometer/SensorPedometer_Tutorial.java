/*
 *  Copyright Based: 2009 Levente Bagi (Pedometer)
 *             Modified the follows by Eric Won Joon Sohn, 2015. (SensorPedometer)
 *
 *  Sensor + Pedometer - Android App
 *	Features: realtime display of sensor data. 
 *			  WIFI connectivity, Streaming sensor data. (e.g. step events)
 * 			  log file generated.
 * 			  step detection enhanced by filtering.
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

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

//import name.wonjoon.sohn.pedometer.AdvancedMultipleSeriesGraph;
//import com.jjoe64.graphviewdemos.MainActivity;
//import name.wonjoon.sohn.pedometer.RealtimeGraph;
// Bluetooth library

public class SensorPedometer_Tutorial extends Activity implements SensorEventListener  {
//public class SensorPedometer extends Activity {
	private static final String TAG = "Pedometer";
	private static final boolean D = true;
	private static boolean showgraph = false;  // to display real time dynamic graphs of sensor data. If On, it will slow down the data logging.
    private SharedPreferences mSettings;
    private PedometerSettings mPedometerSettings;
    private Utils mUtils;

    private TextView mStepValueView;
    private TextView mPaceValueView;
    private TextView mDistanceValueView;
    private TextView mSpeedValueView;
//    private TextView mCaloriesValueView;
	private TextView mThresholdView;

    private float x=0,y=0,z=0;  // acc


//	// Well known SPP UUID
//    private static final UUID MY_UUID =
//		UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // R samsung or motorola or nexus 5 unlocked.
	private static final UUID MY_UUID=
			UUID.fromString("5db4284e-01ff-435c-9b80-2df4e78bd214"); //L, motorola or nexus 5.


	// INSECURE "8ce255c0-200a-11e0-ac64-0800200c9a66"
	// SECURE "fa87c0d0-afac-11de-8a39-0800200c9a66"
	// SPP "0001101-0000-1000-8000-00805F9B34FB"

    //** Insert your server's bt address
    //private static String address = "64:76:BA:AA:C6:BD";  // Eric's Mac book air
	private static String address ="4C:16:F1:B4:1E:49"; // Eric's ZTE Warp Elite
//	private static String address ="14:3E:BF:D4:1B:B9"; // Eric's ZTE R. 4.4
// 	 private static String address ="64:BC:0C:A0:A1:1E"; // Eric's LG Gpad
//	private static String address ="F8:8F:CA:11:1B:C7"; //Google glass (not ...:C6 as in myglass page?)
//	private static String address ="00:1A:7D:DA:71:08"; //odroid
//	private static String address ="34:C3:D2:63:B0:94"; //MXV


//	private static String address ="7E:8F:46:66:22:83"; // AAXA LED projector

    //** WIFI member fields
    public int SERVERPORT = 9999; //R=15004, L = 15002
    //private Button connectPhones;
//	private String serverIpAddress = "10.10.10.4";ƒ
    private String serverIpAddress = "192.168.1.105";
    private boolean connected = false;   // wifi connectivity
    private boolean BTconnected = false;   // BT connectivity
    private boolean writing = false;   // writing to file
    boolean acc_disp = false;
	DatagramSocket ds = null;
//    EditText port;
    EditText ipAdr;
    TextView mStatus;
    ToggleButton mlogWifiToggleButton1;
    ToggleButton mDatalog;
    PrintWriter out;

    // acceleration and step detection related.
    //private StepDetector mStepDector;
//    public  double acc_net = 0;
    private int step=0;

    TextView mDesiredPaceView;
    private int mStepValue;
    private int mPaceValue;
    private float mDistanceValue;
    private float mSpeedValue;
//    private int mCaloriesValue;
	private float mThresholdValue;
    private float mDesiredPaceOrSpeed;
    private int mMaintain;
    private boolean mIsMetric;
    private float mMaintainInc;
    private boolean mQuitting = false; // Set when user selected Quit from menu, can be used by onPause, onStop, onDestroy
    private float   mLastValues[] = new float[3*2];  // running filter
    private float   mLastValues_hr[] = new float[3*2]; // running filter
    private float   mLastValues_fwa[] = new float[3*2]; // running filter

//    /*write to file*/
    private String logdata;
    private String read_str = "";
   // String fileName = "acc.txt"; //new SimpleDateFormat("yyyyMMddhhmm.txt'").format(new Date()); // time stamp in the file name
    String fileName;
    private String filepath;

     private BufferedWriter mBufferedWriter;
    private BufferedReader mBufferedReader;



    // for real time graph
	private final Handler mHandler2 = new Handler();  // check if commenting out this produce trouble
	private Runnable mTimer00;
	private Runnable mTimer0;
	private Runnable mTimer1;
	private Runnable mTimer2;
	private Runnable mTimer3;
	private Runnable r;
	private GraphView graphView00;
	private GraphView graphView0;
	private GraphView graphView1;
	private GraphView graphView2;
	private GraphView graphView3;
	private GraphViewSeries exampleStepSeries0;
	private GraphViewSeries exampleSeries0;
	private GraphViewSeries exampleSeries1;
	private GraphViewSeries exampleSeries2;
	private GraphViewSeries exampleSeries3;
	private double sensorStep = 235;  // mOffset  - 5
	private double sensorXYZ = 0;  // calculated weight xyz acc mean
	private double sensorXYZ_HR = 0;  // Hanning recursive filter
	private double sensorXYZ_fwa = 0;  //five point weighted average filter
	private double sensorX = 0;
	private double sensorY = 0;
	private double sensorZ = 0;
	private List<GraphViewData> seriesStep;
	private List<GraphViewData> seriesXYZ;
	private List<GraphViewData> seriesXYZ_HR;  //Hanning recursive filter
	private List<GraphViewData> seriesX;
	private List<GraphViewData> seriesY;
	private List<GraphViewData> seriesZ;
	int dataCount = 1;

	private float rawacc[] = new float[3]; // compensate gravity

	//the Sensor Manager
	private SensorManager sManager;
	private Sensor sensor;

	private int h;
	private float   mScale[] = new float[2];
    private float   mYOffset;

    DecimalFormat df = new DecimalFormat("##.####");

    // time tag
    static int ACCE_FILTER_DATA_MIN_TIME = 1; // 1ms delay. ~35hz is a limiting sampling rate now. why?
    long lastSaved = System.currentTimeMillis();


	// HR filter in test (to see the effect of Service)
	private float   mLastDirections[] = new float[3*2];
	private float   mLastExtremes[][] = { new float[3*2], new float[3*2] };
	private float   mLastDiff[] = new float[10];
	private int     mLastMatch = -1;
	private float   mAdaptiveDiff = 2; // initial threshold

    /**
     * True, when service is running.
     */
    private boolean mIsRunning;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		if (D)
			Log.e(TAG, "+++ ON CREATE +++");

		// Set up the window layout
		setContentView(R.layout.main);



        //time stamp related
        fileName = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
        filepath = "/mnt/sdcard/" + fileName + "_R.txt"; //TODO: add time tag to file name


        mStepValue = 0;
        mPaceValue = 0;


        mUtils = Utils.getInstance();
        acc_disp =false;

        // for real time graph
		if (showgraph) {
			seriesStep = new ArrayList<GraphViewData>();
			seriesXYZ_HR = new ArrayList<GraphViewData>();
			seriesX = new ArrayList<GraphViewData>();
			seriesY = new ArrayList<GraphViewData>();
			seriesZ = new ArrayList<GraphViewData>();
		}





		//get a hook to the sensor service
        sManager = (SensorManager) getSystemService(SENSOR_SERVICE);
//        sensor = sManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		sensor = sManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        Log.i(TAG, "[onCreate] sensor registered ");



		// init example series data

        // step series
		if (showgraph) {
			exampleStepSeries0 = new GraphViewSeries(new GraphViewData[]{});
			graphView00 = new LineGraphView(
					this // context
					, "step" // heading
			);
			graphView00.addSeries(exampleStepSeries0); // data

			LinearLayout layout = (LinearLayout) findViewById(R.id.graph00);
			layout.addView(graphView00);

			Log.i(TAG, "[onCreate] graph00 registered ");


			// group ACC
			exampleSeries0 = new GraphViewSeries(new GraphViewData[]{});

			graphView0 = new LineGraphView(
					this // context
					, "Group ACC" // heading
			);


			graphView0.addSeries(exampleSeries0); // data

			layout = (LinearLayout) findViewById(R.id.graph0);
			layout.addView(graphView0);

			Log.i(TAG, "[onCreate] graph0 registered ");

			//---------
			exampleSeries1 = new GraphViewSeries(new GraphViewData[]{});

			graphView1 = new LineGraphView(
					this // context
					, "ACC-X" // heading
			);


			graphView1.addSeries(exampleSeries1); // data
			layout = (LinearLayout) findViewById(R.id.graph1);
			layout.addView(graphView1);

			Log.i(TAG, "[onCreate] graph1 registered ");

			// ----------
			exampleSeries2 = new GraphViewSeries(new GraphViewData[]{});

			graphView2 = new LineGraphView(
					this
					, "ACC-Y"
			);
			//((LineGraphView) graphView).setDrawBackground(true);

			graphView2.addSeries(exampleSeries2); // data
			layout = (LinearLayout) findViewById(R.id.graph2);
			layout.addView(graphView2);


			// init example series data
			exampleSeries3 = new GraphViewSeries(new GraphViewData[]{});

			graphView3 = new LineGraphView(
					this // context
					, "ACC-Z" // heading
			);

			graphView3.addSeries(exampleSeries3); // data
			layout = (LinearLayout) findViewById(R.id.graph3);
			layout.addView(graphView3);
		}


		// Net Acceleration adjustment
		h = 480; // TODO: remove this constant
        mYOffset = h * 0.5f;
        mScale[0] = - (h * 0.5f * (1.0f / (SensorManager.STANDARD_GRAVITY * 2)));
        mScale[1] = - (h * 0.5f * (1.0f / (SensorManager.MAGNETIC_FIELD_EARTH_MAX)));
        step = 0; //initial value






    }








	@Override
	public void onSensorChanged(SensorEvent event)
	{

//	    Sensor sensor = event.sensor;

	    if (sManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null) {
	    	if ((System.currentTimeMillis() - lastSaved) > ACCE_FILTER_DATA_MIN_TIME) {
	            lastSaved = System.currentTimeMillis();

				sensorX = event.values[0];
				sensorY = event.values[1];
				sensorZ = event.values[2];
				float vvSum = 0;

				float v = (float)sensorX;

				// **  Hanning recursive smoothing technique (filtering)
				float v_hr = (float) 0.25*(v + 2* mLastValues_hr[0]  + mLastValues_hr[1]);
//                //mLastValues[2] = mLastValues[1]; // shift left
                mLastValues_hr[1] = mLastValues_hr[0]; // shift left
                mLastValues_hr[0] = v_hr; // shift left


                //** five-point weighted average method (Eric defined ver)
                float v_fwa = (float) 0.25*(v +  mLastValues_fwa[0]  + mLastValues_fwa[1] + mLastValues_fwa[2]);
                mLastValues_fwa[2] = mLastValues_fwa[1]; // shift left
                mLastValues_fwa[1] = mLastValues_fwa[0]; // shift left
                mLastValues_fwa[0] = v; // shift left


                sensorXYZ = v;
                sensorXYZ_HR = v_hr;
                sensorXYZ_fwa = v_fwa;
				//sensorStep = mStepValue;

				if (showgraph) {
					seriesStep.add(new GraphViewData(dataCount, mStepValue));
					seriesXYZ_HR.add(new GraphViewData(dataCount, sensorXYZ_HR));
					seriesX.add(new GraphViewData(dataCount, sensorX));
					seriesY.add(new GraphViewData(dataCount, sensorY));
					seriesZ.add(new GraphViewData(dataCount, sensorZ));
				}

				dataCount++;

				// to write , % operator might slight slow reading rate??
				// time, step, sensorXYZ_HR, sensorXYZ_fwa, sensorXYZ, sensorX, sensorY, sensorZ
				logdata= String.valueOf(df.format(lastSaved%10000000)) + "," + String.valueOf(df.format(mStepValue))
						+ "," + String.valueOf(df.format(sensorXYZ_HR)) + "," + String.valueOf(df.format(sensorXYZ_fwa))
						+ "," + String.valueOf(df.format(sensorXYZ)) + "," + String.valueOf(df.format(sensorX)) + ","
						+ String.valueOf(df.format(sensorY)) + "," + String.valueOf(df.format(sensorZ))
						+ "," + String.valueOf(df.format(step));


		/*		Context context = getApplicationContext();
				float number = (float)Math.round(sensorX * 1000) / 1000;
				//string formattedNumber = Float.toString(number);
				CharSequence text = Float.toString(number);
				int duration = Toast.LENGTH_SHORT;
				Toast toast = Toast.makeText(context, text, duration);
				toast.show();
		*/
				int dataSize = 200;
				if (showgraph) {
					if (seriesX.size() > dataSize) {
						seriesStep.remove(0);
						seriesXYZ_HR.remove(0);
						seriesX.remove(0);
						seriesY.remove(0);
						seriesZ.remove(0);
						graphView00.setViewPort(dataCount - dataSize, dataSize);
						graphView0.setViewPort(dataCount - dataSize, dataSize);
						graphView1.setViewPort(dataCount - dataSize, dataSize);
						graphView2.setViewPort(dataCount - dataSize, dataSize);
						graphView3.setViewPort(dataCount - dataSize, dataSize);
					}
				}
		    }
	    }
	    else {
			// fail! we dont have an accelerometer!
		    	Log.d("MYAPP", "no acc");
		    	Toast.makeText(this, "No accelerometer", Toast.LENGTH_LONG).show();


		    /*write to file */
		   // WriteFile(filepath,acc);
		    //mSensorLog.logSensorEvent(event);
	    }
	}



	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1)
	{
		//Do nothing.
	}





    @Override
    protected void onStart() {
        Log.i(TAG, "[ACTIVITY] onStart");
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
		if (D)
			Log.e(TAG, "+ ON RESUME +");

		mSettings = PreferenceManager.getDefaultSharedPreferences(this);
		mPedometerSettings = new PedometerSettings(mSettings);

		mUtils.setSpeak(mSettings.getBoolean("speak", false));


		mStepValueView     = (TextView) findViewById(R.id.step_value);
		mPaceValueView     = (TextView) findViewById(R.id.pace_value);
		mDistanceValueView = (TextView) findViewById(R.id.distance_value);

		// Wifi  - go to onCreate?
		mStatus  = (TextView) findViewById(R.id.status);
		mlogWifiToggleButton1 = (ToggleButton) findViewById(R.id.logWifiToggleButton1);
		ipAdr=(EditText)findViewById(R.id.ipadr);
		ipAdr.setText(serverIpAddress);



		mDatalog = (ToggleButton) findViewById(R.id.datalog);
		mSpeedValueView    = (TextView) findViewById(R.id.speed_value);
		//mCaloriesValueView = (TextView) findViewById(R.id.calories_value);
		mThresholdView = (TextView)  findViewById(R.id.threshold_value);

				mDesiredPaceView   = (TextView) findViewById(R.id.desired_pace_value);

		mIsMetric = mPedometerSettings.isMetric();
		((TextView) findViewById(R.id.distance_units)).setText(getString(
				mIsMetric
						? R.string.kilometers
						: R.string.miles
		));
//        ((TextView) findViewById(R.id.speed_units)).setText(getString(
//                mIsMetric
//                ? R.string.kilometers_per_hour
//                : R.string.miles_per_hour
//        ));

		mMaintain = mPedometerSettings.getMaintainOption();
		((LinearLayout) this.findViewById(R.id.desired_pace_control)).setVisibility(
				mMaintain != PedometerSettings.M_NONE
						? View.VISIBLE
						: View.GONE
		);
		if (mMaintain == PedometerSettings.M_PACE) {
			mMaintainInc = 5f;
			mDesiredPaceOrSpeed = (float)mPedometerSettings.getDesiredPace();
		}
		else
		if (mMaintain == PedometerSettings.M_SPEED) {
			mDesiredPaceOrSpeed = mPedometerSettings.getDesiredSpeed();
			mMaintainInc = 0.1f;
		}







		// Read from preferences if the service was running on the last onPause
        mIsRunning = mPedometerSettings.isServiceRunning();

        // Start the service if this is considered to be an application start (last onPause was long ago)
        if (!mIsRunning && mPedometerSettings.isNewStart()) {
            startStepService();
            bindStepService();
        }
        else if (mIsRunning) {
            bindStepService();
        }

        mPedometerSettings.clearServiceRunning();//?

		//for real time graph
		sManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST);

		// WIFI streaming & logging of ACC, Toggle Button

		mlogWifiToggleButton1.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// TODO
				if (!connected) {
					if (!serverIpAddress.equals("")) {
						//text.setText("onclick if");  exception occurs
						mlogWifiToggleButton1.setText("Streaming");
						Thread cThread = new Thread(new ClientThread());
						cThread.start();
						// to call

						mStatus.setText("Streaming");
						Log.d("ACTIVITY", "wifi clicked");
						connected = true;
					}
				} else {
					mlogWifiToggleButton1.setText("stop Streaming");
					mStatus.setText("No Streaming");
					//connectPhones.setText("don't expect here");
					connected = false;
					acc_disp = false;
					Log.d("ACTIVITY", "wifi unclicked");
				}
			}
		});


		Button button1 = (Button) findViewById(R.id.button_desired_pace_lower);
		button1.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mDesiredPaceOrSpeed -= mMaintainInc;
				mDesiredPaceOrSpeed = Math.round(mDesiredPaceOrSpeed * 10) / 10f;
				displayDesiredPaceOrSpeed();
				setDesiredPaceOrSpeed(mDesiredPaceOrSpeed);
			}
		});
		Button button2 = (Button) findViewById(R.id.button_desired_pace_raise);
		button2.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mDesiredPaceOrSpeed += mMaintainInc;
				mDesiredPaceOrSpeed = Math.round(mDesiredPaceOrSpeed * 10) / 10f;
				displayDesiredPaceOrSpeed();
				setDesiredPaceOrSpeed(mDesiredPaceOrSpeed);
			}
		});
		if (mMaintain != PedometerSettings.M_NONE) {
			((TextView) findViewById(R.id.desired_pace_label)).setText(
					mMaintain == PedometerSettings.M_PACE
							? R.string.desired_pace
							: R.string.desired_speed
			);
		}

		displayDesiredPaceOrSpeed();

		//Advanced multiple series graph
		((ToggleButton) findViewById(R.id.datalog)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
//				 if (!writing) {
//					//startGraphActivity(AdvancedMultipleSeriesGraph.class);
//					 writing = true;
//					Thread accThread1 = new Thread(new AccThread());
//	                accThread1.start();
//	                }
//				 else{
//						//                	 accThread1.kill();
//		        	 mDatalog.setText("Writing stopped");
//		        	 writing = false;
//				 }
				if (mDatalog.isChecked()) {  // On when tobble button is pushed.
					//startGraphActivity(AdvancedMultipleSeriesGraph.class);

					Thread accThread1 = new Thread(new AccThread());
					accThread1.start();

				} else {
//					     accThread1.kill();
					try {
						mBufferedWriter.close(); //close buffer
						Log.d("ACTIVITY", "Close a File.");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					mDatalog.setText("Writing stopped");

					//create new file
					fileName = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
					filepath = "/mnt/sdcard/" + fileName + "_R.txt"; //TODO: add time tag to file name

				}
			}
		});




		// select to display real time sensor data
		if (showgraph) {
			mTimer00 = new Runnable() {
				@Override
				public void run() {
					GraphViewData[] gvd = new GraphViewData[seriesStep.size()];
					seriesStep.toArray(gvd);
					exampleStepSeries0.resetData(gvd);
					mHandler2.post(this); //, 100);
				}
			};
			mHandler2.postDelayed(mTimer00, 50);


			mTimer0 = new Runnable() {
				@Override
				public void run() {
					GraphViewData[] gvd = new GraphViewData[seriesXYZ_HR.size()];
					seriesXYZ_HR.toArray(gvd);
					exampleSeries0.resetData(gvd);
					mHandler2.post(this); //, 100);
				}
			};
			mHandler2.postDelayed(mTimer0, 50);

			mTimer1 = new Runnable() {
				@Override
				public void run() {
					GraphViewData[] gvd = new GraphViewData[seriesX.size()];
					seriesX.toArray(gvd);
					exampleSeries1.resetData(gvd);
					mHandler2.post(this); //, 100);
				}
			};
			mHandler2.postDelayed(mTimer1, 50);

			mTimer2 = new Runnable() {
				@Override
				public void run() {

					GraphViewData[] gvd = new GraphViewData[seriesY.size()];
					seriesY.toArray(gvd);
					exampleSeries2.resetData(gvd);

					mHandler2.post(this);
				}
			};
			mHandler2.postDelayed(mTimer2, 50);


			mTimer3 = new Runnable() {
				@Override
				public void run() {

					GraphViewData[] gvd = new GraphViewData[seriesZ.size()];
					seriesZ.toArray(gvd);
					exampleSeries3.resetData(gvd);

					mHandler2.post(this);
				}
			};
			mHandler2.postDelayed(mTimer3, 50);
		}


    }



    public class AccThread implements Runnable {

        @Override
        public void run () {

        	while(mDatalog.isChecked())
            {
                try
                {
                    WriteFile(filepath,logdata);
					Thread.sleep(5);  //to slow down
                }
                catch (Exception e)
                {
                	e.printStackTrace();
//                        msg1.what = MSG_ERROR;
//                        msg1.obj = e.getMessage();
                }

        	}

        }

    };




	// this class is for streaming to ipaddress
    public class ClientThread implements Runnable {
        Socket socket;
		DataOutputStream dos;
		InetAddress serverAddr;

        public void run() {
            try {
                acc_disp=true;
//                SERVERPORT = Integer.parseInt(PORT.getText().toString());
//                serverIpAddress=ipAdr.getText().toString();

                serverIpAddress=ipAdr.getText().toString();
				serverAddr = InetAddress.getByName(serverIpAddress);
                socket = new Socket(serverAddr, 15002); //15004: right, 15002: left
                connected = true;
                //** PrintStream : stream of bytes , PrintWriter: stream of characters, use PrintWriter since it is less platform dependent.
//                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true); // why slow???
//                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true); // all writers are buffered? Buffered is faster... // slower!
				socket.setTcpNoDelay(true); // doesn't help with my delay of sometime 2-10 seconds: 03/18.
				dos= new DataOutputStream(socket.getOutputStream());

//				input = new DataInputStream(user.getSocket().getInputStream());
//				output = new DataOutputStream(user.getSocket().getOutputStream());


                while (connected) {
//                    out.printf("%10.2f\n",  (float)mService.mStepDetector2.step);
//                    out.printf("%10.2f\n",  (float)mStepValue);  // calling var from service a good way?
					dos.writeInt(mStepValue);
       //             out.printf("%10.2f\n",  1.20f);
                    dos.flush(); //Must be used with buffers. Non-buffered writing byte-by-byte is slow.
                }
            }
            catch (Exception e) {
            	//throw new RuntimeException(e);  //falls here?
            	Log.e("MYAPP", "exception", e);
            }

            finally{
				acc_disp=false;
				connected=false;
//				mStatus.setText("socket closed");
				//out.close();

				// close socket
				if (socket != null) {
					try {
						Log.i(TAG, "closing the socket");
						socket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				// close output stream
				if (dos != null) {
					try {
						dos.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

            }

        }
    };



	private void displayDesiredPaceOrSpeed() {
        if (mMaintain == PedometerSettings.M_PACE) {
            mDesiredPaceView.setText("" + (int)mDesiredPaceOrSpeed);
        }
        else {
            mDesiredPaceView.setText("" + mDesiredPaceOrSpeed);
        }
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "[ACTIVITY] onPause");
        // for real time graph
		mHandler2.removeCallbacks(mTimer1);
		mHandler2.removeCallbacks(mTimer2);

        if (mIsRunning) {
            unbindStepService();
        }
        if (mQuitting) {
            mPedometerSettings.saveServiceRunningWithNullTimestamp(mIsRunning);
        }
        else {
            mPedometerSettings.saveServiceRunningWithTimestamp(mIsRunning);
        }



        super.onPause();
        savePaceSetting();


    }

    @Override
    protected void onStop() {
        Log.i(TAG, "[ACTIVITY] onStop");
        // for real time graph

		sManager.unregisterListener(this);
        super.onStop();
    }


    @Override
    protected void onDestroy() {
        Log.i(TAG, "[ACTIVITY] onDestroy");
        super.onDestroy();
    }

    @Override
    protected void onRestart() {
        Log.i(TAG, "[ACTIVITY] onRestart");
        super.onRestart();// mistake?writeOn
    }

    private void setDesiredPaceOrSpeed(float desiredPaceOrSpeed) {
        if (mService != null) {
            if (mMaintain == PedometerSettings.M_PACE) {
                mService.setDesiredPace((int)desiredPaceOrSpeed);
            }
            else
            if (mMaintain == PedometerSettings.M_SPEED) {
                mService.setDesiredSpeed(desiredPaceOrSpeed);
            }
        }
    }

    private void savePaceSetting() {
        mPedometerSettings.savePaceOrSpeedSetting(mMaintain, mDesiredPaceOrSpeed);
    }

    private StepService mService;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = ((StepService.StepBinder)service).getService();  //return StepService.this

            mService.registerCallback(mCallback);
            mService.reloadSettings();

        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };


    private void startStepService() {
        if (! mIsRunning) {
            Log.i(TAG, "[SERVICE] Start");
            mIsRunning = true;
            startService(new Intent(SensorPedometer_Tutorial.this,
                    StepService.class));
        }
    }

    private void bindStepService() {
        Log.i(TAG, "[SERVICE] Bind");
        bindService(new Intent(SensorPedometer_Tutorial.this,
                StepService.class), mConnection, Context.BIND_AUTO_CREATE + Context.BIND_DEBUG_UNBIND);
    }

    private void unbindStepService() {
        Log.i(TAG, "[SERVICE] Unbind");
        unbindService(mConnection);
    }

    private void stopStepService() {
        Log.i(TAG, "[SERVICE] Stop");
        if (mService != null) {
            Log.i(TAG, "[SERVICE] stopService");
            stopService(new Intent(SensorPedometer_Tutorial.this,
                  StepService.class));
        }
        mIsRunning = false;
    }

    private void resetValues(boolean updateDisplay) {
        if (mService != null && mIsRunning) {
            mService.resetValues();
        }
        else {
            mStepValueView.setText("0");
            mPaceValueView.setText("0");
            mDistanceValueView.setText("0");
            mSpeedValueView.setText("0");
            //mCaloriesValueView.setText("0");
			mThresholdView.setText("0");
            SharedPreferences state = getSharedPreferences("state", 0);
            SharedPreferences.Editor stateEditor = state.edit();
            if (updateDisplay) {
                stateEditor.putInt("steps", 0);
                stateEditor.putInt("pace", 0);
                stateEditor.putFloat("distance", 0);
                stateEditor.putFloat("speed", 0);
//                stateEditor.putFloat("calories", 0);
				stateEditor.putFloat("threshold", 0);
                stateEditor.commit();
            }
        }
    }

    private static final int MENU_SETTINGS = 8;
    private static final int MENU_QUIT     = 9;

    private static final int MENU_PAUSE = 1;
    private static final int MENU_RESUME = 2;
    private static final int MENU_RESET = 3;

    /* Creates the menu items */
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        if (mIsRunning) {
            menu.add(0, MENU_PAUSE, 0, R.string.pause)
            .setIcon(android.R.drawable.ic_media_pause)
            .setShortcut('1', 'p');
        }
        else {
            menu.add(0, MENU_RESUME, 0, R.string.resume)
            .setIcon(android.R.drawable.ic_media_play)
            .setShortcut('1', 'p');
        }
        menu.add(0, MENU_RESET, 0, R.string.reset)
        .setIcon(android.R.drawable.ic_menu_close_clear_cancel)
        .setShortcut('2', 'r');
        menu.add(0, MENU_SETTINGS, 0, R.string.settings)
        .setIcon(android.R.drawable.ic_menu_preferences)
        .setShortcut('8', 's')
        .setIntent(new Intent(this, Settings.class));
        menu.add(0, MENU_QUIT, 0, R.string.quit)
        .setIcon(android.R.drawable.ic_lock_power_off)
        .setShortcut('9', 'q');
        return true;
    }

    /* Handles item selections */
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_PAUSE:
                unbindStepService();
                stopStepService();
                return true;
            case MENU_RESUME:
                startStepService();
                bindStepService();
                return true;
            case MENU_RESET:
                resetValues(true);
                return true;
            case MENU_QUIT:
                resetValues(false);
                unbindStepService();
                stopStepService();
                mQuitting = true;
                finish();
                return true;
        }
        return false;
    }

    // TODO: unite all into 1 type of message
    private StepService.ICallback mCallback = new StepService.ICallback() {
        public void stepsChanged(int value) {
            mHandler.sendMessage(mHandler.obtainMessage(STEPS_MSG, value, 0));
        }
        public void paceChanged(int value) {
            mHandler.sendMessage(mHandler.obtainMessage(PACE_MSG, value, 0));
        }
        public void distanceChanged(float value) {
            mHandler.sendMessage(mHandler.obtainMessage(DISTANCE_MSG, (int)(value*1000), 0));
        }
        public void speedChanged(float value) {
            mHandler.sendMessage(mHandler.obtainMessage(SPEED_MSG, (int)(value*1000), 0));
        }
//        public void caloriesChanged(float value) {
//            mHandler.sendMessage(mHandler.obtainMessage(CALORIES_MSG, (int)(value), 0));
//        }
		public void thresholdChanged(float value) {
			mHandler.sendMessage(mHandler.obtainMessage(THRESHOLD_MSG, (int)(value), 0));
		}
    };

    private static final int STEPS_MSG = 1;
    private static final int PACE_MSG = 2;
    private static final int DISTANCE_MSG = 3;
    private static final int SPEED_MSG = 4;
	private static final int THRESHOLD_MSG = 5;
//    private static final int CALORIES_MSG = 5;

    private Handler mHandler = new Handler() {
        @Override public void handleMessage(Message msg) {
            switch (msg.what) {
                case STEPS_MSG:
                    mStepValue = (int)msg.arg1;
                    mStepValueView.setText("" + mStepValue);
                    break;
                case PACE_MSG:
                    mPaceValue = msg.arg1;
                    if (mPaceValue <= 0) {
                        mPaceValueView.setText("0");
                    }
                    else {
                        mPaceValueView.setText("" + (int)mPaceValue);
                    }
                    break;
                case DISTANCE_MSG:
                    mDistanceValue = ((int)msg.arg1)/1000f;
                    if (mDistanceValue <= 0) {
                        mDistanceValueView.setText("0");
                    }
                    else {
                        mDistanceValueView.setText(
                                ("" + (mDistanceValue + 0.000001f)).substring(0, 5)
                        );
                    }
                    break;
                case SPEED_MSG:
                    mSpeedValue = ((int)msg.arg1)/1000f;
                    if (mSpeedValue <= 0) {
                        mSpeedValueView.setText("0");
                    }
                    else {
                        mSpeedValueView.setText(
                                ("" + (mSpeedValue + 0.000001f)).substring(0, 4)
                        );
                    }
                    break;
				case THRESHOLD_MSG:
					mThresholdValue = msg.arg1;
					if (mThresholdValue <= 0) {
                        mThresholdView.setText("0");
                    }
                    else {
//						mThresholdView.setText("" + mThresholdValue);
//						Log.d(TAG, "threshold: "+ mThresholdValue);
//
//
                        mThresholdView.setText(
								("" + (mThresholdValue + 0.000001f)).substring(0, 4)
						); // float
                    }
					break;
//                case CALORIES_MSG:
//                    mCaloriesValue = msg.arg1;
//                    if (mCaloriesValue <= 0) {
//                        mCaloriesValueView.setText("0");
//                    }
//                    else {
//                        mCaloriesValueView.setText("" + (int)mCaloriesValue);
//                    }
//                    break;
                default:
                    super.handleMessage(msg);
            }
        }

    };







    /*
    private SensorEventListener accelerationListener = new SensorEventListener() {

        @Override
        public void onAccuracyChanged(Sensor sensor, int acc) {

        }
        @Override
        public void onSensorChanged(SensorEvent event) {

            x = event.values[0];
            y = event.values[1];
            z = event.values[2];
            refreshDisplay();
        }
    };
    */


	/*
    private void refreshDisplay() {
    	//text.setText("refreshDisplay");
        if(acc_disp == true){
        	acc_net = mService.mStepDetector2.acc_net;
            String output = String.format("X:%3.2f m/s^2  |  Y:%3.2f m/s^2  |   Z:%3.2f m/s^2", acc_net, acc_net, acc_net);
            mStatus.setText(output);
        }
    }
    */



	// Below here starts write to file.

	public void CreateFile(String path)
	{
	    File f = new File(path);
	    try {
	        Log.d("ACTIVITY", "Create a File.");
	        f.createNewFile();
	    } catch (IOException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
	    }
	}


	public void WriteFile(String filepath, String str)
	{
	    mBufferedWriter = null;

	    if (!FileIsExist(filepath))
	        CreateFile(filepath);

	    try
	    {
//	    	if (!mDatalog.isChecked()) {
//	    		Log.d("ACTIVITY", "Close file");
//	    		mBufferedWriter.close();
//	    	}else {

	        mBufferedWriter = new BufferedWriter(new FileWriter(filepath, true)); // Writer is to characters when Outputstream is to bytes.
	        mBufferedWriter.write(str);
	        mBufferedWriter.newLine();
	        mBufferedWriter.flush();


	       // mBufferedWriter.close(); //why close?
//	    	}
	    }
	    catch (IOException e)
	    {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
	    }
	}


	public boolean FileIsExist(String filepath)
	{
	    File f = new File(filepath);

	    if (! f.exists())
	    {
	        Log.e("ACTIVITY", "File does not exist.");
	        return false;
	    }
	    else
	        return true;
	}







}

