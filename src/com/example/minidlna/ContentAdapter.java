package com.example.minidlna;

import java.io.File;
import java.util.ArrayList;

import org.teleal.cling.android.AndroidUpnpService;
import org.teleal.cling.controlpoint.ActionCallback;
import org.teleal.cling.model.action.ActionException;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.types.ErrorCode;
import org.teleal.cling.support.contentdirectory.callback.Browse;
import org.teleal.cling.support.model.BrowseFlag;
import org.teleal.cling.support.model.DIDLContent;
import org.teleal.cling.support.model.container.Container;
import org.teleal.cling.support.model.item.Item;

import com.example.mynas.R;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;


public class ContentAdapter extends BaseAdapter implements
		OnItemClickListener, OnKeyListener {

	private Context mContext;
	private LayoutInflater mInflater;
	private AndroidUpnpService mUpnpService;
	private Service mService;
	private String mCurrentId;
	private Container rootContainer;
	private ContentNode rootNode;
	private ContentNode mCurrentRoot;
	private ArrayList<String> mList = new ArrayList<String>();
	

	private static final String TAG = "UpnpBrowser";

	final boolean[] assertions = new boolean[3];

	public ContentAdapter(Context context, AndroidUpnpService upnpService, Service service) {
		mContext = context;
		mInflater = LayoutInflater.from(mContext);
		mUpnpService = upnpService;
		mService = service;
		rootContainer = createRootContainer(service);
		rootNode = new ContentNode(null, rootContainer);
		setDirectory(rootNode);
	}
	
	public AndroidUpnpService getUpnpService() {
		return mUpnpService;
	}
	
	protected Container createRootContainer(Service service) {
		Container rootContainer = new Container();
		rootContainer.setId("0");
		rootContainer.setTitle("Content Directory on " + service.getDevice().getDisplayString());
		return rootContainer;
	}

	private void setDirectory(ContentNode containerNode) {
		mCurrentRoot = containerNode;
		mList.clear();

		if (mCurrentRoot.getChildNodes() == null) {
			ActionCallback actionCallback = new Browse(mService, containerNode.getContainer().getId(), BrowseFlag.DIRECT_CHILDREN) {
				public void received(ActionInvocation actionInvocation, DIDLContent didl) {
						
					try {
						// Containers 
						for (Container childContainer : didl.getContainers()) {
							mCurrentRoot.addChildNode(new ContentNode(mCurrentRoot, childContainer));
						}
						
						//  Items
						for (Item childItem : didl.getItems()) {
							mCurrentRoot.addChildNode(new ContentNode(mCurrentRoot, childItem));
						}
					
					} catch (Exception e) {
						e.printStackTrace();
					}
					
					assertions[0] = true; //set File
				}
				
				@Override
				public void updateStatus(Status status) {
					if (!assertions[1] && status.equals(Status.LOADING)) {
						assertions[1] = true;
					} else if (assertions[1] && !assertions[2] && status.equals(Status.OK)) {
						assertions[2] = true;
					}
				}
				
				@Override
				public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
					
				}
			};
			
			new ActionCallback.Default(actionCallback.getActionInvocation(), mUpnpService.getControlPoint()).run();
			actionCallback.success(actionCallback.getActionInvocation());
		}
		
		if (mCurrentRoot.getChildNodes() != null) {
			for (ContentNode childNode : mCurrentRoot.getChildNodes()) {
				if (childNode.isContainer())
					mList.add(childNode.getContainer().getTitle());
				else
					mList.add(childNode.getItem().getTitle());
			}
		} else
			mList.add("No Content");
		
		mCurrentId = mCurrentRoot.getContainer().getId();

		notifyDataSetChanged();
		
	}
	
	@Override
	public int getCount() {
		int count = mList.size();
		return count;
	}

	@Override
	public Object getItem(int position) {
		Object obj = mList.get(position);
		return obj;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;

		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.list_item_icon_text, null);

			holder = new ViewHolder();
			holder.text = (TextView) convertView.findViewById(R.id.text);
			holder.icon = (ImageView) convertView.findViewById(R.id.icon);

			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		String name = mList.get(position);
		holder.text.setText(name);
		if (mCurrentRoot.getChildNodes() != null) {
			ContentNode node = mCurrentRoot.getChildNodes().get(position);
			if (node.isContainer())
				holder.icon.setImageResource(R.drawable.directory);
			else {
				String className = node.getItem().getClazz().getValue();
				if (className.contains("videoItem"))
					holder.icon.setImageResource(R.drawable.bd_movie);
				else if (className.contains("audioItem"))
					holder.icon.setImageResource(R.drawable.bd_music);
				else if (className.contains("imageItem"))
					holder.icon.setImageResource(R.drawable.bd_photo);
				else
					holder.icon.setImageResource(R.drawable.bd_unknown);
			}
		} else
			holder.icon.setImageResource(R.drawable.bd_unknown);

		return convertView;
	}

	static class ViewHolder {
		TextView text;
		ImageView icon;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		File file = new File(String.format("%s/%s", mCurrentId, mList.get(position)));
		if (position < mCurrentRoot.getChildContainers()) {
			setDirectory(mCurrentRoot.getChildNodes().get(position));
		}
		else if (mCurrentRoot.getChildNodes() != null) {
			ContentNode node = mCurrentRoot.getChildNodes().get(position);
			Log.d(TAG, "Item : " + node.getItem().getTitle());
			Log.d(TAG, "Item : " + node.getItem().getFirstResource().getValue());
			Log.d(TAG, "Item : " + node.getItem().getClazz().getValue());

			Intent intent;
			Bundle bundle = new Bundle();
			intent = new Intent(mContext, WebPlayActivity.class);
			intent.putExtra("Title",node.getItem().getTitle());
			intent.putExtra("Url",node.getItem().getFirstResource().getValue());
			mContext.startActivity(intent);
		}
	}

	@Override
	public boolean onKey(View v, int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (!mCurrentId.equals("0")) {
				Log.d(TAG, "onKey: mCurrentId : " + mCurrentId);
				setDirectory(mCurrentRoot.getParentNode());
				return true;
			} else
				return false;
		}
		return false;
	}
}
