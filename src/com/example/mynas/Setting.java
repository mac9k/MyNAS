package com.example.mynas;

import java.io.Serializable;

public class Setting implements Serializable{
	private static final long serialVersionUID=-1231252352;
	private String hostName,hostFPath,hostDPath,hostId,hostPassword,hostFPort,hostTPort;
	public Setting(){
		
	}
	
	
	public void setHostName(String hostName){
		this.hostName = hostName;
	}
	
	public void setHostFPath(String hostFPath){
		this.hostFPath = hostFPath;
	}
	
	public void setHostDPath(String hostDPath){
		this.hostDPath = hostDPath;
	}
	
	public void setHostId(String hostId){
		this.hostId = hostId;
	}
	
	public void setHostPassword(String hostPassword){
		this.hostPassword = hostPassword;
	}

	public void setHostFPort(String hostFPort){
		this.hostFPort = hostFPort;
	}
	
	public void setHostTPort(String hostTPort){
		this.hostTPort = hostTPort;
	}
	////////////////////////////////////////////////////////
	public String getHostName(){
		return hostName;
	}
	
	public String getHostFPath(){
		return hostFPath;
	}
	
	public String getHostDPath(){
		return hostDPath;
	}
	
	public String getHostId(){
		return hostId;
	}
	
	public String getHostPassword(){
		return hostPassword;
	}
	
	public String getHostFPort(){
		return hostFPort;
	}
	
	public String getHostTPort(){
		return hostTPort;
	}

}
