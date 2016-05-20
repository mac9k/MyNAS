package com.example.mynas;

import java.io.File;
import java.util.ArrayList;

import org.apache.commons.net.io.CopyStreamAdapter;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
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

public class DeviceActivity extends Activity {
	private static final String TAG = "In_DeviceActivity";

	private static final int progress_bar_type = 0 ;
	private static final String progress_title = "0";
	private static final String progress_percect = "1";

	private static final int Existing = 2;
	private static final int Success = 3;
	
	private ProgressDialog pDialog;
	private String dCurrent, dRoot;
	private TextView dCurrentTxt;
	private ListView dFileList;
	private DeviceAdapter dAdapter;
	private ArrayList<FileTable> arFiles;
	private AppState appState;
	private Connect connect_Info;
	private Setting	 set_Info;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.device);

		dCurrentTxt = (TextView) findViewById(R.id.device_path);
		dFileList = (ListView) findViewById(R.id.deviceList);

		arFiles = new ArrayList<FileTable>();
		dRoot = Environment.getExternalStorageDirectory().getAbsolutePath();
		dCurrent = dRoot;

		dAdapter = new DeviceAdapter(this, R.layout.device_list, arFiles);
		dFileList.setAdapter(dAdapter);
		dFileList.setOnItemClickListener(mItemClickListener);
		dFileList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);          
		dFileList.setMultiChoiceModeListener(new ModeCallback());

		Intent intent = getIntent();										//intent�� �̿��� activity�� ���
		appState = (AppState) intent.getSerializableExtra("AppState");
		connect_Info = (Connect) intent.getSerializableExtra("Connect");
		set_Info = (Setting) intent.getSerializableExtra("Setting");

		refreshFiles();
	}

	AdapterView.OnItemClickListener mItemClickListener = new AdapterView.OnItemClickListener() { //���丮�� �̵�
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			String Name = arFiles.get(position).getName();
			if (Name.startsWith("[") && Name.endsWith("]")) {
				Name = Name.substring(1, Name.length() - 1);
			}
			String Path = dCurrent + "/" + Name;
			File f = new File(Path);
			if (f.isDirectory()) {
				dCurrent = Path;
				refreshFiles();
			}
		}
	};

	private class ModeCallback implements ListView.MultiChoiceModeListener { // �׼ǹ� ����(����Ʈ LongClick�� Ȱ��ȭ)

		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.dmenu, menu);
			mode.setTitle("Select Items");
			
			return true;
		}

		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return true;
		}

		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			SparseBooleanArray selected;
			switch (item.getItemId()) {
			case R.id.delete: // ���õ� ���� ����
							selected = dAdapter.getSelectedIds();
							for (int i = (selected.size() - 1); i >= 0; i--) {
								if (selected.valueAt(i)) {
									String selecteditem = dAdapter.getItem(selected.keyAt(i)).getName();
									File file =new File(dCurrent+"/"+selecteditem);
									if(file.isFile()){
										file.delete();
									}else {//������ �ƴ� �� ���� �Ұ�
										Toast.makeText(DeviceActivity.this,"Directory is not deleted.", Toast.LENGTH_SHORT).show();
										file.exists();
									}
									
								}
							}
						refreshFiles();
						mode.finish();
				break;
				
			case R.id.upload:  //���õ� ���� ���ε�
							selected = dAdapter.getSelectedIds();
							Log.d(TAG,"selected Count : " + dAdapter.getSelectedCount());
							if(appState==AppState.Connect)new UploadFile().execute(selected);
							else Toast.makeText(DeviceActivity.this,"DisConnection", Toast.LENGTH_SHORT).show();
							mode.finish();
							break;
				
			default:
					
			}
			return true;
		}

		public void onDestroyActionMode(ActionMode mode) {
		}

		public void onItemCheckedStateChanged(ActionMode mode, int position,long id, boolean checked) { // ���õ� ���� ī��Ʈ
			final int checkedCount = dFileList.getCheckedItemCount();
			switch (checkedCount) {
			case 0:
				mode.setSubtitle(null);
				break;
			default:
				mode.setSubtitle("" + checkedCount + " items selected");
				break;
			}
			dAdapter.toggleSelection(position);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) { // �ɼǸ޴� ����
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.daction, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {// �ɼǸ޴� ��� 
		switch (item.getItemId()) {
		case R.id.back: // ���� ���丮�� �̵�
			if (dCurrent.compareTo(dRoot) != 0) {
				int end = dCurrent.lastIndexOf("/");
				String upPath = dCurrent.substring(0, end);
				dCurrent = upPath;
				refreshFiles();
			} else
				Toast.makeText(DeviceActivity.this, "Home", Toast.LENGTH_SHORT)
						.show();
			break;

		}
		return false;
	}
	
	public void refreshFiles() {  //���ϸ�� �ʱ�ȭ, ����Ʈ �ۼ� �� ������Ʈ
		dCurrentTxt.setText(dCurrent);
		arFiles.clear();
		File current = new File(dCurrent);
		String[] files = current.list();
		

		if (files != null) {
			for (int i = 0; i < files.length; i++) {
				String Path = dCurrent + "/" + files[i];
				FileTable table = new FileTable();
				table.setName("");
				File f = new File(Path);
				if (f.isDirectory()) {
					table.setName("[" + files[i] + "]");
					table.setSize(0);
				} else{
					table.setName(files[i]);
					table.setSize(f.length());
				}
				arFiles.add(table);
			}
		}
		dAdapter.notifyDataSetChanged();
	}

	class UploadFile extends AsyncTask<SparseBooleanArray, String, UploadState> { //��Ʈ��ũ�� ����ϱ� ������ AsyncTask�� ����

		protected void onPreExecute() {
			super.onPreExecute();
			showDialog(progress_bar_type);
		}

		@Override
		protected UploadState doInBackground(SparseBooleanArray... params) {
			// TODO Auto-generated method stub
			UploadState uploadState = new UploadState();
			for (int i = (params[0].size() - 1); i >= 0; i--) {
				if (params[0].valueAt(i)) {
					String selecteditem = dAdapter.getItem(params[0].keyAt(i)).getName();
					final File file = new File(dCurrent + "/" + selecteditem);
					
					// �������� ������ ���������� ���Ͽ� Dialog ������Ʈ 
					CopyStreamAdapter streamListener = new CopyStreamAdapter() {
						@Override
						public void bytesTransferred(long totalBytesTransferred,int bytesTransferred, long streamSize) {
							int percent = (int)(totalBytesTransferred * 100 / file.length());
							publishProgress(progress_percect,Integer.toString(percent));
						}
					};
					connect_Info.mFTPClient.setCopyStreamListener(streamListener);

					if(file.isFile()){//������ �´ٸ� ���ε� ����
						publishProgress(progress_title,selecteditem);
						publishProgress(progress_percect,Integer.toString(0));
						uploadState .state = connect_Info.ftpUpload(dCurrent + '/'+ selecteditem, selecteditem,set_Info.getHostFPath());
						uploadState.item = selecteditem;
					}

				}
			}
			dAdapter.clearItemsIds();
			return uploadState;
		}
		
		protected void onProgressUpdate(String... progress) {
			switch (progress[0]) {
			case progress_title:	pDialog.setTitle(progress[1]);  // Title ���
				break;
			case progress_percect:	pDialog.setProgress(Integer.parseInt(progress[1])); // �����Ȳ ���	
				break;
			default:
				break;
			}
		}

		protected void onPostExecute(UploadState uploadState) {
			dismissDialog(progress_bar_type);
			switch(uploadState.state){
			case Existing : Toast.makeText(DeviceActivity.this, "is existing.", Toast.LENGTH_SHORT).show();
				break;
			case Success : Toast.makeText(DeviceActivity.this, "Upload is successed.", Toast.LENGTH_SHORT).show();
				break;
			default :Toast.makeText(DeviceActivity.this, "is  Directory.", Toast.LENGTH_SHORT).show();
			}
		}
	}
	
	protected Dialog onCreateDialog(int id) {
	    switch (id) {
	    case progress_bar_type:
	        pDialog = new ProgressDialog(this);
	        pDialog.setTitle("Title");
	        pDialog.setMessage("Uploading file. Please wait...");
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
	
	public class UploadState{
		String item;
		int state;
		public UploadState() {}
	}
	
	
	protected void onPause(){
		super.onPause();
		finish();
	}

}
