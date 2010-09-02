/*
 Copyright 2009-2010 AdMob, Inc.
 
    Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at
 
  http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/

package com.adwhirl;

import android.app.Activity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.adwhirl.AdWhirlLayout.AdWhirlInterface;
import com.adwhirl.adapters.AdWhirlAdapter;
import com.adwhirl.util.AdWhirlUtil;

public class Invoker extends Activity implements AdWhirlInterface {
	// For more easily detecting memory leaks.
	// byte[] garbage = new byte[1000 * 1024];
	
    /** Called when the activity is first created. */ 
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main);
        LinearLayout layout = (LinearLayout)findViewById(R.id.layout_main);
        
        if(layout == null) {
        	Log.e("AdWhirl", "Layout is null!");
        	return;
        }

        int width = 320;
        int height = 52;
        
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        float density = displayMetrics.density;
        
        width = (int) (width * density);
        height = (int) (height * density);
        
        AdWhirlTargeting.setAge(23);
        AdWhirlTargeting.setGender(AdWhirlTargeting.Gender.MALE);
        AdWhirlTargeting.setKeywords("online games gaming");
        AdWhirlTargeting.setPostalCode("94123");
        AdWhirlTargeting.setTestMode(false);
        
        AdWhirlAdapter.setGoogleAdSenseAppName("AdWhirl Test App");
        AdWhirlAdapter.setGoogleAdSenseCompanyName("AdWhirl");
        
        AdWhirlLayout adWhirlLayout = new AdWhirlLayout(this, "643eb700781e4f47b017ea27d1aba3be");
        adWhirlLayout.setAdWhirlInterface(this);
        RelativeLayout.LayoutParams adWhirlLayoutParams = new RelativeLayout.LayoutParams(width, height);
        layout.addView(adWhirlLayout, adWhirlLayoutParams);
        layout.invalidate();
    }

	public void adWhirlGeneric() {
		Log.e(AdWhirlUtil.ADWHIRL, "In adWhirlGeneric()");
	}
}
