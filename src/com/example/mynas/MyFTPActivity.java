package com.example.mynas;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.io.CopyStreamAdapter;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MyFTPActivity extends Activity {

	private static final String TAG = "In_MYFTP";
	private static final String FTPUSER = "50";  // /etc/password FTP number
	private static final int progress_bar_type = 0 ;
	
	private static final String progress_title = "0";
	private static final String progress_percect = "1";
	
	private static final int Existing = 2;
	private static final int Success = 3;
	
	
	private ProgressDialog pDialog;
	
	private ListView fFileList;
	private TextView fCurrnetTxt;
	private ArrayList<FileTable> arFiles;
	private MyFTPAdapter fAdapter;
	private String fCurrent, mRoot;
	private Connect connect_Info;
	private AppState appState;
	private Setting set_Info;
	Integer Index;
	FTPFile selectFile;
	@Override 
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.device);

		fCurrnetTxt = (TextView) findViewById(R.id.device_path);
		fFileList = (ListView) findViewById(R.id.deviceList);

		Intent intent = getIntent();
		connect_Info = (Connect) intent.getSerializableExtra("Connect");
		mRoot = connect_Info.mRoot;
		fCurrent = mRoot;
		
		appState = (AppState)intent.getSerializableExtra("AppState");
		set_Info = (Setting)intent.getSerializableExtra("Setting");
		
		arFiles = new ArrayList<FileTable>();
		fAdapter = new MyFTPAdapter(this, R.layout.ftp_list, arFiles);
		fFileList.setAdapter(fAdapter);
		fFileList.setOnItemClickListener(mItemClickListener);
		fFileList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		fFileList.setMultiChoiceModeListener(new ModeCallback());

		new ShowFileList().execute();
	}

	AdapterView.OnItemClickListener mItemClickListener = new AdapterView.OnItemClickListener() { //디렉토리간 이동
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			String Name = arFiles.get(position).getName();
				if(Name.startsWith("[")&& Name.endsWith("]")){
					Name = Name.substring(1,Name.length()-1);
				}
				String Path = fCurrent +"/"+  Name;
				Index = position;
				new ChangeDir().execute(Path);
				new ShowFileList().execute();
			}  
	};

	private class ModeCallback implements ListView.MultiChoiceModeListener {//액션바 생성(리스트 LongClick시 활성화)

		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.fmenu, menu);
			mode.setTitle("Select Items");
			return true;
		}

		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {		
			return true;
		}

		public boolean onActionItemClicked(ActionMode mode, MenuItem item) { 
			SparseBooleanArray selected;
			switch (item.getItemId()) {
			case R.id.delete:// 선택된 파일 삭제
				selected = fAdapter.getSelectedIds();
				new DeleteFile().execute(selected);
				new ShowFileList().execute();
				mode.finish();
				break;
			case R.id.download://선택된 파일 업로드
				selected = fAdapter.getSelectedIds();
				new DownloadFile().execute(selected);
				mode.finish();
				break;
			default:
				break;
			}
			return true;
		}

		public void onDestroyActionMode(ActionMode mode) {
		}

		public void onItemCheckedStateChanged(ActionMode mode, int position,long id, boolean checked) { // 선택된 파일 카운트
			final int checkedCount = fFileList.getCheckedItemCount();
			switch (checkedCount) {
			case 0:
				mode.setSubtitle(null);
				break;
			default:
				mode.setSubtitle("" + checkedCount + " items selected");
				break;
			}
			fAdapter.toggleSelection(position);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {// 옵션메뉴 기능
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.daction, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		switch (item.getItemId()) {
		case R.id.back:// 이전 디렉토리로 이동
			if (fCurrent.compareTo(mRoot) != 0) {
				int end = fCurrent.lastIndexOf("/");
				String upPath = fCurrent.substring(0, end);
				fCurrent = upPath;
				new ChangeDir().execute(fCurrent);
				new ShowFileList().execute();
			} else
				Toast.makeText(MyFTPActivity.this, "Home", Toast.LENGTH_SHORT).show();
			break;

		}
		return false;
	}

	private class ShowFileList extends AsyncTask<Void, Void, Void> {//네트워크를 사용하기 때문에 AsyncTask로 구현
		@Override
		protected Void doInBackground(Void... params) {
			refreshFiles();
			return null;
		}
	}
	
	private class DownloadFile extends AsyncTask<SparseBooleanArray, String, DownloadState>{//네트워크를 사용하기 때문에 AsyncTask로 구현
		
		protected void onPreExecute() {
			super.onPreExecute();
			showDialog(progress_bar_type);
		}
		
		@Override
		protected DownloadState doInBackground(SparseBooleanArray... params) {
			// TODO Auto-generated method stub
			DownloadState downloadState = new DownloadState();
			for (int i = (params[0].size() - 1); i >= 0; i--) {
				if (params[0].valueAt(i)) {
					String selectedItem = fAdapter.getItem(params[0].keyAt(i)).getName();
					try {
						final FTPFile ftpFile = connect_Info.mFTPClient.listFiles(fCurrent)[params[0].keyAt(i)];
						// 데이터의 변동이 있을때마다 콜하여 Dialog 업데이트 
						CopyStreamAdapter streamListener = new CopyStreamAdapter() {
							@Override
							public void bytesTransferred(long totalBytesTransferred,int bytesTransferred, long streamSize) {
								int percent = (int)(totalBytesTransferred * 100 / ftpFile.getSize());
								publishProgress(progress_percect,Integer.toString(percent));
							}
						};
						connect_Info.mFTPClient.setCopyStreamListener(streamListener);
						
						if(ftpFile.isFile()){
							publishProgress(progress_title,selectedItem);
							publishProgress(progress_percect,Integer.toString(0));
							downloadState.state=connect_Info.ftpDownload(fCurrent+'/'+selectedItem, selectedItem, set_Info.getHostDPath());
							downloadState.item = selectedItem;
						}
						
					}catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}

			fAdapter.clearItemsIds();
			return downloadState;
		}
		
		protected void onProgressUpdate(String... progress) {
			// setting progress percentage
			switch (progress[0]) {
			case progress_title:	pDialog.setTitle(progress[1]);// Title 등록
				break;
			case progress_percect:	pDialog.setProgress(Integer.parseInt(progress[1]));	 // 진행상황 등록	
				break;
			default:
				break;
			}
		}
		
		protected void onPostExecute(DownloadState downloadState) {
			dismissDialog(progress_bar_type);
			switch(downloadState.state){
			case Existing : Toast.makeText(MyFTPActivity.this, "is existing.", Toast.LENGTH_SHORT).show();
				break;
			case Success : Toast.makeText(MyFTPActivity.this, "Download is successed.", Toast.LENGTH_SHORT).show();
				break;
				
			default :Toast.makeText(MyFTPActivity.this, "is  Directory.", Toast.LENGTH_SHORT).show();
			}
		}
		
	}
	
	private class DeleteFile extends AsyncTask<SparseBooleanArray, Void, Void> {//네트워크를 사용하기 때문에 AsyncTask로 구현

		@Override
		protected Void doInBackground(SparseBooleanArray... params) {
			// TODO Auto-generated method stub
			for (int i = (params[0].size() - 1); i >= 0; i--) {
				String selectedItem = fAdapter.getItem(params[0].keyAt(i)).getName();
				try {
					FTPFile ftpFile = connect_Info.mFTPClient.listFiles(fCurrent)[params[0].keyAt(i)];
					if (ftpFile.isFile())
						connect_Info.ftpRemoveFile(fCurrent + "/"+ selectedItem);
					else
						Toast.makeText(MyFTPActivity.this,"Directory is not deleted.", Toast.LENGTH_SHORT).show();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
			return null;
		}

	}
	 
	private class ChangeDir extends AsyncTask<String, Void, Void> {//네트워크를 사용하기 때문에 AsyncTask로 구현
		@Override
		protected Void doInBackground(String... params) {
			// TODO Auto-generated method stub
			try {
				FTPFile ftpFile = connect_Info.mFTPClient.listFiles(fCurrent)[Index];
				if (ftpFile.isDirectory() && FTPUSER.equals(ftpFile.getUser())) {
					fCurrent = params[0];
					connect_Info.ftpChangeDirectory(fCurrent);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}
	}
	
	private Handler RefreshHandler = new Handler(){//네트워크를 사용하기 때문에  Handler로 구현
		public void handleMessage(Message msg){
			switch(msg.what){
			case 1:fCurrnetTxt.setText(fCurrent);break;
			case 2:fAdapter.notifyDataSetChanged();break;
			}
		};
	};

	public void refreshFiles() { //파일목록 초기화, 리스트 작성 및 업데이트
		try {
			RefreshHandler.sendMessage(Message.obtain(RefreshHandler,1));
			arFiles.clear();
			FTPFile[] ftpFiles = connect_Info.mFTPClient.listFiles(fCurrent);
			int length = ftpFiles.length;
						
			for (int i = 0; i < length; i++) {
				String Name = ftpFiles[i].getName();
				FileTable table =new FileTable();	
				boolean isFile = ftpFiles[i].isFile();
				if (!isFile){
					table.setName("[" + Name + "]");
					table.setSize(0);
				}else{
					table.setName(Name);
					table.setSize(ftpFiles[i].getSize());
				}
				arFiles.add(table);
				RefreshHandler.sendMessage(Message.obtain(RefreshHandler,2));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected Dialog onCreateDialog(int id) {
	    switch (id) {
	    case progress_bar_type:
	        pDialog = new ProgressDialog(this);
	        pDialog.setTitle("Title");
	        pDialog.setMessage("Downloading file. Please wait...");
	        pDialog.setIndeterminate(false);
	        pDialog.setMax(100);
	        pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
	        pDialog.setCancelable(false);
	        pDialog.show();
	        return pDialog;
	    default:
	        return null;
	    }
	}
	
	protected void onPause(){
		super.onPause();
		finish();
	}
	
	
	public class DownloadState{
		String item;
		int state;
		public DownloadState() {}
	}
	

}
