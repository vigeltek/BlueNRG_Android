package com.st.bluenrg;

import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.content.Intent;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
public class SplashScreen extends Activity {
	
    private static int SPLASH_TIME_OUT = 1000;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().hide();
		LinearLayout layout = new LinearLayout(this);
		layout.setBackgroundResource(R.drawable.splash);
	    layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
	    setContentView(layout);
		
		new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(SplashScreen.this, DeviceScanActivity.class);
                startActivity(i);
                finish();
            }
        }, SPLASH_TIME_OUT);
	}
}