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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.pm.*;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.provider.Settings.Secure;
import android.util.Log;

import com.adwhirl.obj.Custom;
import com.adwhirl.obj.Extra;
import com.adwhirl.obj.Ration;
import com.adwhirl.util.AdWhirlUtil;

public class AdWhirlManager {
	public String keyAdWhirl;
	
	private Extra extra;
	private List<Ration> rationsList;
	private double totalWeight = 0;
	private WeakReference<Context> contextReference;
	
	Iterator<Ration> rollovers;
	
	public String localeString;
	public String deviceIDHash;
	
	public Location location;
	
	public AdWhirlManager(WeakReference<Context> contextReference, String keyAdWhirl) {
		Log.i(AdWhirlUtil.ADWHIRL, "Creating adWhirlManager...");
		this.contextReference = contextReference;
		this.keyAdWhirl = keyAdWhirl;
		init();

		localeString = Locale.getDefault().toString();
		Log.d(AdWhirlUtil.ADWHIRL, "Locale is: " + localeString);
		
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
			StringBuffer deviceIDString = new StringBuffer(Secure.ANDROID_ID);
			deviceIDString.append("AdWhirl");
			deviceIDHash = AdWhirlUtil.convertToHex(md.digest(deviceIDString.toString().getBytes()));
		} catch (NoSuchAlgorithmException e) {
			deviceIDHash = "00000000000000000000000000000000";
		}
		Log.d(AdWhirlUtil.ADWHIRL, "Hashed device ID is: " + deviceIDHash);
		
