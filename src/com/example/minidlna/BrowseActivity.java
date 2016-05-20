/*
 * Copyright (C) 2010 Teleal GmbH, Switzerland, 2011 Aquilegia, South Korea
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.example.minidlna;


import java.util.Comparator;

import org.teleal.cling.android.AndroidUpnpService;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.DeviceIdentity;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.types.UDAServiceId;
import org.teleal.cling.registry.DefaultRegistryListener;
import org.teleal.cling.registry.Registry;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * @author Christian Bauer, Aquilegia 
 */
public class BrowseActivity extends ListActivity {
 
	private static final String TAG = "In_BrowseActivity";
    private ArrayAdapter<DeviceDisplay> deviceListAdapter;
	private ListView listview;
    private BrowseRegistryListener registryListener = new BrowseRegistryListener();
    private AndroidUpnpService upnpService;
    private ServiceConnection serviceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {
            upnpService = (AndroidUpnpService) service;

            // Refresh the list with all known devices
            deviceListAdapter.clear();
            for (Device device : upnpService.getRegistry().getDevices()) {
                registryListener.deviceAdded(device);
            }

            // Getting ready for future device advertisements
            upnpService.getRegistry().addListener(registryListener);
        }

        public void onServiceDisconnected(ComponentName className) {
            upnpService = null;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StrictMode.enableDefaults();
        deviceListAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1);
        listview = getListView();
        switchToDeviceList();        
       
        getApplicationContext().bindService(new Intent(this, BrowserUpnpService.class),serviceConnection,Context.BIND_AUTO_CREATE);
    }
    
    public void switchToContentList(DeviceIdentity identity) {
    	Intent intent = new Intent(this, ContentActivity.class);
    	intent.putExtra (ContentActivity.DEVICE_UUID, identity.getUdn().toString());
		startActivity(intent);
    }
    
    public void switchToDeviceList() {
		setListAdapter(deviceListAdapter);
		
        listview.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        DeviceDisplay clickedDisplay = deviceListAdapter.getItem(position);
                        if (clickedDisplay != null) {
                        	Service service2 = clickedDisplay.getDevice().findService(new UDAServiceId("ContentDirectory"));
                        	if (service2 != null) {
                        		DeviceIdentity identity = clickedDisplay.getDevice().getIdentity();
                        		switchToContentList(identity);
                        	}
                        }                        
                    }
                }
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (upnpService != null) {
            upnpService.getRegistry().removeListener(registryListener);
        }
        getApplicationContext().unbindService(serviceConnection);
    }

    protected class BrowseRegistryListener extends DefaultRegistryListener {

        @Override
        public void remoteDeviceDiscoveryStarted(Registry registry, RemoteDevice device) {
            deviceAdded(device);
        }


        @Override
        public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
            deviceAdded(device);
        }

        @Override
        public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
            deviceRemoved(device);
        }

        @Override
        public void localDeviceAdded(Registry registry, LocalDevice device) {
            deviceAdded(device);
        }

        @Override
        public void localDeviceRemoved(Registry registry, LocalDevice device) {
            deviceRemoved(device);
        }

        public void deviceAdded(final Device device) {
            runOnUiThread(new Runnable() {
                public void run() {
                    DeviceDisplay d = new DeviceDisplay(device);

                    int position = deviceListAdapter.getPosition(d);
                    if (position >= 0) {
                        deviceListAdapter.remove(d);
                        deviceListAdapter.insert(d, position);
                    } else {
                        deviceListAdapter.add(d);
                    }
                }
            });
        }

        public void deviceRemoved(final Device device) {
            runOnUiThread(new Runnable() {
                public void run() {
                    deviceListAdapter.remove(new DeviceDisplay(device));
                }
            });
        }
    }

    protected class DeviceDisplay {

        Device device;

        public DeviceDisplay(Device device) {
            this.device = device;
        }

        public Device getDevice() {
            return device;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DeviceDisplay that = (DeviceDisplay) o;
            return device.equals(that.device);
        }

        @Override
        public int hashCode() {
            return device.hashCode();
        }

        @Override
        public String toString() {
            return device.isFullyHydrated() ? device.getDisplayString() : device.getDisplayString() + " *";
        }
    }

    static final Comparator<DeviceDisplay> DISPLAY_COMPARATOR =
            new Comparator<DeviceDisplay>() {
                public int compare(DeviceDisplay a, DeviceDisplay b) {
                    return a.toString().compareTo(b.toString());
                }
            };
}
