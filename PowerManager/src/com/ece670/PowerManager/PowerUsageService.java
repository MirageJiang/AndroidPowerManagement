package com.ece670.PowerManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.android.internal.app.IBatteryStats;
import com.android.internal.os.BatteryStatsImpl;
import com.android.internal.os.PowerProfile;

import android.net.ConnectivityManager;
/*import com.android.settings.fuelgauge.BatteryHistoryPreference;
import com.android.settings.fuelgauge.PowerGaugePreference;
import com.android.settings.fuelgauge.PowerUsageDetail;*/
import android.os.BatteryStats.Uid;

import com.ece670.PowerManager.BatterySipper;
import com.ece670.PowerManager.BatterySipper.DrainType;
import com.ece670.PowerManager.PowerUsageService.AppPowerInformation;
import com.ece670g10.PowerManager.R;

import android.preference.Preference;
//import android.preference.PreferenceActivity;
//import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
//import android.preference.PreferenceScreen;
//import com.android.settings.fuelgauge.BatterySipper;

import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.hardware.SensorManager;
import android.os.BatteryStats;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcel;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
//import android.preference.Preference;
//import android.preference.PreferenceGroup;
import android.telephony.SignalStrength;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;


public class PowerUsageService extends Service {

    private static final boolean DEBUG = true;

    private static final String TAG = "PowerUsageSummary";
    private static final String TAG2 = "My_Power_UsageSummary";

    private static final String KEY_APP_LIST = "app_list";
    private static final String KEY_BATTERY_STATUS = "battery_status";

    private static final int MENU_STATS_TYPE = Menu.FIRST;
    private static final int MENU_STATS_REFRESH = Menu.FIRST + 1;

    private static BatteryStatsImpl sStatsXfer;

    IBatteryStats mBatteryInfo;
    BatteryStatsImpl mStats;
    private final List<BatterySipper> mUsageList = new ArrayList<BatterySipper>();
    private final List<BatterySipper> mWifiSippers = new ArrayList<BatterySipper>();
    private final List<BatterySipper> mBluetoothSippers = new ArrayList<BatterySipper>();

    private PreferenceGroup mAppListGroup;
    private Preference mBatteryStatusPref;

    private int mStatsType = BatteryStats.STATS_SINCE_CHARGED;//BatteryStats.STATS_CURRENT;

    private static final int MIN_POWER_THRESHOLD = 5;
    private static final int MAX_ITEMS_TO_LIST = 100;

    private long mStatsPeriod = 0;
    private double mMaxPower = 1;
    private double mTotalPower;
    private double mWifiPower;
    private double mBluetoothPower;
    private PowerProfile mPowerProfile;

    // How much the apps together have left WIFI running.
    private long mAppWifiRunning;

    //Our variables
    //private String applicationPower = "";
    public static final String BUNDLE_APPLICATION_POWER = "applicationPower";
    public static final String BROADCAST_POWER = "broadcastApplicationPower";
	double ctr;
	Context powerContext;
	public Map<Integer, AppPowerInformation> mapApplications = new HashMap<Integer, AppPowerInformation>(); 
    
