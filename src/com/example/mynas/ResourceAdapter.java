package com.example.mynas;

import java.util.ArrayList;

import com.example.mynas.MyFTPAdapter.ViewHolder;

import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ResourceAdapter extends ArrayAdapter<FileTable>  {
	private static final String TAG = "In_ResourceAdaptor";
	private static final long KB = 1000;
	private static final long MB = 1000 * 1000;
	private static final long GB = 1000 * 1000 * 1000;
	
	private Context rContext;
	private int rResource;
	private ArrayList<FileTable> rList;
	private LayoutInflater rInflater;
	private SparseBooleanArray rSelectedItemsIds;
	
	public ResourceAdapter(Context context, int layoutResource, ArrayList<FileTable> objects) {
		super(context, layoutResource, objects);
		this.rContext = context;
		this.rResource = layoutResource;
		this.rList = objects;
		//this.dInflater = (LayoutInflater) dContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.rInflater = LayoutInflater.from(context);
		this.rSelectedItemsIds = new SparseBooleanArray();
	}
	
public View getView(int position, View convertView, ViewGroup parent){
		
		ViewHolder holder = null ;
		
		if(convertView == null){
			convertView = rInflater.inflate(rResource, null);
			holder = new ViewHolder();
			holder.image = (ImageView)convertView.findViewById(R.id.thumbnail);
			holder.title = (TextView)convertView.findViewById(R.id.list_name);
			holder.size = (TextView)convertView.findViewById(R.id.file_size);
			convertView.setTag(holder);
		}else 
			holder = (ViewHolder)convertView.getTag();
		
		
		String Name = rList.get(position).getName();
		if(Name.startsWith("[")&& Name.endsWith("]")){
			holder.image.setImageResource(R.drawable.directory);
			holder.size.setText("(Dir)");
		}else{
			holder.image.setImageResource(R.drawable.file);
			
			long size = rList.get(position).getSize();  //File size
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
}
