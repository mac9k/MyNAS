package com.example.minidlna;

import java.util.ArrayList;

import com.example.mynas.R;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebPlayActivity extends Activity{
	private final static String TAG = "WebPlayActivity";
	private WebView webView;
	private String title,url;
	
	
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.web);
		
		Intent intent = getIntent();
		title = (String)intent.getStringExtra("Title");
		url = (String)intent.getStringExtra("Url");
		
		webView = (WebView)findViewById(R.id.web);
		webView.setWebViewClient(new MyWebClient());
		WebSettings set = webView.getSettings();
		set.setJavaScriptEnabled(true);
		set.setBuiltInZoomControls(true);
		
		Log.d(TAG,"Url : " + url.toString());
		String temp = url.toString();
		webView.loadUrl(temp);
	}
	
	class MyWebClient extends WebViewClient{
		public boolean shouldOverrideUrlLoading(WebView view, String url){
			view.loadUrl(url);
			return true;
		}
	}
	
	@Override
	protected void onPause(){
		super.onPause();
		webView.loadUrl("");
		finish();
	}
}
