package com.adwhirl.adapters;

import com.adwhirl.AdWhirlLayout;
import com.adwhirl.AdWhirlLayout.ViewAdRunnable;
import com.adwhirl.adapters.MdotMView.MdotMActionListener;
import com.adwhirl.obj.Extra;
import com.adwhirl.obj.Ration;
import com.adwhirl.util.AdWhirlUtil;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This file was provided by MdotM. Please contact support@mdotm.com with any questions or concerns.
 */
public class MdotMAdapter extends AdWhirlAdapter implements MdotMActionListener{
  public MdotMAdapter(AdWhirlLayout adWhirlLayout, Ration ration) {
    super(adWhirlLayout, ration);
  }

  @Override
  public void handle() {
    AdWhirlLayout adWhirlLayout = adWhirlLayoutReference.get();
    if(adWhirlLayout == null) {
      return;
    }

    try {
      MdotMManager.setPublisherId(this.ration.key);
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
    MdotMView mdotm = new MdotMView(activity, this);

    mdotm.setListener(this);
    Extra extra = adWhirlLayout.extra;
    int bgColor = Color.rgb(extra.bgRed, extra.bgGreen, extra.bgBlue);
    int fgColor = Color.rgb(extra.fgRed, extra.fgGreen, extra.fgBlue);

    mdotm.setBackgroundColor(bgColor);
    mdotm.setTextColor(fgColor);
  }	

  public void adRequestCompletedSuccessfully(MdotMView adView) {
    Log.d(AdWhirlUtil.ADWHIRL, "MdotM success");

    AdWhirlLayout adWhirlLayout = adWhirlLayoutReference.get();
    if(adWhirlLayout == null) {
      return;
    }		
    adView.setListener(null);
    adView.setVisibility(View.VISIBLE);

    adWhirlLayout.adWhirlManager.resetRollover();
    adWhirlLayout.handler.post(new ViewAdRunnable(adWhirlLayout, adView));
    adWhirlLayout.rotateThreadedDelayed();
  }

  public void adRequestFailed(MdotMView adView) {
    Log.d(AdWhirlUtil.ADWHIRL, "MdotM failure");
    adView.setListener(null);

    AdWhirlLayout adWhirlLayout = adWhirlLayoutReference.get();
    if(adWhirlLayout == null) {
      return;
    }
    adWhirlLayout.rollover();
  }
}

class MdotMView extends RelativeLayout {
  private static final String MDOTM_BASE_URL = "http://ads.mdotm.com/ads/feed.php?";

  private String adLandingUrl;
  private MdotMActionListener listener;	
  private boolean adSelectionInProgress;

  private ProgressBar activityIndicator;
  private static final int GRADIENT_TOP_ALPHA = (int) (255 * 0.50);

  /**
   * The percentage of the view's height from the top at which the gradient
   * background becomes fully opaque. This makes what is above this point look
   * like a light is shinging from above and reflecting. What is below this
   * point is in the shadow. This is 0.4375 for the iPhone.
   */
  private static final double GRADIENT_STOP = 0.4375;

  /**
   * The color of a mask applied over the entire view when the user presses
   * down on the ad to signal they are going to click it. iPhone uses a 0.5
   * alpha blended with grey 1/2 way on the greyscale (0x88888888).
   */
  private static final int HIGHLIGHT_COLOR = 0xFFFFB400; // Android yellow

  /**
   * The color applied to a highlighted ad before the "shine" of
   * <code>HIGHLIGHT_COLOR</code> is applied. This color is analygous to
   * <code>GRADIENT_BACKGROUND_COLOR</code>.
   */
  private static final int HIGHLIGHT_BACKGROUND_COLOR = 0xFFEE7F27; // android orange

  private int padding;
  private int textColor = Color.WHITE;
  private int backgroundColor = Color.BLACK;

  private Drawable adSelectedBackground;
  private Drawable defaultBackground;

  private  boolean isAdClicked = false;
  private static final Typeface AD_FONT = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);

