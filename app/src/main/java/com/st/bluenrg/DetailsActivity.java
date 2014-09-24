package com.st.bluenrg;

/*
 * Temperatura: -20 +105 C
 * -20_0 ther_0
 * 0_10 ther_25
 * 10_20 ther_33
 * 20_25 ther_50
 * 25_30 ther_66
 * 30_40 ther_75
 * 40_105 ther_100
 * 
 * Pressione: 260-1260 mbar
 * 260 - 400 baro_0
 * 400 - 540 baro_25
 * 540 - 680 baro_33
 * 680 - 820 baro_50
 * 820 - 960 baro_66
 * 960 - 1100 baro_75
 * 1100 - 1260 baro_100
 * 
 * Umidita�: 0-100%
*/

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.os.Bundle;
import android.os.IBinder;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class DetailsActivity extends FragmentActivity implements ActionBar.TabListener {
	//private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private static TextView temperatura, pressione, umidita,segnale;
    private static ImageView temperaturaImg,pressioneImg, umiditaImg;
    private static ProgressBar progressx,progressy,progressz;
    public static int x,y,z;
    private int [][] caratteristiche = new int [5][2];
    
    public static String mDeviceName;
    public static String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    
    private boolean foundAcceleration=false, foundTemperature=false;
     
    Intent gattServiceIntent;
    
    public static final int WRONG_FIRMWARE = 201;
    public static final int BOARD_DISCONNECTED = 202; 


    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
            	String currentTime = new SimpleDateFormat("HH:mm:ss").format(new Date());
            	DeviceScanActivity.log.add(new visualLog(currentTime,"Unable to initialize Bluetooth"));
            	System.out.println("Unable to initialize Bluetooth");
                finish();
            }

            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };


    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {

            	Log.d("ble", "disconnesso");
            	disconnesso();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {

                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {

            	String valoreRisposta =  intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
            	String [] temp = valoreRisposta.split(";");
            	int caso = Integer.parseInt(temp[0]);
            	switch (caso){
            	case 0:
            		foundAcceleration = true;
            		Log.d("stefano", "ACTION_DATA_AVAILABLE accelerometer");

            		x = Integer.parseInt(temp[1]);
            		y = Integer.parseInt(temp[2]);
            		z = Integer.parseInt(temp[3]);

            		progressx.setProgress((x+1024)/20);
            		progressy.setProgress((y+1024)/20);
            		progressz.setProgress((z+1024)/20);


            		break;
            	case 1:
            		foundTemperature = true;
            		temperatura.setText(temp[1]+"°C");
            		int te = (int) Float.parseFloat(temp[1]);
            		if (te<0) temperaturaImg.setImageDrawable(getResources().getDrawable(R.drawable.thermometer_0));
            		else if (te<10)temperaturaImg.setImageDrawable(getResources().getDrawable(R.drawable.thermometer_25));
            		else if (te<20)temperaturaImg.setImageDrawable(getResources().getDrawable(R.drawable.thermometer_33));
            		else if (te<25)temperaturaImg.setImageDrawable(getResources().getDrawable(R.drawable.thermometer_50));
            		else if (te<30)temperaturaImg.setImageDrawable(getResources().getDrawable(R.drawable.thermometer_66));
            		else if (te<40)temperaturaImg.setImageDrawable(getResources().getDrawable(R.drawable.thermometer_75));
            		else temperaturaImg.setImageDrawable(getResources().getDrawable(R.drawable.thermometer_100));
            		break;
            	case 2:
            		pressione.setText(temp[1]+" mbr");
            		int press =  (int) Float.parseFloat(temp[1]);
            		if (press<400) pressioneImg.setImageDrawable(getResources().getDrawable(R.drawable.barometer_0));
            		else if (press<540)pressioneImg.setImageDrawable(getResources().getDrawable(R.drawable.barometer_25));
            		else if (press<680)pressioneImg.setImageDrawable(getResources().getDrawable(R.drawable.barometer_33));
            		else if (press<820)pressioneImg.setImageDrawable(getResources().getDrawable(R.drawable.barometer_50));
            		else if (press<960)pressioneImg.setImageDrawable(getResources().getDrawable(R.drawable.barometer_66));
            		else if (press<1100)pressioneImg.setImageDrawable(getResources().getDrawable(R.drawable.barometer_75));
            		else pressioneImg.setImageDrawable(getResources().getDrawable(R.drawable.barometer_100));
            		break;
            	case 3:
            		float f = Float.parseFloat(temp[1]);
            		umidita.setText(f+" %");
            		umiditaImg.setImageAlpha((int)f);
            		break;
            	case 4:
            		Log.d("freefal", "caduta");

            		final ImageView imageWarning = (ImageView) findViewById(R.id.imageViewWarning);
            		AlphaAnimation animation1 = new AlphaAnimation(0.0f, 1.0f);
            	    animation1.setDuration(300);
            	    animation1.setStartOffset(0);

            	    final AlphaAnimation animation2 = new AlphaAnimation(1.0f, 0.0f);
            	    animation2.setDuration(300);
            	    animation2.setStartOffset(1000);
            	    animation2.setAnimationListener(new AnimationListener(){

            	        @Override
            	        public void onAnimationEnd(Animation arg0) {

            	            imageWarning.setVisibility(View.GONE);
            	        }

            	        @Override
            	        public void onAnimationRepeat(Animation arg0) {


            	        }

            	        @Override
            	        public void onAnimationStart(Animation arg0) {

            	        }

            	    });
            	    
            	    animation1.setAnimationListener(new AnimationListener(){

            	        @Override
            	        public void onAnimationEnd(Animation arg0) {
                    	    imageWarning.startAnimation(animation2);
            	        }

            	        @Override
            	        public void onAnimationRepeat(Animation arg0) {

            	        }

            	        @Override
            	        public void onAnimationStart(Animation arg0) {
            	            imageWarning.setVisibility(View.VISIBLE);
            	        }

            	    });

            	    imageWarning.startAnimation(animation1);
            		break;
            	case 5:

            		segnale.setText(temp[1]+"");
            		break;
            	}
            }
        }
    };
    
    public void disconnesso(){
    	mBluetoothLeService.close();

		setResult(BOARD_DISCONNECTED);
		finish();
    }
    
    
    public boolean richiediDati(int groupPosition, int childPosition){
    	if (mGattCharacteristics != null) {
            final BluetoothGattCharacteristic characteristic =
                    mGattCharacteristics.get(groupPosition).get(childPosition);
            final int charaProp = characteristic.getProperties();
            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {

                if (mNotifyCharacteristic != null) {
                    mBluetoothLeService.setCharacteristicNotification(
                            mNotifyCharacteristic, false);
                    mNotifyCharacteristic = null;
                }
                mBluetoothLeService.readCharacteristic(characteristic);
            }
            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                mNotifyCharacteristic = characteristic;
                mBluetoothLeService.setCharacteristicNotification(
                        characteristic, true);
            }
            return true;
        }
        return false;
    }


    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide fragments for each of the
     * three primary sections of the app. We use a {@link android.support.v4.app.FragmentPagerAdapter}
     * derivative, which will keep every loaded fragment in memory. If this becomes too memory
     * intensive, it may be best to switch to a {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    AppSectionsPagerAdapter mAppSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will display the three primary sections of the app, one at a
     * time.
     */
    ViewPager mViewPager;
	private Thread dataThread;
	private boolean isBound;
	public static LinearLayout pressureHumidityView;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
        
        DeviceScanActivity.accesso = true;

        mAppSectionsPagerAdapter = new AppSectionsPagerAdapter(getSupportFragmentManager());

        final ActionBar actionBar = getActionBar();

        actionBar.setHomeButtonEnabled(true);

        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mAppSectionsPagerAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });


        actionBar.addTab(
                actionBar.newTab()
                        .setText("Motion")
                        .setTabListener(this));
        actionBar.addTab(
                actionBar.newTab()
                        .setText("Environment")
                        .setTabListener(this));
        actionBar.addTab(
                actionBar.newTab()
                        .setText("RSSI")
                        .setTabListener(this));
        
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        String currentTime = new SimpleDateFormat("HH:mm:ss").format(new Date());
    	DeviceScanActivity.log.add(new visualLog(currentTime,"Connected! Device Name: "+mDeviceName));
    	DeviceScanActivity.log.add(new visualLog(currentTime,"Device Address: "+mDeviceAddress));
        System.out.println("deviceName: "+mDeviceName);
        System.out.println("deviceAddress: "+mDeviceAddress);

        gattServiceIntent = new Intent(this, BluetoothLeService.class);
        isBound = bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        
    }
    
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.details, menu);
		return true;
	}
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_log:
            	Intent intent = new Intent(this, LogActivity.class);
        	    startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            
            System.out.println("Connect request result=" + result);
        }
        
        if (dataThread==null) {
            dataThread = new Thread(new Runnable() {
            	@Override
            	public void run() {
            		try {
            			int j=0;
            			while (mGattCharacteristics == null || mGattCharacteristics.size()==0){
            				Thread.sleep(500);
            				System.out.println("thread__waiting_data");
            			}
            			BluetoothGattCharacteristic characteristic0 = mGattCharacteristics.get(caratteristiche[0][0]).get(caratteristiche[0][1]);		    	
            			mBluetoothLeService.setCharacteristicNotification(characteristic0, true);
            			Thread.sleep(150);
            			BluetoothGattCharacteristic characteristic4 = mGattCharacteristics.get(caratteristiche[4][0]).get(caratteristiche[4][1]);		    		    	
            			mBluetoothLeService.setCharacteristicNotification(characteristic4, true);
            			while(true) {
            				for (int i=1; i<=3; i++) {
    	        				BluetoothGattCharacteristic characteristic1 = mGattCharacteristics.get(caratteristiche[i][0]).get(caratteristiche[i][1]);		    	
    	      		    		mBluetoothLeService.readCharacteristic(characteristic1);
    	      		    		Thread.sleep(500);
    	      		    		j++;
    	      		    		Log.d("check", ""+j);
    	      		    		if (j==6 && (!foundAcceleration || !foundTemperature)) {
    	      		    			error();
    	      		    		}
            				}
            				Thread.sleep(500);
            				mBluetoothLeService.readRemoteRssi();
            			}
            		} catch (Exception e) {
            			e.getLocalizedMessage();
            		}
            	}
            });
            dataThread.start();
        }
        
        super.onResume();
    }
    
    private void error() {
    	setResult(WRONG_FIRMWARE);
 	   DetailsActivity.this.finish();

    }

    @Override
    protected void onPause() {

    	unregisterReceiver(mGattUpdateReceiver);
        super.onPause();        
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        chiudiServizio();
        if(dataThread != null){
            Thread moribund = dataThread;
            dataThread = null;
            moribund.interrupt();
          }
        mBluetoothLeService = null;
    }
    
    private void chiudiServizio () {
        if(mBluetoothLeService!=null)mBluetoothLeService.stopSelf();
        stopService(new Intent(this, BluetoothLeService.class));
        if (isBound) {
        	unbindService(mServiceConnection);
        	isBound = false;
        }
    }
    
    @Override
    public void onBackPressed() {
    	chiudiServizio();
        if(dataThread != null){
            Thread moribund = dataThread;
            dataThread = null;
            moribund.interrupt();
          }
        mBluetoothLeService = null;
        finish();
    	}


    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();


        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, unknownServiceString);
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();


            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(
                        LIST_NAME, unknownCharaString);
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        int i=0,j;
        for (ArrayList<BluetoothGattCharacteristic> service : mGattCharacteristics){
        	j=0;
        	for (BluetoothGattCharacteristic gatt : service){
        		UUID uid = gatt.getUuid();
        		if (BluetoothLeService.UUID_ACCELERATION.equals(uid)) {
        			String currentTime = new SimpleDateFormat("HH:mm:ss").format(new Date());
        	    	DeviceScanActivity.log.add(new visualLog(currentTime,"Found acceleration"));
        			caratteristiche[0][0]=i;
        			caratteristiche[0][1]=j;
        			Log.d("stefano", "UUID_ACCELERATION");
                } else if (BluetoothLeService.UUID_TEMPERATURE.equals(uid)) {
                	String currentTime = new SimpleDateFormat("HH:mm:ss").format(new Date());
        	    	DeviceScanActivity.log.add(new visualLog(currentTime,"Found temperature"));
                	caratteristiche[1][0]=i;
        			caratteristiche[1][1]=j;
                } else if (BluetoothLeService.UUID_PRESSURE.equals(uid)) {
                	if (pressureHumidityView.getVisibility()!=View.VISIBLE) {
                		pressureHumidityView.setVisibility(View.VISIBLE);
                	}
                	String currentTime = new SimpleDateFormat("HH:mm:ss").format(new Date());
        	    	DeviceScanActivity.log.add(new visualLog(currentTime,"Found pressure"));
                	caratteristiche[2][0]=i;
        			caratteristiche[2][1]=j;
                } else if (BluetoothLeService.UUID_HUMIDITY.equals(uid)) {
                	if (pressureHumidityView.getVisibility()!=View.VISIBLE) {
                		pressureHumidityView.setVisibility(View.VISIBLE);
                	}
                	String currentTime = new SimpleDateFormat("HH:mm:ss").format(new Date());
        	    	DeviceScanActivity.log.add(new visualLog(currentTime,"Found humidity"));
                	caratteristiche[3][0]=i;
        			caratteristiche[3][1]=j;
                } else if (BluetoothLeService.UUID_FREE_FALL.equals(uid)) {
                	String currentTime = new SimpleDateFormat("HH:mm:ss").format(new Date());
        	    	DeviceScanActivity.log.add(new visualLog(currentTime,"Found free fall detection"));
                	caratteristiche[4][0]=i;
        			caratteristiche[4][1]=j;
                } 
        		j++;
        	}
        	i++;
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
    
    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to one of the primary
     * sections of the app.
     */
    public static class AppSectionsPagerAdapter extends FragmentPagerAdapter {

        public AppSectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            switch (i) {
                case 0:
                    return new AccelerometroSectionFragment();
                case 1:
                    return new SensoriSectionFragment();
                default:
                	return new SegnaleSectionFragment();
            }
        }

        @Override
        public int getCount() {
            return 3;
        }


    }

    /**
     * A dummy fragment representing a section of the app, but that simply displays dummy text.
     */
    public static class AccelerometroSectionFragment extends Fragment {

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.acelerometer_fragment, container, false);
            
            LinearLayout l = (LinearLayout)rootView.findViewById(R.id.layoutSurface);

            GLSurfaceView view = new GLSurfaceView(rootView.getContext());
            view.setRenderer(new OpenGLRenderer(rootView.getContext()));
            	
            l.addView(view);
            progressx = (ProgressBar) rootView.findViewById(R.id.progressBarX);
            progressy = (ProgressBar) rootView.findViewById(R.id.progressBarY);
            progressz = (ProgressBar) rootView.findViewById(R.id.progressBarZ);
            
            return rootView;
        }
    }
    
    
    
    public static class SensoriSectionFragment extends Fragment {

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.sensori_fragment, container, false);
            
            
            temperatura = (TextView) rootView.findViewById(R.id.thermometerText);
            pressione = (TextView) rootView.findViewById(R.id.barometerText);
            umidita = (TextView) rootView.findViewById(R.id.humidityText);
            
            temperaturaImg = (ImageView) rootView.findViewById(R.id.thermometerImg);
            pressioneImg = (ImageView) rootView.findViewById(R.id.barometerImg);
            umiditaImg = (ImageView) rootView.findViewById(R.id.humidityImg);
            
            pressureHumidityView = (LinearLayout) rootView.findViewById(R.id.pressureHumidityView);

            return rootView;
        }
    }
    
    public static class SegnaleSectionFragment extends Fragment {


        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.segnale_fragment, container, false);
            segnale = (TextView) rootView.findViewById(R.id.segnaleText);
            
            return rootView;
        }
        
        
    }
    
}
class OpenGLRenderer implements Renderer {

