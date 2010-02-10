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

package com.adwhirl.util;

public class AdWhirlUtil {	
	public static final String urlConfig = "http://mob.adwhirl.com/getInfo.php?appid=%s&appver=%d&client=2";
	public static final String urlImpression = "http://met.adwhirl.com/exmet.php?appid=%s&nid=%s&type=%d&uuid=%s&country_code=%s&appver=%d&client=2";
	public static final String urlClick = "http://met.adwhirl.com/exclick.php?appid=%s&nid=%s&type=%d&uuid=%s&country_code=%s&appver=%d&client=2";
	public static final String urlCustom = "http://cus.adwhirl.com/custom.php?appid=%s&nid=%s&uuid=%s&country_code=%s%s&appver=%d&client=2";
	
	public static final String locationString = "&location=%f,%f&location_timestamp=%d";
	
	public static String keyAdWhirl;
	
	// Don't change anything below this line
	/***********************************************/ 
	
	public static final int VERSION = 200;

	public static final String ADWHIRL = "AdWhirl SDK";
	
	// Could be an enum, but this gives us a slight performance improvement
	public static final int NETWORK_TYPE_ADMOB = 1;
	public static final int NETWORK_TYPE_JUMPTAP = 2;
	public static final int NETWORK_TYPE_VIDEOEGG = 3;
	public static final int NETWORK_TYPE_MEDIALETS = 4;
	public static final int NETWORK_TYPE_LIVERAIL = 5;
	public static final int NETWORK_TYPE_MILLENIAL = 6;
	public static final int NETWORK_TYPE_GREYSTRIP = 7;
	public static final int NETWORK_TYPE_QUATTRO = 8;
	public static final int NETWORK_TYPE_CUSTOM = 9;
	public static final int NETWORK_TYPE_ADWHIRL = 10;
	public static final int NETWORK_TYPE_MOBCLIX = 11;
	public static final int NETWORK_TYPE_MDOTM = 12;
	public static final int NETWORK_TYPE_4THSCREEN = 13;
	public static final int NETWORK_TYPE_ADSENSE = 14;
	public static final int NETWORK_TYPE_DOUBLECLICK = 15;
	public static final int NETWORK_TYPE_GENERIC = 16;
	public static final int NETWORK_TYPE_EVENT = 17;
	
	public static final int CUSTOM_TYPE_BANNER = 1;
	public static final int CUSTOM_TYPE_ICON = 2;
	
	public static String convertToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9))
                    buf.append((char) ('0' + halfbyte));
                else
                    buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            } while(two_halfs++ < 1);
        }
        return buf.toString();
    }
}
