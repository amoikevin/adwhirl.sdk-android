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

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.adwhirl.adapters.AdWhirlAdapter;
import com.adwhirl.obj.Custom;
import com.adwhirl.obj.Extra;
import com.adwhirl.obj.Ration;
import com.adwhirl.util.AdWhirlUtil;
import com.qwapi.adclient.android.view.QWAdView;

public class AdWhirlLayout extends RelativeLayout {
	public final WeakReference<Activity> activityReference;
	
	// Only the UI thread can update the UI, so we need this for UI callbacks
	public final Handler handler;
	
	// We also need a scheduler for background threads
	public final ScheduledExecutorService scheduler;
	
	public Extra extra;
	
	// The current custom ad
	public Custom custom;
	
	// This is just so our threads can reference us explicitly
	public WeakReference<RelativeLayout> superViewReference;
	
	public Ration activeRation;
	public Ration nextRation;
	
	// The Quattro callbacks don't contain a reference to the view, so we keep it here
	public QWAdView quattroView;
	
	public AdWhirlInterface adWhirlInterface;  
	
	public AdWhirlManager adWhirlManager;
	
	private boolean hasWindow;
	private boolean isRotating;
	
	public AdWhirlLayout(final Activity context, final String keyAdWhirl) {
		super(context);
		this.activityReference = new WeakReference<Activity>(context);
		this.superViewReference = new WeakReference<RelativeLayout>(this);
		
		this.hasWindow = true;
		this.isRotating = true;
		
		handler = new Handler();
		scheduler = Executors.newScheduledThreadPool(1);
		scheduler.schedule(new InitRunnable(this, keyAdWhirl), 0, TimeUnit.SECONDS);
		
		setHorizontalScrollBarEnabled(false);
		setVerticalScrollBarEnabled(false);
	}
	
	@Override
	protected void onWindowVisibilityChanged(int visibility) {
		 if(visibility == VISIBLE) {
			 this.hasWindow = true;
			 if(!this.isRotating) {
				 this.isRotating = true;
				 rotateThreadedNow();
			 }
		}
		 else {
			 this.hasWindow = false;
		 }
	}
	
	private void rotateAd() {
		if(!this.hasWindow) {
			this.isRotating = false;
			return;
		}
		
		Log.i(AdWhirlUtil.ADWHIRL, "Rotating Ad");
		nextRation = adWhirlManager.getRation();
		
		handler.post(new HandleAdRunnable(this));
	}
	
	// Initialize the proper ad view from nextRation
	private void handleAd() {
		// We shouldn't ever get to a state where nextRation is null unless all networks fail
		if(nextRation == null) {
			Log.e(AdWhirlUtil.ADWHIRL, "nextRation is null!");
			rotateThreadedDelayed();
			return;
		}
		
		String rationInfo = String.format("Showing ad:\n\tnid: %s\n\tname: %s\n\ttype: %d\n\tkey: %s\n\tkey2: %s", nextRation.nid, nextRation.name, nextRation.type, nextRation.key, nextRation.key2);
		Log.d(AdWhirlUtil.ADWHIRL, rationInfo);

		try {
		  AdWhirlAdapter.handle(this, nextRation);
		}
		catch(Throwable t) {
		  Log.w(AdWhirlUtil.ADWHIRL, "Caught an exception in adapter:", t);
		    rollover();
		    return;
		}
	}
	
	// Rotate immediately
	public void rotateThreadedNow() {
		scheduler.schedule(new RotateAdRunnable(this), 0, TimeUnit.SECONDS);
	}
	
	// Rotate in extra.cycleTime seconds
	public void rotateThreadedDelayed() {
		Log.d(AdWhirlUtil.ADWHIRL, "Will call rotateAd() in " + extra.cycleTime + " seconds");
		scheduler.schedule(new RotateAdRunnable(this), extra.cycleTime, TimeUnit.SECONDS);
	}
	
	// Remove old views and push the new one
	public void pushSubView(ViewGroup subView) {
		RelativeLayout superView = superViewReference.get();
		if(superView == null) {
			return;
		}
		superView.removeAllViews();

		RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
		superView.addView(subView, layoutParams);
		
		Log.d(AdWhirlUtil.ADWHIRL, "Added subview");
		
		this.activeRation = nextRation;
		countImpression();
	}
	
	public void rollover() {
		nextRation = adWhirlManager.getRollover();
		handler.post(new HandleAdRunnable(this));
	}

	private void countImpression() {		        
		String url = String.format(AdWhirlUtil.urlImpression, adWhirlManager.keyAdWhirl, activeRation.nid, activeRation.type, adWhirlManager.deviceIDHash, adWhirlManager.localeString, AdWhirlUtil.VERSION);
		scheduler.schedule(new PingUrlRunnable(url), 0, TimeUnit.SECONDS);
	}
	
	private void countClick() {
		String url = String.format(AdWhirlUtil.urlClick, adWhirlManager.keyAdWhirl, activeRation.nid, activeRation.type, adWhirlManager.deviceIDHash, adWhirlManager.localeString, AdWhirlUtil.VERSION);
		scheduler.schedule(new PingUrlRunnable(url), 0, TimeUnit.SECONDS);
	}
	
