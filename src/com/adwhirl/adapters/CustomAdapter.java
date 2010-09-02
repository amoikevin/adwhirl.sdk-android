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

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import com.adwhirl.AdWhirlLayout;
import com.adwhirl.obj.Ration;
import com.adwhirl.util.AdWhirlUtil;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.util.Log;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView.ScaleType;

public class CustomAdapter extends AdWhirlAdapter {
	public CustomAdapter(AdWhirlLayout adWhirlLayout, Ration ration) {
		super(adWhirlLayout, ration);
	}

	@Override
	public void handle() {
		 AdWhirlLayout adWhirlLayout = adWhirlLayoutReference.get();
		 if(adWhirlLayout == null) {
			 return;
		 }
		
		 adWhirlLayout.scheduler.schedule(new FetchCustomRunnable(this), 0, TimeUnit.SECONDS);
	}
	
	public void displayCustom() {
		 AdWhirlLayout adWhirlLayout = adWhirlLayoutReference.get();
		 if(adWhirlLayout == null) {
			 return;
		 }
		 
	    Activity activity = adWhirlLayout.activityReference.get();
	    if(activity == null) {
	    	return;
	    }
	    
		switch(adWhirlLayout.custom.type) {
		case AdWhirlUtil.CUSTOM_TYPE_BANNER:
			Log.d(AdWhirlUtil.ADWHIRL, "Serving custom type: banner");
			
			RelativeLayout bannerView = new RelativeLayout(activity);
			if(adWhirlLayout.custom.image == null) {
				adWhirlLayout.rotateThreadedNow();
				return;
			}
			ImageView bannerImageView = new ImageView(activity);
			bannerImageView.setImageDrawable(adWhirlLayout.custom.image);
			bannerImageView.setScaleType(ScaleType.FIT_CENTER);
			RelativeLayout.LayoutParams bannerViewParams = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
			bannerView.addView(bannerImageView, bannerViewParams);
			adWhirlLayout.pushSubView(bannerView);
			break;

		case AdWhirlUtil.CUSTOM_TYPE_ICON:
			Log.d(AdWhirlUtil.ADWHIRL, "Serving custom type: icon");
			RelativeLayout iconView = new RelativeLayout(activity);
			if(adWhirlLayout.custom.image == null) {
				adWhirlLayout.rotateThreadedNow();
				return;
			}
			iconView.setLayoutParams(new LayoutParams(320, 50));  // Size of the banner
			ImageView blendView = new ImageView(activity);
			int backgroundColor = Color.rgb(adWhirlLayout.extra.bgRed, adWhirlLayout.extra.bgGreen, adWhirlLayout.extra.bgBlue);
			GradientDrawable blend = new GradientDrawable(Orientation.TOP_BOTTOM, new int[] {Color.WHITE, backgroundColor, backgroundColor, backgroundColor}); 
			blendView.setBackgroundDrawable(blend);
			RelativeLayout.LayoutParams blendViewParams = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
			iconView.addView(blendView, blendViewParams);
			ImageView iconImageView = new ImageView(activity);
			iconImageView.setImageDrawable(adWhirlLayout.custom.image);
			iconImageView.setId(10);
			iconImageView.setPadding(4, 0, 6, 0);
			iconImageView.setScaleType(ScaleType.CENTER);
			RelativeLayout.LayoutParams iconViewParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT);
			iconView.addView(iconImageView, iconViewParams);
			ImageView frameImageView = new ImageView(activity);
			InputStream drawableStream = getClass().getResourceAsStream("/com/adwhirl/assets/ad_frame.gif"); 
			Drawable adFrameDrawable = new BitmapDrawable(drawableStream);
			frameImageView.setImageDrawable(adFrameDrawable);
			frameImageView.setPadding(4, 0, 6, 0);
			frameImageView.setScaleType(ScaleType.CENTER);
			RelativeLayout.LayoutParams frameViewParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT);
			iconView.addView(frameImageView, frameViewParams);
			TextView iconTextView = new TextView(activity);
			iconTextView.setText(adWhirlLayout.custom.description);
			iconTextView.setTypeface(Typeface.DEFAULT_BOLD, 1);
			iconTextView.setTextColor(Color.rgb(adWhirlLayout.extra.fgRed, adWhirlLayout.extra.fgGreen, adWhirlLayout.extra.fgBlue));
			RelativeLayout.LayoutParams textViewParams = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
			textViewParams.addRule(RelativeLayout.RIGHT_OF, iconImageView.getId());
			textViewParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
			textViewParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
			textViewParams.addRule(RelativeLayout.CENTER_VERTICAL);
			textViewParams.addRule(RelativeLayout.CENTER_IN_PARENT);
			iconTextView.setGravity(Gravity.CENTER_VERTICAL);
			iconView.addView(iconTextView, textViewParams);
			adWhirlLayout.pushSubView(iconView);
			break;

		default:
			Log.w(AdWhirlUtil.ADWHIRL, "Unknown custom type!");
			adWhirlLayout.rotateThreadedNow();
			return;
		}

		adWhirlLayout.adWhirlManager.resetRollover();
		adWhirlLayout.rotateThreadedDelayed();
	}

	private static class FetchCustomRunnable implements Runnable {
		private CustomAdapter customAdapter;

		public FetchCustomRunnable(CustomAdapter customAdapter) {
			this.customAdapter = customAdapter;
		}
		
		public void run() {
			AdWhirlLayout adWhirlLayout = customAdapter.adWhirlLayoutReference.get();
			if(adWhirlLayout == null) {
				return;
			}	

			adWhirlLayout.custom = adWhirlLayout.adWhirlManager.getCustom(customAdapter.ration.nid);
			if(adWhirlLayout.custom == null) {
				adWhirlLayout.rotateThreadedNow();
				return;
			}
			
			adWhirlLayout.handler.post(new DisplayCustomRunnable(customAdapter));
		}
	}

	private static class DisplayCustomRunnable implements Runnable {
		private CustomAdapter customAdapter;

		public DisplayCustomRunnable(CustomAdapter customAdapter) {
			this.customAdapter = customAdapter;
		}

		public void run() {
			customAdapter.displayCustom();
		}
	}
}
