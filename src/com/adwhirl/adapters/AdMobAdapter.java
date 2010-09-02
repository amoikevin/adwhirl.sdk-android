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

package com.adwhirl.adapters;

import java.util.GregorianCalendar;

import android.app.Activity;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

import com.admob.android.ads.AdListener;
import com.admob.android.ads.AdManager;
import com.admob.android.ads.AdView;
import com.adwhirl.AdWhirlLayout;
import com.adwhirl.AdWhirlTargeting;
import com.adwhirl.AdWhirlLayout.ViewAdRunnable;
import com.adwhirl.obj.Extra;
import com.adwhirl.obj.Ration;
import com.adwhirl.util.AdWhirlUtil;

public class AdMobAdapter extends AdWhirlAdapter implements AdListener {
  public AdMobAdapter(AdWhirlLayout adWhirlLayout, Ration ration) {
    super(adWhirlLayout, ration);
  }

  @Override
  public void handle() { 
	 AdWhirlLayout adWhirlLayout = adWhirlLayoutReference.get();
	 if(adWhirlLayout == null) {
		 return;
	 }
	 
    try {
      AdManager.setPublisherId(ration.key);	
    }
    // Thrown on invalid publisher id
    catch(IllegalArgumentException e) {
      adWhirlLayout.rollover();
      return;
    }

    Activity activity = adWhirlLayout.activityReference.get();
    if(activity == null) {
    	return;
    }
    	
    AdView adMob = new AdView(activity);
    adMob.setAdListener(this);

    Extra extra = adWhirlLayout.extra;
    int bgColor = Color.rgb(extra.bgRed, extra.bgGreen, extra.bgBlue);
    int fgColor = Color.rgb(extra.fgRed, extra.fgGreen, extra.fgBlue);
    adMob.setBackgroundColor(bgColor);
    adMob.setPrimaryTextColor(fgColor);

	final AdWhirlTargeting.Gender gender = AdWhirlTargeting.getGender();
	if (gender == AdWhirlTargeting.Gender.FEMALE) {
		AdManager.setGender(AdManager.Gender.FEMALE);
	}
	else if (gender == AdWhirlTargeting.Gender.MALE) {
		AdManager.setGender(AdManager.Gender.MALE);
	}

	final GregorianCalendar birthDate = AdWhirlTargeting.getBirthDate();
	if (birthDate != null) {
		AdManager.setBirthday(birthDate);
	}

	final String postalCode = AdWhirlTargeting.getPostalCode();
	if (!TextUtils.isEmpty(postalCode)) {
		AdManager.setPostalCode(postalCode);
	}

	final String keywords = AdWhirlTargeting.getKeywords();
	if (!TextUtils.isEmpty(keywords)) {
		adMob.setKeywords(keywords);
	}

    if(extra.locationOn == 1) {
    	AdManager.setAllowUseOfLocation(true);
    }
    
    // The AdMob view has to be in the view hierarchy to make a request - for newer versions of AdMob SDK!
    adMob.setVisibility(View.INVISIBLE);
    
    // This should be addView() and not pushSubView(), since we don't count the impression yet.
    adWhirlLayout.addView(adMob, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

    // AdMob callbacks will queue rotate
  }

  // This block contains the AdMob listeners
  /*******************************************************************/
  public void onReceiveAd(AdView adView) {
    Log.d(AdWhirlUtil.ADWHIRL, "AdMob success");
    adView.setAdListener(null);

	 AdWhirlLayout adWhirlLayout = adWhirlLayoutReference.get();
	 if(adWhirlLayout == null) {
		 return;
	 }

    // The AdMob view has to be in the view hierarchy to make a request - for newer versions of AdMob SDK!
    adWhirlLayout.removeView(adView);
    adView.setVisibility(View.VISIBLE);
    adWhirlLayout.adWhirlManager.resetRollover();
    adWhirlLayout.handler.post(new ViewAdRunnable(adWhirlLayout, adView));
    adWhirlLayout.rotateThreadedDelayed();
  }

  public void onFailedToReceiveAd(AdView adView) {
    Log.d(AdWhirlUtil.ADWHIRL, "AdMob failure");
    adView.setAdListener(null);

	 AdWhirlLayout adWhirlLayout = adWhirlLayoutReference.get();
	 if(adWhirlLayout == null) {
		 return;
	 }
	 
    // The AdMob view has to be in the view hierarchy to make a request - for newer versions of AdMob SDK!
    adWhirlLayout.removeView(adView);
    adWhirlLayout.rollover();
  }

  public void onFailedToReceiveRefreshedAd(AdView adView)	{
    // Don't call adView.refreshAd so this is never called.
  }

  public void onReceiveRefreshedAd(AdView adView) {
    // Don't call adView.refreshAd so this is never called.
  }

  /*******************************************************************/
  // End of AdMob listeners
}