		Log.i(AdWhirlUtil.ADWHIRL, "Finished creating adWhirlManager");
	}

	// This fetches the configuration
	private void init() {
		while(extra == null) {
			fetchConfig();
			if(extra == null) {
				try {
					Log.d(AdWhirlUtil.ADWHIRL, "Sleeping for 30 seconds");
					Thread.sleep(30 * 1000);
				} catch (InterruptedException e) {
					Log.e(AdWhirlUtil.ADWHIRL, "Thread unable to sleep", e);
				}
			}
		}
	}
	
	public Extra getExtra() {
		if(totalWeight <= 0) {
			Log.i(AdWhirlUtil.ADWHIRL, "Sum of ration weights is 0 - no ads to be shown");
			return null;
		}
		else {
			return this.extra;
		}
	}
	
	public Ration getRation() {
		Random random = new Random();
		
		double r = random.nextDouble() * totalWeight;
		double s = 0;
		
		Log.d(AdWhirlUtil.ADWHIRL, "Dart is <" + r + "> of <" + totalWeight + ">");
		
		Iterator<Ration> it = this.rationsList.iterator();
		Ration ration = null;
		while(it.hasNext()) {
			ration = it.next();
			s += ration.weight;
			
			if(s >= r) {
				break;
			}
		}
		
		return ration;
	}
	
	public Ration getRollover() {
		if(this.rollovers == null) {
			return null;
		}
		
		Ration ration = null;
		if(this.rollovers.hasNext()) {
			ration = this.rollovers.next();
		}
		
		return ration;
	}
	
	public void resetRollover() {
		this.rollovers = this.rationsList.iterator();
	}
	
	public Custom getCustom(String nid) {
        HttpClient httpClient = new DefaultHttpClient();
        
        String locationString;
        if(extra.locationOn == 1) {
        	location = getLocation();
        	if(location != null) {
        		locationString = String.format(AdWhirlUtil.locationString, location.getLatitude(), location.getLongitude(), location.getTime());
        	}
        	else {
        		locationString = "";
        	}
        }
        else {
        	location = null;
        	locationString = "";
        }
        
        String url = String.format(AdWhirlUtil.urlCustom, this.keyAdWhirl, nid, deviceIDHash, localeString, locationString, AdWhirlUtil.VERSION);
        HttpGet httpGet = new HttpGet(url); 
 
        HttpResponse httpResponse;
        try {
            httpResponse = httpClient.execute(httpGet);
     
            Log.d(AdWhirlUtil.ADWHIRL,httpResponse.getStatusLine().toString());
 
            HttpEntity entity = httpResponse.getEntity();
 
            if (entity != null) {
                InputStream inputStream = entity.getContent();
                String jsonString = convertStreamToString(inputStream);                
                return parseCustomJsonString(jsonString);
            }
        } catch (ClientProtocolException e) {
        	Log.e(AdWhirlUtil.ADWHIRL, "Caught ClientProtocolException in getCustom()", e);
        } catch (IOException e) {
        	Log.e(AdWhirlUtil.ADWHIRL, "Caught IOException in getCustom()", e);
        }
		
		return null;
	}
	
    public void fetchConfig() {
        HttpClient httpClient = new DefaultHttpClient();
        
        String url = String.format(AdWhirlUtil.urlConfig, this.keyAdWhirl, AdWhirlUtil.VERSION);
        HttpGet httpGet = new HttpGet(url); 
 
        HttpResponse httpResponse;
        try {
            httpResponse = httpClient.execute(httpGet);
     
            Log.d(AdWhirlUtil.ADWHIRL,httpResponse.getStatusLine().toString());
 
            HttpEntity entity = httpResponse.getEntity();
 
            if (entity != null) {
                InputStream inputStream = entity.getContent();
                String jsonString = convertStreamToString(inputStream);                
                parseConfigurationString(jsonString);
            }
        } catch (ClientProtocolException e) {
        	Log.e(AdWhirlUtil.ADWHIRL, "Caught ClientProtocolException in fetchConfig()", e);
        } catch (IOException e) {
        	Log.e(AdWhirlUtil.ADWHIRL, "Caught IOException in fetchConfig()", e);
        }
    }
    
	private String convertStreamToString(InputStream is) {
	    BufferedReader reader = new BufferedReader(new InputStreamReader(is), 8192);
	    StringBuilder sb = new StringBuilder();
	
	    String line = null;
	    try {
	        while ((line = reader.readLine()) != null) {
	            sb.append(line + "\n");
	        }
	    } 
	    catch (IOException e) {
        	Log.e(AdWhirlUtil.ADWHIRL, "Caught IOException in convertStreamToString()", e);
        	return null;
	    } 
	    finally {
	        try {
	            is.close();
	        } catch (IOException e) {
	        	Log.e(AdWhirlUtil.ADWHIRL, "Caught IOException in convertStreamToString()", e);
	            return null;
	        }
	    }
	    
	    return sb.toString();
	}
	
    private void parseConfigurationString(String jsonString) {
    	Log.d(AdWhirlUtil.ADWHIRL, "Received jsonString: " + jsonString);
    	
    	try {    
	        JSONObject json = new JSONObject(jsonString);
	
	        parseExtraJson(json.getJSONObject("extra"));
	        parseRationsJson(json.getJSONArray("rations"));
    	}
    	catch (JSONException e) {
    		Log.e(AdWhirlUtil.ADWHIRL, "Unable to parse response from JSON. This may or may not be fatal.", e);
    		this.extra = new Extra();
    	}
    	catch (NullPointerException e) {
    		Log.e(AdWhirlUtil.ADWHIRL, "Unable to parse response from JSON. This may or may not be fatal.", e);
    		this.extra = new Extra();
    	}
    }
    
    private void parseExtraJson(JSONObject json) {
    	Extra extra = new Extra();
    	
    	try {    
	        extra.cycleTime = json.getInt("cycle_time");
	        extra.locationOn = json.getInt("location_on");
	        extra.transition = json.getInt("transition");

		// Due to legacy clients, the server reports alpha on a scale of 0.0-1.0 instead of 0-255

	        JSONObject backgroundColor = json.getJSONObject("background_color_rgb");
	        extra.bgRed = backgroundColor.getInt("red");
	        extra.bgGreen = backgroundColor.getInt("green");
	        extra.bgBlue = backgroundColor.getInt("blue");
	        extra.bgAlpha = backgroundColor.getInt("alpha") * 255;
	        
	        JSONObject textColor = json.getJSONObject("text_color_rgb");
	        extra.fgRed = textColor.getInt("red");
	        extra.fgGreen = textColor.getInt("green");
	        extra.fgBlue = textColor.getInt("blue");
	        extra.fgAlpha = textColor.getInt("alpha") * 255;
    	}
    	catch (JSONException e) {
    		Log.e(AdWhirlUtil.ADWHIRL, "Exception in parsing config.extra JSON. This may or may not be fatal.", e);
    	}
    	
    	this.extra = extra;
    }
    
    private void parseRationsJson(JSONArray json) {
    	List<Ration> rationsList = new ArrayList<Ration>();
    	
    	this.totalWeight = 0;
    	
    	try {
	    	int i;
	    	for(i=0; i<json.length(); i++) {
				JSONObject jsonRation = json.getJSONObject(i); 
				if(jsonRation == null) {
					continue;
				}
				
				Ration ration = new Ration();

			    ration.nid = jsonRation.getString("nid");
			    ration.type = jsonRation.getInt("type");
			    ration.name = jsonRation.getString("nname");
			    ration.weight = jsonRation.getInt("weight");
			    ration.priority = jsonRation.getInt("priority");
			    
			    switch(ration.type) {
			    case AdWhirlUtil.NETWORK_TYPE_ADMOB:
				    ration.key = jsonRation.getString("key");
			    	break;
			    	
			    case AdWhirlUtil.NETWORK_TYPE_QUATTRO:
			    	JSONObject keyObj = jsonRation.getJSONObject("key");
			    	ration.key = keyObj.getString("siteID");
			    	ration.key2 = keyObj.getString("publisherID");
			    	break;
			    	
			    case AdWhirlUtil.NETWORK_TYPE_MILLENNIAL:
				    ration.key = jsonRation.getString("key");
			    	break;
			    	
			    case AdWhirlUtil.NETWORK_TYPE_CUSTOM:
			    	break;
			    	
			    case AdWhirlUtil.NETWORK_TYPE_GENERIC:
			    	break;
			    	
			    case AdWhirlUtil.NETWORK_TYPE_EVENT:
				    ration.key = jsonRation.getString("key");
			    	break;
			    	
			    default:
			    	Log.w(AdWhirlUtil.ADWHIRL, "Don't know how to fetch key for unexpected ration type: " + ration.type);
			    	continue;
			    }
				
			    this.totalWeight += ration.weight;
			    
				rationsList.add(ration);
	    	}
    	}
    	catch (JSONException e) {
    		Log.e(AdWhirlUtil.ADWHIRL, "JSONException in parsing config.rations JSON. This may or may not be fatal.", e);
		}
    	
    	Collections.sort(rationsList);
    	
    	this.rationsList = rationsList;
    	this.rollovers = this.rationsList.iterator();
    }    
    
    private Custom parseCustomJsonString(String jsonString) {
    	Log.d(AdWhirlUtil.ADWHIRL, "Received custom jsonString: " + jsonString);

        Custom custom = new Custom();
    	try {    
	        JSONObject json = new JSONObject(jsonString);
	
	        custom.type = json.getInt("ad_type");
	        custom.imageLink = json.getString("img_url");
	        custom.link = json.getString("redirect_url");
	        custom.description = json.getString("ad_text");
	        
			custom.image = fetchImage(custom.imageLink);
    	}
    	catch (JSONException e) {
    		Log.e(AdWhirlUtil.ADWHIRL, "Caught JSONException in parseCustomJsonString()", e);
    		return null;
    	}
    	
    	return custom;
    }
    
	private Drawable fetchImage(String urlString) {
		try {
			URL url = new URL(urlString);
			InputStream is = (InputStream) url.getContent();
			Drawable d = Drawable.createFromStream(is, "src");
			return d;
		} catch (Exception e) {
			Log.e(AdWhirlUtil.ADWHIRL, "Unable to fetchImage(): ", e);
			return null;
		}
	}    
	
	public Location getLocation() {
		if(contextReference == null) {
			return null;
		}
		
		Context context = contextReference.get();
		if(context == null) {
			return null;
		}
		
		Location location = null;

		if (context.checkCallingOrSelfPermission( android.Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED) {
			LocationManager lm = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);	
			location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		} else if (context.checkCallingOrSelfPermission( android.Manifest.permission.ACCESS_COARSE_LOCATION ) == PackageManager.PERMISSION_GRANTED) {
			LocationManager lm = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);	
			location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		}
		return location;
	}
}
