package ${YYAndroidPackageName};

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;
import java.lang.reflect.Field;
import java.io.File;
import java.nio.IntBuffer;
import java.util.Locale;

import java.io.InputStream;
import java.io.IOException;

import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.GLES11Ext;
import android.os.Environment;

import android.app.Application;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.ConfigurationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageInfo;
import android.content.pm.Signature;
import android.provider.Settings.Secure;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.util.Log;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.yoyogames.runner.RunnerJNILib;

/*
import java.security.MessageDigest;
import android.util.Base64;
import java.security.NoSuchAlgorithmException;
*/

//----------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------
public class DemoRenderer implements GLSurfaceView.Renderer 
{
	public enum eState 
	{
		Startup,
		DownloadGameDroidInit,
		DownloadGameDroidWait,
		Splash,
		Splash2,
		InitRunner,
		WaitForDoStartup,
		DoStartup,
		Process,
	};
	public static eState m_state;
	public int m_renderCount;
	private Context m_context;
	private int m_width;
	private int m_height;
	public static String m_apkFilePath;
	private String m_packageName;
	private String m_apkOriginalPath;
	public static String m_saveFilesDir;
	private int m_tex;
	private int m_texWidth;
	private int m_texHeight;
	public static final String kGameAssetsDROID = "GameAssetsDROID.zip";
	
	public static int m_defaultFrameBuffer = -1;
	
	public boolean m_pauseRunner = false;
	
	public DemoRenderer( Context _context )
	{
		m_context = _context;
		m_state = eState.Startup;
		m_renderCount = 0;
		m_packageName = m_context.getPackageName();
	}
	
	public InputStream getResourceAsReader( String path)
    {
		System.out.println(path);
		try{
            android.content.res.AssetManager assetManager = m_context.getResources().getAssets();
        	return assetManager.open(path);
		}
		catch(Exception ee)
		{
			System.out.println("Exception while getting Resource");			
			return null;
		}
    }	
      
    public void onSurfaceCreated(GL10 gl, EGLConfig config) 
    {    	
	    if (m_state != eState.Startup) {
	    	Log.i("yoyo", "onSurfaceCreated() aborted on re-create");
	    	return;
	    }
	    
	    if (gl instanceof GL11) {
	    	IntBuffer intBuffer = IntBuffer.allocate(1);
	    	gl.glGetIntegerv(GLES11Ext.GL_FRAMEBUFFER_BINDING_OES, intBuffer);
	    	
	    	m_defaultFrameBuffer = intBuffer.get(0);
	    	Log.i("yoyo", "Renderer instance is gl1.1, framebuffer object is: " + m_defaultFrameBuffer);
	    }
    
 		if (RunnerActivity.YoYoRunner) {
    		m_saveFilesDir = Environment.getExternalStorageDirectory() + "/GMstudio";
			File studioDir = new File( m_saveFilesDir );
			studioDir.mkdir();
			m_saveFilesDir = m_saveFilesDir + '/';
		} else {
			m_saveFilesDir = m_context.getFilesDir().getAbsolutePath() + "/";
		} // end else
    	
		m_apkFilePath = null;
		ApplicationInfo appInfo = null;
		PackageManager packMgmr = m_context.getPackageManager();
		try {
			appInfo = packMgmr.getApplicationInfo("${YYAndroidPackageName}", 0);
		} 
		catch (NameNotFoundException e) {
			e.printStackTrace();
			throw new RuntimeException("Unable to locate assets, aborting...");
		} // end catch
		m_apkFilePath = appInfo.sourceDir;
		m_apkOriginalPath = appInfo.sourceDir;
		Log.i("yoyo", "File Path :: " + m_apkFilePath );
		
		// load splash screen in and bind it 
        int[] textures = new int[1];
        gl.glGenTextures(1, textures, 0);

        m_tex = textures[0];
        gl.glBindTexture(GL10.GL_TEXTURE_2D, m_tex);

        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);

        //gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        //gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

        //gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_REPLACE);
        
        ActivityManager activityMgmr = (ActivityManager)m_context.getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo info = activityMgmr.getDeviceConfigurationInfo();
        if (info.reqGlEsVersion >= 0x20000) {
        	Log.i("yoyo", "OpenGL ES-2.0 is supported: " + info.reqGlEsVersion);
        }
        else {
        	Log.i("yoyo", "OpenGL ES-CM 1.1 is supported: " + info.reqGlEsVersion);
        }
        
