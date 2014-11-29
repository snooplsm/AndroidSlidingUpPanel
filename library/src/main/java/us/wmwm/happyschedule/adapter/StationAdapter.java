package us.wmwm.happyschedule.adapter;

import java.util.ArrayList;
import java.util.List;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import us.wmwm.happyschedule.dao.Db;
import us.wmwm.happyschedule.views.StationHeader;
import us.wmwm.happyschedule.views.StationView;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;


public class StationAdapter extends CursorAdapter implements StickyListHeadersAdapter {
	
	List<String> letters = new ArrayList<String>();

    private boolean departureVisionOnly;
	
	public StationAdapter(Context context, boolean departureVisionOnly) {
		super(context, null, true);
//		char A = 'A';
//		int AIND = (int)A;
//		int max = ((int)A) + 26;
//		for(int i = 0; i < 26; i++) {
//			A = (char)(((int)AIND) + (i));
//			for(int j = 0; j < 10; j++) {
//				letters.add(String.valueOf(A));
//			}
//		}
        this.departureVisionOnly = departureVisionOnly;
		swapCursor(Db.get().getStops(departureVisionOnly));
	}

    public void filter(String key) {
        swapCursor(Db.get().getStops(departureVisionOnly,key));
    }

	@Override
	public Cursor getItem(int position) {
		Cursor cursor = (Cursor) super.getItem(position);
		return cursor;
	}

	public String getId(Cursor cursor) {
		return cursor.getString(0);
	}
	
	public String getName(Cursor cursor) {
		return cursor.getString(2);
	}
	
	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		StationView t = new StationView(parent.getContext());
		t.setData(getId(getItem(position)), getName(getItem(position)));
		return t;
	}

	@Override
	public View getHeaderView(int position, View convertView, ViewGroup parent) {
		StationHeader h;
		if (convertView == null) {
			h = new StationHeader(parent.getContext());
			h.setLayoutParams(new ViewGroup.LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		} else {
			h = (StationHeader) convertView;
		}
		h.setData(String.valueOf(getItem(position).getString(2).charAt(0)));
		return h;
	}

	@Override
	public long getHeaderId(int position) {
		return (int) getName(getItem(position)).charAt(0);
	}

	@Override
	public void bindView(View arg0, Context arg1, Cursor arg2) {
		StationView t = (StationView) arg0;
		t.setData(getId(arg2), getName(arg2));
	}

	@Override
	public View newView(Context ctx, Cursor c, ViewGroup parent) {
		StationView t = new StationView(parent.getContext());
		return t;
	}

}
