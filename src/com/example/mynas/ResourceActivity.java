package com.example.mynas;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.apache.commons.net.ftp.FTPFile;



import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

enum FileState {
	NotInit(0), Init(1), Err(2);
	private final int value;
	private FileState(int value) {this.value = value;}
	public int value() {return value;}
}

enum FileExtension {
	NotInit(0), Init(1), Music(2), Movie(3), Err(4);
	private final int value;
	private FileExtension (int value) {this.value = value;}
	public int value() {return value;}
}


public class ResourceActivity extends Activity  {

	private static final String TAG = "In_ResourceActivity";
	private static final String FTPUSER = "50";  // 서버에 등록된 ID 번호 
	private Connect connect_Info;
	private AppState appState;
	private Setting set_Info;
	private ResourceAdapter rAdapter;
	private ListView rFileList;
	private TextView rCurrnetTxt;
	private String rCurrent, mRoot;
	private ArrayList<FileTable> arFiles;
	private FileState fileState = FileState.NotInit;
	private FileExtension fileExtension = FileExtension.NotInit;
	private String extension="";
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.device);
		
		rCurrnetTxt = (TextView) findViewById(R.id.device_path);
		rFileList = (ListView) findViewById(R.id.deviceList);
		
		Intent intent = getIntent();
		connect_Info = (Connect) intent.getSerializableExtra("Connect");
		appState = (AppState)intent.getSerializableExtra("AppState");
		set_Info = (Setting)intent.getSerializableExtra("Setting");
		
		mRoot = connect_Info.mRoot;
		rCurrent = mRoot;
		

		
		arFiles = new ArrayList<FileTable>();
		rAdapter = new ResourceAdapter(this, R.layout.ftp_list, arFiles);
		rFileList.setAdapter(rAdapter);
		rFileList.setOnItemClickListener(mItemClickListener);
		rFileList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		
		defaultResource();
	}
	
	AdapterView.OnItemClickListener mItemClickListener = new AdapterView.OnItemClickListener() { //디렉토리간 이동, 영화 및 음악 분류 후  액티비티 실행
		public void onItemClick(AdapterView<?> parent, View view, int position,	long id) {
			String Name = arFiles.get(position).getName();
						
			if(Name.startsWith("[")&& Name.endsWith("]")){
				Name = Name.substring(1,Name.length()-1);
				new ChangeDir().execute(Name);
			}else{
				Intent intent;
				FileTable fileTable = new FileTable();
				
				switch(fileExtension.value()){
				case 0:
				case 1:Log.d(TAG,"ETC");break;
				case 2:Log.d(TAG,"MUSIC");
										intent = new Intent(ResourceActivity.this, ResourceExeActivity.class);
										intent.putExtra("Setting", set_Info);
										intent.putExtra("Connect", connect_Info);
										intent.putExtra("FileState", fileState);
										intent.putExtra("FileExtension", fileExtension);
										
										fileTable = arFiles.get(position);
										intent.putExtra("FileTable", fileTable);
										startActivity(intent);
										
				break;
				case 3:Log.d(TAG,"MOVIE"); 
										intent = new Intent(ResourceActivity.this, ResourceExeActivity.class);
										intent.putExtra("Setting", set_Info);
										intent.putExtra("Connect", connect_Info);
										intent.putExtra("FileState", fileState);
										intent.putExtra("FileExtension", fileExtension);
										
										fileTable = arFiles.get(position);
										intent.putExtra("FileTable", fileTable);
										startActivity(intent);
				break;

				}
			}
			
				
			
		}  
	};
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.daction, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) { // 옵션메뉴 생성
		// TODO Auto-generated method stub
		switch (item.getItemId()) {
		case R.id.back:	defaultResource();
			break;

		}
		return false;
	}

	private class ChangeDir extends AsyncTask<String, Void, Integer> {  //using AsyncTask because of network
		@Override														//return value is using for opPostExecute
		protected Integer doInBackground(String... params) {
			// TODO Auto-generated method stub
			switch(params[0]){
			case "Music": extension = ".MP3,.Mp3,.mp3"; fileExtension = FileExtension.Music;
						  RefreshHandler.sendMessage(Message.obtain(RefreshHandler,1,"Music"));break;
			case "Movie": extension = ".MP4,.Mp4,.mp4"; fileExtension = FileExtension.Movie; 
						  RefreshHandler.sendMessage(Message.obtain(RefreshHandler,1,"Movie"));break;
				default:
			}
			
			
			findExtension(rCurrent); //확장자로 파일 분류
			
			return 1;
		}
		
		protected void onPostExecute(Integer value){
			fileState = FileState.NotInit;
		}
	}
	
	public void defaultResource(){ //기본 디렉토리 
		
		FileTable music = new FileTable("[Music]");
		FileTable movie = new FileTable("[Movie]");
		
		arFiles.clear();
		arFiles.add(music);
		arFiles.add(movie);
		
		
		RefreshHandler.sendMessage(Message.obtain(RefreshHandler,1,"Resource"));
		RefreshHandler.sendMessage(Message.obtain(RefreshHandler,2));
		
	}
	 
	private Handler RefreshHandler = new Handler(){  // Not Using MainThread 
		public void handleMessage(Message msg){
			switch(msg.what){
			case 1:rCurrnetTxt.setText((String)msg.obj);break;
			case 2:rAdapter.notifyDataSetChanged(); break;
			}
		};
	};
	
	public void findExtension(String current){
		if(fileState == FileState.NotInit){
			fileState = FileState.Init; 
			arFiles.clear();
		}
		try {
			
			FTPFile[] ftpFile = connect_Info.mFTPClient.listFiles(current);
			
			for(FTPFile file : ftpFile){
				if(file.isDirectory() && FTPUSER.equals(file.getUser())){ //디렉토리와 권한을 조건으로 재귀호출을 통해 모든 파일 조사
					findExtension(current+"/"+file.getName());
				}else{//파일이라면 확장자를 비교해 파일 분류 
					StringTokenizer st = new StringTokenizer(extension,",");
					while(st.hasMoreTokens()){
						if((file.getName()).endsWith(st.nextToken())){
							FileTable table = new FileTable();
							table.setName(file.getName());
							table.setSize(file.getSize());
							table.setPath(current +"/"+file.getName());
							arFiles.add(table);
						}
					}
				}
			}
			RefreshHandler.sendMessage(Message.obtain(RefreshHandler,2));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
}
