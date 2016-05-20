package com.example.mynas;


import java.io.IOException;
import java.util.List;

import com.example.minidlna.BrowseActivity;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

enum AppState {
	NotInit(0), Init(1), Connect(2), DisConnect(3), Err(4);
	private final int value;
	private AppState(int value) {this.value = value;}
	public int value() {return value;}
}

public class MainActivity extends Activity {
	private static final String TAG = "In_Main";
	private static final int SHOW_SUBACTIVITY = 1;
	private ImageButton deviceBtn, ftpBtn, settingBtn, transMissionBtn,
			mediaBtn, logBtn;

	private Setting set_Info;
	private AppState appState;
	private Connect connect_Info = new Connect();
	public static Context mContext;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		appState = AppState.NotInit;
		deviceBtn = (ImageButton) findViewById(R.id.device);
		ftpBtn = (ImageButton) findViewById(R.id.ftp);
		settingBtn = (ImageButton) findViewById(R.id.setting);
		transMissionBtn = (ImageButton) findViewById(R.id.transmission);
		mediaBtn = (ImageButton) findViewById(R.id.media);
		logBtn = (ImageButton) findViewById(R.id.log);

		deviceBtn.setOnClickListener(OnClicklistener);
		ftpBtn.setOnClickListener(OnClicklistener);
		settingBtn.setOnClickListener(OnClicklistener);
		transMissionBtn.setOnClickListener(OnClicklistener);
		mediaBtn.setOnClickListener(OnClicklistener);
		logBtn.setOnClickListener(OnClicklistener);
		
		mContext = this;
	
	}

	private View.OnClickListener OnClicklistener = new View.OnClickListener() { //��� ����
		Intent intent;

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			switch (v.getId()) {
			case R.id.device:
				intent = new Intent(MainActivity.this, DeviceActivity.class);
				intent.putExtra("AppState", appState);
				intent.putExtra("Connect", connect_Info);
				intent.putExtra("Setting", set_Info);
				startActivity(intent);
				break;

			case R.id.ftp:
				if(appState == AppState.Connect){
					intent = new Intent(MainActivity.this, MyFTPActivity.class);
					intent.putExtra("Connect",connect_Info);
					intent.putExtra("AppState", appState);
					intent.putExtra("Setting", set_Info);
					startActivity(intent);
				}else Toast.makeText(MainActivity.this, "Not Connected with Devices", Toast.LENGTH_SHORT).show();
				break;
 
			case R.id.setting:
				intent = new Intent(MainActivity.this, SetUpActivity.class);
				startActivityForResult(intent, SHOW_SUBACTIVITY);
				break;

			case R.id.transmission:
					if(appState == AppState.Connect){
						intent = new Intent(MainActivity.this, WebActivity.class);
						intent.putExtra("Setting", set_Info);
						startActivity(intent);
					}else Toast.makeText(MainActivity.this, "Not Connected with Devices", Toast.LENGTH_SHORT).show();

				break;

			case R.id.media:
					if(appState == AppState.Connect){
						//intent = new Intent(MainActivity.this, BrowseActivity.class);
						intent = new Intent(MainActivity.this, ResourceActivity.class);
						intent.putExtra("Setting", set_Info);
						intent.putExtra("Connect",connect_Info);
						startActivity(intent);
					}else Toast.makeText(MainActivity.this, "Not Connected with Devices", Toast.LENGTH_SHORT).show();
				break;

			case R.id.log:
					if(appState == AppState.Connect){
						intent = new Intent(MainActivity.this, LogActivity.class);//LogActivity					
						intent.putExtra("Connect",connect_Info);
						startActivity(intent);
					}else Toast.makeText(MainActivity.this, "Not Connected with Devices", Toast.LENGTH_SHORT).show();
				
				break;
			default:

			}
		}
	};

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case SHOW_SUBACTIVITY:
			if (resultCode == RESULT_OK) {
				set_Info = (Setting) data.getSerializableExtra("Setting");
				appState = AppState.Init;
				openServer();
			}
			break;
		}
	}

	public void openServer() { // Thread�� ����Ͽ� ftp ���� �� ����
		Thread connectThread = new Thread(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				boolean state =connect_Info.ftpConnect(set_Info.getHostName(),
										set_Info.getHostId(), set_Info.getHostPassword(),
										Integer.parseInt(set_Info.getHostFPort()));
				if(state == true){
					appState = AppState.Connect;
					ConnectHandler.sendMessage(Message.obtain(ConnectHandler,1));
				}
				else {
					appState = AppState.Err;
					ConnectHandler.sendMessage(Message.obtain(ConnectHandler,3));
				}
				Log.d(TAG,"appState : "  + appState);
				
				
			}
		});
		connectThread.setDaemon(true);
		connectThread.start();
	}
	
	public void closeServer(){  // Thread�� ����Ͽ� ftp ���� ����
		Thread disconnectThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				boolean state = connect_Info.ftpDisconnect();
				if(state == true) appState = AppState.DisConnect;
				else appState = AppState.Err;
				Log.d(TAG,"appState : "  + appState);
				ConnectHandler.sendMessage(Message.obtain(ConnectHandler,2));
			}
		});
		disconnectThread.setDaemon(true);
		disconnectThread.start();
	}
	
	public void sleep(int time){
		try { Thread.sleep(time);}
		catch (Exception e){
			e.printStackTrace();
			Log.e("sleep","error");
		}
	}
	
	private Handler ConnectHandler = new Handler(){
		public void handleMessage(Message msg){
			String str;
			switch(msg.what){
			case 1:str = topActivity();
					if(str.equals("com.example.mynas.SetUpActivity"))
							Toast.makeText(MainActivity.this, "FTP Connected", Toast.LENGTH_SHORT).show(); 
							break;
			case 2:str = topActivity();
					if(str.equals("com.example.mynas.SetUpActivity"))
							Toast.makeText(MainActivity.this, "FTP DisConnected", Toast.LENGTH_SHORT).show(); 
							break;
			case 3:Toast.makeText(MainActivity.this, "FTP Err", Toast.LENGTH_SHORT).show(); break;
			}
		};
	};
	
	public String topActivity(){
		ActivityManager am = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
		List<ActivityManager.RunningTaskInfo> Info = am.getRunningTasks(1);
		ComponentName topActivity = Info.get(0).topActivity;
		String topactivityname = topActivity.getClassName();
		return topactivityname;
	}
	
	protected void onResume(){
		super.onResume();
		if(appState == AppState.DisConnect){
			openServer();
		}
	}
	protected void onDestroy(){
		super.onDestroy();
		if(appState == AppState.Connect){
			closeServer();
		}
		//disconnectServer call
	}
	

	
}

