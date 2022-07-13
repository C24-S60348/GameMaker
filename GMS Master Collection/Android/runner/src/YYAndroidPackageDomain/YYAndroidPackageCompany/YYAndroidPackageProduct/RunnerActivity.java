package ${YYAndroidPackageName};

import android.app.Application;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings.Secure;
import android.util.Log;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.Menu;
import android.view.MenuItem;
import android.view.InputDevice;
import android.view.WindowManager;
import android.view.Surface;
import android.widget.AbsoluteLayout;
import android.widget.EditText;


import com.android.vending.licensing.LicenseChecker;
import com.android.vending.licensing.LicenseCheckerCallback;
import com.android.vending.licensing.Obfuscator;
import com.android.vending.licensing.ServerManagedPolicy;
import com.android.vending.licensing.AESObfuscator;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.EnumSet;

import ${YYAndroidPackageName}.DemoRenderer;
import com.adwhirl.AdWhirlLayout;
import com.flurry.android.FlurryAgent;
import com.google.ads.AdView;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Tracker;
import com.google.analytics.tracking.android.GAServiceManager;
import com.mopub.mobileads.MoPubView;
import com.verizon.vcast.apps.LicenseAuthenticator;
import com.yoyogames.runner.RunnerJNILib;


 
import com.amazon.ags.api.AmazonGames;
import com.amazon.ags.api.AmazonGamesCallback;
import com.amazon.ags.api.AmazonGamesClient;
import com.amazon.ags.api.AmazonGamesFeature;
import com.amazon.ags.api.AmazonGamesStatus;

import com.amazon.ags.api.whispersync.*;
import com.amazon.ags.api.ErrorCode;
import com.amazon.ags.constants.whispersync.ConflictStrategy;
import java.io.IOException;
import java.io.FilenameFilter;

import ${YYAndroidPackageName}.Gamepad;

import org.ini4j.Ini;

//----------------------------------------------------------------------------------------------------

public class RunnerActivity extends Activity implements SensorEventListener
{
	// The Singleton for RunnerActivity
	public static RunnerActivity CurrentActivity;

	public static final String BASE64_PUBLIC_KEY = "${YYAndroidGoogleLicensingPublicKey}";
    private static final byte[] SALT = new byte[] { -5, 12, -68, 7, -12, 67, 3, 4, 4, 19, 6, 7, 16, 11, 9, 51, 71, 34, 19, 16 };

    public static final int DIALOG_CANNOT_CONNECT_ID = 1;
    public static final int DIALOG_BILLING_NOT_SUPPORTED_ID = 2;
    public static final int DIALOG_BILLING_PURCHASE_ERROR = 3;
    public static final int PREFERENCES_GROUP_ID = 1;
    public static final int SETTINGS_ID = 1;
    public static final int EXIT_ID = 2;
    
    
    public static boolean Verizon = false; // *** Verizon here
    public static boolean Flurry = false;
    public static String FlurryCode = "";
    public static boolean mbGoogleAnalytics = false;
    public static String GACode = "";
    //public static GoogleAnalyticsTracker GATracker;	//V1.5
	public static GoogleAnalytics mGaInstance;	//V2
	public static Tracker	mGaTracker;			//V2
	
	public static RunnerDownloadTask DownloadTask;
	public static DownloadStatus DownloadTaskStatus;
	public static AdvertisingPocketChange mPocketChange;

    public static String InputStringResult;
	public static int  ShowQuestionYesNo; 
	
    public static float AccelX;
    public static float AccelY;
    public static float AccelZ;
    public static int Orientation;
    public static int DefaultOrientation;
	public static String m_versionName;
    
    public static Handler ViewHandler = new Handler();
    public static int DisplayWidth;
    public static int DisplayHeight;
    public static boolean XPeriaPlay = false;
    public static boolean YoYoRunner = false;
    public static String SaveFilesDir = null;
    public static boolean FocusOverride  = false;  
	public static boolean HasFocus = false;
    
    public static IAdvertising mAdProvider;
	public static IniBundle mYYPrefs;    
	public static AmazonGames agsGameClient;
	public static boolean isLoggedInGameCircle = false;
	EnumSet<AmazonGamesFeature> agsGameFeatures;
	
	public static WhisperSyncClient mWhisperSyncClient;
		
	private static IRunnerBilling mRunnerBilling;
	public static IRunnerBilling RunnerBilling() {

		if (mRunnerBilling != null) {
			return mRunnerBilling;
		}
		else {
			Log.i("yoyo", "BILLING: Unsupported or not activated. Check Global Game Settings.");
			throw new NullPointerException();
		}
	}
    
    // License checking
    private LicenseCheckerCallback mLicenseCheckerCallback;
    private LicenseChecker mChecker;   
    

    private DemoGLSurfaceView mGLView;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;

    private static Method mSetSystemUiVisibility = null;
    
    private Handler mHandler = new Handler();
    private Handler mLicenseHandler = new Handler();
    private Runnable mUpdateTimerTask = new Runnable() {
    	public void run() {
    	}
    };
    
    private class MyLicenseCheckerCallback implements LicenseCheckerCallback 
	{
    	public void allow() 
	    {
    	    if (isFinishing()) {
        	    // Don't update UI if Activity is finishing.
            	return;
	        }
    	    Log.i("yoyo", "!!!!##### Successful license check #####!!!!!! ");
       		// Should allow user access.
       	 	//displayResult(getString(R.string.allow));
        	//displayResult( "Allow Access");
    	}

		public void dontAllow() 
    	{
        	if (isFinishing()) {
            	// Don't update UI if Activity is finishing.
	            return;
   			}
	        //displayResult(getString(R.string.dont_allow));
    	    displayResult( getString(R.string.license_fail) );
        	// Should not allow access. An app can handle as needed,
        	// typically by informing the user that the app is not licensed
        	// and then shutting down the app or limiting the user to a
        	// restricted set of features.
        	// In this example, we show a dialog that takes the user to Market.
        	//showDialog(0);
	    }
       
		public void applicationError( ApplicationErrorCode _error ) 
		{
    		// log the error
	        Log.i("yoyo", "License Error - " + _error.toString());
        	
    	    // then call dontAllow
       		dontAllow();
	    } // end applicationError
	    
	} // end MyLicenseCheckerCallback
    