        // load the actual texture in
        InputStream is = getResourceAsReader( "splash.png" );
        Bitmap bitmap = null;
        try {
        	BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inDither=false;
            opt.inPreferredConfig = Bitmap.Config.ARGB_8888;
        	bitmap = BitmapFactory.decodeStream( is, null, opt );
        	m_texWidth = bitmap.getWidth();
        	m_texHeight = bitmap.getHeight();
        } 
        finally 
        {
        	try {
        		is.close();
        	} 
        	catch ( IOException _e ) {
        		// do nothing
        	} // end catch
        	
        }
		
        Bitmap pow2Bitmap = Bitmap.createBitmap(1024, 1024, Bitmap.Config.ARGB_8888);
    	GLUtils.texImage2D( GL10.GL_TEXTURE_2D, 0, pow2Bitmap, 0);
    	GLUtils.texSubImage2D(GL10.GL_TEXTURE_2D, 0, 0, 0, bitmap);
    	bitmap.recycle();
    	
		RunnerJNILib.SetKeyValue( 0, RunnerActivity.CurrentActivity.isTablet() ? 1 : 0, "" );
		RunnerJNILib.SetKeyValue( 1, 0, m_context.getCacheDir().getAbsolutePath() );
		RunnerJNILib.SetKeyValue( 2, 0, Locale.getDefault().getLanguage() );
		RunnerJNILib.SetKeyValue( 3, (int)m_context.getResources().getDisplayMetrics().densityDpi, "" );
		RunnerJNILib.SetKeyValue( 4, (int)m_context.getResources().getDisplayMetrics().densityDpi, "" );
		RunnerJNILib.SetKeyValue( 5, android.os.Build.VERSION.SDK_INT, android.os.Build.VERSION.RELEASE );
		PackageInfo pi;
		try {
			pi = packMgmr.getPackageInfo("${YYAndroidPackageName}", PackageManager.GET_SIGNATURES);
			
			for( Signature s : pi.signatures ) {
				RunnerJNILib.AddString( s.toCharsString() );

/*				MessageDigest md = MessageDigest.getInstance("SHA-1");
				md.update(s.toByteArray());
				String keyHash = Base64.encodeToString(md.digest(), Base64.DEFAULT);
				Log.i("yoyo", "*** KeyHash=" + keyHash);
*/
			} // end for
		} 
		catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}/* catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}*/
    }

    public void onSurfaceChanged(GL10 gl, int w, int h) 
    {    
    	m_width = w;
    	m_height = h;
        gl.glViewport(0, 0, w, h);
    	Log.i("yoyo", "onSurfaceChanged :: width="+m_width+" height="+m_height);
    }

    public void onDrawFrame(GL10 gl) {
    
    	// if we are paused then do go to sleep and don't do anything
    	if (m_pauseRunner) {
        	try {	
    			Thread.sleep(100);
    		} catch (InterruptedException e) {
    		    Log.i("yoyo", "Paused runner has thrown an exception!");
    		    e.printStackTrace();
    		} // end catch
    		return;
    	}
    	
    	switch( m_state ) {
    		case Startup:
				m_state = eState.Splash;
	    		gl.glClearColor( 0, 0, 0, 0 );
    			gl.glClear( GL10.GL_COLOR_BUFFER_BIT );
    			break;
	    	case Splash:	    	
    			if (RunnerActivity.YoYoRunner) {
					m_state = eState.Splash2;
    			} // end if
	    		else {
		    		m_state = eState.InitRunner;
    			} // end else
    			RunnerJNILib.RenderSplash( m_apkFilePath, "assets/splash.png", m_width, m_height, 1024, 1024, m_texWidth, m_texHeight );
	    		break;
    		case Splash2:
				m_state = eState.DownloadGameDroidInit;
    			RunnerJNILib.RenderSplash( m_apkFilePath, "assets/splash.png", m_width, m_height, 1024, 1024, m_texWidth, m_texHeight );
	    		break;
    		case DownloadGameDroidInit:	    	
    			RunnerJNILib.RenderSplash( m_apkFilePath, "assets/splash.png", m_width, m_height, 1024, 1024, m_texWidth, m_texHeight );
    			m_apkFilePath = m_saveFilesDir + kGameAssetsDROID;
				File fAssets = new File( m_apkFilePath );
	  		    Log.i("yoyo", "!!! Asset file - " + m_apkFilePath + " " + fAssets.exists() + " l=" + fAssets.lastModified() );
				File fLock = new File( m_saveFilesDir + "GameDownload.lock" );
	  		    Log.i("yoyo", "!!! Lock file - " + fLock.getAbsolutePath() + " " + fLock.exists() + " l=" + fLock.lastModified() );
				if (!fLock.exists() || (fLock.exists() && (fLock.lastModified() < fAssets.lastModified()))) {
					m_saveFilesDir = m_context.getFilesDir().getAbsolutePath() + "/";
					m_apkFilePath = m_saveFilesDir + kGameAssetsDROID;
	    			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( m_context );
		    		String ipAddress = prefs.getString( "hostIpAddress", "none" );
	    			String portNumber = prefs.getString( "hostPortNumber", "none" );
	    			final String url = "http://" + ipAddress + ":" + portNumber + "/" + kGameAssetsDROID;
	    			RunnerActivity.DownloadTaskStatus = DownloadStatus.NotConnected;
	    			m_state = eState.DownloadGameDroidWait;
					RunnerActivity.ViewHandler.post( new Runnable() {
						public void run() {
							RunnerActivity.DownloadTask = new RunnerDownloadTask();
			    			RunnerActivity.DownloadTask.execute( url, m_apkFilePath );
						}
					 });
				} // end if
				else {
		  		    Log.i("yoyo", "GameDownload.lock exists...");
					fLock.delete();
	    			m_state = eState.InitRunner;
				} // end else
    			break;
	    	case DownloadGameDroidWait:	    	
    			RunnerJNILib.RenderSplash( m_apkFilePath, "assets/splash.png", m_width, m_height, 1024, 1024, m_texWidth, m_texHeight );
				switch( RunnerActivity.DownloadTaskStatus ) {
				case SettingsChanged:
    				m_state = eState.DownloadGameDroidInit;
    				break;
	    		case Complete:
	    			m_state = eState.InitRunner;
    				break;
    			} // end switch
    			break;
    		case InitRunner:  
    			RunnerJNILib.RenderSplash( m_apkFilePath, "assets/splash.png", m_width, m_height, 1024, 1024, m_texWidth, m_texHeight );
	    		m_state = eState.WaitForDoStartup;
				RunnerActivity.ViewHandler.post( new Runnable() {
					public void run() {
						RunnerActivity.CurrentActivity.doSetup( m_apkFilePath );
					}
	    		});
    		
    			break;
			case WaitForDoStartup:
    			RunnerJNILib.RenderSplash( m_apkFilePath, "assets/splash.png", m_width, m_height, 1024, 1024, m_texWidth, m_texHeight );
				break;
			case DoStartup:
	    		// free the textures
    			int[] textures = new int[1];
    			gl.glDeleteTextures(1, textures, 0);
    	
    			RunnerJNILib.Startup(m_apkFilePath, m_saveFilesDir, m_packageName);
    		
	    		m_state = eState.Process;
    			break;
    		case Process:
    			int keypadStatus = 0;
	    		if (RunnerActivity.XPeriaPlay && (m_context.getResources().getConfiguration().navigation == 2) &&  
    				(m_context.getResources().getConfiguration().navigationHidden == 1)) {
    				keypadStatus = 1;
    			} // end if
	    		//Log.i("yoyo","keypad status = " +  m_context.getResources().getConfiguration().navigationHidden + " nav = " + m_context.getResources().getConfiguration().navigation + " status = " + keypadStatus);
    	        if (!RunnerJNILib.Process(m_width, m_height, RunnerActivity.AccelX, RunnerActivity.AccelY, RunnerActivity.AccelZ, keypadStatus, RunnerActivity.Orientation )) {            	
        	    	RunnerJNILib.ExitApplication();
            	} // end if
	    		break;
    	}
        m_renderCount--;
	}
}