    private Cube mCube;
    private Context 	context;
	
	/** Constructor to set the handed over context */
	public OpenGLRenderer(Context context) {
		this.context = context;
		
		this.mCube = new Cube();
	}

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        
        mCube.loadGLTexture(gl, this.context);
        
        gl.glEnable(GL10.GL_TEXTURE_2D);			
		gl.glShadeModel(GL10.GL_SMOOTH); 			

        gl.glClearColor(1.0f, 1.0f, 1.0f, 0.0f);
        gl.glClearDepthf(1.0f);
        gl.glEnable(GL10.GL_DEPTH_TEST);
        gl.glDepthFunc(GL10.GL_LEQUAL);

        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT,
                  GL10.GL_NICEST);
            
    }
    
    public float getRotationOfAxisValue(float value, float z) {
        float rot = 0;
        if (z<0) {
            rot = -(value/2048)*180.f;
        } else {
            rot = (value/2048)*180.f;
        }
        return rot;
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);        
        gl.glLoadIdentity();
        
        gl.glTranslatef(0.0f, 0.0f, -10.0f);
        gl.glRotatef(getRotationOfAxisValue(DetailsActivity.x,DetailsActivity.z), 0.0f, 1.0f, 0.0f);
        gl.glRotatef(-getRotationOfAxisValue(DetailsActivity.y,DetailsActivity.z), 1.0f, 0.0f, 0.0f);
        
        mCube.draw(gl); 
        gl.glLoadIdentity();                                    
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
    	if(height == 0) { 						
			height = 1; 						
		}
        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        GLU.gluPerspective(gl, 45.0f, (float)width / (float)height, 0.1f, 100.0f);

        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();
    }
}

