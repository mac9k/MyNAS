package com.example.mynas;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.net.ftp.FTPFile;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

public class LogActivity extends Activity {
	private final static String TAG = "TestActivity";
	private TextView myLog;
	private Connect connect_Info;
	private String logUrl, temp;
	

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.log);

		myLog = (TextView)findViewById(R.id.log);
		
		Intent intent = getIntent();
		connect_Info = (Connect) intent.getSerializableExtra("Connect");
		openLog();//Retrive and Open
	}
	
	public void openLog(){
		try {
			File tempFile = File.createTempFile("tempLog", null, this.getCacheDir());
			new DownloadLog().execute(tempFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private class DownloadLog extends AsyncTask<File, Void, File>{ //서버에 위치를 지정한 log 파일을 다운로드 후 열기
		@Override
		protected File doInBackground(File... params) {
			// TODO Auto-generated method stub
			try {
				String path ="/sda/log";
				FTPFile ftpFile[] = connect_Info.mFTPClient.listFiles(path);
				int length = ftpFile.length;
				
				for(int i=0; i< length; i++){
					if(ftpFile[i].getName().equals("vsftpd.log")){
						path = path + "/vsftpd.log";
						connect_Info.ftpTempDownload(path, params[0].getPath());
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return params[0];
		}
		
		protected void onPostExecute(File tempFile) {
			StringBuilder text = new StringBuilder();

			try {
				BufferedReader br = new BufferedReader(new FileReader(tempFile));
				String line;

				while ((line = br.readLine()) != null) {
					text.append(line);
					text.append('\n');
				}
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			myLog.setText(text);
		}

	}
}
