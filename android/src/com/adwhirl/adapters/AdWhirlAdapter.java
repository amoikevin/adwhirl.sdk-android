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

import android.util.Log;

import com.adwhirl.AdWhirlLayout;
import com.adwhirl.obj.Ration;
import com.adwhirl.util.AdWhirlUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public abstract class AdWhirlAdapter {
	protected final AdWhirlLayout adWhirlLayout;
	protected Ration ration;
	
	public AdWhirlAdapter(AdWhirlLayout adWhirlLayout, Ration ration) {
		this.adWhirlLayout = adWhirlLayout;
		this.ration = ration;
	}
	
	public static AdWhirlAdapter getAdapter(AdWhirlLayout adWhirlLayout, Ration ration) {	
		try {
			switch(ration.type) {
				case AdWhirlUtil.NETWORK_TYPE_ADMOB:
					if(Class.forName("com.admob.android.ads.AdView") != null) {
						return getNetworkAdapter("com.adwhirl.adapters.AdMobAdapter", adWhirlLayout, ration);
					}
					else {
						return unknownAdNetwork(adWhirlLayout, ration);
					}  
				
				case AdWhirlUtil.NETWORK_TYPE_QUATTRO:
					if(Class.forName("com.qwapi.adclient.android.view.QWAdView") != null) {
                        return getNetworkAdapter("com.adwhirl.adapters.QuattroAdapter", adWhirlLayout, ration);
					}
					else {
						return unknownAdNetwork(adWhirlLayout, ration);
					}
					
				case AdWhirlUtil.NETWORK_TYPE_MILLENNIAL:
					if(Class.forName("com.millennialmedia.android.MMAdView") != null) {
                        return getNetworkAdapter("com.adwhirl.adapters.MillennialAdapter", adWhirlLayout, ration);
					}
					else {
						return unknownAdNetwork(adWhirlLayout, ration);
					}
					
				case AdWhirlUtil.NETWORK_TYPE_CUSTOM:
					return new CustomAdapter(adWhirlLayout, ration);
					
				case AdWhirlUtil.NETWORK_TYPE_GENERIC:
					return new GenericAdapter(adWhirlLayout, ration);
				
				case AdWhirlUtil.NETWORK_TYPE_EVENT:
					return new EventAdapter(adWhirlLayout, ration);
											
				default:
					return unknownAdNetwork(adWhirlLayout, ration);
			}
		}
		catch(ClassNotFoundException e) {
			return unknownAdNetwork(adWhirlLayout, ration);
		}
		catch(VerifyError e) {
		  Log.e("AdWhirl", "YYY - Caught VerifyError", e);
          return unknownAdNetwork(adWhirlLayout, ration);
		}
	}
	
  public static AdWhirlAdapter getNetworkAdapter(String networkAdapter, AdWhirlLayout adWhirlLayout, Ration ration) {
	  AdWhirlAdapter adWhirlAdapter = null;

	  try {
    	  @SuppressWarnings("unchecked")
    	  Class<? extends AdWhirlAdapter> adapterClass = (Class<? extends AdWhirlAdapter>) Class.forName(networkAdapter);
    	  
    	  Class<?>[] parameterTypes = new Class[2];
    	  parameterTypes[0] = AdWhirlLayout.class;
    	  parameterTypes[1] = Ration.class;
    	  
    	  Constructor<? extends AdWhirlAdapter> constructor = adapterClass.getConstructor(parameterTypes);
    	 
    	  Object[] args = new Object[2];
    	  args[0] = adWhirlLayout;
    	  args[1] = ration;
    	  
    	  adWhirlAdapter = constructor.newInstance(args);
	  }
	  catch(ClassNotFoundException e) {}
	  catch(SecurityException e) {}
	  catch(NoSuchMethodException e) {}
	  catch(InvocationTargetException e) {}
	  catch(IllegalAccessException e) {}
	  catch(InstantiationException e) {}
	  
	  return adWhirlAdapter;
	}
	
	public static AdWhirlAdapter unknownAdNetwork(AdWhirlLayout adWhirlLayout, Ration ration) {
		Log.w(AdWhirlUtil.ADWHIRL, "Unsupported ration type: " + ration.type);
		return null;
	}
	
	public abstract void handle();
}
