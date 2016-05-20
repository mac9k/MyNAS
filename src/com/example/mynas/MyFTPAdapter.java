package com.example.mynas;

import java.util.ArrayList;

import android.content.Context;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class MyFTPAdapter extends ArrayAdapter<FileTable> {
	private static final String TAG = "In_FTPAdaptor";
	private static final long KB = 1000;
	private static final long MB = 1000 * 1000;
	private static final long GB = 1000 * 1000 * 1000;
		
	private Context dContext;
	private int dResource;
	private ArrayList<FileTable> dList;
	private LayoutInflater dInflater;
	private SparseBooleanArray dSelectedItemsIds;
	
	public MyFTPAdapter(Context context, int layoutResource, ArrayList<FileTable> objects) {
		super(context, layoutResource, objects);
		this.dContext = context;
		this.dResource = layoutResource;
		this.dList = objects;
		this.dInflater = LayoutInflater.from(context);
		this.dSelectedItemsIds = new SparseBooleanArray();
	}
	
	public View getView(int position, View convertView, ViewGroup parent){
		
		ViewHolder holder = null ;
		
		if(convertView == null){
			convertView = dInflater.inflate(dResource, null);
			holder = new ViewHolder();
			holder.image = (ImageView)convertView.findViewById(R.id.thumbnail);
			holder.title = (TextView)convertView.findViewById(R.id.list_name);
			holder.size = (TextView)convertView.findViewById(R.id.file_size);
			convertView.setTag(holder);
		}else 
			holder = (ViewHolder)convertView.getTag();
		
		
		String Name = dList.get(position).getName();
		if(Name.startsWith("[")&& Name.endsWith("]")){
			holder.image.setImageResource(R.drawable.directory);
			holder.size.setText("(Dir)");
		}else{
			holder.image.setImageResource(R.drawable.file);
			
			long size = dList.get(position).getSize();  //File size
			if(size < KB)
				holder.size.setText(size + " B");
			else if(size <MB)
				holder.size.setText(size/KB + " KB");
			else if(size <GB)
				holder.size.setText(size/MB + " MB");
			else 
				holder.size.setText(size/GB + " GB");
		}
		
		
		holder.title.setText(Name);
		

		return convertView;
	}
	
	@Override
	public void remove(FileTable object) {
		dList.remove(object);
		notifyDataSetChanged();
	}

 
	public void toggleSelection(int position) {
		selectView(position, !dSelectedItemsIds.get(position));
	}
 
	public void removeSelection() {
		dSelectedItemsIds = new SparseBooleanArray();
		notifyDataSetChanged();
	}
 
	public void selectView(int position, boolean value) {
		if (value)
			dSelectedItemsIds.put(position, value);
		else
			dSelectedItemsIds.delete(position);
		notifyDataSetChanged();
	}
 
	public int getSelectedCount() {
		return dSelectedItemsIds.size();
	}
 
	public SparseBooleanArray getSelectedIds() {
		return dSelectedItemsIds;
	}
	@Override
	public long getItemId(int position) {
		return position;
	}
	
	public void clearItemsIds(){
		dSelectedItemsIds.clear();
	}

	@Override
	public int getCount() {
		return dList.size();
	}
	
	////////////////////
	static class ViewHolder {
		ImageView image;
        TextView title;
        TextView size;
	}
}
