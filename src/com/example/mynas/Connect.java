package com.example.mynas;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import android.util.Log;

public class Connect implements Serializable{	
	private static final long serialVersionUID=-1231252353;  // Class Serial ID
	private final static String TAG = "In_Connect";
	
	private static final int Directory = 1;
	private static final int Existing = 2;
	private static final int Success = 3;
	
	public static String FtpCurrent, deviceCurrent;
	public static FTPClient mFTPClient = null;
	public static final int progress_bar_type = 0 ;
	private ArrayList<String> arFiles;
	public String mRoot,fullPath,fileName;
	public boolean ftpConnect(String host, String username,String password, int port){
	    try {
	        mFTPClient = new FTPClient();	//FTP Client 객체 생성
	        mFTPClient.connect(host,port);	//서버 접속
            mFTPClient.enterLocalPassiveMode();//FTP를 Passive mode 로 접속 
	        if (FTPReply.isPositiveCompletion(mFTPClient.getReplyCode())) {  //응답코드가 정상이면 수행
	            boolean status = mFTPClient.login(username, password); // 사용자 인증
	            mFTPClient.setControlEncoding("UTF-8");
	            mFTPClient.setAutodetectUTF8(true);
	            mFTPClient.setFileType(FTP.BINARY_FILE_TYPE);// 파일 형태 설정
	    		FtpCurrent = ftpGetCurrentWorkingDirectory(); // 접속 주소 리턴
	    		mFTPClient.setBufferSize(1024*1024); // 버퍼사이즈
	    		mFTPClient.setSoTimeout(10000); // 커넥션 타임아웃 
	    		mRoot= FtpCurrent;   		
	    		
	            return status;
	        }
	    } catch(Exception e) {
	        Log.d(TAG, "Error: ftpConnect() " );
	        e.printStackTrace();
	    }
	    return false;
	}
	
	public static boolean ftpDisconnect(){
	    try {
	        mFTPClient.logout();  // 로그아웃
	        mFTPClient.disconnect(); //접속종료
	        
	        return true;
	    } catch (Exception e) {
	        Log.d(TAG, "Error : ftpDisconnect()");
	        e.printStackTrace();
	    }
	    return false;
	}

	public static String ftpGetCurrentWorkingDirectory(){
	    try {
	        String workingDir = mFTPClient.printWorkingDirectory();  //현 위치 디렉토리 리턴
	        return workingDir;
	    } catch(Exception e) {
	        Log.d(TAG, "Error : ftpGetCurrentWorkingDirectory()");
	    }

	    return null;
	}

	public static boolean ftpChangeDirectory(String directory_path){
	    try {
	        mFTPClient.changeWorkingDirectory(directory_path); // 디렉토리 간 이동
	    } catch(Exception e) {
	        Log.d(TAG, "Error: ftpChangeDirectory()");
	        e.printStackTrace();
	    }

	    return true;
	}

	public boolean ftpRemoveFile(String filePath){
	    try {
	        boolean status = mFTPClient.deleteFile(filePath);  // 파일 삭제
	        return status;
	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	    return false;
	}

	public static int ftpDownload(String srcFilePath, String desFileName, String desFilePath)
	{
	    int status = Success;
	    try {
	    	File dCurrent = new File(desFilePath); // 디렉토리 객체 생성
	    	String[] deviceFiles = dCurrent.list(); // 현 디렉토리 파일 목록들 리턴 
	    	int length = deviceFiles.length;
	    	boolean isRepeated = false;
	    	
	    	for(int i = 0 ; i < length; i++){  // 중복 체크
	    		String name = deviceFiles[i];
	    		if(name.contains(desFileName) && !isRepeated){
	    			isRepeated = true;
	    			status = Existing;
	    			break;
	    		}
	    	}
	    	
	    	if(!isRepeated){ // 중복되지 않는다면 파일 지정한 경로에 파일 다운로드 시작
		    	FileOutputStream desFileStream = new FileOutputStream( desFilePath+"/"+desFileName);
		    	mFTPClient.retrieveFile(srcFilePath, desFileStream);
		        desFileStream.close();	         
	    	}else 
	    		Log.d(TAG,"Error : File is existed");
	    	
	        return status; 
	    } catch (IOException e) {
	        Log.d(TAG, "Error : ftpDownload()");
	        e.printStackTrace();
	    }

	    return Directory;
	}

	public int ftpUpload(String srcFilePath, String desFileName,String desDirectory){
	    int status = Success;
		try {
			String[] ftpFiles = mFTPClient.listNames(desDirectory);  // 디렉토리 객체  생성
			int length = ftpFiles.length;							
			boolean isRepeated = false;
			
			for (int i = 0; i < length; i++) {  // 중복체크
				String name = ftpFiles[i];
				if (name.contains(desFileName) && !isRepeated){
					isRepeated = true;
					status = Existing;
					break;
				}
			} 
	        
	        if (ftpChangeDirectory(desDirectory)&& !isRepeated){  //중복되지 않고 해당 경로로 이동가능하면 다운로드 시작
	            FileInputStream srcFileStream = new FileInputStream(srcFilePath);
	        	mFTPClient.storeFile(desFileName, srcFileStream);
	            srcFileStream.close();
	        }
	        

			mFTPClient.changeWorkingDirectory(mRoot); // 업로드를 위해 움직인 경로를  처음 접속 경로로 변경 (변경을 안할시엔  list 가   null을 리턴함)
				
	        
	        return status;
	        
	    } catch (Exception e) {
	        Log.d(TAG, "Error : ftpUpload()");
	        e.printStackTrace();
	    }
		
	    return status;
	}
	
	public boolean ftpTempDownload(String srcFilePath, String desFilePath){ 
	    boolean status = false;
	    try {
	        FileOutputStream desFileStream = new FileOutputStream(desFilePath);
	        status = mFTPClient.retrieveFile(srcFilePath, desFileStream);  //지정 경로 다운 시작
	        desFileStream.close();

	        return status;
	    } catch (Exception e) {
	        Log.d(TAG, "Error : ftpTempDownlad()");
	    }

	    return status;
	}
	
}