class Cube {

	/** The texture pointer */
    private int[] textures = new int[1];
    
    private FloatBuffer mVertexBuffer;
    private FloatBuffer mColorBuffer;
    private FloatBuffer textureBuffer; 
    private ByteBuffer  mIndexBuffer;
        
    private float vertices[] = {
                                -2.0f, -2.0f, -2.0f,
                                2.0f, -2.0f, -2.0f,
                                2.0f,  2.0f, -2.0f,
                                -2.0f, 2.0f, -2.0f,
                                -2.0f, -2.0f,  2.0f,
                                2.0f, -2.0f,  2.0f,
                                2.0f,  2.0f,  2.0f,
                                -2.0f,  2.0f,  2.0f
                                };
    private float colors[] = {
                               1.0f,  1.0f,  1.0f,  1.0f,
                               0.0f,  0.0f,  0.0f,  0.0f,
                               1.0f,  1.0f,  1.0f,  1.0f,
                               0.0f,  0.0f,  0.0f,  0.0f,
                               1.0f,  1.0f,  1.0f,  1.0f,
                               0.0f,  0.0f,  0.0f,  0.0f,
                               1.0f,  1.0f,  1.0f,  1.0f,
                               0.0f,  0.0f,  0.0f,  0.0f
                            };

   
    private byte indices[] = {
                              0, 4, 5, 0, 5, 1,
                              1, 5, 6, 1, 6, 2,
                              2, 6, 7, 2, 7, 3,
                              3, 7, 4, 3, 4, 0,
                              4, 7, 6, 4, 6, 5,
                              3, 0, 1, 3, 1, 2
                              };
    