  public interface MdotMActionListener {		
    public void adRequestCompletedSuccessfully(MdotMView adView) ;
    public void adRequestFailed(MdotMView adView);
  }

  public MdotMView(Context context, MdotMActionListener listener) {	
    super(context);	
    this.listener = listener;
    init();
    setAdSelectionInProgress(false);
  }

  /**
   * Constructs an advertisement view from a layout XML file.
   * 
   * @param context is the application's context.
   * @param attrs are attributes set in the XML layout for this view.
   * 
   * @see android.view.View#View(android.content.Context, android.util.AttributeSet)
   */
  public MdotMView( Context context, AttributeSet attrs ){	
    this( context, attrs, 0 );
  }

  /**
   * Constructs an advertisement view from a layout XML file.
   * 
   * @param context is the application's context.
   * @param attrs are attributes set in the XML layout for this view.
   * @param defStyle is the theme ID to apply to this view.
   * 
   * @see android.view.View#View(Context, AttributeSet, int)
   */
  public MdotMView( Context context, AttributeSet attrs, int defStyle ){	
    super( context, attrs, defStyle );
  }

  private void init() {	
    fetchAd();
    setFocusable( true );
    setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS );	
    setClickable( true );
  }

  @Override
  public boolean dispatchKeyEvent(KeyEvent event) {
    int action =  event.getAction();
    if(! this.isAdClicked ) {
      if(action == MotionEvent.ACTION_UP){
        isAdClicked = true;
        clicked();
      }
    }
    return super.dispatchKeyEvent(event);
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent motionEvent) {
    int action =  motionEvent.getAction();
    float x = motionEvent.getX();
    float y = motionEvent.getY();

    int left = getLeft();
    int top = getTop();
    int right = getRight();
    int bottom = getBottom();

    Log.i(MdotMManager.LOG_TAG, "  Selected  ");
    if (x < left || x > right || y < top || y > bottom) {
      Log.i(MdotMManager.LOG_TAG, "Always outside of ad display area ");
      if(! this.isAdClicked ) {
        drawBackgroundView(true);
      }
    } else {
      if(action == MotionEvent.ACTION_UP) {
        if(! this.isAdClicked ) {
          isAdClicked = true;
          setClickable(false);
          clicked();
          drawBackgroundView(true);
        }
      } else if (action == MotionEvent.ACTION_DOWN) {
        if(! this.isAdClicked ) {
          drawBackgroundView(false);
        }
      }
    }
    return super.dispatchTouchEvent(motionEvent);
  }


  private void drawBackgroundView(boolean isDefaultBackground){
    Log.d("X-Value", " drawBackgroundView   :: "+isDefaultBackground);

    if(defaultBackground == null ){
      defaultBackground = populateDrawablwBackGround( Color.WHITE , backgroundColor);
    }

    if(adSelectedBackground == null){
      adSelectedBackground  =  populateDrawablwBackGround(HIGHLIGHT_BACKGROUND_COLOR, HIGHLIGHT_COLOR );			
    }

    if(isDefaultBackground ){        	
      setBackgroundDrawable(defaultBackground);
    }else{
      setBackgroundDrawable(adSelectedBackground);
    }
  }

  private static Drawable populateDrawablwBackGround(int backgroundColor, int color){		
    Rect rect = new Rect(0,0,320,48);
    Bitmap bitmap = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);

    Paint paint = new Paint();
    paint.setColor(backgroundColor);
    paint.setAntiAlias(true);
    canvas.drawRect(rect, paint);

    // Draw a "shine" or reflection from above on the top half. It is a
    // gradient that
    // becomes fully opaque part way down the ad.
    int topColor = Color.argb(GRADIENT_TOP_ALPHA, Color.red(color), Color
        .green(color), Color.blue(color));
    int[] gradientColors = { topColor, color };
    GradientDrawable shine = new GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM, gradientColors);

    int stop = (int) (rect.height() * GRADIENT_STOP) + rect.top;
    shine.setBounds(rect.left, rect.top, rect.right, stop);
    shine.draw(canvas);

    // Draw the "shadow" below the reflection. It is a fully opaque section
    // below
    // where the gradient stops.
    Rect shadowRect = new Rect(rect.left, stop, rect.right, rect.bottom);
    Paint shadowPaint = new Paint();
    shadowPaint.setColor(color);
    canvas.drawRect(shadowRect, shadowPaint);

    return new BitmapDrawable(bitmap);
  }



  private void clicked() {
    Log.i(MdotMManager.LOG_TAG, "  Selected to clicked  ");

    if(this.adLandingUrl !=  null){
      //   Open up the browser with the clicked URL.
      //   This can also open the Market application.  For example the click URL
      //   to Ringer Mobile is:  http://market.android.com/details?id=6278196384529742763			
      //   The ID in the URL can be obtained on the Market's main developer console by
      //   hovering over the link of your application.

      // walk down the 302 redirects on the clicked url until we get a real url.
      // we need to do this asynchronously.

      if(!isAdSelectionInProgress()){
        final String clickedUrl = this.adLandingUrl;
        setAdSelectionInProgress(true);
        showActivityIndicator();
        new Thread() {

          public void run() {

            URL destinationURL = null;
            try{

              URL url = new URL( clickedUrl );
              destinationURL = url;

              HttpURLConnection.setFollowRedirects( true );

              HttpURLConnection redirectConnection = (HttpURLConnection) url.openConnection();
              redirectConnection.setConnectTimeout(500);
              redirectConnection.setRequestProperty( "User-Agent", MdotMManager.getUserAgent() );  
              redirectConnection.connect();
              redirectConnection.getResponseCode();
              destinationURL = redirectConnection.getURL();

              if ( Log.isLoggable( MdotMManager.LOG_TAG, Log.DEBUG ) ) {
                Log.d( MdotMManager.LOG_TAG, " Final click destination URL:  " + destinationURL );
              }

            } catch (MalformedURLException e) {
              Log.w(  MdotMManager.LOG_TAG, "Malformed click URL.  Will try to follow anyway.  " + clickedUrl, e );
            } catch (IOException e) {
              Log.w(  MdotMManager.LOG_TAG, "Could not determine final click destination URL.  Will try to follow anyway.  " + clickedUrl, e );
            }

            if ( destinationURL != null )  {
              if ( Log.isLoggable(  MdotMManager.LOG_TAG, Log.DEBUG ) ) {	
                Log.d(  MdotMManager.LOG_TAG, "Opening " + destinationURL );
              }

              // Uri doesn't understand java.net.URI so we have to URL->String->Uri
              Intent i = new Intent( Intent.ACTION_VIEW, Uri.parse(destinationURL.toString()) );
              i.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );

              try {

                getContext().startActivity( i );

              } catch (Exception e) {

                Log.e( MdotMManager.LOG_TAG, "Could not open browser on ad click to " + destinationURL, e );
              }
            }

            // Lets reset clickable and progress flag 
            adNetworkCompleted();
          }
        }.start();	  
      } else {	
        Log.w(MdotMManager.LOG_TAG, "ad selection under progress");
      }  
    }else{
      Log.w(MdotMManager.LOG_TAG, "selected ad is null");
    }
  }

  private void adNetworkCompleted(){
    Log.w(MdotMManager.LOG_TAG, "On ad network completed");
    setClickable(true);
    setAdSelectionInProgress(false);
    isAdClicked = false;
    hideActivityIndicator();
  }

  private void showActivityIndicator(){
    post(new Thread(){
      public void run() {
        if (activityIndicator != null) {
          activityIndicator.setVisibility(VISIBLE);
          activityIndicator.setBackgroundDrawable(defaultBackground);
        }
      }
    });
  }

  private void hideActivityIndicator(){
    post(new Thread(){
      public void run() {
        if (activityIndicator != null) {
          activityIndicator.setVisibility(INVISIBLE);
          setBackgroundDrawable(defaultBackground);
        }
      }
    });
  }

  private void setAdSelectionInProgress(boolean isProgress){
    this.adSelectionInProgress =  isProgress;
  }

  private boolean isAdSelectionInProgress(){
    return adSelectionInProgress;
  }

  private  String generateURLString(){
    StringBuilder urlBuilder = new StringBuilder(MDOTM_BASE_URL);

    // Ad (AdWhirl SDK) version 
    urlBuilder.append("appver=");
    urlBuilder.append(AdWhirlUtil.VERSION);

    // Android OS version 
    urlBuilder.append("&v=");
    urlBuilder.append(MdotMManager.getSystemVersion());

    urlBuilder.append("&apikey=");
    urlBuilder.append("mdotm");

    urlBuilder.append("&appkey=");
    urlBuilder.append(MdotMManager.getAppKey());

    urlBuilder.append("&deviceid=");
    try {
      urlBuilder.append(MdotMManager.getDeviceId( getContext() ));
    } catch (NoSuchAlgorithmException e) {
      urlBuilder.append("00000000000000000000000000000000");
    }

    urlBuilder.append("&width=");
    urlBuilder.append(320);

    urlBuilder.append("&height=");
    urlBuilder.append(50);

    urlBuilder.append("&fmt=");
    urlBuilder.append("json"); 

    urlBuilder.append("&ua=");
    try {
      urlBuilder.append( URLEncoder.encode(MdotMManager.getUserAgent(), "UTF-8") );
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    } 

    urlBuilder.append("&test=");
    urlBuilder.append(MdotMManager.getTestModeValue());

    return urlBuilder.toString();
  }

  private void fetchAd(){
    Log.d(MdotMManager.LOG_TAG, "  fetching Ad started ");

    String urlString =  generateURLString();
    Log.d(MdotMManager.LOG_TAG, "   Genrerated url "+urlString);

    HttpResponse httpResponse;
    HttpClient httpClient = new DefaultHttpClient();
    HttpGet httpGet = new HttpGet(urlString); 

    try {        	
      httpResponse = httpClient.execute(httpGet);
      Log.d(MdotMManager.LOG_TAG, httpResponse.getStatusLine().toString());

      HttpEntity entity = httpResponse.getEntity();

      if (entity != null) {
        InputStream inputStream = entity.getContent();
        String jsonString = convertStreamToString(inputStream);  
        AdUnit adunit =  generateAdUnitFromJsonString(jsonString);
        initializeAdView(adunit, getContext());
      }
    } catch (ClientProtocolException e) {
      Log.e(MdotMManager.LOG_TAG, "Caught ClientProtocolException in getCustom()", e);
    } catch (IOException e) {
      Log.e(MdotMManager.LOG_TAG, "Caught IOException in getCustom()", e);
    }	
  }


  private void initializeAdView(AdUnit ad, Context context){

    if ( ad != null ) {

      this.adLandingUrl = ad.landingUrl;
      // The user can interact with the ad by clicking on it.
      setFocusable(true);
      setClickable(true);

      Bitmap icon = fetchImage(ad.imageUrl, false);

      padding = 8;

      if( icon != null ) {
        // put it into a view and push it into the ad.

        activityIndicator = new ProgressBar(getContext());
        activityIndicator.setIndeterminate(false);
        LayoutParams activityParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        activityIndicator.setLayoutParams(activityParams);
        activityIndicator.setVisibility(INVISIBLE);
        activityIndicator.setMinimumHeight(20);
        activityIndicator.setMinimumWidth(20);
        activityIndicator.setMax(100);
        activityIndicator.setBackgroundDrawable(defaultBackground);                  

        // set the attributes.
        if ( ad.adType == AdUnit.AD_BANNER ) {
          createAdWithBannerView(icon );
          if(activityIndicator !=  null){
            activityIndicator.setId(2); // for alignment.
            activityParams.addRule(ALIGN_PARENT_RIGHT);
          }

        } else if ( ad.adType == AdUnit.AD_ICON_WITH_TEXT_MESSAGE ) {

          createAdWithIconView(icon, ad);
          if(activityIndicator !=  null){
            activityIndicator.setId(3); // for alignment.
            activityParams.addRule(ALIGN_PARENT_RIGHT);
          }

        } else {

          Log.w(MdotMManager.LOG_TAG, "Woooo!! unable to display ad, We got unsupported ad type.");
          onAdViewFailure();
          return;
        }         

        if(activityIndicator != null){

          addView(activityIndicator);
        }

      }

      drawBackgroundView(true);

      final int visibility = MdotMView.super.getVisibility();
      setVisibility(visibility);
      onAdViewSuccess();

    }else{

      Log.w(MdotMManager.LOG_TAG, "Ad is not loaded");
      onAdViewFailure();
      return;
    }
  }


  private void onAdViewSuccess(){

    if( listener !=  null ){
      listener.adRequestCompletedSuccessfully(this);
    } else {
      Log.d(MdotMManager.LOG_TAG, "  Unable to call mdotmListenres while success of AdView ");
    }
  }


  private void onAdViewFailure(){

    if( listener !=  null){	   

      listener.adRequestFailed(this);

    }else{

      Log.d(MdotMManager.LOG_TAG, " Unable to call mdotmListenres  while failure of AdView");
    }
  }


  private void createAdWithBannerView(Bitmap icon){

    ImageView bannerView = new ImageView(getContext());
    bannerView.setImageBitmap(icon);

    //padding = (AdView.HEIGHT - icon.getHeight())/2;
    padding = (52 - icon.getHeight())/2;
    LayoutParams params =	new LayoutParams(  ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
    params.setMargins(padding, padding, 0, padding);

    bannerView.setLayoutParams(params);
    bannerView.setId(1);
    addView(bannerView);
  }


  private void createAdWithIconView(Bitmap icon, AdUnit ad){  
    ImageView iconView = new ImageView(getContext());
    iconView.setImageBitmap(icon);

    LayoutParams  params = new LayoutParams(icon.getWidth(), icon.getHeight());
    params.setMargins(padding, padding, 0, padding);
    iconView.setLayoutParams(params);

    iconView.setId(1);
    addView(iconView);

    TextView  adTextView = new TextView( getContext());
    adTextView.setText( ad.adText);
    adTextView.setTypeface(AD_FONT);
    adTextView.setTextColor(textColor);
    adTextView.setTextSize(13.0f);
    adTextView.setId(2);

    LayoutParams adTextViewParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    // if the icon was not null then align with that too.
    if(icon != null){    	

      adTextViewParams.addRule(RIGHT_OF, 1);
    }

    //left top right  bottom
    adTextViewParams.setMargins( 20, 4, 10, 10 );
    adTextViewParams.addRule(CENTER_IN_PARENT );
    //adTextViewParams.addRule(ALIGN_PARENT_TOP);
    adTextView.setLayoutParams(adTextViewParams);

    addView(adTextView);
    setGravity(CENTER_HORIZONTAL | CENTER_VERTICAL);
  }


  private AdUnit generateAdUnitFromJsonString(String jsonString) {

    if(jsonString == null ){
      return  null;
    }

    jsonString = jsonString.replace("[", "");
    jsonString = jsonString.replace("]", "");

    if(jsonString.trim().length() < 1){
      Log.d(MdotMManager.LOG_TAG, "Neglecting, Invalid AD response.");
      return  null;
    }

    JSONObject jsonObject =  null;
    try {

      jsonObject =  new JSONObject(jsonString);

    } catch (JSONException exception) {

      Log.d(MdotMManager.LOG_TAG, "Caught JSONException while creating JSON object from string:  "+jsonString);
      jsonObject =  null;
      exception.printStackTrace();
    }

    if( jsonObject == null ){

      return null;

    }

    AdUnit adUnit =  new AdUnit();

    try {

      adUnit.adType = jsonObject.getInt("ad_type");
      adUnit.launchType = jsonObject.getInt("launch_type");
      adUnit.adText=jsonObject.getString("ad_text");
      adUnit.imageUrl=jsonObject.getString("img_url");
      adUnit.landingUrl=jsonObject.getString("landing_url");
      Log.d(MdotMManager.LOG_TAG, "Created MdotM adUnit successfully");

    } catch (JSONException e) {

      adUnit =  null;
      Log.d(MdotMManager.LOG_TAG, "Caught JSONException in generateAdUnitFromJsonString()", e);
      e.printStackTrace();
    }

    return adUnit;
  }

  private String convertStreamToString(InputStream inputStream) {
    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream), 8192);
    StringBuilder sb = new StringBuilder();

    String line = null;
    try {
      while ((line = reader.readLine()) != null) {
        sb.append(line + "\n");
      }
    } catch (IOException e) {
      Log.e(AdWhirlUtil.ADWHIRL, "Caught IOException in convertStreamToString()", e);
      return null;
    } finally {
      try {
        if( inputStream !=  null ) {
          inputStream.close();
        }
      } catch (IOException e) {
        Log.e(AdWhirlUtil.ADWHIRL, "Caught IOException while closing inputStream in convertStreamToString()", e);
      }

      try {
        if( reader !=  null ) {
          reader.close();
        }
      } catch (IOException e) {
        Log.e(AdWhirlUtil.ADWHIRL, "Caught IOException while closing reader in convertStreamToString()", e);	
      }	
    }

    if (sb == null) {
      return  null;
    }

    return sb.toString();
  }

  private static Bitmap fetchImage( String imageURL, boolean useCaches ) {
    Bitmap image = null;
    if ( imageURL != null ) {
      InputStream is = null;
      try{
        // Open a new URL and get the InputStream to load data from it.
        URL url = new URL( imageURL );
        URLConnection conn = url.openConnection();
        conn.setConnectTimeout( 0 );
        conn.setReadTimeout( 0 );
        conn.setUseCaches( useCaches );
        conn.connect();
        is = conn.getInputStream();

        // Create a bitmap.
        image = BitmapFactory.decodeStream( is );

        // Note we do not want to save the image to the file system. 
      } catch (Throwable e) {
        image = null;
        Log.w( MdotMManager.LOG_TAG, "Problem while fetchImage()  :  " + imageURL, e );
      } finally {
        if ( is != null ){
          try{
            is.close();		
          }catch (IOException e) {
            // We are ignoring and continuing 
          }
        }
      }
    }else{
      Log.w( MdotMManager.LOG_TAG, "Image url is null" );
    }
    return image;
  }


  public void setTextColor(int textColor) {
    this.textColor = textColor;
  }

  public int getTextColor() {
    return textColor;
  } 

  public void setBackgroundColor(int backgroundColor) {
    this.backgroundColor = backgroundColor;
  }

  public int getBackgroundColor() {
    return backgroundColor;
  }

  public void setListener(MdotMActionListener listener) {
    this.listener = listener;
  }

  public MdotMActionListener getListener() {
    return listener;
  }

  public boolean onTouch(View v, MotionEvent event) {
    return false;
  }

  public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
    return false;
  } 
}

