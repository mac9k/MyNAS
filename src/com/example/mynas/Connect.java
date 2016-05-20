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
	        mFTPClient = new FTPClient();	//FTP Client ��ü ����
	        mFTPClient.connect(host,port);	//���� ����
            mFTPClient.enterLocalPassiveMode();//FTP�� Passive mode �� ���� 
	        if (FTPReply.isPositiveCompletion(mFTPClient.getReplyCode())) {  //�����ڵ尡 �����̸� ����
	            boolean status = mFTPClient.login(username, password); // ����� ����
	            mFTPClient.setControlEncoding("UTF-8");
	            mFTPClient.setAutodetectUTF8(true);
	            mFTPClient.setFileType(FTP.BINARY_FILE_TYPE);// ���� ���� ����
	    		FtpCurrent = ftpGetCurrentWorkingDirectory(); // ���� �ּ� ����
	    		mFTPClient.setBufferSize(1024*1024); // ���ۻ�����
	    		mFTPClient.setSoTimeout(10000); // Ŀ�ؼ� Ÿ�Ӿƿ� 
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
	        mFTPClient.logout();  // �α׾ƿ�
	        mFTPClient.disconnect(); //��������
	        
	        return true;
	    } catch (Exception e) {
	        Log.d(TAG, "Error : ftpDisconnect()");
	        e.printStackTrace();
	    }
	    return false;
	}

	public static String ftpGetCurrentWorkingDirectory(){
	    try {
	        String workingDir = mFTPClient.printWorkingDirectory();  //�� ��ġ ���丮 ����
	        return workingDir;
	    } catch(Exception e) {
	        Log.d(TAG, "Error : ftpGetCurrentWorkingDirectory()");
	    }

	    return null;
	}

	public static boolean ftpChangeDirectory(String directory_path){
	    try {
	        mFTPClient.changeWorkingDirectory(directory_path); // ���丮 �� �̵�
	    } catch(Exception e) {
	        Log.d(TAG, "Error: ftpChangeDirectory()");
	        e.printStackTrace();
	    }

	    return true;
	}

	public boolean ftpRemoveFile(String filePath){
	    try {
	        boolean status = mFTPClient.deleteFile(filePath);  // ���� ����
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
	    	File dCurrent = new File(desFilePath); // ���丮 ��ü ����
	    	String[] deviceFiles = dCurrent.list(); // �� ���丮 ���� ��ϵ� ���� 
	    	int length = deviceFiles.length;
	    	boolean isRepeated = false;
	    	
	    	for(int i = 0 ; i < length; i++){  // �ߺ� üũ
	    		String name = deviceFiles[i];
	    		if(name.contains(desFileName) && !isRepeated){
	    			isRepeated = true;
	    			status = Existing;
	    			break;
	    		}
	    	}
	    	
	    	if(!isRepeated){ // �ߺ����� �ʴ´ٸ� ���� ������ ��ο� ���� �ٿ�ε� ����
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
			String[] ftpFiles = mFTPClient.listNames(desDirectory);  // ���丮 ��ü  ����
			int length = ftpFiles.length;							
			boolean isRepeated = false;
			
			for (int i = 0; i < length; i++) {  // �ߺ�üũ
				String name = ftpFiles[i];
				if (name.contains(desFileName) && !isRepeated){
					isRepeated = true;
					status = Existing;
					break;
				}
			} 
	        
	        if (ftpChangeDirectory(desDirectory)&& !isRepeated){  //�ߺ����� �ʰ� �ش� ��η� �̵������ϸ� �ٿ�ε� ����
	            FileInputStream srcFileStream = new FileInputStream(srcFilePath);
	        	mFTPClient.storeFile(desFileName, srcFileStream);
	            srcFileStream.close();
	        }
	        

			mFTPClient.changeWorkingDirectory(mRoot); // ���ε带 ���� ������ ��θ�  ó�� ���� ��η� ���� (������ ���ҽÿ�  list ��   null�� ������)
				
	        
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
	        status = mFTPClient.retrieveFile(srcFilePath, desFileStream);  //���� ��� �ٿ� ����
	        desFileStream.close();

	        return status;
	    } catch (Exception e) {
	        Log.d(TAG, "Error : ftpTempDownlad()");
	    }

	    return status;
	}
	
}
