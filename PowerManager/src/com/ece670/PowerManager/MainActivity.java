package com.ece670.PowerManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;









import com.ece670.PowerManager.PowerUsageService.AppPowerInformation;
import com.ece670g10.PowerManager.R;

import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

//import android.os.Handler;

//service




public class MainActivity extends Activity{
	
	TextView txtText;
	PowerReceiver powerReceiver =  new PowerReceiver();
	String appPower = "";
	int ctr = 0;
	IntentFilter serviceFilter;
	private ArrayAdapter<appInfo> myAppListAdapter;
	public Map<String, appInfo> mapApplications = new HashMap<String, appInfo>();
	private ListView appSearchListView;
	private List<appInfo> appInfoList;
	private Button record;
	private EditText dirEditText;
	private boolean startRecording = false;
	private int collectedAmount = 0;
	private AlertDialog dialog;
	private AlertDialog.Builder builder;
	private Map<String, Integer> countMap = new HashMap<String, Integer>();
	public static final String FILE_NAME = "file_name";
	public static final String FILE_INTENT = "file_itent";

	private TextView countNum;
	//private ArrayAdapter<appInfo> appInfoListAdapter;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		Intent service = new Intent( getApplicationContext() , PowerUsageService.class);
		this.startService(service); 
		//this.startActivity(service);
		//Thread thread = new Thread(null,runnable,"ThreadName");
		//thread.start();
		appInfoList = new ArrayList<appInfo>();
		appSearchListView = (ListView) findViewById(R.id.search_result_list);
		dirEditText = (EditText) findViewById(R.id.dir_edit_text);
		record = (Button)findViewById(R.id.button1);
		countNum = (TextView)findViewById(R.id.textView1);
		countNum.setTextColor(Color.parseColor("#FFFFFF"));
		countNum.setText("0");
		builder = new AlertDialog.Builder(this);
		record.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				if(startRecording == false){
					record.setText("Recording");
				//	builder.setMessage("Recording data to file....finished(0)").setTitle(
				//			"Please wait....");