class AdUnit {	
  /**
   * Example response from server 
	[{"img_url":"http:\/\/ads.mdotm.com\/ads\/test\/2-320-static.png",
		"landing_url":"http:\/\/www.mdotm.com\/apps\/click\/mdotm\/76af29cc28109e55d57bd707fbec72c4\/38aa07b5\/39F71E34-F5E7-5EE1-97F0-53743753AF54",
		"ad_type":1,
		"ad_text":"",
		"launch_type":1}]
   */

  public static final int AD_BANNER = 1;
  public static final int AD_ICON_WITH_TEXT_MESSAGE = 2;

  public static final int AD_LAUNCH_TYPE_ON_SAFARI= 1;
  public static final int AD_LAUNCH_TYPE_ON_CANVAS = 2;

  public int adType;
  public int launchType;
  public String adText;
  public String landingUrl;
  public String imageUrl;

  public AdUnit(){		

  }	
}

class MdotMManager {
  public static final String LOG_TAG = "MdotM SDK";
  private static final String SDK_VERSION_DATE = "20101012";

  private static String appKey;
  private static String devicId;
  private static String userAgent;
  private static boolean testMode = false;
  private static String systemVersion = null;

  public static int getTestModeValue(){
    // If app under test mode 1 other wise 2
    return ( isTestMode() ) ? 1 : 2 ;
  }

