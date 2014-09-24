package com.st.bluenrg;

import java.util.List;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;

public class LogActivity extends Activity {
	private ListView logListView;
    private RssAdapter mLogAdap;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_log);
		
		logListView = (ListView) findViewById(R.id.list);
		
		try{
			mLogAdap = new RssAdapter(LogActivity.this, R.layout.list_item,
            		DeviceScanActivity.log);
            int count = mLogAdap.getCount();
            if (count != 0 && mLogAdap != null) {
            	logListView.setAdapter(mLogAdap);
            }
        	}catch (Exception e){
        		AlertDialog.Builder builder = new AlertDialog.Builder(LogActivity.this);
        		builder.setMessage("Nessuna connessione di rete")
        		       .setTitle("Attenzione");
        		AlertDialog dialog = builder.create();
        		dialog.show();
        	}
		
	}

	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		getMenuInflater().inflate(R.menu.log, menu);
		return true;
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
			case android.R.id.home:	finish(); return true;
            case R.id.menu_info:
            	Intent intent = new Intent(this, AboutActivity.class);
        	    startActivity(intent);
                break;
        }
        return true;
    }
    
    private class RssAdapter extends ArrayAdapter<visualLog> {
        private List<visualLog> logLst;

        public RssAdapter(Context context, int textViewResourceId, List<visualLog> Lst) {
            super(context, textViewResourceId, Lst);
            this.logLst = Lst;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            LogHolder logHolder = null;
            if (convertView == null) {
                view = View.inflate(LogActivity.this, R.layout.list_item, null);
                logHolder = new LogHolder();
                logHolder.titleTextView = (TextView) view.findViewById(R.id.title_view);
                logHolder.timeTextView = (TextView) view.findViewById(R.id.time_view);
                view.setTag(logHolder);
            } else {
            	logHolder = (LogHolder) view.getTag();
            }
            visualLog current = logLst.get(position);

            logHolder.titleTextView.setText(current.getMessage());
            logHolder.timeTextView.setText(current.getTime());
            
            return view;
        }
    }

    static class LogHolder {
        public TextView titleTextView;
        public TextView timeTextView;
    }

}
