package com.yoyogames.runner;

import java.lang.reflect.Field;
import java.lang.Thread;
import java.net.URLConnection;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import android.app.AlertDialog;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Debug;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.view.View;
import android.view.LayoutInflater;
import android.view.Display;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.EditText;
import com.flurry.android.FlurryAgent;

import com.amazon.ags.api.AmazonGamesClient;
import com.amazon.ags.api.achievements.AchievementsClient;
import com.amazon.ags.api.achievements.UpdateProgressResponse;
import com.amazon.ags.api.leaderboards.LeaderboardsClient;
import com.amazon.ags.api.leaderboards.SubmitScoreResponse;
import com.amazon.ags.api.AGResponseCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

//import com.openfeint.api.ui.Dashboard;
import ${YYAndroidPackageName}.RunnerActivity;
import ${YYAndroidPackageName}.RunnerBilling;
import ${YYAndroidPackageName}.RunnerFacebook;
import ${YYAndroidPackageName}.DemoRenderer;
import ${YYAndroidPackageName}.CatalogEntry;
import ${YYAndroidPackageName}.Gamepad;
import ${YYAndroidPackageName}.R;

// Wrapper for native library
public class RunnerJNILib {

	public static final int eOF_UserLoggedIn = 0;
	public static final int eOF_UserLoggedOut = 1;
	public static final int eOF_AchievementSendOK = 2;
	public static final int eOF_AchievementSendFail = 3;
	public static final int eOF_HighScoreSendOK = 4;
	public static final int eOF_HighScoreSendFail = 5;	 

    public static Context ms_context;
    public static MediaPlayer ms_mp;
    public static RunnerFacebook m_runnerFacebook;
    public static boolean ms_exitcalled = false;	
    
     
 	// ----------------------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------------------
   public static void Init() {    
    	System.loadLibrary("openal");    
        System.loadLibrary("yoyo");		
        ms_mp = null;
    } // end Init
     
	// ----------------------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------------------
    public static void Init(Context _context) {
    	ms_context = _context;
    }
    
	// ----------------------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------------------
    // Exit the application in a thread safe way
    public static void ExitApplication() {
		if(!ms_exitcalled)
		{
		Log.i("yoyo", "First exit application called");
			ms_exitcalled = true;
    		RunnerActivity.ViewHandler.post( new Runnable() {
    			public void run() {
    				RunnerActivity.CurrentActivity.finish();
    			}
    		});
    	}
    }

	// ----------------------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------------------
    public static native void Startup(String _apkPath, String _saveFilesDir, String _packageName);
    public static native boolean Process(int _width, int _height, float _accelX, float _accelY, float _accelZ, int _keypadStatus, int _orientation );
    public static native void TouchEvent( int _type, int _index, float _x, float _y);
    public static native void RenderSplash( String _apkPath, String _splashName, int  _screenWidth, int _screenHeight, int _texWidth, int _texHeight, int _pngWidth, int _pngHeight );
    public static native void Resume( int _param );
    public static native void Pause( int _param );
    public static native void AddString( String _string );
    public static native void OFNotify( int _enum, String _param1, String _param2, String _param3, String _param4 );
    public static native void KeyEvent( int _type, int _keycode );
    public static native void SetKeyValue( int _type, int _val, String _valString );
    public static native String GetAppID( int _param );
    public static native String GetSaveFileName( String _fileName );
    public static native String[] ExpandCompressedFile( String _destLocalPath, String _compressedFileName );
    public static native void HttpResult( byte[] _resultString, int _httpStatus, int _id);
    public static native void HttpResultString( String _resultString, int _httpStatus, int _id);
    public static native void InputResult( String _resultString, int _httpStatus, int _id);
    public static native void LoginResult( String _userName, String _password, int _id);
    public static native void CloudResultData( byte[] _resultString, int _status, int _id);
    public static native void CloudResultString( String _resultString, int _status, int _id);
    
    public static native int getGuiHeight();
    public static native int getGuiWidth();
    public static native int dsMapCreate();
    public static native int dsListCreate();
	public static native void dsMapAddString(int _dsMap, String _key, String _value);
	public static native void dsMapAddInt(int _dsMap, String _key, int _value);
	public static native void dsListAddString(int _dsList, String _value);
	public static native void dsListAddInt(int _dsMap, int _value);

	public static native void IAPEvent(int _id);
	public static native void CallInappPurchase(String _ident);
	public static native void callreward(int _scriptid,int _num,String _type);

	// Bluetooth controller feedback
	public static native void iCadeEventDispatch(int _button, boolean _down);
	public static native void registerGamepadConnected(int _deviceIndex, int _buttonCount, int _axisCount);

	// ----------------------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------------------
	public static int GetDefaultFrameBuffer() {
		return DemoRenderer.m_defaultFrameBuffer;
	}