  private static boolean isTestMode(){	
    return testMode;
  }

  public static String getDeviceId( Context context ) throws NoSuchAlgorithmException{
    if(( devicId == null ) || (devicId.length() < 32 )){
      TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
      devicId = telephonyManager.getDeviceId();
    }
    Log.d(MdotMManager.LOG_TAG, devicId);
    return devicId;
  }


  /**
   * Generates the User-Agent HTTP header used by the browser on this phone.
   * 
   * @return The user-agent for the WebKit browser specific to this phone.
   */
  static String getUserAgent() {

    if ( userAgent == null ) {            
      // This is copied from the Android source frameworks/base/core/java/android/webkit/WebSettings.java.
      // It is somewhat of a hack since Android does not expose this to us but AdMob servers need it
      // for ad picking.
      StringBuffer arg = new StringBuffer();

      // Android version
      final String version = Build.VERSION.RELEASE;
      if ( version.length() > 0 ){
        arg.append( version );
      } else {
        // default to "1.0"
        arg.append( "1.0" );
      }
      arg.append( "; " );

      // Initialize the mobile user agent with the default locale.
      final Locale l = Locale.getDefault();
      final String language = l.getLanguage();
      if ( language != null ) {

        arg.append( language.toLowerCase() );
        final String country = l.getCountry();

        if ( country != null ) {

          arg.append( "-" );
          arg.append( country.toLowerCase() );
        }

      } else{
        // default to "en"
        arg.append( "en" );
      }

      // Add the device model name and Android build ID.
      final String model = Build.MODEL;
      if ( model.length() > 0 ) {

        arg.append( "; " );
        arg.append( model );
      }

      final String id = Build.ID;
      if ( id.length() > 0 ) {

        arg.append( " Build/" );
        arg.append( id );
      }

      // Mozilla/5.0 (Linux; U; Android 1.0; en-us; dream) AppleWebKit/525.10+ (KHTML, like Gecko) Version/3.0.4 Mobile Safari/523.12.2 (AdMob-ANDROID-20090701)
      final String base = "Mozilla/5.0 (Linux; U; Android %s) AppleWebKit/525.10+ (KHTML, like Gecko) Version/3.0.4 Mobile Safari/523.12.2 (AdWhirl-MdotM-ANDROID-%s)";
      userAgent = String.format( base, arg, SDK_VERSION_DATE );

      if ( Log.isLoggable( LOG_TAG, Log.DEBUG ) ) {	
        Log.d( LOG_TAG, "Phone's user-agent is:  " + userAgent );
      }
    }
    return userAgent;
  }


  public static String getSystemVersion(){
    if( ( systemVersion == null ) || ( systemVersion.length() == 0 ) ){
      final String version = Build.VERSION.RELEASE;
      if ( version.length() > 0 ){
        systemVersion = version;
      } else {
        // default is "1.0"
        systemVersion = "1.0";
      }
    }
    return systemVersion;
  }

  public static void setPublisherId(String publisherId) {
    if( ( publisherId == null ) || ( publisherId.length() != 32 ) ){
      if( MdotMManager.appKey == null ){

        MdotMManager.appKey = "76af29cc28109e55d57bd707fbec72c4";
      }
    }else {
      MdotMManager.appKey = publisherId;
    }
  }

  public static String getAppKey() {
    return appKey;
  }
}