					dialog = builder.create();
					dialog.show();
					collectedAmount = 0;
					if(dirEditText.getText() != null){
						    Intent intent = new Intent(FILE_INTENT);
						    String tmp = ""+dirEditText.getText();
		                	intent.putExtra(FILE_NAME, tmp);
		                	Log.e("e","send intent");
		                	sendBroadcast(intent);
					}
					startRecording = true;

				}else{
					record.setText("Record");
					startRecording = false;
				}

			}
		});
		myAppListAdapter = new AppAdapter(this,
				R.layout.active_app_searching, appInfoList);
		appSearchListView.setAdapter(myAppListAdapter);
		
		
		
		//txtText = (TextView) findViewById(R.id.txtText);
		//Application filter for broadcast
		serviceFilter =  new IntentFilter(PowerUsageService.BROADCAST_POWER);
		registerReceiver(powerReceiver, serviceFilter);
		
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		unregisterReceiver(powerReceiver);
		super.onPause();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		registerReceiver(powerReceiver, serviceFilter);
		super.onResume();
	}

	public class PowerReceiver extends BroadcastReceiver{
		@Override
		public void onReceive(Context context, Intent intent){
			Bundle bundle = intent.getExtras();
			String powerInfo[] = bundle.getString(PowerUsageService.BUNDLE_APPLICATION_POWER).split("-");
			List<String> powerList = new ArrayList<String>();
			
			/*  Data Validations for UI*/
			if(powerInfo[0] == "0" || powerInfo[1] == null)
				return;
			String[] blockList = {"Power Manager", ""}; 
			try  
		     {  
				
		         Double.parseDouble(powerInfo[2].trim().replace("E", ""));
		         return;  
		      } catch(NumberFormatException nfe)  
		      {    
		      }  
			if(powerInfo[2] == powerInfo[0] || powerInfo[2] == powerInfo[1] )
				return;
			if(powerInfo[2].matches("[0-9]+") || Arrays.asList(blockList).contains(powerInfo[2].trim()))
				return;
			
			powerList.add(powerInfo[1]);
			powerList.add(powerInfo[0]);
			if(powerInfo[3].contentEquals("true"))
				showToast(powerInfo[2] + " has been killed");
			
						
			if(!mapApplications.containsKey(powerInfo[2])){
				appInfo application = new appInfo(powerInfo[2],powerList);
				//appInfoList.add(application);
				mapApplications.put(powerInfo[2], application);
			}else{
				//appInfoList.
				mapApplications.get(powerInfo[2]).updatePower(powerList);
			//	mapApplications.put(powerInfo[2], application);
			}
//			int size = mapApplications.size();
//			if (countMap.containsKey(powerInfo[2])) {
//					
//				int count = countMap.get(powerInfo[2]);
//				Log.e("countMap", powerInfo[2] + ": " + count);// + " active? :" + powerInfo[3]);	
//			if (startRecording && count < 61
//						&& dirEditText.getText() != null) {
////					addDataToFile(dirEditText.getText() + "",
////							mapApplications.get(powerInfo[2]).getName() + ".txt",
////							mapApplications.get(powerInfo[2]).getTotalPower() + " ");
//					//showToast("Count: " + count);
//					
//					countNum.setText(count+"");
//					if (count == 60) {
//						Log.d("DEBUG","collectedAmount: " + String.valueOf(collectedAmount));
//						collectedAmount++;
//						//if(collectedAmount == size)
//						//builder.setMessage("Recording data to file....finished").setTitle("Finished");
//						Log.d("DEBUG", "I am in");
//					}
//					countMap.put(powerInfo[2],
//							count + 1);
//				}else if(collectedAmount == size){
//					Log.i("DEBUG", "Finished");
//					showToast("Finished");
//					startRecording = false;
//					record.setText("record");
//					//builder.setMessage("Recording data to file....finished").setTitle("Finished");
//					collectedAmount = 0;
//				}
//			} else if (startRecording
//					&& dirEditText.getText() != null) {
//
////				addDataToFile(dirEditText.getText() + "",
////						mapApplications.get(powerInfo[2]).getName() + ".txt",
////						mapApplications.get(powerInfo[2]).getTotalPower() + " ");
//				countMap.put(powerInfo[2], 1);
//			}
			try{
				if(null != powerInfo[4]){
				int count = Integer.parseInt(powerInfo[4]);
				 if (count != 0)
					 countNum.setText(""+count);
				 }
			}
			catch(NumberFormatException nfe){}		
			
			if(powerInfo[5]!=null && !Boolean.parseBoolean(powerInfo[5]))
				record.setText("record");
		         
		    
		         
			updateDiscoveredList();
			//txtText.setText(""+ ctr + " - " + " Power: " + powerInfo[0] + " - Power Diff: " + powerInfo[1] + powerInfo[2] + "\n\r" );
			//ctr++;
		}
	}
	
	private void updateDiscoveredList() {
		appInfoList.clear();
		Iterator<appInfo> bIter = mapApplications.values().iterator();
		while (bIter.hasNext()) {
			appInfoList.add(bIter.next());
		}
		Collections.sort(appInfoList, new myComparator());
		runOnUiThread(new Runnable() {
			public void run() {
				myAppListAdapter.notifyDataSetChanged();
			}
		});
	}

	
	public class myComparator implements Comparator<appInfo> {
		@Override
		public int compare(appInfo lhs, appInfo rhs) {
			
			try{
				return (int) (Double.parseDouble(rhs.totalPower.replace("E", "")) - Double.parseDouble(lhs.totalPower.replace("E", "")));
			}catch(NumberFormatException  e){
				return 0;
			}
		}

	}
	
//	private void addDataToFile(String foldername, String filename,
//			String content) {
//		File newFolder = new File(Environment.getExternalStorageDirectory(),
//				foldername);
//		if (!newFolder.exists()) {
//			newFolder.mkdir();
//		}
//		File newFile = new File(newFolder, filename);
//		try {
//			if (!newFile.exists()) {
//				newFile.createNewFile();
//			}
//			OutputStream fWriter = new FileOutputStream(newFile, true);
//			fWriter.write(content.getBytes());
//			fWriter.flush();
//			fWriter.close();
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//	}
	
	
	  private void showToast(String text) {
		    Toast toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG);
		    toast.setGravity(Gravity.CENTER_HORIZONTAL, 0, 0);
		    toast.show();
		  }

}