	/** Queue for fetching name and icon for an application */
    private ArrayList<BatterySipper> mRequestQueue = new ArrayList<BatterySipper>();
    private Thread mRequestThread;
    private boolean mAbort;
    private String fileName;
    private boolean startRecording = false;
    private int countRegsData = 0;
    private int appsChecked = 0;

    
    private BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                //String batteryLevel = com.android.settings.Utils.getBatteryPercentage(intent);
                int level = intent.getIntExtra("level", 0);
                int scale = intent.getIntExtra("scale", 100);
                String batteryLevel = String.valueOf(level * 100 / scale) + "%";
                
                
                //String batteryStatus = com.android.settings.Utils.getBatteryStatus(getResources(),
                //        intent);
                String batteryStatus = "Discharging";
                
                
                String batterySummary = context.getResources().getString(
                        R.string.power_usage_level_and_status, batteryLevel, batteryStatus);
                mBatteryStatusPref.setTitle(batterySummary);
            }
        }
    };

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	/*@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		
		/////////////////////////
 //       mStats = sStatsXfer;
	
	    //addPreferencesFromResource(R.xml.power_usage_summary);
//	    mBatteryInfo = IBatteryStats.Stub.asInterface(
//	            ServiceManager.getService("batteryinfo"));
	    //mAppListGroup = (PreferenceGroup) android.preference.PreferenceFragment.findPreference(KEY_APP_LIST);
	    ///mBatteryStatusPref = mAppListGroup.findPreference(KEY_BATTERY_STATUS);
//	    mPowerProfile = new PowerProfile(this);		// ? what to do ?   
	    /////////////////////////
	    //while(true){
//	    	refreshStats(); // for testing
	    //}
	}*/

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		sStatsXfer = mStats;
		this.unregisterReceiver(mBatteryInfoReceiver);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		////return super.onStartCommand(intent, flags, startId);
		
		///////////////////////// From onCreate
		mStats = sStatsXfer;

		//addPreferencesFromResource(R.xml.power_usage_summary);
		mBatteryInfo = IBatteryStats.Stub.asInterface(
		ServiceManager.getService("batteryinfo"));
		//mAppListGroup = (PreferenceGroup) android.preference.PreferenceFragment.findPreference(KEY_APP_LIST);
		/*mBatteryStatusPref = mAppListGroup.findPreference(KEY_BATTERY_STATUS);*/
		mPowerProfile = new PowerProfile(this);		// ? what to do ?   
		powerContext = this;
		/////////////////////////
		//while(true){
		//refreshStats(); // for testing
		//}
		ctr += 1;


		FileNameReceiver fileNameReceiver =  new FileNameReceiver();
		IntentFilter activityFilter = new IntentFilter(MainActivity.FILE_INTENT);
		registerReceiver(fileNameReceiver, activityFilter);


		Thread thread = new Thread(null,runnable,"ThreadName");
		thread.start();
		
		return START_NOT_STICKY;
	}
	
	private Runnable runnable = new Runnable(){
		
		@Override
		public void run(){
			
			while(true){
				mStats = sStatsXfer;

				//addPreferencesFromResource(R.xml.power_usage_summary);
				mBatteryInfo = IBatteryStats.Stub.asInterface(
				ServiceManager.getService("batteryinfo"));
				//mAppListGroup = (PreferenceGroup) android.preference.PreferenceFragment.findPreference(KEY_APP_LIST);
				/*mBatteryStatusPref = mAppListGroup.findPreference(KEY_BATTERY_STATUS);*/
				mPowerProfile = new PowerProfile(powerContext);
				ctr += 1;
				refreshStats();
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}
	};
	
	
	//// ======================================================================================= ////
	//// ======================================================================================= ////
    private void addNotAvailableMessage() {
        Preference notAvailable = new Preference(this);
        notAvailable.setTitle(R.string.power_usage_not_available);
        mAppListGroup.addPreference(notAvailable);
    }
    
	private void refreshStats() {
        if (mStats == null) {
            load();
        }
        mMaxPower = 0;
        mTotalPower = 0;
        mWifiPower = 0;
        mBluetoothPower = 0;
        mAppWifiRunning = 0;

        //mAppListGroup.removeAll();
        mUsageList.clear(); //<----------- uncomment this
        mWifiSippers.clear();
        mBluetoothSippers.clear();
        ////mAppListGroup.setOrderingAsAdded(false);

        //mBatteryStatusPref.setOrder(-2);
        ////mAppListGroup.addPreference(mBatteryStatusPref);
        //BatteryHistoryPreference hist = new BatteryHistoryPreference(this, mStats);
        //hist.setOrder(-1);
        //mAppListGroup.addPreference(hist);
        
        Log.i(TAG, "refreshstats - before power profile");
        /***if (mPowerProfile.getAveragePower(PowerProfile.POWER_SCREEN_FULL) < 10) {
            addNotAvailableMessage();
            return;
        }***/
        
        Log.i(TAG, "refreshstats - after power profile");
        processAppUsage();
        processMiscUsage();

        Collections.sort(mUsageList); //<----------- uncomment this
        for (BatterySipper sipper : mUsageList) {
            //if (sipper.getSortValue() < MIN_POWER_THRESHOLD) continue;
            final double percentOfTotal =  ((sipper.getSortValue() / mTotalPower) * 100);
            //if (percentOfTotal < 1) continue;
            //PowerGaugePreference pref = new PowerGaugePreference(this, sipper.getIcon(), sipper);
            final double percentOfMax = (sipper.getSortValue() * 100) / mMaxPower;
            sipper.percent = percentOfTotal;
            //Log.i(TAG2, sipper.name + " -> " + sipper.uidObj + " --- " + 
            //		(int) Math.ceil(sipper.percent) + " -- " + percentOfMax);
            //Log.i(TAG2, sipper.name + " -> " + sipper.cpuFgTime + " - " + sipper.cpuTime + " - " 
            //		+ sipper.usageTime + " - " + sipper.tcpBytesReceived + " - " + sipper.tcpBytesSent);
            //pref.setTitle(sipper.name);
            //pref.setOrder(Integer.MAX_VALUE - (int) sipper.getSortValue()); // Invert the order
            //pref.setPercent(percentOfMax, percentOfTotal);
            //if (sipper.uidObj != null) {
                //pref.setKey(Integer.toString(sipper.uidObj.getUid()));
            //}
            //mAppListGroup.addPreference(pref);
            //if (mAppListGroup.getPreferenceCount() > (MAX_ITEMS_TO_LIST+1)) break;
        }
        /*synchronized (mRequestQueue) {
            if (!mRequestQueue.isEmpty()) {
                if (mRequestThread == null) {
                    mRequestThread = new Thread(this, "BatteryUsage Icon Loader");
                    mRequestThread.setPriority(Thread.MIN_PRIORITY);
                    mRequestThread.start();
                }
                mRequestQueue.notify();
            }
        }*/
        Log.i(TAG, "refreshstats - complete");
    }

    private void processAppUsage() {
        SensorManager sensorManager = (SensorManager)this.getSystemService(
                Context.SENSOR_SERVICE);
        final int which = mStatsType;
        final int speedSteps = mPowerProfile.getNumSpeedSteps();
        final double[] powerCpuNormal = new double[speedSteps];
        final long[] cpuSpeedStepTimes = new long[speedSteps];
        for (int p = 0; p < speedSteps; p++) {
            powerCpuNormal[p] = mPowerProfile.getAveragePower(PowerProfile.POWER_CPU_ACTIVE, p);
        }
        final double averageCostPerByte = getAverageDataCost();
        long uSecTime = mStats.computeBatteryRealtime(SystemClock.elapsedRealtime() * 1000, which);
        long appWakelockTime = 0;
        BatterySipper osApp = null;
        mStatsPeriod = uSecTime;
        SparseArray<? extends Uid> uidStats = mStats.getUidStats();
        final int NU = uidStats.size();
        for (int iu = 0; iu < NU; iu++) {
            Uid u = uidStats.valueAt(iu);
            double power = 0;
            double highestDrain = 0;
            String packageWithHighestDrain = null;
            String processName = null;
            //mUsageList.add(new AppUsage(u.getUid(), new double[] {power}));
            Map<String, ? extends BatteryStats.Uid.Proc> processStats = u.getProcessStats();
            long cpuTime = 0;
            long cpuFgTime = 0;
            long wakelockTime = 0;
            long gpsTime = 0;
            if (processStats.size() > 0) {
                // Process CPU time
                for (Map.Entry<String, ? extends BatteryStats.Uid.Proc> ent
                        : processStats.entrySet()) {
                    //if (DEBUG) Log.i(TAG, "Process name = " + ent.getKey());
                    processName = ent.getKey();
                    Uid.Proc ps = ent.getValue();
                    final long userTime = ps.getUserTime(which);
                    final long systemTime = ps.getSystemTime(which);
                    final long foregroundTime = ps.getForegroundTime(which);
                    cpuFgTime += foregroundTime * 10; // convert to millis
                    final long tmpCpuTime = (userTime + systemTime) * 10; // convert to millis
                    int totalTimeAtSpeeds = 0;
                    // Get the total first
                    for (int step = 0; step < speedSteps; step++) {
                        cpuSpeedStepTimes[step] = ps.getTimeAtCpuSpeedStep(step, which);
                        totalTimeAtSpeeds += cpuSpeedStepTimes[step];
                    }
                    if (totalTimeAtSpeeds == 0) totalTimeAtSpeeds = 1;
                    // Then compute the ratio of time spent at each speed
                    double processPower = 0;
                    for (int step = 0; step < speedSteps; step++) {
                        double ratio = (double) cpuSpeedStepTimes[step] / totalTimeAtSpeeds;
                        processPower += ratio * tmpCpuTime * powerCpuNormal[step];
                    }
                    cpuTime += tmpCpuTime;
                    power += processPower;
                    if (packageWithHighestDrain == null
                            || packageWithHighestDrain.startsWith("*")) {
                        highestDrain = processPower;
                        packageWithHighestDrain = ent.getKey();
                    } else if (highestDrain < processPower
                            && !ent.getKey().startsWith("*")) {
                        highestDrain = processPower;
                        packageWithHighestDrain = ent.getKey();
                    }
                }
                //if (DEBUG && highestDrain > 0) Log.i(TAG, "Max drain of " + highestDrain 
                //        + " by " + packageWithHighestDrain);
            }
            if (cpuFgTime > cpuTime) {
           //     if (DEBUG && cpuFgTime > cpuTime + 10000) {
           //         Log.i(TAG, "WARNING! Cputime is more than 10 seconds behind Foreground time");
           //     }
                cpuTime = cpuFgTime; // Statistics may not have been gathered yet.
            }
            power /= 1000;

            // Process wake lock usage
            Map<String, ? extends BatteryStats.Uid.Wakelock> wakelockStats = u.getWakelockStats();
            for (Map.Entry<String, ? extends BatteryStats.Uid.Wakelock> wakelockEntry
                    : wakelockStats.entrySet()) {
                Uid.Wakelock wakelock = wakelockEntry.getValue();
                // Only care about partial wake locks since full wake locks
                // are canceled when the user turns the screen off.
                BatteryStats.Timer timer = wakelock.getWakeTime(BatteryStats.WAKE_TYPE_PARTIAL);
                if (timer != null) {
                    wakelockTime += timer.getTotalTimeLocked(uSecTime, which);
                }
            }
            wakelockTime /= 1000; // convert to millis
            appWakelockTime += wakelockTime;

            // Add cost of holding a wake lock
            power += (wakelockTime
                    * mPowerProfile.getAveragePower(PowerProfile.POWER_CPU_AWAKE)) / 1000;
            
            // Add cost of data traffic
            long tcpBytesReceived = u.getTcpBytesReceived(mStatsType);
            long tcpBytesSent = u.getTcpBytesSent(mStatsType);
            power += (tcpBytesReceived+tcpBytesSent) * averageCostPerByte;

            // Add cost of keeping WIFI running.
            long wifiRunningTimeMs = u.getWifiRunningTime(uSecTime, which) / 1000;
            mAppWifiRunning += wifiRunningTimeMs;
            power += (wifiRunningTimeMs
                    * mPowerProfile.getAveragePower(PowerProfile.POWER_WIFI_ON)) / 1000;

            // Process Sensor usage
            Map<Integer, ? extends BatteryStats.Uid.Sensor> sensorStats = u.getSensorStats();
            for (Map.Entry<Integer, ? extends BatteryStats.Uid.Sensor> sensorEntry
                    : sensorStats.entrySet()) {
                Uid.Sensor sensor = sensorEntry.getValue();
                int sensorType = sensor.getHandle();
                BatteryStats.Timer timer = sensor.getSensorTime();
                long sensorTime = timer.getTotalTimeLocked(uSecTime, which) / 1000;
                double multiplier = 0;
                switch (sensorType) {
                    case Uid.Sensor.GPS:
                        multiplier = mPowerProfile.getAveragePower(PowerProfile.POWER_GPS_ON);
                        gpsTime = sensorTime;
                        break;
                    default:
                        android.hardware.Sensor sensorData =
                                sensorManager.getDefaultSensor(sensorType);
                        if (sensorData != null) {
                            multiplier = sensorData.getPower();
                        }
                }
                power += (multiplier * sensorTime) / 1000;
            }
            if (DEBUG && power>0) Log.i(TAG, "Process name = " + packageWithHighestDrain);
            if (DEBUG&& power>0) Log.i(TAG, "UID " + u.getUid() + ": power=" + power);
            
            if(power>0 && u.getUid() != 0 && packageWithHighestDrain != null){
            	
            	double powerDifference = 0;
            	AppPowerInformation appPowerInfo = mapApplications.get(u.getUid());
            	boolean killApp = false;
            	double appTotalPower = 0;
            	
            	if(appPowerInfo != null){
            		powerDifference = power - appPowerInfo.previousPower;
            		appPowerInfo.previousPower = power;
            		if(powerDifference == 0 ){
            			appPowerInfo.countInactive++;
            			if(appPowerInfo.countInactive >= 10)
            				appPowerInfo.active = false;
            		}
            		else{
            			appPowerInfo.currentPower += powerDifference;
            			appPowerInfo.countInactive = 0;
            			appPowerInfo.active = true;
            			appPowerInfo.killed = false;            			
            		}
            	}
            	else{
            		appPowerInfo = new AppPowerInformation(packageWithHighestDrain,power);            		
            	}
            	
            	mapApplications.put(u.getUid(), appPowerInfo);
            	
            	int size = mapApplications.size();
            	
            		final PackageManager pm = getApplicationContext().getPackageManager();
            		ApplicationInfo ai;
            		try {
            		    ai = pm.getApplicationInfo( packageWithHighestDrain, 0);
            		} catch (final NameNotFoundException e) {
            		    ai = null;
            		}
            		
            		if(ai != null){
            			final String applicationName = (String) (ai != null ? pm.getApplicationLabel(ai) : "(unknown)");
            			appTotalPower = power - appPowerInfo.initialPower;
            			            			
	            		if (!applicationName.toUpperCase().contains("SYSTEM") && !applicationName.toUpperCase().trim().equals("POWER MANAGER")
	            				&& appTotalPower != 0 ){
	            			
	            			//Record the app power
	            			if (startRecording && fileName != null && applicationName.equalsIgnoreCase(fileName.toUpperCase())) {
	        					
	            				int dataPoints = 60;
	            				Log.e("countRegData", applicationName + ": " + countRegsData);// + " active? :" + powerInfo[3]);	
	            				if (startRecording && countRegsData < dataPoints+1) {
	            					addDataToFile("powerData", 
	            							fileName + ".txt", 
	            							Double.toString(appTotalPower) + " ");
	            					Log.i("powerRecording", fileName);
	            					if (countRegsData == dataPoints) {
	            						startRecording = false;
	            						Log.i("DEBUG", "Finished");
	            					}
	            					countRegsData++;
	            				}
	            			} 
	            			
	            			double powerAv = 0;

	            			if(appPowerInfo.UpdatePowerArr(appPowerInfo.currentPower) && appPowerInfo.getPowerAverage() > 500d){
	            				appPowerInfo.active = false;            				
	            				appPowerInfo.currentPower = -1;
			                		killProcess(packageWithHighestDrain,this);
	            			}
		            		
		            		String extraInfo = ""+ Double.toString(appTotalPower) + "-" + Double.toString(powerDifference) + "-" 
		            		+ applicationName + "-" + killApp + "-" +  countRegsData + "-" + startRecording;
		                	Intent intent = new Intent(BROADCAST_POWER);
		                	intent.putExtra(BUNDLE_APPLICATION_POWER, extraInfo);//power); 
		                	sendBroadcast(intent);
		                	
	                	}
                	}
                }
        //    }
            

            // Add the app to the list if it is consuming power
            if (power != 0 || u.getUid() == 0) {
                BatterySipper app = new BatterySipper(this, mRequestQueue, mHandler,
                        packageWithHighestDrain, BatterySipper.DrainType.APP, 0, u,
                        new double[] {power});
                app.cpuTime = cpuTime;
                app.gpsTime = gpsTime;
                app.wifiRunningTime = wifiRunningTimeMs;
                app.cpuFgTime = cpuFgTime;
                app.wakeLockTime = wakelockTime;
                app.tcpBytesReceived = tcpBytesReceived;
                app.tcpBytesSent = tcpBytesSent;
                if (u.getUid() == Process.WIFI_UID) {
                    mWifiSippers.add(app);
                } else if (u.getUid() == 1002){//Process.BLUETOOTH_GID) {
                    mBluetoothSippers.add(app);
                } else {
                    mUsageList.add(app);
                }
                if (u.getUid() == 0) {
                    osApp = app;
                }
                if (DEBUG && power>0) Log.i(TAG," CPUTime " + app.cpuTime + " - gpsTime: " + app.gpsTime);
                
                if (DEBUG && power>0) Log.i(TAG," Bytes Sent\\Received " + app.tcpBytesSent + " - " + app.tcpBytesReceived);
            }
            if (u.getUid() == Process.WIFI_UID) {
                mWifiPower += power;
            } else if (u.getUid() == 1002){//Process.BLUETOOTH_GID) {
                mBluetoothPower += power;
            } else {
                if (power > mMaxPower) mMaxPower = power;
                mTotalPower += power;
            }
            if (DEBUG && power>0) Log.i(TAG, "Added w/Sensor information power = " + power + "Total Power = " 
            		+ mTotalPower);
        }

        // The device has probably been awake for longer than the screen on
        // time and application wake lock time would account for.  Assign
        // this remainder to the OS, if possible.
        if (osApp != null) {
            long wakeTimeMillis = mStats.computeBatteryUptime(
                    SystemClock.uptimeMillis() * 1000, which) / 1000;
            wakeTimeMillis -= appWakelockTime + (mStats.getScreenOnTime(
                    SystemClock.elapsedRealtime(), which) / 1000);
            if (wakeTimeMillis > 0) {
                double power = (wakeTimeMillis
                        * mPowerProfile.getAveragePower(PowerProfile.POWER_CPU_AWAKE)) / 1000;
                if (DEBUG) Log.i(TAG, "OS wakeLockTime " + wakeTimeMillis + " power " + power);
                osApp.wakeLockTime += wakeTimeMillis;
                osApp.value += power;
                osApp.values[0] += power;
                if (osApp.value > mMaxPower) mMaxPower = osApp.value;
                mTotalPower += power;
            }
        }
    }

    private void addPhoneUsage(long uSecNow) {
        long phoneOnTimeMs = mStats.getPhoneOnTime(uSecNow, mStatsType) / 1000;
        double phoneOnPower = mPowerProfile.getAveragePower(PowerProfile.POWER_RADIO_ACTIVE)
                * phoneOnTimeMs / 1000;
        addEntry(this.getString(R.string.power_phone), DrainType.PHONE, phoneOnTimeMs, 
        		111, phoneOnPower);
    }

    private void addScreenUsage(long uSecNow) {
        double power = 0;
        long screenOnTimeMs = mStats.getScreenOnTime(uSecNow, mStatsType) / 1000;
        power += screenOnTimeMs * mPowerProfile.getAveragePower(PowerProfile.POWER_SCREEN_ON);
        final double screenFullPower =
                mPowerProfile.getAveragePower(PowerProfile.POWER_SCREEN_FULL);
        for (int i = 0; i < BatteryStats.NUM_SCREEN_BRIGHTNESS_BINS; i++) {
            double screenBinPower = screenFullPower * (i + 0.5f)
                    / BatteryStats.NUM_SCREEN_BRIGHTNESS_BINS;
            long brightnessTime = mStats.getScreenBrightnessTime(i, uSecNow, mStatsType) / 1000;
            power += screenBinPower * brightnessTime;
            if (DEBUG) {
                Log.i(TAG, "Screen bin power = " + (int) screenBinPower + ", time = "
                        + brightnessTime);
            }
        }
        power /= 1000; // To seconds
        addEntry(this.getString(R.string.power_screen), DrainType.SCREEN, screenOnTimeMs,
                111, power);
    }

    private void addRadioUsage(long uSecNow) {
        double power = 0;
        final int BINS = SignalStrength.NUM_SIGNAL_STRENGTH_BINS;
        long signalTimeMs = 0;
        for (int i = 0; i < BINS; i++) {
            long strengthTimeMs = mStats.getPhoneSignalStrengthTime(i, uSecNow, mStatsType) / 1000;
            power += strengthTimeMs / 1000
                    * mPowerProfile.getAveragePower(PowerProfile.POWER_RADIO_ON, i);
            signalTimeMs += strengthTimeMs;
        }
        long scanningTimeMs = mStats.getPhoneSignalScanningTime(uSecNow, mStatsType) / 1000;
        power += scanningTimeMs / 1000 * mPowerProfile.getAveragePower(
                PowerProfile.POWER_RADIO_SCANNING);
        BatterySipper bs =
                addEntry(this.getString(R.string.power_cell), DrainType.CELL,
                        signalTimeMs, 111, power);
        if (signalTimeMs != 0) {
            bs.noCoveragePercent = mStats.getPhoneSignalStrengthTime(0, uSecNow, mStatsType)
                    / 1000 * 100.0 / signalTimeMs;
        }
    }

    private void aggregateSippers(BatterySipper bs, List<BatterySipper> from, String tag) {
        for (int i=0; i<from.size(); i++) {
            BatterySipper wbs = from.get(i);
            if (DEBUG) Log.i(TAG, tag + " adding sipper " + wbs + ": cpu=" + wbs.cpuTime);
            bs.cpuTime += wbs.cpuTime;
            bs.gpsTime += wbs.gpsTime;
            bs.wifiRunningTime += wbs.wifiRunningTime;
            bs.cpuFgTime += wbs.cpuFgTime;
            bs.wakeLockTime += wbs.wakeLockTime;
            bs.tcpBytesReceived += wbs.tcpBytesReceived;
            bs.tcpBytesSent += wbs.tcpBytesSent;
        }
    }

    private void addWiFiUsage(long uSecNow) {
        long onTimeMs = mStats.getWifiOnTime(uSecNow, mStatsType) / 1000;
        long runningTimeMs = mStats.getGlobalWifiRunningTime(uSecNow, mStatsType) / 1000;
        if (DEBUG) Log.i(TAG, "WIFI runningTime=" + runningTimeMs
                + " app runningTime=" + mAppWifiRunning);
        runningTimeMs -= mAppWifiRunning;
        if (runningTimeMs < 0) runningTimeMs = 0;
        double wifiPower = (onTimeMs * 0 /* TODO */
                * mPowerProfile.getAveragePower(PowerProfile.POWER_WIFI_ON)
            + runningTimeMs * mPowerProfile.getAveragePower(PowerProfile.POWER_WIFI_ON)) / 1000;
        if (DEBUG) Log.i(TAG, "WIFI power=" + wifiPower + " from procs=" + mWifiPower);
        BatterySipper bs = addEntry(this.getString(R.string.power_wifi), DrainType.WIFI,
                runningTimeMs, 111, wifiPower + mWifiPower);
        aggregateSippers(bs, mWifiSippers, "WIFI");
    }

    private void addIdleUsage(long uSecNow) {
        long idleTimeMs = (uSecNow - mStats.getScreenOnTime(uSecNow, mStatsType)) / 1000;
        double idlePower = (idleTimeMs * mPowerProfile.getAveragePower(PowerProfile.POWER_CPU_IDLE))
                / 1000;
        addEntry(this.getString(R.string.power_idle), DrainType.IDLE, idleTimeMs,
                111, idlePower);
    }

    private void addBluetoothUsage(long uSecNow) {
        long btOnTimeMs = mStats.getBluetoothOnTime(uSecNow, mStatsType) / 1000;
        double btPower = btOnTimeMs * mPowerProfile.getAveragePower(PowerProfile.POWER_BLUETOOTH_ON)
                / 1000;
        int btPingCount = mStats.getBluetoothPingCount();
        btPower += (btPingCount
                * mPowerProfile.getAveragePower(PowerProfile.POWER_BLUETOOTH_AT_CMD)) / 1000;
        BatterySipper bs = addEntry(this.getString(R.string.power_bluetooth),
                DrainType.BLUETOOTH, btOnTimeMs, 111,
                btPower + mBluetoothPower);
        aggregateSippers(bs, mBluetoothSippers, "Bluetooth");
    }

    private double getAverageDataCost() {
        final long WIFI_BPS = 1000000; // TODO: Extract average bit rates from system 
        final long MOBILE_BPS = 200000; // TODO: Extract average bit rates from system
        final double WIFI_POWER = mPowerProfile.getAveragePower(PowerProfile.POWER_WIFI_ACTIVE)
                / 3600;
        final double MOBILE_POWER = mPowerProfile.getAveragePower(PowerProfile.POWER_RADIO_ACTIVE)
                / 3600;
        final long mobileData = mStats.getMobileTcpBytesReceived(mStatsType) +
                mStats.getMobileTcpBytesSent(mStatsType);
        final long wifiData = mStats.getTotalTcpBytesReceived(mStatsType) +
                mStats.getTotalTcpBytesSent(mStatsType) - mobileData;
        final long radioDataUptimeMs = mStats.getRadioDataUptime() / 1000;
        final long mobileBps = radioDataUptimeMs != 0
                ? mobileData * 8 * 1000 / radioDataUptimeMs
                : MOBILE_BPS;

        double mobileCostPerByte = MOBILE_POWER / (mobileBps / 8);
        double wifiCostPerByte = WIFI_POWER / (WIFI_BPS / 8);
        if (wifiData + mobileData != 0) {
            return (mobileCostPerByte * mobileData + wifiCostPerByte * wifiData)
                    / (mobileData + wifiData);
        } else {
            return 0;
        }
    }

    private void processMiscUsage() {
        final int which = mStatsType;
        long uSecTime = SystemClock.elapsedRealtime() * 1000;
        final long uSecNow = mStats.computeBatteryRealtime(uSecTime, which);
        final long timeSinceUnplugged = uSecNow;
        if (DEBUG) {
            Log.i(TAG, "Uptime since last unplugged = " + (timeSinceUnplugged / 1000));
        }

        addPhoneUsage(uSecNow);
        addScreenUsage(uSecNow);
        addWiFiUsage(uSecNow);
        addBluetoothUsage(uSecNow);
        addIdleUsage(uSecNow); // Not including cellular idle power
        // Don't compute radio usage if it's a wifi-only device
        if (!isWifiOnly(this)) {
            addRadioUsage(uSecNow);
        }
    }

    private BatterySipper addEntry(String label, BatterySipper.DrainType drainType, long time, int iconId,
            double power) {
        if (power > mMaxPower) mMaxPower = power;
        mTotalPower += power;
        BatterySipper bs = new BatterySipper(this, mRequestQueue, mHandler,
                label, drainType, iconId, null, new double[] {power});
        bs.usageTime = time;
        bs.iconId = iconId;
        mUsageList.add(bs);
        return bs;
    }

    public static boolean isWifiOnly(Context context) {
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        return (cm.isNetworkSupported(ConnectivityManager.TYPE_MOBILE) == false);
    }
    
    private void load() {
        try {
            byte[] data = mBatteryInfo.getStatistics();
            Parcel parcel = Parcel.obtain();
            parcel.unmarshall(data, 0, data.length);
            parcel.setDataPosition(0);
            mStats = com.android.internal.os.BatteryStatsImpl.CREATOR
                    .createFromParcel(parcel);
            mStats.distributeWorkLocked(BatteryStats.STATS_SINCE_CHARGED);
            Log.i(TAG, "load() - " + mStats.getDischargeCurrentLevel() + " ---- Avg. Power (Awake): " +
            		mPowerProfile.getAveragePower(PowerProfile.POWER_CPU_AWAKE) + " - Speed steps: " +
            		mPowerProfile.getNumSpeedSteps() + " -- Avg. Power (Active): " + 
            		mPowerProfile.getAveragePower(PowerProfile.POWER_CPU_ACTIVE, 0));
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException:", e);
        }
    }

    static final int MSG_UPDATE_NAME_ICON = 1;

    Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_NAME_ICON:
                    BatterySipper bs = (BatterySipper) msg.obj;
                    /*PowerGaugePreference pgp = 
                            (PowerGaugePreference) findPreference(
                                    Integer.toString(bs.uidObj.getUid()));
                    if (pgp != null) {
                        pgp.setIcon(bs.icon);
                        pgp.setTitle(bs.name);
                    }*/
                    break;
            }
            super.handleMessage(msg);
        }
    };

    
	public static void killProcess(String packageName, Context mContext){
    	//android.os.Process.killProcess(pid);
		ActivityManager activityMgr= (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
		activityMgr.killBackgroundProcesses(packageName);
		//activityMgr.restartPackage(context.getPackageName());
	} 
	
	private void addDataToFile(String foldername, String filename,
			String content) {
		File newFolder = new File(Environment.getExternalStorageDirectory(),
				foldername);
		if (!newFolder.exists()) {
			newFolder.mkdir();
		}
		File newFile = new File(newFolder, filename);
		try {
			if (!newFile.exists()) {
				newFile.createNewFile();
			}
			OutputStream fWriter = new FileOutputStream(newFile, true);
			fWriter.write(content.getBytes());
			fWriter.flush();
			fWriter.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	class AppPowerInformation{
		public String  packageName;
		public double initialPower;
		public double currentPower;
		public double previousPower;
		public int countInactive;
		public boolean active;
		public boolean killed;
		public double[] previousPowerArr = new double[5];
		public int countAverage;
		public AppPowerInformation(String packageName, double power) {
	        this.packageName = packageName;
	        this.initialPower = power;
	        this.previousPower = power;
	        this.active = false;
	        this.currentPower = 0;
	        this.countInactive = 0;
	        this.countAverage = 0;
		}
		
		
		public boolean UpdatePowerArr(double power){
			boolean val = false;
			
			if(this.countAverage == 5)
				this.countAverage = 0;
			else if(this.countAverage == 4)
				val = true;
			
			this.previousPowerArr[this.countAverage] = power;
			
			this.countAverage++;			
			return val;
		}
		
		public double getPowerAverage(){
			double sumPower = 0;
			
			for(int i = 0; i<5;i++){
				sumPower += this.previousPowerArr[i];
			}
			
			return sumPower/5d;
		}

	} 
	 
	public class FileNameReceiver extends BroadcastReceiver{
		@Override
		public void onReceive(Context context, Intent intent){
			Bundle bundle = intent.getExtras();
			fileName = bundle.getString(MainActivity.FILE_NAME);
		    startRecording = true;
		    Log.e("powerRecording", "in receiver, Value of fileName = " + fileName);
	    }
	} 
	
	
}