	// ----------------------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------------------
    public static void OpenURL( String _url ) {
    	Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(_url));
    	ms_context.startActivity(myIntent);
    } // end OpenURL
    
    
    private static boolean mPlaybackStateStored = false;
    private static int mStoredPlaybackPosition;
    private static int mStoredPlaybackSessionId;
    private static boolean mStoredPlaybackLoop;
    private static long mStoredPlaybackOffset;
    private static long mStoredPlaybackSize;
    private static float mStoredVolume=1.0f;
     
	// ----------------------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------------------
    public static void PlayMP3( String _mp3, int _loop ) {
    	_mp3 = _mp3.replace(' ', '_');
		Log.i( "yoyo", "Request to play mp3 - \"" + _mp3 + "\"");
    	if (ms_mp != null) {
    		StopMP3();
    	} // end if
    	
    	boolean fSuccessful = false;
    	try {
    		Class resRaw = Class.forName("${YYAndroidPackageName}.R$raw");    		
			Field field = resRaw.getField("mp3_" + _mp3);
			int id = field.getInt(null);
			if (id != 0) {
				Log.i( "yoyo", "Playing mp3 - \"" + _mp3 + "\" id="+id);
				mStoredPlaybackSessionId = id;
				ms_mp = MediaPlayer.create( ms_context, id);
				ms_mp.setLooping( (_loop != 0) );
				//ms_mp.prepare();
				ms_mp.start();        
		  		fSuccessful = true;
			} // end if
    	} // end try
 		catch ( Exception e ) {
 			//Log.e( "yoyo", "unable to play mp3 - \"" + _mp3 + "\"", e);
	    	fSuccessful = false;
 		} // end else		
 		
		if (!fSuccessful) {
			try {
				// find the mp3 in the zip
				Log.i( "yoyo", "Request to play zip - \"" + DemoRenderer.m_apkFilePath + "\"");
				ZipFile zip = new ZipFile( DemoRenderer.m_apkFilePath );
				ZipEntry zipEntry = zip.getEntry( "assets/" + _mp3.toLowerCase() + ".mp3" );
				if (zipEntry != null) {
				
					// find the offset within the zip file
					Enumeration<? extends ZipEntry > zipEntries = zip.entries();
					long offset = 0;
			    	fSuccessful = false;
					while( zipEntries.hasMoreElements() ) {
						ZipEntry entry = (ZipEntry)zipEntries.nextElement();
						long fileSize = 0;
						long extra = (entry.getExtra() == null) ? 0 : entry.getExtra().length;
						offset += 30 + entry.getName().length() + extra;
						if (!entry.isDirectory()) {
							fileSize = entry.getCompressedSize();
						} // end if
						if (entry.getCrc() == zipEntry.getCrc()) {
							fSuccessful = true;
							break;
						} // end if
						offset += fileSize;
					} // end while
					
					if (fSuccessful) {
						// open the MediaPlayer on the file...
						mStoredPlaybackSessionId = -1;
						mStoredPlaybackOffset = offset;
						mStoredPlaybackSize = zipEntry.getSize();
						File inputFile = new File( DemoRenderer.m_apkFilePath );
						FileInputStream is = new FileInputStream( inputFile );
						
			    		//Log.i("yoyo", "Starting MP3 state. mStoredPlaybackOffset: " + mStoredPlaybackOffset + " mStoredPlaybackSize: " + mStoredPlaybackSize);
						ms_mp = new MediaPlayer();
						ms_mp.setDataSource( is.getFD(), mStoredPlaybackOffset, mStoredPlaybackSize );
						ms_mp.setLooping( (_loop != 0) );
						ms_mp.prepare();
						ms_mp.start();						
						
						is.close();
					} // end if
								
				} // end if
				
				zip.close();
				zip = null;
			} // end try
			catch( Exception e ) {
				Log.i( "yoyo", "Exception while opening mp3 - " + e );
			} // end catch
		} // end if
    } // end PlayMP3
     
    public static void PauseMP3()
    {
     if (ms_mp != null) {
   		Log.i( "yoyo", "pause mp3");
			try {
		    	ms_mp.pause();
		    }
			catch( Exception e ) {
				Log.i( "yoyo", "Exception while pausing mp3 - " + e );
			} // end catch
		} // end if
    }
    
    public static void ResumeMP3()
    {
	if (ms_mp != null) {
   		Log.i( "yoyo", "resume mp3");
			try {
		    	ms_mp.start();
		    }
			catch( Exception e ) {
				Log.i( "yoyo", "Exception while resuming mp3 - " + e );
			} // end catch
		} // end if
    } 
     
	// ----------------------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------------------
    public static void StopMP3() {
    	 if (ms_mp != null) {
   			Log.i( "yoyo", "stop mp3");
			try {
		    	ms_mp.stop();
		    	ms_mp.release();
		    }
			catch( Exception e ) {
				Log.i( "yoyo", "Exception while stopping mp3 - " + e );
			} // end catch
	    	ms_mp = null;
		} // end if
	} // end StopMP3
     
	// ----------------------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------------------
	public static boolean PlayingMP3() {
    	 boolean ret = false;
    	 if (ms_mp != null) {   			 
 			ret = ms_mp.isPlaying();
     	 }
     	 else {
     	 	Log.i("yoyo", "PlayingMP3(): ms_mp is NULL");
     	 }
    	 return ret;
    } // end PlayingMP3
     
	// ----------------------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------------------
    public static void SetMP3Volume( float _vol ) {
    	if (ms_mp != null) {
    		ms_mp.setVolume( _vol, _vol );
    		mStoredVolume = _vol;
    	} // end if
    } // SetMP3Volume

	// ----------------------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------------------
    public static void StoreMP3State() {
   		//Log.i("yoyo", "Storing MP3 state");
    	if (ms_mp != null) {
    		if (ms_mp.isPlaying()) {
	    		mStoredPlaybackPosition = ms_mp.getCurrentPosition();
    			mStoredPlaybackLoop = ms_mp.isLooping();
    			mPlaybackStateStored = true;
    		}
    	}
   		//Log.i("yoyo", "Stored MP3 state mPlaybackStateStored=" + mPlaybackStateStored + " ms_mp=" + ms_mp);
    } // StoreMP3State
    
	// ----------------------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------------------
    public static void RestoreMP3State() {
    
   		//Log.i("yoyo", "Restoring MP3 state mPlaybackStateStored=" + mPlaybackStateStored + " ms_mp=" + ms_mp);
    	if (mPlaybackStateStored && (ms_mp == null)) {    		
    		FileInputStream is = null;
    		if (mStoredPlaybackSessionId != -1) {
	    		ms_mp = MediaPlayer.create(ms_context, mStoredPlaybackSessionId);
	    	} // end if
	    	else {
	    		try {
					//Log.i( "yoyo", "Request to restore mp3 playback from zip - \"" + DemoRenderer.m_apkFilePath + "\"");
					File inputFile = new File( DemoRenderer.m_apkFilePath );
					is = new FileInputStream( inputFile );
					ms_mp = new MediaPlayer();
		    		//Log.i("yoyo", "Restoring MP3 state. mStoredPlaybackOffset: " + mStoredPlaybackOffset + " mStoredPlaybackSize: " + mStoredPlaybackSize);
					ms_mp.setDataSource( is.getFD(), mStoredPlaybackOffset, mStoredPlaybackSize );
				} // end try
				catch( Exception e ) {
					Log.i( "yoyo", "Exception while opening mp3 - " + e );
				} // end catch
	    	} // end else	    	
	    	try {
    			ms_mp.setLooping(mStoredPlaybackLoop);
    			ms_mp.setVolume(mStoredVolume,mStoredVolume);
    			if (is != null) {
    				ms_mp.prepare();
    			} // end if
    			ms_mp.seekTo(mStoredPlaybackPosition );
    			ms_mp.start(); 
    			if (is != null) {
	    			is.close();
    			} // end if
    		} catch( Exception e ) {
					Log.i( "yoyo", "Exception while opening mp3 - " + e );
    		} // end catch
    		
    		
    		//Log.i("yoyo", "Restoring MP3 state. ms_mp: " + ms_mp + " Loop: " + mStoredPlaybackLoop + " seeking to: " + mStoredPlaybackPosition  + " session id: " + mStoredPlaybackSessionId);
    		
    		mPlaybackStateStored = false;
    	}
    }

	// ----------------------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------------------
    private static int mAdX;
    private static int mAdY;
    private static int mAdNum;
    
    public static void MoveAds( int _x, int _y, int _num )
    {	
    	if (RunnerActivity.mAdProvider != null) 
    	{
    	
    		Log.i("yoyo", "MoveAds(2) _x="+_x+" _y="+_y+" _num="+_num);
	    	
    		Display d = ((WindowManager) ms_context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
			
    		int dw = RunnerJNILib.getGuiWidth();
			int dh = RunnerJNILib.getGuiHeight();
			int ddw = d.getWidth();
			int ddh = d.getHeight();
			float xscale = (float)ddw/(float)dw;
			float yscale = (float)ddh/(float)dh;
	    	
    		mAdX = (int)((float)_x*xscale);
    		mAdY = (int)((float)_y*yscale);
    		mAdNum = _num;
    		RunnerActivity.mAdProvider.move( mAdX, mAdY, _num);	
    	} // end if
    } // end EnableAds
    
    public static void EnableAds( int _x, int _y, int _num )
    {
		
		if (RunnerActivity.mAdProvider != null) 
		{	
    		Log.i("yoyo", "EnableAds(2) _x="+_x+" _y="+_y+" _num="+_num);
    		Display d =  ((WindowManager) ms_context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
    		
    	//	RunnerActivity.mAdProvider.m_activity.getWindowManager().getDefaultDisplay();
		
    		int dw = RunnerJNILib.getGuiWidth();
			int dh = RunnerJNILib.getGuiHeight();
			int ddw = d.getWidth();
			int ddh = d.getHeight();
			float xscale = (float)ddw/(float)dw;
			float yscale = (float)ddh/(float)dh;
	    	
    		mAdX = (int)((float)_x*xscale);
    		mAdY = (int)((float)_y*yscale);
    		mAdNum = _num;
    		RunnerActivity.mAdProvider.enable( mAdX, mAdY, _num);	
    	} // end if
    	
    } // end EnableAds
    
    public static void AdsRewardCallback(int _scriptid)
    {
		if (RunnerActivity.mAdProvider != null) {
    		RunnerActivity.mAdProvider.reward_callback(_scriptid);
    	} // end if
    }

    public static void AdsSetup(String userid)
    {
		if (RunnerActivity.mAdProvider != null) {
    		RunnerActivity.mAdProvider.setup(userid);
    	} // end if
    }
    public static void PlayHavenUpdateNotificationBadge()
    {
		if (RunnerActivity.mAdProvider != null)
		{
			RunnerActivity.mAdProvider.pc_badge_update();
		}
    }
	public static void PlayHavenHideNotificationBadge()
	{
		if (RunnerActivity.mAdProvider != null)
		{
			RunnerActivity.mAdProvider.pc_badge_hide();
		}
	}
	public static void PlayHavenAddNotificationBadge(int _x, int _y,int _width, int _height,String _ident)
	{
		if (RunnerActivity.mAdProvider != null)
		{
			RunnerActivity.mAdProvider.pc_badge_add(_x,_y,_width,_height,_ident);
		}
	}
	
	public static void PlayHavenPositionNotificationBadge(int _x, int _y,int _width, int _height)
	{
		if (RunnerActivity.mAdProvider != null)
		{
			RunnerActivity.mAdProvider.pc_badge_move(_x,_y,_width,_height);
		}
	}
    
    public static void PocketChangeDisplayReward()
    {
	
		if(RunnerActivity.mPocketChange!= null)
			RunnerActivity.mPocketChange.display();
    }
    
    public static void PocketChangeDisplayShop()
    {
		
		if(RunnerActivity.mPocketChange!= null)
			RunnerActivity.mPocketChange.displayshop();
    }
    
    public static void AdsEngagementLaunch()
    {
		if (RunnerActivity.mAdProvider != null) {
    		RunnerActivity.mAdProvider.engagement_launch();
    	} // end if
    }
    
    public static boolean AdsEngagementActive()
    {
		if (RunnerActivity.mAdProvider != null) {
    		return RunnerActivity.mAdProvider.engagement_active();
    	} // end if
		return false;
    }
     
    public static boolean AdsEngagementAvailable()
    {
		if (RunnerActivity.mAdProvider != null) {
    		return RunnerActivity.mAdProvider.engagement_available();
    	} // end if
		
		return false;
    } 
    
    public static boolean AdsInterstitialAvailable()
    {
		if (RunnerActivity.mAdProvider != null) {
    		return RunnerActivity.mAdProvider.interstitial_available();
    	} // end if
    	else
    	{
    		Log.i("yoyo","null ad provider found");
    	}
		return false;
    
    }
    
    public static boolean AdsInterstitialDisplay()
    {
		if (RunnerActivity.mAdProvider != null) {
    		return RunnerActivity.mAdProvider.interstitial_display();
    	} // end if
    	else
    	{
    		Log.i("yoyo","null ad provider found");
    	}
    	
		return false;
    }
    
    
	// ----------------------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------------------
	
	public static int AdsDisplayWidth(int slot)
	{
	 	if (RunnerActivity.mAdProvider != null) {
    		return RunnerActivity.mAdProvider.getAdDisplayWidth(slot);
    	} // end if
    	return 0;
	}
	public static int AdsDisplayHeight(int slot)
	{
	   	if (RunnerActivity.mAdProvider != null) {
    		return RunnerActivity.mAdProvider.getAdDisplayHeight(slot);
    	} // end if
    	return 0;
	}
	
    public static void DisableAds(int _slot)
    {
    	Log.i("yoyo", "DisableAds");
    	if (RunnerActivity.mAdProvider != null) {
    		RunnerActivity.mAdProvider.disable(_slot);
    	} // end if
    } // end DisableAds
    
    public static void AdsEvent(String _ident)
    {
		Log.i("yoyo", "AdsEvent:"+_ident);
		if (RunnerActivity.mAdProvider != null) {
    		RunnerActivity.mAdProvider.event(_ident);
    	} // end if
    }
     
    public static void AdsEventPreload(String _ident)
    {
		Log.i("yoyo", "AdsPreload:"+_ident);
		if (RunnerActivity.mAdProvider != null) {
    		RunnerActivity.mAdProvider.event_preload(_ident);
    	} // end if
    }
	// ----------------------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------------------
    public static void SetThreadPriority( int _priority ) {
    	Log.i("yoyo", "SetThreadPriority("+_priority);
 		Thread th = Thread.currentThread();
 		th.setPriority(_priority);
    }
     
	// ----------------------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------------------
    public static void LeaveRating( String _text, String _yes, String _no, String _url )
    {
    	Log.i( "yoyo", "LeaveRating("+_text+", "+_yes+", "+_no+", "+_url+")");
    	final String sText = _text;
    	final String sYes = _yes;
    	final String sNo = _no;
    	final String sUrl = _url;
    	RunnerActivity.ViewHandler.post( new Runnable() {
    		 public void run() {
    	    		AlertDialog.Builder builder = new AlertDialog.Builder(ms_context);
    	    		builder.setMessage( sText)
    	    			.setCancelable(false)
    	    			.setPositiveButton( sYes, new DialogInterface.OnClickListener() {
    	    				public void onClick( DialogInterface dialog, int id ) {
    	    					// goto URL
    	    					OpenURL( sUrl );
    	    				}
    	     			})
    	    			.setNegativeButton( sNo, new DialogInterface.OnClickListener() {
    	    				public void onClick( DialogInterface dialog, int id ) {
    	    					dialog.cancel();
    	    				}
    	    			});
    	    		AlertDialog alert  = builder.create();
    	    		alert.show();    		 
    		 }
    	});
    } // end LeaveRating
     
	// ----------------------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------------------
    public static void OpenGameCircleAchievements()
    {
    	Log.i( "yoyo", "OpenGameCircleAchievements()");
    	RunnerActivity.ViewHandler.post( new Runnable() {
    		 public void run() {
    			RunnerActivity.agsGameClient.getAchievementsClient().showAchievementsOverlay();
    		 }
    	});
    } // end OpenGameCircleAchievements
     
	// ----------------------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------------------
    public static void OpenGameCircleLeaderboards()
    {
    	Log.i( "yoyo", "OpenGameCircleLeaderboards()");
    	RunnerActivity.ViewHandler.post( new Runnable() {
    		 public void run() {
    			RunnerActivity.agsGameClient.getLeaderboardsClient().showLeaderboardsOverlay();
    		 }
    	});
    } // end OpenGameCircleLeaderboards
     
	// ----------------------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------------------
    public static void SendGameCircleAchievement( String _achievement, float _percentageDone )
    {
    	Log.i( "yoyo", "SendGameCircleAchievement(" + _achievement + "," + _percentageDone + ")");
		AchievementsClient acClient = RunnerActivity.agsGameClient.getAchievementsClient();
		acClient.updateProgress(_achievement, _percentageDone, _achievement + " = " + _percentageDone).setCallback(new AGResponseCallback<UpdateProgressResponse>() {
		 
			@Override
			public void onComplete(UpdateProgressResponse result) {
				if (result.isError()) {
					// Add optional error handling here.  Not required since re-tries and on-device request caching are automatic
					Log.i( "yoyo", "ERROR! sending Game Circle Achievement " + result.getUserData().toString() );
				} else {
					// Continue game flow
					Log.i( "yoyo", "Game Circle Achievement sent " + result.getUserData().toString() );
				}
			}
		});
	} // end SendGameCircleAchievement
     
	// ----------------------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------------------
    public static void SendGameCircleHighScore( String _leaderboard, int _score )
    {
    	Log.i( "yoyo", "SendGameCircleHighScore(" + _leaderboard + "," + _score + ")");
		LeaderboardsClient lbClient = RunnerActivity.agsGameClient.getLeaderboardsClient();
		lbClient.submitScore(_leaderboard, _score, _leaderboard + " = " + _score).setCallback(new AGResponseCallback<SubmitScoreResponse>() {
		 
			@Override
			public void onComplete(SubmitScoreResponse result) {
				if (result.isError()) {
					// Add optional error handling here.  Not required since re-tries and on-device request caching are automatic
					Log.i( "yoyo", "ERROR! sending Game Circle Leaderboard score " + result.getUserData().toString() );
				} else {
					// Continue game flow
					Log.i( "yoyo", "Game Circle Leaderboard score sent " + result.getUserData().toString() );
				}
			}
		});
    } // end SendGameCircleHighScore
    
    
	// ----------------------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------------------     
    public static void EnableInAppBilling(String _purchaseIndex)
    {
    	RunnerActivity.CurrentActivity.RunnerBilling().enableInAppPurchases(_purchaseIndex);

    } // end EnableInAppBilling
    
    public static void EnableServerlessInAppBilling(String[] _purchases) {
		
		if ((_purchases.length & 0x1) != 0) {
			throw new IllegalArgumentException("There must be an even number of strings forming key-value pairs for in-app billing purchases");
		}						    		
    	        
    	// Setup catalog entries
    	ArrayList<CatalogEntry> catalogEntries = new ArrayList<CatalogEntry>();
    	CatalogEntry currentCatalogEntry = new CatalogEntry();
    	for (int n = 0; n < _purchases.length; n += 2) {
		
			String key = _purchases[n];
			String value = _purchases[n+1];			
			Log.i("yoyo", "Found " + key + ":" + value);
			
			if (key.equals("title")) {
				currentCatalogEntry.title = value;
			}
			else if (key.equals("description")) {
				currentCatalogEntry.description = value;
			}
			else if (key.equals("content_url")) {
				currentCatalogEntry.contentURL = value;
			}
			else if (key.equals("price")) {
				currentCatalogEntry.price = value;
			}				
			else if (key.equals("local_path")) {
				currentCatalogEntry.localPath = value;
			}				
			else if (key.equals("id")) {							// change state here by convention
				currentCatalogEntry.purchaseID = value;				
				catalogEntries.add(currentCatalogEntry);
				currentCatalogEntry = new CatalogEntry();				
			}
    	}		
										
		Log.i("yoyo", "Found " + catalogEntries.size() + " catalog entries");
		CatalogEntry[] catalogArray = new CatalogEntry[catalogEntries.size()];
		for (int n = 0; n < catalogEntries.size(); n++) {
			catalogArray[n] = catalogEntries.get(n);
		}				

						
		RunnerActivity.CurrentActivity.RunnerBilling().setGMLPurchasesList(catalogArray);
		RunnerActivity.CurrentActivity.RunnerBilling().enableInAppPurchases(null);
 
    } // end EnableServerlessInAppBilling
    
	// ----------------------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------------------
    public static void RestoreInAppPurchases()
    {		
    	RunnerActivity.CurrentActivity.RunnerBilling().restorePurchasedItems();
    } // RestoreInAppPurchases
    
	// ----------------------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------------------
    public static void AcquireInAppPurchase(int purchaseIndex)
    {
		RunnerActivity.CurrentActivity.RunnerBilling().purchaseCatalogItem(purchaseIndex);
    } // AcquireInAppPurchase
    
    // ----------------------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------------------
    public static void ConsumeInAppPurchase(int purchaseIndex)
    {
		RunnerActivity.CurrentActivity.RunnerBilling().consumeCatalogItem(purchaseIndex);		
    }// ConsumeInAppPurchase
    
	// ----------------------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------------------
    public static int GetAppStoreState()
    {
    	return RunnerActivity.CurrentActivity.RunnerBilling().mBillingServiceStatus;
    } // GetAppStoreState
    
	// ----------------------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------------------
    public static int GetPurchasesAvailableCount()
    {
    	return RunnerActivity.CurrentActivity.RunnerBilling().availablePurchasesCount();
    } // GetPurchasesAvailableCount
    
	// ----------------------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------------------
    public static boolean GetProductPurchased(String productId) 
    {
    	return RunnerActivity.CurrentActivity.RunnerBilling().productPurchased(productId);
    }

	// ----------------------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------------------
    public static boolean GetProductDownloaded(String productId) 
    {
    	return RunnerActivity.CurrentActivity.RunnerBilling().productDownloaded(productId);
    }
    
	// ----------------------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------------------
    public static String GetPurchaseProperty(int purchaseIndex, String property)
    {		
	    if (RunnerActivity.CurrentActivity.RunnerBilling().availablePurchasesCount() > 0)
	    {
	    	CatalogEntry[] availablePurchases = RunnerActivity.CurrentActivity.RunnerBilling().getAvailablePurchases();
		    if ((purchaseIndex >= 0) && (purchaseIndex < availablePurchases.length))
			{
				// Translate the received string into the correct field (Reflection won't work in release - obfuscation?)
				if (property.equals("title")) {
					return new String(availablePurchases[purchaseIndex].title);
				}
				else if (property.equals("description")) {
					return new String(availablePurchases[purchaseIndex].description);
				}
				else if (property.equals("purchaseID")) {
					return new String(availablePurchases[purchaseIndex].purchaseID);
				}
				else if (property.equals("contentURL")) {
					return new String(availablePurchases[purchaseIndex].contentURL);
				}
				else if (property.equals("price")) {
					return new String(availablePurchases[purchaseIndex].price);
				}
				else if (property.equals("localPath")) {
					return new String(availablePurchases[purchaseIndex].localPath);
				}
				else if (property.equals("purchaseState")) {
					return availablePurchases[purchaseIndex].purchaseStateString();
				}
				else {
					Log.i("yoyo", "Unable to find purchase property: " + property);
				}
    		}
    	}
    	return new String("Property not valid");
    } // RetrieveAvailableInAppPurchases
    
	// ----------------------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------------------
    public static String[] GetDownloadedFileList(String contentId)
    {
    	String[] returnArray = null;
   		JSONObject jsonObject = RunnerActivity.CurrentActivity.RunnerBilling().getDownloadedFilesJSON();
        try
        {
	        if (jsonObject != null)
    	    {
    	    	// Log.i("yoyo", "Looking for JSON content for id: " + contentId);    	    	
    	    	if (jsonObject.has(contentId))
    	    	{
	    	    	JSONArray fileNames = jsonObject.getJSONArray(contentId);
    		    	returnArray = new String[fileNames.length()];
    	    		for (int n = 0; n < fileNames.length(); n++)
   		    		{
    		    		returnArray[n] = fileNames.getString(n);
   		    		}
    	    	}
	        }
	    }
	    catch (JSONException e) {
	    	e.printStackTrace();
	    }
	    return returnArray;
    }
    
	// ----------------------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------------------
    public static boolean DownloadFileTo( String _urlFile, String _to )
    {
    	boolean ret = false;
    	try {
			Log.i( "yoyo", "DownloadFileTo( " + _urlFile + " , " + _to + " )" );
			URL url = new URL( _urlFile );
			URLConnection connection = url.openConnection();
			connection.setUseCaches( false );
			connection.connect();
			FileOutputStream fs = new FileOutputStream( new File( _to ) );
			InputStream in = connection.getInputStream();
			byte[] buffer = new byte[1024];
			int len1 = 0;
			while( (len1 = in.read(buffer)) > 0) {
				Log.i( "yoyo", "downloaded " + len1 + " bytes" );
				fs.write( buffer, 0, len1 );
			} // end while
			fs.close();
			ret = true;
		} // end try
		catch( MalformedURLException _e) {
			Log.i( "yoyo", "Exception on DownloadFileTo" + _e );
		} // end catch
		catch( ProtocolException _e) {
			Log.i( "yoyo", "Exception on DownloadFileTo" + _e );
		} // end catch
		catch( FileNotFoundException _e) {
			Log.i( "yoyo", "Exception on DownloadFileTo" + _e );
		} // end catch
		catch( IOException _e) {
			Log.i( "yoyo", "Exception on DownloadFileTo" + _e );
		} // end catch
		
		return ret;
    } // end DownloadFileTo

	// ----------------------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------------------
	public static void FacebookInit(String _appID) {
			
		try {
					
			Log.i("yoyo", "Received Facebook app ID: " + _appID);
			if ((_appID == null) || (_appID.length() == 0)) {			
			
				Log.i("yoyo", "No app ID supplied - acquiring Facebook app ID from manifest");								
				_appID = RunnerActivity.CurrentActivity.getFacebookAppId();
	        }
	        	        
	        // Ensure that the app ID does not have any extraneous quotes, as it will do if it's coming in from the ini file
	        String appID = _appID.replace("\"", "");
        
	        Log.i("yoyo", "RunnerJNILib; Initialising Facebook using appID: " + appID);
			if (m_runnerFacebook == null) {
				m_runnerFacebook = new RunnerFacebook(ms_context);
    		}
    		m_runnerFacebook.initFacebook(appID);	        	        
		}
		catch (Exception e) {				
		    e.printStackTrace();
		}			
	}
    
    public static String FacebookAccessToken() {
		if(m_runnerFacebook.msFacebook!=null)
			if(m_runnerFacebook.msFacebook.isSessionValid()) 
				return m_runnerFacebook.msFacebook.getAccessToken();
				
		return "";
    }
    
    public static String FacebookUserId() {
    
		if(m_runnerFacebook.msFacebook!=null)
			if(m_runnerFacebook.msFacebook.isSessionValid()) 
				return m_runnerFacebook.getUserId();
		return "";
    }
	
	// ----------------------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------------------
	public static String FacebookLoginStatus() {		
	    	
		if(m_runnerFacebook.msFacebook!=null)
		{
			return m_runnerFacebook.facebookLoginStatus();		
		}
		return "";
	}
    
    // Called by GM to request that we login to Facebook and request permissions
    public static void FacebookLogin(String[] permissions) {
    
    	if (m_runnerFacebook == null) {
			m_runnerFacebook = new RunnerFacebook(ms_context);
    	}
    
    	Log.i( "yoyo", "Logging into Facebook");    	    	
		m_runnerFacebook.setupFacebook(permissions);
	}
	
	// ----------------------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------------------
	public static void FacebookLogout() {
	
		Log.i( "yoyo", "Logging out of Facebook"); 
		m_runnerFacebook.logout();
	}
	
	// ----------------------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------------------
	// Pass over a Facebook graph request			
	public static void FacebookGraphRequest(String graphPath, String httpMethod, String[] keyValuePairs, int dsMapResponse) {
	
		m_runnerFacebook.graphRequest(graphPath, httpMethod, keyValuePairs, dsMapResponse);
	}
    
 	// ----------------------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------------------
    // Pass over a request for a Facebook dialog    
	public static void FacebookDialog(String dialogType, String[] keyValuePairs, int dsMapResponse) {
	
		m_runnerFacebook.dialog(dialogType, keyValuePairs, dsMapResponse);
    }	
    
 	// ----------------------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------------------
	public static void ShowMessage(String _message ) {
    	Log.i( "yoyo", "ShowMessage(\""+_message+"\")");
    	
    	final String sMessage = _message;
    	final CountDownLatch latch = new CountDownLatch(1);
    	RunnerActivity.ViewHandler.post( new Runnable() {
    		 public void run() {
    	    		AlertDialog.Builder builder = new AlertDialog.Builder(ms_context);
    	    		builder.setMessage( sMessage)
    	    			.setCancelable(false)
    	    			.setPositiveButton( "OK", new DialogInterface.OnClickListener() {
    	    				public void onClick( DialogInterface dialog, int id ) {
    	    					latch.countDown();
    	    				}
    	     			});
    	    		AlertDialog alert  = builder.create();
    	    		alert.show();    		 
    		 }
    	});
    	
    	try {
    		latch.await();
    	} catch( InterruptedException e ) {
    		Thread.currentThread().interrupt();
    	} // end catch
	} // end ShowMessage
	
 	// ----------------------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------------------
	public static void ShowMessageAsync(String _message, int _id ) {
    	Log.i( "yoyo", "ShowMessageAsync(\""+_message+"\","+_id+")");
    	
    	final String sMessage = _message;
    	final int idDialog = _id;
    	RunnerActivity.ViewHandler.post( new Runnable() {
    		 public void run() {
    	    		AlertDialog.Builder builder = new AlertDialog.Builder(ms_context);
    	    		builder.setMessage( sMessage)
    	    			.setCancelable(false)
    	    			.setPositiveButton( "OK", new DialogInterface.OnClickListener() {
    	    				public void onClick( DialogInterface dialog, int id ) {
    	    					RunnerJNILib.InputResult( "OK", 1,  idDialog);	
    	    				}
    	     			});
    	    		AlertDialog alert  = builder.create();
    	    		alert.show();    		 
    		 }
    	});
    	
	} // end ShowMessage
	
	
 	// ----------------------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------------------
	public static String InputString( String _message, String _default ) {
    	Log.i( "yoyo", "InputString(\"" +_message+ "\", \""+_default+"\")");
    	
    	final String sMessage = _message;
    	final String sDefault = _default;
    	final CountDownLatch latch = new CountDownLatch(1);
    	RunnerActivity.ViewHandler.post( new Runnable() 
    	{
    		 public void run() {
    	    		AlertDialog.Builder builder = new AlertDialog.Builder(ms_context);
    	    		final EditText input = new EditText(ms_context);
    	    		input.setText(sDefault);
					builder.setView(input);
    	    		builder.setMessage(sMessage)
    	    			.setCancelable(false)
    	    			.setPositiveButton( "OK", new DialogInterface.OnClickListener() {
    	    				public void onClick( DialogInterface dialog, int id ) {
    	    					RunnerActivity.InputStringResult = input.getText().toString();
    	    					latch.countDown();
    	    				}
    	     			});
					builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							RunnerActivity.InputStringResult = sDefault;
    	    				latch.countDown();
							return;   
						}
					});

    	    		AlertDialog alert  = builder.create();
    	    		alert.show();    		 
    		 }
    	});
    	
    	try {
    		latch.await();
    	} catch( InterruptedException e ) {
    		Thread.currentThread().interrupt();
    	} // end catch
    	return RunnerActivity.InputStringResult;
	} // end ShowMessage	
	
 	// ----------------------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------------------
	public static void InputStringAsync( String _message, String _default, int _id ) {
    	Log.i( "yoyo", "InputStringAsync(\"" +_message+ "\", \""+_default+"\","+_id+")");
    	
    	final String sMessage = _message;
    	final String sDefault = _default;
    	final int idDialog = _id;
    	RunnerActivity.ViewHandler.post( new Runnable() 
    	{
    		 public void run() {
    	    		AlertDialog.Builder builder = new AlertDialog.Builder(ms_context);
    	    		final EditText input = new EditText(ms_context);
    	    		input.setText(sDefault);
					builder.setView(input);
    	    		builder.setMessage(sMessage)
    	    			.setCancelable(false)
    	    			.setPositiveButton( "OK", new DialogInterface.OnClickListener() {
    	    				public void onClick( DialogInterface dialog, int id ) {
    	    					RunnerActivity.InputStringResult = input.getText().toString();
    	    					RunnerJNILib.InputResult( RunnerActivity.InputStringResult, 1,  idDialog);	
    	    				}
    	     			});
					builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							RunnerActivity.InputStringResult = sDefault;
   	    					RunnerJNILib.InputResult( RunnerActivity.InputStringResult, 0,  idDialog);	
						}
					});

    	    		AlertDialog alert  = builder.create();
    	    		alert.show();    		 
    		 }
    	});    	
	} // end InputStringAsync	
	
	
 	// ----------------------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------------------
	public static int ShowQuestion(String _message ) {
    	Log.i( "yoyo", "ShowQuestion(\""+_message+"\")");
    	
    	final String sMessage = _message;
    	final CountDownLatch latch = new CountDownLatch(1);
    	RunnerActivity.ViewHandler.post( new Runnable() {
    		 public void run() {
    	    		AlertDialog.Builder builder = new AlertDialog.Builder(ms_context);
    	    		builder.setMessage( sMessage)
    	    			.setCancelable(false)
    	    			.setPositiveButton( "Yes", new DialogInterface.OnClickListener() {
    	    				public void onClick( DialogInterface dialog, int id ) {
								RunnerActivity.ShowQuestionYesNo = 1;
    	    					latch.countDown();
    	    				}
    	     			});
					builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							RunnerActivity.ShowQuestionYesNo = 0;
    	    				latch.countDown();
							return;   
						}
					});
    	    		AlertDialog alert  = builder.create();
    	    		alert.show();    		 
    		 }
    	});
    	
    	try {
    		latch.await();
    	} catch( InterruptedException e ) {
    		Thread.currentThread().interrupt();
    	} // end catch
    	return RunnerActivity.ShowQuestionYesNo;
	} // end ShowMessage
	
 	// ----------------------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------------------
	public static void ShowQuestionAsync(String _message, int _id ) {
    	Log.i( "yoyo", "ShowQuestionAsync(\""+_message+"\","+_id+")");
    	
    	final String sMessage = _message;
    	final int idDialog = _id;
    	RunnerActivity.ViewHandler.post( new Runnable() {
    		 public void run() {
    	    		AlertDialog.Builder builder = new AlertDialog.Builder(ms_context);
    	    		builder.setMessage( sMessage)
    	    			.setCancelable(false)
    	    			.setPositiveButton( "Yes", new DialogInterface.OnClickListener() {
    	    				public void onClick( DialogInterface dialog, int id ) {
	   	    					RunnerJNILib.InputResult( "1", 1,  idDialog);	
    	    				}
    	     			});
					builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
   	    					RunnerJNILib.InputResult( "0", 0,  idDialog);	
						}
					});
    	    		AlertDialog alert  = builder.create();
    	    		alert.show();    		 
    		 }
    	});    	
	} // end ShowQuestionAsync
	
	
	
 	// ----------------------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------------------
	public static void HttpGet(String _url, int _id ) {
    	Log.i( "yoyo", "HttpGet(\""+_url+"\", "+_id+")");
		// lets run this on the main UI thread...
		final String url = _url;
		final int id = _id;
    	new Thread( new Runnable() {
    		 public void run() {
    		 
    			HttpURLConnection conn;
    			try 
    			{
    				URL connectURL = new URL(url);
    				conn = (HttpURLConnection)connectURL.openConnection();
    			}
    			catch( MalformedURLException _mue ) {
    				RunnerJNILib.HttpResultString( "MalformedURLException", 404,  id);	
    				conn = null;
    			}
				catch( IOException _ioe) {
					RunnerJNILib.HttpResultString( "IOException1", 404,  id);	
    				conn = null;
				} 
    			if (conn != null) {
    				try
    				{
    					//conn.setDoInput(true);
    					//conn.setDoOutput(true);
    					conn.setUseCaches(false);
    					//conn.setRequestMethod("GET");
	    				
    					conn.connect();
	    				
    					int responseCode = conn.getResponseCode();
    					byte[] result = null;
    					if (responseCode == 200) {
    						InputStream is = conn.getInputStream();
    						ByteArrayOutputStream out = new ByteArrayOutputStream();
    						byte[] buffer = new byte[ 4096 ];
    						int n = -1;
    						while( (n=is.read(buffer)) != -1 ) {
    							out.write(buffer, 0, n );
    						} // end while
    						result = out.toByteArray();
    						out.close();
    					} // end if
    					else {
    						result = new byte[0];
    					} // end else
    					
	    					    				
					   	Log.i( "yoyo", "http_get result = \"" + result+"\", responseCode=" + responseCode + ", id=" + id);
    					RunnerJNILib.HttpResult( result, responseCode,  id);	
    					
    					conn.disconnect();
    					conn = null;
    				}
    				catch( Exception _e) {
					   	Log.i( "yoyo", "Exception = " + _e.toString());
    					RunnerJNILib.HttpResultString( "IOException", 404,  id);	
    					conn = null;
    				}
    			} // end if    		 
    		 } // end run
    	}).start();
	} // end HttpGet
	
 	// ----------------------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------------------
	public static void HttpPost(String _url, String _post, int _id ) {
  //  	Log.i( "yoyo", "HttpPost(\""+_url+"\", \""+_post+"\", "+_id+")");
		// lets run this on the main UI thread...
		final String url = _url;
		final String post = _post;
		final int id = _id;
    	new Thread( new Runnable() {
    		 public void run() {
    		 
    			HttpURLConnection conn;
    			try 
    			{
    				URL connectURL = new URL(url);
    				conn = (HttpURLConnection)connectURL.openConnection();
    			}
    			catch( MalformedURLException _mue ) {
    				RunnerJNILib.HttpResultString( "MalformedURLException", 404,  id);	
    				conn = null;
    			}
				catch( IOException _ioe) {
					RunnerJNILib.HttpResultString( "IOException", 404,  id);	
    				conn = null;
				} 
    			if (conn != null) {
    				try
    				{
    					conn.setDoInput(true);
    					conn.setDoOutput(true);
    					conn.setUseCaches(false);
    					conn.setRequestMethod("POST");
	    				
    					conn.connect();
	    				
    					byte[] postBytes = post.getBytes("UTF-8");
    					conn.getOutputStream().write(postBytes);
    					conn.getOutputStream().flush();
    					conn.getOutputStream().close();
    						    				
    					int responseCode = conn.getResponseCode();
    					byte[] result = null;
    					if (responseCode == 200) {
    						InputStream is = conn.getInputStream();
    						ByteArrayOutputStream out = new ByteArrayOutputStream();
    						byte[] buffer = new byte[ 4096 ];
    						int n = -1;
    						while( (n=is.read(buffer)) != -1 ) {
    							out.write(buffer, 0, n );
    						} // end while
    						result = out.toByteArray();
    						out.close();
    					} // end if
	    					    				
    					RunnerJNILib.HttpResult( result, responseCode,  id);	
    					
    					conn.disconnect();
    					conn = null;
    				}
    				catch( Exception _e) {
    					RunnerJNILib.HttpResultString( "IOException", 404,  id);	
    					conn = null;
    				}
    			} // end if    		 
    		 } // end run
    	}).start();
	} // end HttpPost
 	// ----------------------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------------------
	public static void ShowLogin(String _defaultUsername, String _defaultPassword, int _id ) {
    	Log.i( "yoyo", "LoginDialog(\"" +_defaultUsername+ "\", \""+_defaultPassword+"\","+_id+")");
    	
    	final String sDefaultUserName = _defaultUsername;
    	final String sDefaultPassword = _defaultPassword;
    	final int idDialog = _id;
    	RunnerActivity.FocusOverride = true;
    	RunnerActivity.ViewHandler.post( new Runnable() 
    	{
    		 public void run() {
    	    		AlertDialog.Builder builder = new AlertDialog.Builder(RunnerActivity.CurrentActivity);
    	    		LayoutInflater factory = LayoutInflater.from(RunnerActivity.CurrentActivity);
    	    		final View textEntryView = factory.inflate( R.layout.userpasslayout, null );
					builder.setView(textEntryView);
					final EditText userNameEditText = (EditText)textEntryView.findViewById(R.id.username);
					final EditText passwordEditText = (EditText)textEntryView.findViewById(R.id.password);
					userNameEditText.setText( sDefaultUserName );
					passwordEditText.setText( sDefaultPassword );
    	    		builder.setCancelable(false)
    	    			.setPositiveButton( "OK", new DialogInterface.OnClickListener() {
    	    				public void onClick( DialogInterface dialog, int id ) {
    	    					RunnerJNILib.LoginResult( userNameEditText.getText().toString() + '#' + passwordEditText.getText().toString(), userNameEditText.getText().toString(), idDialog ); 
    	    				}
    	     			});
					builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							RunnerJNILib.LoginResult( null, null, idDialog );
						}
					});

    	    		AlertDialog alert  = builder.create();
    	    		alert.show();    		 
    		 }
    	});
	} // end ShowLogin
	
	public static void RestrictOrientation( boolean _landscape, boolean _portrait ) {
    	Log.i( "yoyo", "RestrictOrientation(\"" +_landscape+ "\", \""+_portrait+"\")");
    	
    	Activity activity = RunnerActivity.CurrentActivity;
    	if (_landscape && !_portrait) {
    		activity.setRequestedOrientation( ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE );
    	} // end if
    	else
    	if (!_landscape && _portrait) {
    		activity.setRequestedOrientation( ActivityInfo.SCREEN_ORIENTATION_PORTRAIT );
    	} // end if
    	else {
    		activity.setRequestedOrientation( ActivityInfo.SCREEN_ORIENTATION_SENSOR );
    	} // end else
	} // end RestrictOrientation
	
	public static boolean isNetworkConnected()
	{
        ConnectivityManager conMan = (ConnectivityManager) ms_context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetwork = conMan.getActiveNetworkInfo();
		return (activeNetwork != null) && activeNetwork.isConnected();
	} // end isNetworkConnected

	public static void powersaveEnable( boolean _enable )
	{
		final boolean enable = _enable;
		RunnerActivity.ViewHandler.post( new Runnable() {
			public void run() {
				if (enable) {
					RunnerActivity.CurrentActivity.getWindow().clearFlags( android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON );
				} // end if
				else {
					RunnerActivity.CurrentActivity.getWindow().addFlags( android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON );
				} // end else
			}
		});
	} // end powersaveEnable

	public static void analyticsEvent( String _string )
	{
		if( RunnerActivity.Flurry )
		{
			Log.i( "yoyo", "Flurry Analytics event: " + _string );
			FlurryAgent.logEvent( _string );
		}
		else if( RunnerActivity.mbGoogleAnalytics )
		{
			RunnerActivity.googleAnalyticsEvent( _string );
		}
		
	} // end analyticsEvent
	
	public static void analyticsEventExt( String _event, String[] keyValuePairs )
	{
		if( RunnerActivity.Flurry )
		{
			Log.i( "yoyo", "Flurry Analytics Extended event: " + _event );
			Map<String, String> params = new HashMap<String, String>();
			for (int n = 0; n < keyValuePairs.length; n += 2) 
			{
          		params.put(keyValuePairs[n], keyValuePairs[n+1]);
          		Log.i( "yoyo", keyValuePairs[n] + " " + keyValuePairs[n+1] );
    		}
			FlurryAgent.logEvent(_event, params);
		}
		else if( RunnerActivity.mbGoogleAnalytics )
		{
			String label = keyValuePairs[0];
			int value = Integer.parseInt( keyValuePairs[1] );
			RunnerActivity.googleAnalyticsEvent( _event, label, value );
		}
	}
	
	public static void cloudStringSave( String _data, String _desc, int _id )
	{
		//Log.i("yoyo", "cloudStringSave: " + _data + " " + _desc + " " + _id );
		RunnerActivity.wsUploadData( _data, _desc, _id );
	}
	
	public static void cloudSynchronise( int _id)
	{
		//Log.i("yoyo", "cloudSynchronise" );
		//RunnerActivity.wsSynchroniseFiles();
		RunnerActivity.wsSynchroniseData(_id);
	}

	// ----------------------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------------------
    public static void DumpUsedMemory() {
    	Debug.MemoryInfo memoryInfo = new Debug.MemoryInfo();
		Debug.getMemoryInfo(memoryInfo);

		String memMessage = String.format("App Memory: Pss=%.2f MB\nPrivate=%.2f MB\nShared=%.2f MB",
		memoryInfo.getTotalPss() / 1024.0,
		memoryInfo.getTotalPrivateDirty() / 1024.0,
		memoryInfo.getTotalSharedDirty() / 1024.0);

		Log.i( "yoyo", memMessage );
	} // end DumpUsedMemory

	
	// ----------------------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------------------
	public static Context GetApplicationContext()
	{
		return ms_context;
	}

	// ----------------------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------------------
	public static int GamepadsCount()
	{
		return Gamepad.DeviceCount();
	}

	public static boolean GamepadConnected(int deviceIndex)
	{
		return Gamepad.DeviceConnected(deviceIndex);
	}

	public static String GamepadDescription(int deviceIndex)
	{
		return Gamepad.GetDescriptor(deviceIndex);
	}

	public static float[] GamepadAxesValues(int deviceIndex)
	{
		return Gamepad.GetAxesValues(deviceIndex);
	}

	public static float[] GamepadButtonValues(int deviceIndex)
	{
		return Gamepad.GetButtonValues(deviceIndex);
	}

	public static int GamepadGMLMapping(int deviceIndex, int inputId)
	{
		return Gamepad.GetGamepadGMLMapping(deviceIndex, inputId);
	}
}
