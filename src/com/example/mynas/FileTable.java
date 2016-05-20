package com.example.mynas;

import java.io.Serializable;

public class FileTable implements Serializable {
	private static final long serialVersionUID = -1231252355;
	private String name;
	private long size;
	private String path;

	public FileTable() {
	}

	public FileTable(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public long getSize() {
		return size;
	}

	public String getPath() {
		return path;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public void setPath(String path) {
		this.path = path;
	}
}