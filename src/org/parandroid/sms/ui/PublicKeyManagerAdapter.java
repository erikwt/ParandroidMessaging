package org.parandroid.sms.ui;

import java.util.ArrayList;

import org.parandroid.sms.R;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class PublicKeyManagerAdapter extends BaseAdapter {

	private ArrayList<String> name;
	private ArrayList<String> number;
	
	public PublicKeyManagerAdapter(final Activity context, ListView listview) {
		name = new ArrayList<String>();
		number = new ArrayList<String>();
		
		listview.setAdapter(this);

		name.add("Erik");
		name.add("Lau");
		name.add("Sha");

		number.add("56456464");
		number.add("45645664");
		number.add("34543244");
	}
	
	public int getCount() {
		return name.size();
	}

	public Object getItem(int position) {
		return name.get(position);
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		return null;
	}

}
