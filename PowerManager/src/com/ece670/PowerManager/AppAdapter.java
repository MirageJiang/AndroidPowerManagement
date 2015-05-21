package com.ece670.PowerManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;



import com.ece670.PowerManager.PowerUsageService.AppPowerInformation;
import com.ece670g10.PowerManager.R;




import android.app.Activity;
import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

public class AppAdapter extends ArrayAdapter<appInfo> {
	private Context context;
	private int layoutResouceId;
	private List<appInfo> apps = null;
	
	/**
	 * Constructor for BeaconAdatper.
	 * @param context The application context.
	 * @param resource The layout for the beacon adapter.
	 * @param discoveredBeaconList The discovered beacon list.
	 */
	public AppAdapter(Context context, int resource, List<appInfo> appInfoList) {
		super(context, resource, appInfoList);
		this.context = context;
		this.layoutResouceId = resource;
		this.apps = appInfoList;
	}
	
	public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        AppHolder holder = null;
        
        if(row == null)
        {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            row = inflater.inflate(layoutResouceId, parent, false);
            
            holder = new AppHolder();
            holder.AppNameTextView = (TextView)row.findViewById(R.id.uuid);
            holder.currentPowerTextView = (TextView)row.findViewById(R.id.major);
            holder.totalPowerTextView = (TextView)row.findViewById(R.id.power_reading_value);
            row.setTag(holder);
        }
        else
        {
            holder = (AppHolder)row.getTag();
        }
        
        appInfo app = apps.get(position);
        holder.AppNameTextView.setText(app.getName());
        holder.currentPowerTextView.setText(app.getCurrentPower());
        holder.totalPowerTextView.setText(app.getTotalPower());

        
        return row;
    }
	
	static class AppHolder
    {
        TextView AppNameTextView;
        TextView totalPowerTextView;
        TextView currentPowerTextView;

    }

	
}