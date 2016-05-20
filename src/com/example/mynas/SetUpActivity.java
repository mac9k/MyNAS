package com.example.mynas;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

public class SetUpActivity extends Activity {
	private static final String TAG =  "In_SetUp";
	private EditText name,fPath,dPath,fport,tport,id,password;
	
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.setup);
		
		name = (EditText)findViewById(R.id.name);
		fPath = (EditText)findViewById(R.id.ftp_path);
		dPath = (EditText)findViewById(R.id.device_path);
		id	 = (EditText)findViewById(R.id.id);
		password =(EditText)findViewById(R.id.password);
		fport = (EditText)findViewById(R.id.fport);
		tport = (EditText)findViewById(R.id.tport);
		

		name.setText("mac9.iptime.org");// 
		fPath.setText("sda");//   //
		dPath.setText("/storage/emulated/0/Download");
		id.setText("ftp");
		password.setText("lee0624");
		fport.setText("9090");
		tport.setText("9091");
	}
	
	public void mOnClick(View v){
		switch(v.getId()){
		case R.id.yes: Setting set_Info = new Setting();
						set_Info.setHostName(name.getText().toString());
						set_Info.setHostFPort(fport.getText().toString());
						set_Info.setHostTPort(tport.getText().toString());
						set_Info.setHostFPath(fPath.getText().toString());
						set_Info.setHostDPath(dPath.getText().toString());
						set_Info.setHostId(id.getText().toString());
						set_Info.setHostPassword(password.getText().toString());
						
						
						Intent result = getIntent();
						result.putExtra("Setting", set_Info);
						setResult(RESULT_OK,result);
						finish();
						break;
		
		case R.id.back:finish();break;
		}
	}
	
	protected void onPause(){
		super.onPause();
		finish();
	}
}