    private float texture[] = { 
    		0.0f,0.0f, 1.0f,0.0f,
    		1.0f,1.0f, 0.0f,1.0f,
            0.0f,0.0f, 1.0f,0.0f,
            1.0f,1.0f, 0.0f,1.0f,
            0.0f,0.0f, 1.0f,0.0f,
            1.0f,1.0f, 0.0f,1.0f,
            0.0f,0.0f, 1.0f,0.0f,
            1.0f,1.0f, 0.0f,1.0f
          };
                
    public Cube() {
            ByteBuffer byteBuf = ByteBuffer.allocateDirect(vertices.length * 4);
            byteBuf.order(ByteOrder.nativeOrder());
            mVertexBuffer = byteBuf.asFloatBuffer();
            mVertexBuffer.put(vertices);
            mVertexBuffer.position(0);
                
            byteBuf = ByteBuffer.allocateDirect(colors.length * 4);
            byteBuf.order(ByteOrder.nativeOrder());
            mColorBuffer = byteBuf.asFloatBuffer();
            mColorBuffer.put(colors);
            mColorBuffer.position(0);
                
            mIndexBuffer = ByteBuffer.allocateDirect(indices.length);
            mIndexBuffer.put(indices);
            mIndexBuffer.position(0);
            
            byteBuf = ByteBuffer.allocateDirect(texture.length * 4);
            byteBuf.order(ByteOrder.nativeOrder());
        	textureBuffer = byteBuf.asFloatBuffer();
        	textureBuffer.put(texture);
        	textureBuffer.position(0);
    }

    public void draw(GL10 gl) {

    	    gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);
    	    
    	    gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
            gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);


            gl.glFrontFace(GL10.GL_CW);
            
            gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexBuffer);
            gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, textureBuffer);

            
            gl.glDrawElements(GL10.GL_TRIANGLES, 36, GL10.GL_UNSIGNED_BYTE, 
                            mIndexBuffer);
                
            gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
            gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

    }
    
    

    public void loadGLTexture(GL10 gl, Context context) {

    	Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(),
    			R.drawable.texture_cube);

    	gl.glGenTextures(1, textures, 0);

    	gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);


    	gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
    	gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
    	gl.glTexParameterf(GL10.GL_TEXTURE_2D,
                GL10.GL_TEXTURE_WRAP_S,
                GL10.GL_REPEAT);
    	gl.glTexParameterf(GL10.GL_TEXTURE_2D,
                GL10.GL_TEXTURE_WRAP_T,
                GL10.GL_REPEAT);
    	
    	GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);
    	
    	bitmap.recycle();
    }
    
}

