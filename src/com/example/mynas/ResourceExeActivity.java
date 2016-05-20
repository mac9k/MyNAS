package com.example.mynas;

import com.example.streaming.GetProxy;
import com.example.streaming.moviePlayer;
import com.example.streaming.musicPlayer;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.MediaController;

public class ResourceExeActivity  extends Activity{
	private final static String TAG ="ResourceExeActivity";
	
	private static View frame1;
	private static moviePlayer movieView;
	private static musicPlayer musicView;
	private static Activity A;
	
	private Setting set_Info;
	private Connect connect_Info;
	private FileState fileState;
	private FileTable fileTable;
	private FileExtension fileExtension;
	
	private static String videoUrl;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.onresource);
		A = this;
		
		Intent intent = getIntent();
		connect_Info = (Connect) intent.getSerializableExtra("Connect");
		fileState = (FileState)intent.getSerializableExtra("FileState");
		set_Info = (Setting)intent.getSerializableExtra("Setting");
		fileTable = (FileTable)intent.getSerializableExtra("FileTable");
		fileExtension = (FileExtension)intent.getSerializableExtra("FileExtension");
		
		videoUrl = "ftp://"+set_Info.getHostName() +"/" + fileTable.getPath();
		
		getFragmentManager().beginTransaction().add(R.id.container, new PlaceholderFragment(connect_Info, set_Info, fileExtension , fileTable)).commit();
		
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onPause() {
		super.onPause(); 
		if(fileExtension == FileExtension.Movie){
			movieView.pause();
			movieView.release();
		}else if(fileExtension == FileExtension.Music){
			musicView.pause();
			musicView.release();
		}
	}  

	@Override
	public void onResume() {
		if(fileExtension == FileExtension.Movie){
			if (movieView != null) {
				movieView.setDisplay();
			}
		}else if(fileExtension == FileExtension.Music){
			if(musicView != null){
			}
		}
		super.onResume();

	}
	
	public void someMethodThatUsesActivity(Activity myActivityReference) {
	    myActivityReference.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
	}

	
	public static class PlaceholderFragment extends Fragment {
		static private final int BUFFER_SIZE = 0;// Mb
		static private final int NUM_FILES = 0;// Count files in cache dir
		private GetProxy proxy;
		private Connect connect;
		private Setting set;
		private FileExtension fileExtension;
		private FileTable table;
		private View rootView;
		
		public PlaceholderFragment(Connect connect_Info, Setting set_Info, FileExtension fileExtension, FileTable fileTable) {
			// TODO Auto-generated constructor stub
			this.connect = connect_Info;
			this.set = set_Info;
			this.fileExtension = fileExtension;
			this.table = fileTable;
		}
		

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,	Bundle savedInstanceState) {

			if(fileExtension == FileExtension.Movie){
				getActivity().getActionBar().hide();
				
				rootView = inflater.inflate(R.layout.movie, container,	false);	
				frame1 = rootView.findViewById(R.id.frame1);
				movieView = (moviePlayer) rootView.findViewById(R.id.movieView);
				
				proxy = new GetProxy();//��ü��
				proxy.setPaths("/ProxyBuffer", videoUrl, BUFFER_SIZE, NUM_FILES);
				String proxyUrl = proxy.getPathURL(connect, set);	 // ������ ��ġ�� Url�� �����Ѵ�.
				movieView.setVideoPath(proxyUrl);  // movieView�� Url ��� 
				movieView.setMediaController(new mediac(A, frame1)); // media ��Ʈ�ѷ� ���
				movieView.setRotation(90);
				movieView.setTranslationX(-312f);
				movieView.setTranslationY(312f);

				movieView.setSeekListener(new moviePlayer.SeekListener() {
					@Override
					public void onSeek(int msec) {
					}

					@Override
					public void onSeekComplete(MediaPlayer mp) {
					}
				});
				
				movieView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
					@Override
					public void onPrepared(final MediaPlayer mp) {
						Log.d(TAG,"MovieView Start");
						movieView.start();
						}
				});
				
			}else if(fileExtension == FileExtension.Music){
				
				rootView = inflater.inflate(R.layout.music, container,	false);			
				frame1 = rootView.findViewById(R.id.frame1);
				musicView = (musicPlayer) rootView.findViewById(R.id.musicView);
				
				
				proxy = new GetProxy();
				proxy.setPaths("/ProxyBuffer", videoUrl, BUFFER_SIZE, NUM_FILES);
				String proxyUrl = proxy.getPathURL(connect, set);				
				musicView.setAudioPath(proxyUrl);
				musicView.setMediaController(new mediac(A, frame1));
				musicView.setSeekListener(new musicPlayer.SeekListener() {
					@Override
					public void onSeek(int msec) {
					}
					@Override
					public void onSeekComplete(MediaPlayer mp) {
					}
				});
				musicView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
					@Override
					public void onPrepared(final MediaPlayer mp) {
						Log.d(TAG,"MusicView Start");
						musicView.start();
						}
				});
			}
			
			return rootView;
		}
	

		public void onPause(){
			super.onPause();
			proxy.stopProxy();
			Log.d(TAG,"In Fragment OnPause");
		}
		
		public void onDestroy(){
			super.onDestroy();
		}
		
		public void onResume(){
			super.onResume();
		}
		
		public class mediac extends MediaController {
			public mediac(Context context, View anchor) {
				super(context);
				super.setAnchorView(anchor);
			}

			@Override
			public void setAnchorView(View view) {
			}
		}
		
	}
}
