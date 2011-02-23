package com.adwhirl.adapters;

import com.adwhirl.AdWhirlLayout;
import com.adwhirl.AdWhirlTargeting;
import com.adwhirl.AdWhirlLayout.ViewAdRunnable;
import com.adwhirl.AdWhirlTargeting.Gender;
import com.adwhirl.obj.Extra;
import com.adwhirl.obj.Ration;
import com.adwhirl.util.AdWhirlUtil;
import com.inmobi.androidsdk.EducationType;
import com.inmobi.androidsdk.EthnicityType;
import com.inmobi.androidsdk.GenderType;
import com.inmobi.androidsdk.InMobiAdDelegate;
import com.inmobi.androidsdk.impl.InMobiAdView;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.util.Log;

import java.util.Date;

/**
 * An adapter for the InMobi Android SDK.
 * 
 * Note: The InMobi site Id is looked up using ration.key
 */

public final class InMobiAdapter extends AdWhirlAdapter implements InMobiAdDelegate {
  private Extra extra = null;
  public int adUnit = InMobiAdDelegate.INMOBI_AD_UNIT_320X48; //default size 9
  public InMobiAdapter(AdWhirlLayout adWhirlLayout, Ration ration) {
    super(adWhirlLayout, ration);
    extra = adWhirlLayout.extra;
  }

  @Override
  public void handle() {

    AdWhirlLayout adWhirlLayout = adWhirlLayoutReference.get();
    if (adWhirlLayout == null) {
      return;
    }
    
    Activity activity = adWhirlLayout.activityReference.get();
    if (activity == null) {
      return;
    }
    
    Context context = activity.getApplicationContext();
    InMobiAdView adView = InMobiAdView.requestAdUnitWithDelegate(context, this, activity, adUnit);
    adView.loadNewAd();
  }

  @Override
  public void adRequestCompleted(InMobiAdView adView) {
    Log.d(AdWhirlUtil.ADWHIRL, "InMobi success");

    AdWhirlLayout adWhirlLayout = adWhirlLayoutReference.get();
    if (adWhirlLayout == null) {
      return;
    }

    adWhirlLayout.adWhirlManager.resetRollover();
    adWhirlLayout.handler.post(new ViewAdRunnable(adWhirlLayout, adView));
    adWhirlLayout.rotateThreadedDelayed();
    adView.stopReceivingNotifications();
  }

  @Override
  public void adRequestFailed(InMobiAdView adView) {
    Log.d(AdWhirlUtil.ADWHIRL, "InMobi failure");     
    adView.stopReceivingNotifications();
    AdWhirlLayout adWhirlLayout = adWhirlLayoutReference.get();
    if (adWhirlLayout == null) {
      return;
    }
    adWhirlLayout.rollover();
  }

  @Override
  public int age() {
    return AdWhirlTargeting.getAge();
  }

  @Override
  public String areaCode() {
    return null;
  }

  @Override
  public Location currentLocation() {
    return null;
  }

  @Override
  public Date dateOfBirth() {
    return null;
  }

  @Override
  public EducationType education() {
    return null;
  }

  @Override
  public EthnicityType ethnicity() {
    return null;
  }

  @Override
  public GenderType gender() {
    Gender gender = AdWhirlTargeting.getGender();
    if (Gender.MALE == gender) {
      return GenderType.G_M;
    }
    if (Gender.FEMALE == gender) {
      return GenderType.G_F;
    }
    return GenderType.G_None;
  }

  @Override
  public int income() {
    return 0;
  }

  @Override
  public String interests() {
    return null;
  }

  @Override
  public boolean isLocationInquiryAllowed() {
    if (extra.locationOn == 1) {
      return true;
    } else {
      return false;
    }
  }

  @Override
  public boolean isPublisherProvidingLocation() {
    return false;
  }

  @Override
  public String keywords() {
    return AdWhirlTargeting.getKeywords();
  }

  @Override
  public String postalCode() {
    return AdWhirlTargeting.getPostalCode();
  }

  @Override
  public String searchString() {
    return null;
  }

  @Override
  public String siteId() {
    return ration.key;
  }

  @Override
  public boolean testMode() {
    return AdWhirlTargeting.getTestMode();
  }
}
