package com.example.streaming;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import com.example.mynas.Connect;
import com.example.mynas.Setting;
import com.example.streaming.HttpParser.ProxyRequest;

import android.net.Uri;
import android.os.Environment;
import android.util.Log;

public class GetProxy {

	private final static String LOCAL_IP_ADDRESS = "127.0.0.1";
	private final static String TAG = "HttpGetProxy";
	private final static String postfix = "-ml";

	private int remotePort = -1;
	private long urlSize = 0;
	private int localPort;
	private ServerSocket localServer = null;
	private String mUrl, remoteHost, remotePath;
	private String mMediaFilePath, newPath,  file_name, tempFolder;
	private File file_path, file_path_temp;
	private Proxy proxy = null; 
	private boolean startProxy, error = false, ftpEnable = false;
	private Thread prox;
	private FTPClient subFTPClient;
	private InputStream ftp = null;
	private Uri originalURI;
	
	private Connect connect;
	private Setting set;
	
	public GetProxy() { // 서버소켓 초기화 및 시작
		try{
			localServer = new ServerSocket(0, 1, InetAddress.getByName(LOCAL_IP_ADDRESS));
			localPort = localServer.getLocalPort(); // 랜덤으로 포트 번호 할당	
			startProxy();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void startProxy() {
		startProxy = true;
		prox = new Thread() { 
			public void run() {
				while (startProxy) {
					try {
						if (proxy != null){
							proxy.closeSockets();
						}
						Socket player = localServer.accept();  //MediaPlayer의 접속을 대기 (Lock)
						proxy = new Proxy(player);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		};
		prox.start();
	}  

	public void stopProxy(){
		startProxy = false;
		try {
			if (localServer != null) {
				localServer.close();
				localServer = null;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getPathURL(Connect connect, Setting set) {
		File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + tempFolder + "/" + file_name);
		if (file.exists() == true) {
			Log.d(TAG,"File is existed in SDCard.");
			return file.getAbsolutePath();
		} else {
			Log.d(TAG,"File is existed in FTPServer.");
			originalURI = Uri.parse(mUrl);
			remoteHost = originalURI.getHost();
			remotePath = originalURI.getPath();
			String localUrl = mUrl.replace(remoteHost, LOCAL_IP_ADDRESS + ":" + localPort);
			
			if (localUrl.contains("ftp://") == true) 
				localUrl = localUrl.replaceFirst("ftp://", "http://");
			
			ftpEnable = true;
			
			this.connect = connect;
			this.set = set;
			subFTPClient = this.connect.mFTPClient;
			return localUrl;
		}
	}

	public void setPaths(String dirPath, String url, int maxSize, int maxnum) {
		tempFolder = dirPath;
		dirPath = Environment.getExternalStorageDirectory().getAbsolutePath()+ dirPath;
		new File(dirPath).mkdirs();
		long maxsize1 = maxSize * 1024L * 1024L;
		mUrl = url;
		file_name = Uri.decode(mUrl.substring(mUrl.lastIndexOf("/") + 1)); 
		mMediaFilePath = dirPath + "/" + file_name; 
		file_path = new File(mMediaFilePath);
		file_path_temp = new File(dirPath + "/" + file_name + postfix);
		error = false;
		
		asynRemoveBufferFile(dirPath, maxsize1, maxnum, postfix , file_path_temp.getPath());
	}
	
	public void asynRemoveBufferFile(final String dirPath, final long maxSize,final int maxNum, 
			final String temp, final String current) { // 디렉토리 정리, 100mb, 최대 파일 갯수 20개
		new Thread() {
			public void run() {
				List<File> lstBufferFile = getFilesSortByDate(dirPath); // 수정시간 오름 순으로 정렬
				
				for (int i = 0; i < lstBufferFile.size(); i++) { // postfix, 현재파일과 같다면 삭제
					if ((lstBufferFile.get(i).getName().endsWith(temp))	&& (!lstBufferFile.get(i).getPath().equals(current))) {
						lstBufferFile.get(i).delete();
						lstBufferFile.remove(i);
					}
				}
				
				while (lstBufferFile.size() > maxNum) { 
					lstBufferFile.get(0).delete();
					lstBufferFile.remove(0);
				}
				long size = maxSize + 1;
				while (size > maxSize) {
					size = 0;
					for (int i = 0; i < lstBufferFile.size(); i++) {
						size = size + lstBufferFile.get(i).length();
					}
					if ((size > maxSize) && (lstBufferFile.size() > 1)) {
						lstBufferFile.get(0).delete();
						lstBufferFile.remove(0);
					}
				}
			}
		}.start();
	}
	
	static private List<File> getFilesSortByDate(String dirPath) {
		List<File> result = new ArrayList<File>();
		File dir = new File(dirPath);
		File[] files = dir.listFiles();
		if (files == null || files.length == 0)
			return result;
		Arrays.sort(files, new Comparator<File>() {
			public int compare(File f1, File f2) {
				return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
			}
		});
		for (int i = 0; i < files.length; i++) {
			result.add(files[i]);
		}
		return result;
	}




	private class Proxy {
		private Socket sckPlayer = null;   
		private HttpParser httpParser = null; 
		private int bytes_read;
		private byte[] file_buffer = new byte[1448];
		private byte[] local_request = new byte[1024];
		private byte[] remote_reply = new byte[1448 * 50];
		private ProxyRequest request = null;
		private boolean isExists = false;
		private String header = "";
		private RandomAccessFile  fInputStream = null, fOutputStream=null;
		private FTPFile[] files = null;

		public Proxy(Socket sckPlayer) {
			this.sckPlayer = sckPlayer;// 127.0.0.1
			run();
		}


		public void closeSockets() {
			try {
				if (sckPlayer != null) {
					sckPlayer.close();
					sckPlayer = null;
				}
				if(ftp != null) {
					try {
						subFTPClient.abort();
					} catch (IOException e) {
						e.printStackTrace();
						subFTPClient.disconnect();
					}
					ftp.close();
					ftp = null;
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private boolean connect() {//Media 파일을 retrieve 할 때마다 접속하기 위한 메서드
			boolean result = false;
			try {
				if ((subFTPClient != null) && (subFTPClient.isConnected() == true)) {
					subFTPClient.disconnect();
				}
				subFTPClient = new FTPClient();
				subFTPClient.connect(remoteHost, 9090);
				subFTPClient.enterLocalPassiveMode();
				if (FTPReply.isPositiveCompletion(subFTPClient.getReplyCode())) {
					result = subFTPClient.login(set.getHostId()	, set.getHostPassword());
					subFTPClient.setControlEncoding("UTF-8");
					subFTPClient.setAutodetectUTF8(true);
					subFTPClient.setFileType(FTP.BINARY_FILE_TYPE);
					subFTPClient.setBufferSize(1024*1024);
				}
				subFTPClient.setSoTimeout(5000);
			} catch (Exception e) {
				e.printStackTrace();
			} 
			return result;
		}
		
		private void complete(){
			try{
				if(fOutputStream != null){
					fOutputStream.close();
				}
				String str = file_path_temp.getAbsolutePath();
				File isDone = new File(str);

				if(urlSize == isDone.length()){
					File moveFile = new File(mMediaFilePath);
					isDone.renameTo(moveFile);
					Log.d(TAG,"Remove Postfix");
				}
				
			}catch(IOException e){
				e.printStackTrace();
			} 
		}
		
		private void readFromServer(){
			if ((subFTPClient == null) || (subFTPClient.isConnected() != true)) {			
				if (connect() == false) {
					error = true; 	
				}
			}
			try {					
				fOutputStream = new RandomAccessFile(file_path_temp, "rwd");
				
				if (urlSize == 0) {		
					files = subFTPClient.listFiles(remotePath);
					if (files.length == 1 && files[0].isFile()) {
						urlSize = files[0].getSize();
					}
				}
				if (request._rangePosition > 0) {
					subFTPClient.setRestartOffset(request._rangePosition);
					if (fOutputStream != null) {
						fOutputStream.seek(request._rangePosition);
					}
				}  
				ftp = subFTPClient.retrieveFileStream(remotePath);
			} catch (Exception e) {
				e.printStackTrace();
				Log.d(TAG,"Time Set1");
				ftp = null;
			}
			
			error = false;
			try {
				if ((request._rangePosition > 0) || (request._overRange == true)) {
					header = "HTTP/1.1 206 Partial Content\r\nAccept-Ranges: bytes\r\nContent-Length: "
							+ Long.toString(urlSize - request._rangePosition)
							+ "\r\nContent-Range: bytes "
							+ Long.toString(request._rangePosition)
							+ "-"
							+ Long.toString(urlSize - 1)
							+ "/" 
							+ urlSize
							+ "\r\nContent-Type: application/octet-stream\r\n\r\n";
				} else {
						header = "HTTP/1.1 200 OK\r\nAccept-Ranges: bytes\r\nContent-Length: "
							+ Long.toString(urlSize)
							+ "\r\nContent-Disposition: attachment\r\nContent-Type: application/octet-stream\r\n\r\n";
				} 
									
				sckPlayer.getOutputStream().write(header.getBytes(), 0, header.length());
				Log.d(TAG,"Header : " + header);			
				while ((ftp != null) && ((bytes_read = ftp.read(remote_reply)) != -1)&& (mMediaFilePath == newPath)) {
					sckPlayer.getOutputStream().write(remote_reply, 0, bytes_read);
					fOutputStream.write(remote_reply, 0, bytes_read);
				}
				sckPlayer.getOutputStream().flush();
			} catch (Exception e) {
				e.printStackTrace();
				
			} 
		
		}


		public void run() {
			if (mMediaFilePath != newPath) {
				error = false;
				urlSize = 0;
				newPath = mMediaFilePath;
			} 

			try {
				httpParser = new HttpParser(remoteHost, remotePort,	LOCAL_IP_ADDRESS, localPort);
				while (((bytes_read = sckPlayer.getInputStream().read(local_request)) != -1) && (mMediaFilePath == newPath)) {
					byte[] buffer = httpParser.getRequestBody(local_request,bytes_read);
					if (buffer != null) {
						request = httpParser.getProxyRequest(buffer, urlSize);
						break;
					}
				}   
 
			} catch (Exception e) {
				e.printStackTrace();
			}
			 
			isExists = file_path.exists();

			if (isExists){
				closeSockets();
				return;
			}else if ((ftpEnable == true)){
				readFromServer(); 
			}
			
			closeSockets();
			complete();
			
			
		}
		
	}	
}