    public void displayResult( final String result ) 
    {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		if (!RunnerJNILib.isNetworkConnected()) {
			builder.setMessage( "Please ensure you have an active data connection for license check" )
				.setCancelable(false)
				.setNegativeButton( "Retry", new DialogInterface.OnClickListener() {
					public void onClick( DialogInterface dialog, int id ) {
		        		mChecker.checkAccess( mLicenseCheckerCallback );
		        		dialog.dismiss();
					}});
		} // end if
		else { 
			builder.setMessage( result )
				.setCancelable(false)
				.setNegativeButton( "Retry", new DialogInterface.OnClickListener() {
					public void onClick( DialogInterface dialog, int id ) {
		        		mChecker.checkAccess( mLicenseCheckerCallback );
		        		dialog.dismiss();
					}})
				.setPositiveButton( "Buy", new DialogInterface.OnClickListener() {
					public void onClick( DialogInterface dialog, int id ) {
						 Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse( "market://details?id="+getPackageName() ));
						 startActivity(myIntent);
						 RunnerJNILib.ExitApplication();
					}
 				});
 		} // end else
		final AlertDialog alert  = builder.create();
		
    	mLicenseHandler.post( new Runnable() {
    		public void run() {
    			Log.i("yoyo", "License display"+ result );
    			
        		alert.show();
    		}
    	});
    }
    
    public boolean isTablet() {
    	try {
    		DisplayMetrics dm = getResources().getDisplayMetrics();
    		float screenWidth = dm.widthPixels / dm.xdpi;
    		float screenHeight = dm.heightPixels / dm.ydpi;
    		double size = Math.sqrt( (screenWidth*screenWidth) + (screenHeight*screenHeight) );
    		return (size >= 6);
    	} catch( Throwable _t ) {
    		Log.i( "yoyo", "Failed to compute screen size" );
    		return false;
    	} // end catch
    } // end isTablet
    
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
    	Log.i("yoyo", "onCreate");
        super.onCreate(savedInstanceState);
		RunnerJNILib.Init( this );
        
        // check to see if we are the YoYoRunner or not.
        YoYoRunner = checkIsYoYoRunner();		
		DownloadTask = new RunnerDownloadTask();        
		

		try
		{
			m_versionName = this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName;
		}
		catch (NameNotFoundException e)
		{
			m_versionName = "1.0.xxx";
			Log.v("yoyo", e.getMessage());
		}

		// set the current orientation
		switch( getResources().getConfiguration().orientation ) {
		default:
		case Configuration.ORIENTATION_LANDSCAPE:
		case Configuration.ORIENTATION_SQUARE:
			Orientation = 0;
			break;
		case Configuration.ORIENTATION_PORTRAIT:
			Orientation = 1;
			break;
		} // end switch
        
        // Grab singleton
        CurrentActivity = this;
        
        // Grab the display settings for the game
    	Display display = getWindowManager().getDefaultDisplay();
    	DisplayWidth = display.getWidth();
    	DisplayHeight = display.getHeight();
    	
        this.requestWindowFeature( Window.FEATURE_NO_TITLE );

		// Grab the sensor manager and accelerometer        
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER); 
        
        DefaultOrientation  = getDeviceDefaultOrientation();
        Log.i( "yoyo", "###@@@@!!!~~~~###### default orientation - " + DefaultOrientation);
        
        // Setup the various things needed
        checkXPeriaPlay();
        checkLicensing();
        setupView();		
    }

	public void setupIniFile()
	{		
		// Tidier version
		if (YoYoRunner)
		{
			// Get file path for where things would be if we loaded them from USB
			String apkFilePath = null;
			String saveFilesDir = null;

			saveFilesDir = Environment.getExternalStorageDirectory() + "/GMstudio";			
			saveFilesDir = saveFilesDir + '/';

			apkFilePath = saveFilesDir + DemoRenderer.kGameAssetsDROID;	
			
			// Now test to see if the lock file exists in this location - if it doesn't the data is either not there or is stale
			File fAssets = new File( apkFilePath );
			//Log.i("yoyo", "!!! Asset file - " + m_apkFilePath + " " + fAssets.exists() + " l=" + fAssets.lastModified() );
			File fLock = new File( saveFilesDir + "GameDownload.lock" );
			//Log.i("yoyo", "!!! Lock file - " + fLock.getAbsolutePath() + " " + fLock.exists() + " l=" + fLock.lastModified() );
			if (!fLock.exists() || (fLock.exists() && (fLock.lastModified() < fAssets.lastModified())))
			{
				// I think if it gets to this point then the Runner is going to try and retrieve the asset zip via wifi
				// and therefore there's nothing we can do (we don't have it at this point)
				Log.i("yoyo", "Don't have up-to-date INI file at this point");
			}
			else
			{
				Log.i("yoyo", "File Path for INI:: " + apkFilePath );

				// Now load the ini file
				InputStream is = null;
				Bundle bundle = null;
				try {
					ApplicationInfo ai = getPackageManager().getApplicationInfo(this.getComponentName().getPackageName(), PackageManager.GET_META_DATA);
					bundle = ai.metaData;        	 

					ZipFile zip = new ZipFile( apkFilePath );
					Enumeration<? extends ZipEntry > zipEntries = zip.entries();
					long offset = 0;
					while( zipEntries.hasMoreElements() ) {
						ZipEntry entry = (ZipEntry)zipEntries.nextElement();

						if (entry.getName().indexOf(".ini") > 0) {
							Log.d( "yoyo", "found INI file - " + entry.getName() );
							is = zip.getInputStream( entry );
							break;
						} // end if

					} // end while

				} catch( Exception _e ) {
					Log.d( "yoyo", "Exception while setting up Ini" +_e.toString() );
				} // end catch
				mYYPrefs = new IniBundle( bundle, is );
				Log.i("yoyo", "INI loaded" );
			}				
		}
		else
		{
			// Just use manifest file if this is a package
			Log.i("yoyo", "Loading INI from manifest file" );
			Bundle bundle = null;
			try {
				ApplicationInfo ai = getPackageManager().getApplicationInfo(this.getComponentName().getPackageName(), PackageManager.GET_META_DATA);
				bundle = ai.metaData;   
				
			} catch( Exception _e ) {
				Log.d( "yoyo", "Exception while setting up Ini" +_e.toString() );
			} // end catch

			mYYPrefs = new IniBundle( bundle, null );				
			Log.i("yoyo", "INI loaded" );
		}
	}

	public void doSetup( String _zipName )
	{
		Log.d( "yoyo", "doSetup called - " + _zipName );

		// If we haven't successfully loaded the INI file, try again here
		// This should only be the case if this is the Runner and it's pulling the data across wifi
		if (mYYPrefs == null)
		{
			InputStream is = null;
			Bundle bundle = null;
			try {
				ApplicationInfo ai = getPackageManager().getApplicationInfo(this.getComponentName().getPackageName(), PackageManager.GET_META_DATA);
    			bundle = ai.metaData;        	 

				ZipFile zip = new ZipFile( _zipName );
				Enumeration<? extends ZipEntry > zipEntries = zip.entries();
				long offset = 0;
				while( zipEntries.hasMoreElements() ) {
					ZipEntry entry = (ZipEntry)zipEntries.nextElement();

					if (entry.getName().indexOf(".ini") > 0) {
						Log.d( "yoyo", "found INI file - " + entry.getName() );
						is = zip.getInputStream( entry );
						break;
					} // end if

				} // end while

			} catch( Exception _e ) {
				Log.d( "yoyo", "Exception while setting up Ini" +_e.toString() );
			} // end catch
			mYYPrefs = new IniBundle( bundle, is );
		}

		// RK :: moved this line so that AmazonGameCircle is available if testing over Wifi...
		if (RunnerActivity.mYYPrefs != null) {
			RunnerJNILib.SetKeyValue( 6, RunnerActivity.mYYPrefs.getInt("YYAmazonGameCircle"), "" );
		} // end if
	    setupAdvertising();  
		setupFlurry();
		//make sure we only have one or the other!
		if( !Flurry ) {
			setupGoogleAnalytics();
		}
        setupInAppBilling();
        setupVerizon();
        setupAmazonGameCircle();
		Gamepad.EnumerateDevices(mYYPrefs);

		DemoRenderer.m_state = DemoRenderer.eState.DoStartup;
	} // end doSetup

    
    /* 
     * Called when the activity is being started up 
     */
    @Override
    protected void onStart() {    

    	Log.i("yoyo", "onStart");
        super.onStart();
                
        // registerUnlockReceiver();
        
        if (Flurry) {
	        Log.i( "yoyo", "@@@@@@@ Flurry session started code = " + FlurryCode);
			FlurryAgent.setReportLocation(false);
			FlurryAgent.setLogEvents(true);
        	FlurryAgent.onStartSession( this, FlurryCode );
        } // end if
		if(mbGoogleAnalytics)
		{
			mGaTracker.sendView("/GameStart");
		}
    }
    
    /**
     * Called when this activity is no longer visible.
     */
    @Override
    protected void onStop() {
    
    	Log.i("yoyo", "onStop");
        super.onStop();		
        
		// Not doing this now as onStop() occurs when going off to the Market app and we need to
		// still be listening for a response from the market even whilst stopped or it may send its
		// response before our application has been fully reawoken
        // if (checkCallingOrSelfPermission("com.android.vending.BILLING")==0) {        	
	    //     RunnerBillingResponseHandler.unregister(mBillingPurchaseObserver);	        	        
	    // } 
	    
        if (Flurry) {
	        Log.i( "yoyo", "@@@@@@@ Flurry session stopped code = " + FlurryCode);
        	FlurryAgent.onEndSession( this );
        } // end if
        if(mbGoogleAnalytics)
		{
			// We send an empty event so we get accurate time-on-page/site info.
			mGaTracker.sendEvent("", "", "", null);
			//do a manual dispatch to make sure all events are sent...
			GAServiceManager.getInstance().dispatch();
		}
        
    }
    
    /**
     * Called when this activity is being destroyed
     */
    @Override
    protected void onDestroy() {
    
    	Log.i("yoyo", "onDestroy");
        super.onDestroy();

		if (mRunnerBilling != null) {
			mRunnerBilling.Destroy();
		}
	    
	    if( mbGoogleAnalytics )
	    {
			//GATracker.stopSession();
			mGaInstance.closeTracker(mGaTracker);
	    }
	    
	    // Kill the activity completely!
	    java.lang.System.exit(0);
    }

	protected void resumeApp()
	{
		Gamepad.EnumerateDevices(mYYPrefs);		

    	if (mGLView != null) {
			
			mGLView.onResume();
			setSystemUiVisibility( 0x1);
		}
		if (RunnerJNILib.ms_context != null) {					
		
			Log.i("yoyo", "Resuming the C++ Runner/resetting GL state");
			RunnerJNILib.Resume(0);
		}
		
		if(mAdProvider!= null)
		{
			mAdProvider.onResume();
		}
	}
    
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
    		
    	Log.i("yoyo", "onWindowFocusChanged(" + hasFocus + "|" + FocusOverride + ")");
	    super.onWindowFocusChanged(hasFocus);

    	if (hasFocus|FocusOverride) {
			HasFocus = true;

			if (mGLView.mRenderer.m_pauseRunner == true)
			{
				resumeApp();

    			if (RunnerJNILib.ms_context != null) {		
					// Cause the MP3 player to resume playing the MP3 that was playing when paused
					RunnerJNILib.RestoreMP3State();
				}
				
				// Unblock onDrawFrame() that causes the runner to process
				mGLView.mRenderer.m_pauseRunner = false;
			}
    	}
    	else {
			HasFocus = false;

    		// Block onDrawFrame() that causes the runner to process
	    	mGLView.mRenderer.m_pauseRunner = true;
    	
    		// Store the state of the MP3 player so that we can resume playing the current MP3 from the same spot on resume
	    	RunnerJNILib.StoreMP3State();
    		// Stops all sounds and requests that it doesn't attempt to play anymore until we've resumed
    		RunnerJNILib.StopMP3();     		    		
    	}
    	FocusOverride = false;
    }
    
    @Override
    protected void onPause() {		

		Log.i("yoyo", "onPause");
		super.onPause();

    	mSensorManager.unregisterListener(this);
    	
    	Log.i("yoyo", "Pausing the Runner");
		RunnerJNILib.Pause(0);
				
		if (mGLView != null) {
		    setSystemUiVisibility( 0x0 );
	    	mGLView.onPause();
    	}

		mGLView.mRenderer.m_pauseRunner = true;
		if(mAdProvider!= null)
		{
			mAdProvider.onPause();
		}
	}
   
    @Override
    protected void onResume() {

		// As per https://developer.nvidia.com/sites/default/files/akamai/mobile/docs/android_lifecycle_app_note.pdf:
		// onResume technically means the start of the "foreground" lifespan of the app, but it does not mean that
		// the app is fully visible and should be rendering, so game updating/rendering does not occur here
		Log.i("yoyo", "onResume");
    	super.onResume();		

		// If we never lost focus, resume
		if (HasFocus) {	
			Log.i("yoyo", "App still has focus");
			if (mGLView.mRenderer.m_pauseRunner == true)
			{
				Log.i("yoyo", "Runner is paused - unpausing");
				resumeApp();

    			if (RunnerJNILib.ms_context != null) {		
					// Cause the MP3 player to resume playing the MP3 that was playing when paused
					RunnerJNILib.RestoreMP3State();
				}
				
				// Unblock onDrawFrame() that causes the runner to process
				mGLView.mRenderer.m_pauseRunner = false;
			}
		}

		mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
	}		
    
    @Override
    public boolean onCreateOptionsMenu( Menu menu )
    {
    	super.onCreateOptionsMenu( menu );
    	
    	if (YoYoRunner) {
	    	menu.add( PREFERENCES_GROUP_ID, SETTINGS_ID, 0, R.string.menu_settings );
    		menu.add( PREFERENCES_GROUP_ID, EXIT_ID, 0, R.string.menu_exit);
		} // end if
    	
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected( MenuItem item )
    {
    	switch( item.getItemId() ) {
		case SETTINGS_ID: 
			Intent settingsActivity = new Intent( getBaseContext(), RunnerPreferenceActivity.class );
			startActivity( settingsActivity );
			break;
		case EXIT_ID:
    		AlertDialog.Builder builder = new AlertDialog.Builder(this);
    		builder.setMessage( "Are you sure you want to exit?")
    			.setCancelable(false)
    			.setPositiveButton( "Yes", new DialogInterface.OnClickListener() {
    				public void onClick( DialogInterface dialog, int id ) {
    					RunnerJNILib.ExitApplication();
    				}
     			})
    			.setNegativeButton( "No", new DialogInterface.OnClickListener() {
    				public void onClick( DialogInterface dialog, int id ) {
    					dialog.cancel();    					
    				}
    			});
    		AlertDialog alert  = builder.create();
    		alert.show();
			break;
    	} // end switch
    	
    	return true;
    } // end onOptionsItemSelected
    
    @Override
    public boolean onKeyDown( int keyCode, KeyEvent event ) {

    	// record the key events ready to be passed down to the game	
    	if (keyCode != 0) {
	    	RunnerJNILib.KeyEvent( 0, keyCode );
	    }
    	
    	if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP) || 
    		(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) || 
    		(keyCode == KeyEvent.KEYCODE_HOME) || 
    		(keyCode == KeyEvent.KEYCODE_MENU))
	  		return super.onKeyDown(keyCode, event );  	
	  	else
	    	return true;
    }
    
    @Override
    public boolean onKeyUp( int keyCode, KeyEvent event ) {

    	RunnerJNILib.KeyEvent( 1, keyCode );
    	if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP) || 
    		(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)|| 
    		(keyCode == KeyEvent.KEYCODE_HOME)|| 
    		(keyCode == KeyEvent.KEYCODE_MENU))
	  		return super.onKeyUp(keyCode, event );  	
	  	else
	    	return true;
    } 

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}
	
	public int getDeviceDefaultOrientation(){

		try {
			Class<?> displayClass = Class.forName("android.view.Display");

			// API level 8
			Method getRotationMethod = displayClass.getDeclaredMethod("getRotation");
			if (getRotationMethod != null) { 						
				WindowManager lWindowManager =  (WindowManager) getSystemService(WINDOW_SERVICE);

				Configuration cfg = getResources().getConfiguration();
				int lRotation = lWindowManager.getDefaultDisplay().getRotation();

				if( (((lRotation == Surface.ROTATION_0) ||(lRotation == Surface.ROTATION_180)) &&   
						(cfg.orientation == Configuration.ORIENTATION_LANDSCAPE)) ||
						(((lRotation == Surface.ROTATION_90) ||(lRotation == Surface.ROTATION_270)) &&    
						(cfg.orientation == Configuration.ORIENTATION_PORTRAIT))){
					return Configuration.ORIENTATION_LANDSCAPE;
				}
			} // end if
		}
		catch (Exception e) {
			Log.i("yoyo", "ERROR: Enumerating API level " + e.getMessage());
		}
		return Configuration.ORIENTATION_PORTRAIT;
	}

	@Override
	public void onSensorChanged(SensorEvent event) {			
		switch( DefaultOrientation ) {
		default:
		case Configuration.ORIENTATION_PORTRAIT:
			AccelX = event.values[0] / 9.80665f;
			AccelY = event.values[1] / 9.80665f;
			AccelZ = event.values[2] / 9.80665f;
			break;
		case Configuration.ORIENTATION_LANDSCAPE:
			AccelX = event.values[1] / 9.80665f;
			AccelY = -event.values[0] / 9.80665f;
			AccelZ = event.values[2] / 9.80665f;
			break;				
		} // end switch
	}
	
	@Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    
    	Log.i("yoyo", "Got activity result: " + resultCode);
        super.onActivityResult(requestCode, resultCode, data);
        
        
        // It's shit to have to infest RunnerActivity with this, but there you go...
		if (RunnerFacebook.msFacebook != null) {
			RunnerFacebook.msFacebook.authorizeCallback(requestCode, resultCode, data);
		} // end if
    } 
     
    @Override
    protected Dialog onCreateDialog(int id) 
    {
        switch (id) 
        {
        	case DIALOG_CANNOT_CONNECT_ID:
            	return createDialog(R.string.cannot_connect_title, R.string.cannot_connect_message);
            	
        	case DIALOG_BILLING_NOT_SUPPORTED_ID:
            	return createDialog(R.string.billing_not_supported_title, R.string.billing_not_supported_message);
            	
            case DIALOG_BILLING_PURCHASE_ERROR:
            	return createDialog(R.string.billing_failed_title, R.string.billing_failed_message);
            	
	        default:
    	        return null;
        }
    }

	@Override
	public boolean dispatchKeyEvent(KeyEvent event)
	{		
		Gamepad.handleKeyEvent(event);		
		return super.dispatchKeyEvent(event);
	}

	@Override
	public boolean dispatchGenericMotionEvent(MotionEvent event)
	{
		if ((event.getSource() == InputDevice.SOURCE_JOYSTICK) || (event.getSource() == InputDevice.SOURCE_GAMEPAD)) {
			Gamepad.handleMotionEvent(event);
			return true;
		} // end if
		return super.dispatchGenericMotionEvent(event);
	}
    
    /* Checks that this activity really is the YoYo Runner */
    private boolean checkIsYoYoRunner() {
    
    	boolean yoyoRunner = false;
		try {
			InputStream is = getResources().getAssets().open( "game.droid" );
			if (is != null) {
				yoyoRunner = false;
				Log.i( "yoyo", "#######!!!!!!! Checking for runner - found assets " );
			} // end if
			else {
				yoyoRunner = true;
				Log.i( "yoyo", "#######!!!!!!! Checking for runner - not found assets" );
			} // end else
			is.close();
		} 
		catch( Exception e ) {
			yoyoRunner = true;
			Log.i( "yoyo", "#######!!!!!!! Checking for runner! failed");
		} // end catch
		
		return yoyoRunner;
    }
    
    
  
    
    
    /* If advertising is enable for this game set it up using this routine */
    private void setupAdvertising() {
		//try {
			//ApplicationInfo ai = getPackageManager().getApplicationInfo(this.getComponentName().getPackageName(), PackageManager.GET_META_DATA);
        	//Bundle bundle = ai.metaData;   
        	boolean UseTestAds = false;
        	
        	String DeviceID=null;
        	if(YoYoRunner)
        	{
        		String TAString = mYYPrefs.getString("UseTestAds");
	        	
	        	
        		if(TAString!= null)
        		{
	        		UseTestAds = true;
        		}
        		DeviceID = mYYPrefs.getString("DeviceId");
	    
        	}
        	
        	
        	String PCString = mYYPrefs.getString("YYAndroidPocketchangeID");
        	
        	if(PCString!= null)
        	{
        		Log.i("yoyo", "XXXXXX Creating Pocketchange XXXXX ");
        		mPocketChange = new AdvertisingPocketChange(this,PCString,UseTestAds);
        	}
        	else
        	{
        		Log.i("yoyo", "XXXXXX PocketChange ID not found XXXXX ");
        	}
        	     	 
        	boolean advertising = mYYPrefs.getBoolean("YYAdvertising");        	 
			//Display d = this.getWindowManager().getDefaultDisplay();
	    	//FrameLayout.LayoutParams adWhirlLayoutParams = new FrameLayout.LayoutParams(0, 0, d.getWidth(), 72);
       		AbsoluteLayout layout = (AbsoluteLayout)findViewById(R.id.ad);
			if (advertising) {
	        	String value = mYYPrefs.getString("YYAdvertisingProvider");        	 
	        	Log.d("yoyo", "YYAdvertisingProvider value is =" +  value);

				IAdvertising ad = null;
				if (value.equals( "MOPUB" )) {
					ad = new AdvertisingMopub( this, layout,UseTestAds );						
					
				} // end if
				else
				if (value.equals( "ADMOB" )) {
					ad = new AdvertisingAdMob( this, layout,UseTestAds,DeviceID );						
				} // end if
				else
				if (value.equals( "MILLENNIAL MEDIA" )) {
					ad = new AdvertisingMillenialMedia( this, layout,UseTestAds );						
				} // end if
				else if(value.equals("ADCOLONY"))
				{			
					boolean useV4VC = mYYPrefs.keyExists("UseV4VC"); ;
					if(useV4VC)
					{
					//	useV4VC = true;
						Log.i("yoyo","found v4vc key");
					}
					else
					{
						Log.i("yoyo","no v4vc key");
					}
					ad = new AdvertisingAdColony(this,layout,UseTestAds,useV4VC);
				}
				else if(value.equals("PLAYHAVEN"))
				{
					ad = new AdvertisingPlayHaven(this,layout,UseTestAds);
				}

				// now set the definitions up
				if (ad != null) {
		        	String key = mYYPrefs.getString("YYAdvertisingKey");        	 
					String[] keys = key.split("\\|");
					for( int i=0; i<keys.length; i+=2) {
						String k = keys[i];
						String v = ((i+1) < keys.length) ? keys[i+1] : "0";
						Log.d( "yoyo", "key = " + k + " t=" + v );
						ad.define( i/2, k, AdvertisingBase.ConvertToAdType(Integer.parseInt(v)) );
					} // end for
				} // end if

				mAdProvider = ad;
			} // end if
		//} 
    
    }
    
    AmazonGamesCallback agsGameCallback = new AmazonGamesCallback() {
        @Override
        public void onServiceReady() {
	       Log.d("yoyo", "Game Circle :: OK!");
	       RunnerActivity.isLoggedInGameCircle = true;
        }
         
        @Override
        public void onServiceNotReady(AmazonGamesStatus reason) {
            switch (reason) {
            case CANNOT_AUTHORIZE:
                /**
                 * The service could not authorize the client. This should only
                 * occur if the network is not available the first time the game
                 * attempts to connect.
                 */
	       		Log.d("yoyo", "Game Circle :: CANNOT_AUTHORIZE");
	       		break;
            case CANNOT_BIND:
                /**
                * The service could not authorize the client. This
                * should only occur if the network is not available the
                * first time the game attempts to connect.
                */
	       		Log.d("yoyo", "Game Circle :: CANNOT_BIND");
	       		break;
            case NOT_AUTHENTICATED:
                /**
                 * The device is not registered with an account.
                 */
	       		Log.d("yoyo", "Game Circle :: NOT_AUTHENTICATED");
	       		break;
            case NOT_AUTHORIZED:
                /**
                 * The game in not authorized to use the service. Check your
                 * package name and signature registered in the Developer's
                 * Portal.
                 */
	       		Log.d("yoyo", "Game Circle :: NOT_AUTHORIZED");
	       		break;
            case SERVICE_NOT_OPTED_IN:
                /**
                 * The device is not opted-in to use the service.
                 */
	       		Log.d("yoyo", "Game Circle :: SERVICE_NOT_OPTED_IN");
	       		break;
                              
            default:
	       		Log.d("yoyo", "Game Circle :: Service Not ready unknown reason...");
                break;
            }
        }
    };
    
    private void setupAmazonGameCircle()
    {
       	int value = mYYPrefs.getInt("YYAmazonGameCircle");        	 
       	Log.d("yoyo", "Amazon Game Circle value =" +  value);
       	if (value != 0) {
       	
	       	Log.d("yoyo", "Initializing Game Circle");			
			agsGameFeatures = EnumSet.noneOf(      AmazonGamesFeature.class );
			// CH: I know this is an arse about tit way of doing it but I don't know Java well enough to do it the right way round
			if ((value & 0x1) != 0) {
				Log.d("yoyo", "Game Circle uses achievements");
				agsGameFeatures.add(AmazonGamesFeature.Achievements);
			}
			if ((value & 0x2) != 0) {
				Log.d("yoyo", "Game Circle uses leaderboards");
				agsGameFeatures.add(AmazonGamesFeature.Leaderboards);
			}
			if ((value & 0x4) != 0) {
				Log.d("yoyo", "Game Circle uses whispersync");
				agsGameFeatures.add(AmazonGamesFeature.Whispersync);
			}
											   
			agsGameClient = AmazonGamesClient.initialize( getApplication(), agsGameCallback, agsGameFeatures );
			mWhisperSyncClient = AmazonGamesClient.getInstance().getWhisperSyncClient();
		} // end if
										   
	} // end setupAmazonGameCircle

    /* whisper sync */
    static class WSBlobCallback implements SynchronizeBlobCallback
    {
		static final int STATUS_NEW_GAME_DATA = 0;
		static final int STATUS_ALREADY_SYNCHRONISED = 1;
		static final int STATUS_CONFLICT_DEFERRAL = 2;
		static final int STATUS_UPLOAD_SUCCESS = 3;
		static final int STATUS_ERROR=-100;
		
		public int mCallId;
		
		public WSBlobCallback(int callId) {
			mCallId = callId;
		}
		
		@Override
		public void onAlreadySynchronized() {
			// nothing to do, data is already in sync
			Log.d("yoyo", "SyncBlobCallback : onAlreadySynchronized" );
			//!need to pass "id" or "token"
			RunnerJNILib.CloudResultString( "AlreadySynchronized", STATUS_ALREADY_SYNCHRONISED, mCallId );
		}
	 
		@Override
		public void onConflictDeferral() {
			// a conflict was encountered, but the gamer chose to ignore it
			Log.d("yoyo", "SyncBlobCallback : onConflictDeferral" );
			RunnerJNILib.CloudResultString( "ConflictDeferral", STATUS_CONFLICT_DEFERRAL, mCallId );
		}
	 
		@Override
		public void onGameUploadSuccess() {
			// current data has been synchronized
			Log.d("yoyo", "SyncBlobCallback : onGameUploadSuccess" );
			RunnerJNILib.CloudResultString( "GameUploadSuccess", STATUS_UPLOAD_SUCCESS, mCallId );
		}
	 
		@Override
		public void onSynchronizeFailure(ErrorCode errorCode) {
			// sync failed
			Log.d("yoyo", "SyncBlobCallback : onSynchronizeFailure - ErrorCode " + errorCode );
			RunnerJNILib.CloudResultString( "SynchroniseFailure: " + errorCode.toString(), STATUS_ERROR - errorCode.ordinal(), mCallId );
		}
		
		@Override
		public boolean onNewGameData(byte[] data) {
			//Process the new data.  If for any reason
			//your game chooses not to, or cannot use this data
			//return false.  If it is successfully processed 
			//and local data is updated with this new information
			//return true

			Log.d("yoyo", "SyncBlobCallback : onNewGameData" );
			RunnerJNILib.CloudResultData( data, STATUS_NEW_GAME_DATA, mCallId );
			
			return true;
		}
	}
    
    //upload data to cloud
    public static void wsUploadData( String data, String desc, int id )
    {
		//Log.i("yoyo", "wsUploadData: " + data + "\n" + desc + "\n" + id );
		if( !RunnerActivity.isLoggedInGameCircle ) 
		{
			RunnerJNILib.CloudResultString( "Not logged in to GameCircle", -1, id );
			return;	
		}
		
		final int callId = id;
		SynchronizeBlobCallback blobCallback = new WSBlobCallback( callId );
		SynchronizeBlobProgressRequest syncBlobRequest = new SynchronizeBlobProgressRequest(blobCallback);
		
		//concatenate the description & data string
		byte[] descBytes = desc.getBytes();
		byte[] dataBytes = data.getBytes();
		byte[] mySaveGameByteArray = new byte[ descBytes.length + dataBytes.length + 1];	//+1 for terminator
		
		System.arraycopy(descBytes, 0, mySaveGameByteArray, 0, descBytes.length);
		System.arraycopy(dataBytes, 0, mySaveGameByteArray, descBytes.length+1, dataBytes.length);
		
		syncBlobRequest.setData(mySaveGameByteArray);
		syncBlobRequest.setDescription(desc);
		mWhisperSyncClient.synchronizeProgress(syncBlobRequest);
    }
    
    public static void wsSynchroniseData( int id )
    {
		if( !RunnerActivity.isLoggedInGameCircle ) 
		{
			RunnerJNILib.CloudResultString( "Not logged in to GameCircle", -1, id );
			return;	
		}
		
		final int callId = id;
		SynchronizeBlobCallback blobCallback = new WSBlobCallback( callId );
		SynchronizeBlobRequest syncBlobRequest = new SynchronizeBlobRequest(blobCallback);
		syncBlobRequest.setConflictStrategy(ConflictStrategy.AUTO_RESOLVE_TO_CLOUD);
		
		mWhisperSyncClient.synchronize(syncBlobRequest);
    }
    
    //-----------------------------------------------------------------------------------------------------------
    
    /* If advertising is enable for this game set it up using this routine */
    private void setupVerizon() {
    
        if (Verizon) {
        	ApplicationInfo ai;
			try {
				ai = getPackageManager().getApplicationInfo(this.getComponentName().getPackageName(), PackageManager.GET_META_DATA);
	        	Bundle bundle = ai.metaData;        	 
	        	String valueKeyWord = bundle.getString("VERIZON_KEYWORD");        	 
	        	Log.d("yoyo", "VERIZON_KEYWORD value =" +  valueKeyWord);
	        	int testVerizon = bundle.getInt( "VERIZON_TEST", 0 );
	        	int trialVerizon = bundle.getInt( "VERIZON_TRIAL", 0 );
	        	Log.d("yoyo", "VERIZON_TEST value =" +  testVerizon);
	        	Log.d("yoyo", "VERIZON_TRIAL value =" +  trialVerizon);
	        	
	        	
	        	// check the Verizon
	        	LicenseAuthenticator licenseAuthenticator = new LicenseAuthenticator(this);
	        	int retval = LicenseAuthenticator.LICENSE_VALIDATION_FAILED;
	        	if (testVerizon > 0) {
	        		retval = licenseAuthenticator.checkTestLicense( valueKeyWord, LicenseAuthenticator.LICENSE_OK);
	        	} // end if
	        	else {
	        		retval = licenseAuthenticator.checkLicense( valueKeyWord );
	        	} // end else
	        	
	        	String msg = null;
	        	String debugMsg = null;
				switch (retval) {
				case LicenseAuthenticator.LICENSE_OK:
					break;
				case LicenseAuthenticator.LICENSE_TRIAL_OK:
					break;
				case LicenseAuthenticator.LICENSE_VALIDATION_FAILED:
					msg = "You have not purchased this application or purchase period has expired. Launch V CAST Apps client to purchase";
					break;
				case LicenseAuthenticator.ITEM_NOT_FOUND:
					msg = "This item is not available for your device or is no longer available in the V CAST Apps catalog.";
					break;
				case LicenseAuthenticator.LICENSE_NOT_FOUND:
					msg = "TYou have not purchased this application or purchase period has expired.";
					break;
				case LicenseAuthenticator.ERROR_CONTENT_HANDLER:
					debugMsg = "LicenseAuthenticator.ERROR_CONTENT_HANDLER : V CAST Apps is not installed or there is a problem connecting to it";
					break;
				case LicenseAuthenticator.ERROR_ILLEGAL_ARGUMENT:
					msg = "Error occurred while validating license.";
					debugMsg = "LicenseAuthenticator.ERROR_ILLEGAL_ARGUMENT : Keyword is empty or null";
					break;
				case LicenseAuthenticator.ERROR_SECURITY:
					msg = "Error occurred while validating license. If error persists launch V Cast Apps client to redownload application.";
					debugMsg = "LicenseAuthenticator.ERROR_SECURITY : check AndroidManifest.xml";
					break;
				case LicenseAuthenticator.ERROR_GENERAL:
					msg = "Error occurred while validating license.";
					debugMsg = "LicenseAuthenticator.ERROR_GENERAL : check AndroidManifest.xml";
					break;
				case LicenseAuthenticator.ERROR_UNABLE_TO_CONNECT_TO_CDS:
					msg = "Error occurred while validating license. Please try again later.";
					debugMsg = "LicenseAuthenticator.ERROR_UNABLE_TO_CONNECT_TO_CDS : Cannot connect to Content Delivery Server.";
					break;
				default:
					debugMsg = "Unknown error value from Verizon : " + Integer.toString(retval) ;
					break;
				} // end switch
				
				// if failure then display message
				if ((msg!=null) || (debugMsg != null)) {
				
					if ((debugMsg == null) && (msg != null)) {
						debugMsg = msg;
					} // end if
					Log.d( "yoyo", debugMsg );
				
					// display the dialog if we have a message
					if (msg != null) {
						AlertDialog.Builder builder = new AlertDialog.Builder(this);
						builder.setMessage( msg )
							.setCancelable(false)
							.setPositiveButton( "Exit", new DialogInterface.OnClickListener() {
								public void onClick( DialogInterface dialog, int id ) {
									 RunnerJNILib.ExitApplication();
								}
							});
						final AlertDialog alert  = builder.create();
						
						mLicenseHandler.post( new Runnable() {
							public void run() {
								alert.show();
							}
						});
					} // end if
					else {
				 		RunnerJNILib.ExitApplication();
					} // end else
				} // end if	        	
			} 
			catch (NameNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}        	 
        } // end if
    }
    
    /* Sets up Flurry */
    private void setupFlurry() 
    {
    	Log.i("yoyo", "---- setupFlurry ---------");
    	String flurryId = mYYPrefs.getString("FlurryId");
    	if(flurryId == null )
    	{
    		flurryId = mYYPrefs.getString("FLURRY_KEY");
    	}
    	
   		if( flurryId != null )
   		{
   			FlurryCode = flurryId;
   			Flurry = true;
   		
   			//since this is called after onStart, we need to start the flurry session here
   			int version = FlurryAgent.getAgentVersion();
   			Log.i( "yoyo", "@@@@@@@ Flurry session started code = " + FlurryCode);
   			Log.i("yoyo", "Flurry Agent Version = " + version );
			FlurryAgent.setReportLocation(false);
			FlurryAgent.setLogEvents(true);
			FlurryAgent.setLogEnabled(true);
			FlurryAgent.setLogLevel(Log.VERBOSE);
        	FlurryAgent.onStartSession( this, FlurryCode );
   		}
    }
    
    /* Sets up Google Analytics */
    private void setupGoogleAnalytics() 
    {
		String trackingId = mYYPrefs.getString("TrackingID");
		Log.i( "yoyo", "GA tracking ID: " + trackingId );
   		if( trackingId != null )
   		{
   			GACode = trackingId;
   			mbGoogleAnalytics = true;
   		}
		
		if( mbGoogleAnalytics )
		{
			mGaInstance = GoogleAnalytics.getInstance(this);	//V2
			mGaInstance.setDebug(true);
			mGaTracker = mGaInstance.getTracker(GACode);
			
			mGaTracker.sendView("/GameStart");	//->onStart
			Log.i( "yoyo", "@@@@@ started Google Analytics with TrackingID: " + GACode);
		}
    }
    
    public static void googleAnalyticsEvent(String actionName, String label, int value )
    {
		if( mbGoogleAnalytics )
		{
			Long lvalue = new Long(value);
			mGaTracker.sendEvent(
				"GMEvent",  // Category
				 actionName,  // Action
				 label, // Label
				 lvalue);       // Value
			Log.i( "yoyo", "@@@@@@@ Google Analytics event ext: " + actionName + "," + label + "," + value);
		}
    }
    
    public static void googleAnalyticsEvent(String actionName )
    {
		if( mbGoogleAnalytics )
		{
			mGaTracker.sendEvent(
				"GMEvent",  // Category
				 actionName,  // Action
				 "", // Label
				 null);       // Value -should be null for no value

			Log.i( "yoyo", "@@@@@@@ Google Analytics event: " + actionName);
		}
    }
    
    /* Gets the facebook app ID from the manifest, if it's available */
    public String getFacebookAppId() {
    	    	
		try {
			ApplicationInfo ai = getPackageManager().getApplicationInfo(this.getComponentName().getPackageName(), PackageManager.GET_META_DATA);
        	Bundle bundle = ai.metaData;
        	
        	String appID = bundle.getString("YYFacebookAppId");
        	
        	// We add "fb" on to the front of the app ID to stop it from turning it into an integer so strip this
        	if (appID.startsWith("fb")) {
        		appID = appID.replaceFirst("fb", "");
        	}        				
			return appID;	    		
	    }
	    catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null; 
    }
    
    /* Checks to see if the current device is an XPeria play */
    private void checkXPeriaPlay() {
    
	    Log.i( "yoyo", "@@@@@@@ Build.Display = " + android.os.Build.DISPLAY + " BRAND=" + android.os.Build.BRAND + 
        						" DEVICE=" + android.os.Build.DEVICE + " MANUFACTURER=" + android.os.Build.MANUFACTURER+ 
        						" MODEL=" + android.os.Build.MODEL + " PRODUCT="  + android.os.Build.PRODUCT
        						);
        XPeriaPlay = (android.os.Build.MANUFACTURER.equals("Sony Ericsson")) && (android.os.Build.MODEL.startsWith("R800"));
        Log.i( "yoyo", "@@@@@@@ XPeriaPlay=" + XPeriaPlay + " manufacturer=" + android.os.Build.MANUFACTURER.equals("Sony Ericsson") + " model=" +  android.os.Build.MODEL.startsWith("R800"));
    }
    
    /* Checks the security for the application */
    private void checkLicensing() {
    
	    if (checkCallingOrSelfPermission("com.android.vending.CHECK_LICENSE")==0) {
        	mLicenseCheckerCallback = new MyLicenseCheckerCallback();
        	String deviceId = Secure.getString(getContentResolver(), Secure.ANDROID_ID);
        	Log.i("yoyo", "deviceId="+deviceId);
        	ServerManagedPolicy policy = new ServerManagedPolicy( this, new AESObfuscator( SALT, getPackageName(), deviceId));
        	
        	if( BASE64_PUBLIC_KEY == null || BASE64_PUBLIC_KEY == "")
        	{
        		Log.i("yoyo", "Invalid license key found");
        	}
        	
        	mChecker = new LicenseChecker( this, policy, BASE64_PUBLIC_KEY);
        	mChecker.checkAccess( mLicenseCheckerCallback );
        } // end if  
        else {
        	Log.i("yoyo", "@@@@@@ Google Licensing permission not set" );
        } // end else
    }
    
    /* Sets up the GL view that's reponsible for rendering (and GM processing...) */
    private void setupView() {
    
	    setContentView( R.layout.main );
        mGLView = (DemoGLSurfaceView)findViewById(R.id.demogl);
        try {
        
        	Class parTypes[] = new Class[1];
        	parTypes[0] = Integer.TYPE;
	        mSetSystemUiVisibility = android.view.View.class.getMethod("setSystemUiVisibility", parTypes );
	        
	        setSystemUiVisibility( 1 );
	        
	    } 
	    catch( Exception e ) {
	    	Log.i( "yoyo", "Exception while getting setSystemUiVisibility :: " + e.toString() );
	    }
    }
    
    private void setSystemUiVisibility( int _vis ) {
    	if (mSetSystemUiVisibility != null) {
    		try {
    			mSetSystemUiVisibility.invoke( mGLView, _vis );
	    		Log.i( "yoyo", "mSetSystemUiVisibility(" + _vis + ")" );
    		} catch( Exception _e ) {
    			Log.i( "yoyo", "Exception while calling setSystemUiVisibility " + _e.toString() );
    		} // end catch
    	} // end if
    	else {
	    		Log.i( "yoyo", "!!!!Unable to do mSetSystemUiVisibility(" + _vis + ")" );
    	} // end else
    }

    private Dialog createDialog(int titleId, int messageId) 
    {
        String helpUrl = replaceLanguageAndRegion(getString(R.string.help_url));
        Log.i("yoyo", helpUrl);

        final Uri helpUri = Uri.parse(helpUrl);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(titleId)
            .setIcon(android.R.drawable.stat_sys_warning)
            .setMessage(messageId)
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(R.string.learn_more, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, helpUri);
                    startActivity(intent);
                }
            });
        return builder.create();
    }
    
    //public void SendOFAchievement( String _achievement, float _percentageDone )
    //{
    //	Log.i( "yoyo", "SendOFAchievement(" + _achievement + ", " + _percentageDone + ")");
    //	new Achievement( _achievement ).unlock( new Achievement.UnlockCB() {
    //		public void onSuccess(boolean _newUnlock) {
    //			RunnerJNILib.OFNotify( RunnerJNILib.eOF_AchievementSendOK, "", "", "", "" );
    //			//RunnerActivity.this.setResult( Activity.RESULT_OK );
    //			//RunnerActivity.this.finish();
    //		} // end onSuccess
    //		
    //		public void onFailure(String exceptionMessage) {
    //			RunnerJNILib.OFNotify( RunnerJNILib.eOF_AchievementSendFail, exceptionMessage, "", "", "" );
    //			//RunnerActivity.this.setResult( Activity.RESULT_CANCELED );
    //			//RunnerActivity.this.finish();
    //		} // end onFailure
    //	});
    //} // end SendOFAchievement
     
    //public void SendOFHighScore( String _leaderboard, int _score )
    //{
    //	Log.i( "yoyo", "SendOFHighScore(" + _leaderboard + ", " + _score + ")");
    //	Score s = new Score( _score, null );
    //	Leaderboard l = new Leaderboard( _leaderboard );
    //	s.submitTo( l, new Score.SubmitToCB() {
    //		public void onSuccess( boolean _newHighScore ) {
    //			RunnerJNILib.OFNotify( RunnerJNILib.eOF_HighScoreSendOK, "", "", "", "" );
    //			//RunnerActivity.this.setResult( Activity.RESULT_OK );
    //			//RunnerActivity.this.finish();
    //		} // end onSuccess
    //		public void onFailure( String exceptionMessage ) {
    //			RunnerJNILib.OFNotify( RunnerJNILib.eOF_HighScoreSendFail, exceptionMessage, "", "", "" );
    //			//RunnerActivity.this.setResult( Activity.RESULT_CANCELED );
    //			//RunnerActivity.this.finish();
    //		} // end onFailure
    //	});
    //} // end SendOFHighScore
    
    /**
     * Replaces the language and/or country of the device into the given string.
     * The pattern "%lang%" will be replaced by the device's language code and
     * the pattern "%region%" will be replaced with the device's country code.
     *
     * @param str the string to replace the language/country within
     * @return a string containing the local language and region codes
     */
    private String replaceLanguageAndRegion(String str) {
        // Substitute language and or region if present in string
        if (str.contains("%lang%") || str.contains("%region%")) {
            Locale locale = Locale.getDefault();
            str = str.replace("%lang%", locale.getLanguage().toLowerCase());
            str = str.replace("%region%", locale.getCountry().toLowerCase());
        }
        return str;
    }

	/**     
     */
	protected void SelectGooglePlayBilling() {

		// Only select Google Play billing if the requisite permission is available in AndroidManifest.xml
		if (checkCallingOrSelfPermission("com.android.vending.BILLING")==0) {					

			Log.i("yoyo", "BILLING: Using Google Play billing");
			mRunnerBilling = new RunnerBilling(this);
		}
		else {
			Log.i("yoyo", "BILLING: Google Play permissions not available, selecting NULL billing solution");
			mRunnerBilling = new NullBilling(this);
		}
	}
    
    /**	 
     */
    protected void setupInAppBilling() {		

		Log.i("yoyo", "BILLING: setupInAppBilling");
		try {
			// Amazon IAP does not require any special billing permissions so just use it if option is built into the manifest
			ApplicationInfo ai = RunnerActivity.CurrentActivity.getPackageManager().getApplicationInfo(
				RunnerActivity.CurrentActivity.getComponentName().getPackageName(), 
				PackageManager.GET_META_DATA);
        	Bundle bundle = ai.metaData;

			boolean amazonIAPValue = bundle.getBoolean("YYUseAmazonIAP");
			Log.i("yoyo", "BILLING: Amazon setting: " + amazonIAPValue);

			if (amazonIAPValue) {

				Log.i("yoyo", "BILLING: Using Amazon Store billing");
				mRunnerBilling = new AmazonBilling(this);
			}
			else {
				SelectGooglePlayBilling();
			}			
		}
		catch (Exception e) {
			Log.i("yoyo", "BILLING: Unable to determine billing method via Manifest, selecting Googe Play as fallback" + e.getMessage());
			SelectGooglePlayBilling();
		}
    }
    
    @Override
    public void onConfigurationChanged( Configuration newConfig ) {
    	super.onConfigurationChanged(newConfig);
    	
    	if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
    		Orientation = 0;
    	} // end if 
    	else 
    	if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
    		Orientation = 1;
    	} // end if
    } // end onConfigurationChanged            
}