	//We intercept clicks to provide raw metrics
	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {  
		switch(event.getAction()) {
		//Sending on an ACTION_DOWN isn't 100% correct... user could have touched down and dragged out. Unlikely though.
		case MotionEvent.ACTION_DOWN:
		    Log.d(AdWhirlUtil.ADWHIRL, "Intercepted ACTION_DOWN event");
		    if(activeRation != null) {
			countClick();
			
			if(activeRation.type == 9) {
				if(custom != null && custom.link != null) {
					Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(custom.link));
				    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				    try {
				    	if(activityReference == null) {
				    		return false;
				    	}
				    	Activity activity = activityReference.get();
				    	if(activity == null) {
				    		return false;
				    	}
				    	activity.startActivity(intent);
				    } catch (Exception e) {
	        			Log.w(AdWhirlUtil.ADWHIRL, "Could not handle click to " + custom.link, e );
				    }
				}
				else {
					Log.w(AdWhirlUtil.ADWHIRL, "In onInterceptTouchEvent(), but custom or custom.link is null");
				}
			}
			break;
		    }
		}
	
		// Return false so subViews can process event normally.
		return false;
	}
	
	public interface AdWhirlInterface {
		public void adWhirlGeneric();
	}
	
	public void setAdWhirlInterface(AdWhirlInterface i) {
		this.adWhirlInterface = i;
	}
	
	private static class InitRunnable implements Runnable {
		private WeakReference<AdWhirlLayout> adWhirlLayoutReference;
		private String keyAdWhirl;

		public InitRunnable(AdWhirlLayout adWhirlLayout, String keyAdWhirl) {
			adWhirlLayoutReference = new WeakReference<AdWhirlLayout>(adWhirlLayout);
			this.keyAdWhirl = keyAdWhirl;
		}
		
		public void run() {
			AdWhirlLayout adWhirlLayout = adWhirlLayoutReference.get();
			if(adWhirlLayout != null) {
				Activity activity = adWhirlLayout.activityReference.get();
				if(activity == null) {
					return;
				}
				
				adWhirlLayout.adWhirlManager = new AdWhirlManager(new WeakReference<Context>(activity.getApplicationContext()), keyAdWhirl);
				adWhirlLayout.extra = adWhirlLayout.adWhirlManager.getExtra();
			
				if(adWhirlLayout.extra == null) {
					Log.e(AdWhirlUtil.ADWHIRL, "Unable to get configuration info or bad info, exiting AdWhirl");
					return;
				}
				
				adWhirlLayout.rotateAd();
			}
		}
	}
	
	// Callback for external networks
	private static class HandleAdRunnable implements Runnable {
		private WeakReference<AdWhirlLayout> adWhirlLayoutReference;

		public HandleAdRunnable(AdWhirlLayout adWhirlLayout) {
			adWhirlLayoutReference = new WeakReference<AdWhirlLayout>(adWhirlLayout);
		}
		public void run() {
			AdWhirlLayout adWhirlLayout = adWhirlLayoutReference.get();
			if(adWhirlLayout != null) {
				adWhirlLayout.handleAd();
			}
		}
	}

	// Callback for pushing views from ad callbacks
	public static class ViewAdRunnable implements Runnable {
		private WeakReference<AdWhirlLayout> adWhirlLayoutReference;
		private ViewGroup nextView;

		public ViewAdRunnable(AdWhirlLayout adWhirlLayout, ViewGroup nextView) {
			adWhirlLayoutReference = new WeakReference<AdWhirlLayout>(adWhirlLayout);
			this.nextView = nextView;
		}
		public void run() {
			AdWhirlLayout adWhirlLayout = adWhirlLayoutReference.get();
			if(adWhirlLayout != null) {
				adWhirlLayout.pushSubView(nextView);
			}
		}
	}
	
	private static class RotateAdRunnable implements Runnable {
		private WeakReference<AdWhirlLayout> adWhirlLayoutReference;
		
		public RotateAdRunnable(AdWhirlLayout adWhirlLayout) {
			adWhirlLayoutReference = new WeakReference<AdWhirlLayout>(adWhirlLayout);
		}
		public void run() {
			AdWhirlLayout adWhirlLayout = adWhirlLayoutReference.get();
			if(adWhirlLayout != null) {
				adWhirlLayout.rotateAd();
			}
		}
	}
	
	private static class PingUrlRunnable implements Runnable {
		private String url;
		
		public PingUrlRunnable(String url) {
			this.url = url;
		}
		
		public void run() {
			Log.d(AdWhirlUtil.ADWHIRL, "Pinging URL: " + url);
			
	        HttpClient httpClient = new DefaultHttpClient();
	        HttpGet httpGet = new HttpGet(url); 
	 
	        try {
	            httpClient.execute(httpGet);
	        } catch (ClientProtocolException e) {
	        	Log.e(AdWhirlUtil.ADWHIRL, "Caught ClientProtocolException in PingUrlRunnable", e);
	        } catch (IOException e) {
	        	Log.e(AdWhirlUtil.ADWHIRL, "Caught IOException in PingUrlRunnable", e);
	        }
		}
	}
}
