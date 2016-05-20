package com.example.mynas;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebActivity extends Activity {
	private final static String TAG = "In_WebActivity";
	private final static int TPort = 9091;
	private WebView webView;
	private Setting set_Info;
	
	
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.web);
		
		
		Intent intent =getIntent();
		set_Info = (Setting)intent.getSerializableExtra("Setting");
		
		
		webView = (WebView)findViewById(R.id.web);
		webView.setWebViewClient(new MyWebClient());
		WebSettings set = webView.getSettings();
		set.setJavaScriptEnabled(true);
		set.setBuiltInZoomControls(true);
		webView.loadUrl("http://" + set_Info.getHostName() + ":"+ set_Info.getHostTPort());
	}
	
	class MyWebClient extends WebViewClient{
		public boolean shouldOverrideUrlLoading(WebView view, String url){
			view.loadUrl(url);
			return true;
		}
	}
	
	protected void onPause(){
		super.onPause();
		finish();
	}
}
