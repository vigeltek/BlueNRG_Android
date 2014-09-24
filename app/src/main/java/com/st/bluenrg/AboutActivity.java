package com.st.bluenrg;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;

public class AboutActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);
		
		WebView  browser=(WebView) findViewById(R.id.webView);
		browser.setVerticalScrollBarEnabled(false);
		
		TextView tvTitle = (TextView) findViewById(R.id.textViewName);
		TextView tvVersion = (TextView) findViewById(R.id.textViewVersion);
		
		tvTitle.setText(getString(R.string.app_name)+" App");
		try {
			tvVersion.setText("v "+getApplicationContext().getPackageManager()
					.getPackageInfo(getApplicationContext().getPackageName(), 0).versionName);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		
        String url = "file:///android_asset/about.html";
        browser.loadUrl(url);
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
			case android.R.id.home:	finish(); return true;
        }
        return true;
    }
	
	public void facebook(View v) {
	}
	
	public void twitter(View v) {
	}

}
