package ${YYAndroidPackageName};

import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.GLES10;
import android.util.AttributeSet;
import android.util.Log;
import android.content.Context;
import android.os.Handler;
import android.view.MotionEvent;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL;
import javax.microedition.khronos.opengles.GL10;
import com.yoyogames.runner.RunnerJNILib;

//----------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------

class DemoGLSurfaceView extends GLSurfaceView implements GLSurfaceView.EGLConfigChooser
{
    public DemoRenderer mRenderer;
	private Context m_context;
	int m_prev;

	// this is the number of milliseconds between refresh
	private int m_fpsTime;
	private Handler m_refreshHandler = new Handler();
	private Runnable m_refreshTick = new Runnable() {
		public void run() {
			m_refreshHandler.postDelayed( this, m_fpsTime);
			if (mRenderer.m_renderCount <= 0) {
				mRenderer.m_renderCount++;
				requestRender();
			} // end if
		}
	};
	
	public EGLConfig chooseConfig( EGL10 egl, EGLDisplay display )
	{
   		Log.i( "yoyo", "chooseConfig");

		int[] num_config = new int[1];
		int[] mMinConfigSpec = {
			EGL10.EGL_RED_SIZE, 5,
			EGL10.EGL_GREEN_SIZE, 6,
			EGL10.EGL_BLUE_SIZE, 5,
			EGL10.EGL_DEPTH_SIZE, 16,
			//EGL10.EGL_NATIVE_RENDERABLE, 1,
			//EGL10.EGL_CONFIG_CAVEAT, EGL10.EGL_NONE,

			EGL10.EGL_NONE
		};

		// First get number of valid configs
		egl.eglChooseConfig(display, mMinConfigSpec, null, 0, num_config);

		if (num_config[0] <= 0)
		{
			throw new IllegalArgumentException( "No EGL configs match our minimum required spec" );
		}

		// Now that we know the number we can grab them
		int numConfigs = num_config[0];
		EGLConfig[] configs = new EGLConfig[numConfigs];
		egl.eglChooseConfig(display, mMinConfigSpec, configs, numConfigs, num_config);		

		// Now work out if we can only support 16bit display formats
		boolean only16Bit = true;
		EGLContext testContext = egl.eglCreateContext(display, configs[0], EGL10.EGL_NO_CONTEXT, null);
		int eglError = egl.eglGetError();
		if ((eglError == EGL10.EGL_SUCCESS) && (testContext != null) && (testContext != EGL10.EGL_NO_CONTEXT))
		{			
			EGLSurface surf = egl.eglCreateWindowSurface(display, configs[0], getHolder(), null);

			if (surf == null || surf == EGL10.EGL_NO_SURFACE) {
				int error = egl.eglGetError();
				if (error != EGL10.EGL_SUCCESS)
				{
					Log.i("yoyo", "window surface can't be created");
				}
			}
			else
			{
				/*
				 * Before we can issue GL commands, we need to make sure
				 * the context is current and bound to a surface.
				 */
				if (!egl.eglMakeCurrent(display, surf, surf, testContext)) {
					/*
					 * Could not make the context current, probably because the underlying
					 * SurfaceView surface has been destroyed.
					 */
					Log.i("EGLHelper", "eglMakeCurrent broke");
				}
				else
				{

					GL gl = testContext.getGL();
					if (gl instanceof GL10)
					{
						GL10 glInterface = (GL10)gl;
						
						String extString = glInterface.glGetString( GL10.GL_EXTENSIONS );
						Log.i( "yoyo", "OpenGL ES Extensions : " + extString);       	

						// Now parse extension list to see if it has the "GL_OES_rgb8_rgba8" extension
						if ((extString.contains("GL_OES_rgb8_rgba8") == true)/* ||
							(extString.contains("GL_ARM_rgba8") == true)*/ )
						{
							Log.i( "yoyo", "Device supports 32bit display formats");       	
							only16Bit = false;
						}
					}

					// clean up
					egl.eglMakeCurrent(display, EGL10.EGL_NO_SURFACE,
						EGL10.EGL_NO_SURFACE,
						EGL10.EGL_NO_CONTEXT);						
				}

				// clean up
				egl.eglDestroySurface(display, surf);
			}				

			// Destroy the temporary context again
			egl.eglDestroyContext(display, testContext);
		}

		// Force 16bit if the appropriate option has been selected
		if ((m_context != null) && (m_context instanceof RunnerActivity) && (((RunnerActivity)m_context).mYYPrefs != null))
		{
			int allow24BitModes = ((RunnerActivity)m_context).mYYPrefs.getInt("YYUse24Bit");
			if (allow24BitModes == 1)
			{
				Log.i( "yoyo", "24 bit colour depth allowed");
				// we don't want to change the previously set value
			}
			else
			{
				Log.i( "yoyo", "16 bit colour depth forced");
				only16Bit = true;		
			}
		}
		else
		{
			if (m_context == null)
			{
				Log.i( "yoyo", "Context NULL");    
			}

			if (!(m_context instanceof RunnerActivity))
			{
				Log.i( "yoyo", "Context not RunnerActivity");   
			}

			if (((RunnerActivity)m_context).mYYPrefs == null)
			{
				Log.i( "yoyo", "mYYPrefs null");   
			}
		}

		// Now sort them into some sort of preferential order
		// For the moment we'll just just choose the most fully-featured, but for performance reasons
		// it may be more sensible to go for the minimum required
		int[] configSortKeys = new int[numConfigs];
		for(int i = 0; i < numConfigs; i++)
		{
			configSortKeys[i] = generateConfigSortKey(egl,display,configs[i], only16Bit);
		}

		// Sort the list
		// Just use a crappy bubble sort for the moment
		EGLConfig tempConfig;
		int tempSortKey;
		boolean sorted = false;		
		while(!sorted)
		{
			sorted = true;
			for(int i = 0; i < (numConfigs - 1); i++)
			{
				if (configSortKeys[i] < configSortKeys[i+1])
				{
					// Swap
					sorted = false;

					tempConfig = configs[i];
					configs[i] = configs[i+1];
					configs[i+1] = tempConfig;

					tempSortKey = configSortKeys[i];
					configSortKeys[i] = configSortKeys[i+1];
					configSortKeys[i+1] = tempSortKey;
				}
			}
		}

		for(EGLConfig config : configs)
		{
        	Log.i( "yoyo", "found EGL config : " + printConfig(egl,display,config));       	
        }

		// Right, now iterate through the list and try them in order until we find one that works
		// In theory they should all work, but apparently this isn't always the case
		// On at least one phone the tests always succeed, but I'm still keeping this in here as an additional safety feature
		int validConfig = -1;
		boolean gotExtensions = false;
		for(int i = 0; i < numConfigs; i++)
		{
        	Log.i( "yoyo", "Trying EGL config : " + printConfig(egl,display,configs[i]));       	
			//Log.i( "yoyo", "Trying EGL config : " + findConfigAttrib(egl, display, configs[i], EGL10.EGL_CONFIG_ID, 0) );

			//int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
			//int[] attrib_list = {EGL_CONTEXT_CLIENT_VERSION, mEGLContextClientVersion, EGL10.EGL_NONE };
			EGLContext tempContext = egl.eglCreateContext(display, configs[i], EGL10.EGL_NO_CONTEXT, null);//mEGLContextClientVersion != 0 ? attrib_list : null);
			eglError = egl.eglGetError();
			if ((eglError == EGL10.EGL_SUCCESS) && (tempContext != null) && (tempContext != EGL10.EGL_NO_CONTEXT))
			{
				//Log.i( "yoyo", "Passed context check - trying surface...");

				// Now test surface creation
				EGLSurface surf = egl.eglCreateWindowSurface(display, configs[i], getHolder(), null);

				if (surf == null || surf == EGL10.EGL_NO_SURFACE) {
					int error = egl.eglGetError();
					if (error != EGL10.EGL_SUCCESS)
					{
						Log.i("yoyo", "Surface can't be created - can't use this mode");
					}
				}
				else
				{
					//Log.i( "yoyo", "Passed surface check - try eglMakeCurrent");

					/*
					 * Before we can issue GL commands, we need to make sure
					 * the context is current and bound to a surface.
					 */
					if (!egl.eglMakeCurrent(display, surf, surf, tempContext)) {
						
						Log.i("yoyo", "eglMakeCurrent failed - can't use this mode");
					}
					else
					{
						// Passed all tests - this is a valid config
						Log.i( "yoyo", "Selected EGL config working");						

						// clean up
						egl.eglMakeCurrent(display, EGL10.EGL_NO_SURFACE,
							EGL10.EGL_NO_SURFACE,
							EGL10.EGL_NO_CONTEXT);					

						validConfig = i;						
					}

					// clean up
					egl.eglDestroySurface(display, surf);
				}						

				// Destroy the temporary context again
				egl.eglDestroyContext(display, tempContext);			
			}
			else
			{
				Log.i( "yoyo", "Selected EGL config failed");
			}

			if (validConfig != -1)
				break;	
        }

		if (validConfig != -1)
			return configs[validConfig];
		else
		{
			throw new IllegalArgumentException( "No valid EGL configs match our minimum required spec" );			
		}
    } // end chooseConfig
    
    private  String printConfig(EGL10 egl, EGLDisplay display, EGLConfig config) {
    	
		int id = findConfigAttrib(egl, display, config, EGL10.EGL_CONFIG_ID, 0);
    	 int r = findConfigAttrib(egl, display, config,	EGL10.EGL_RED_SIZE, 0);
         int g = findConfigAttrib(egl, display, config, EGL10.EGL_GREEN_SIZE, 0);
         int b = findConfigAttrib(egl, display, config, EGL10.EGL_BLUE_SIZE, 0);
         int a = findConfigAttrib(egl, display, config, EGL10.EGL_ALPHA_SIZE, 0);
         int d = findConfigAttrib(egl, display, config, EGL10.EGL_DEPTH_SIZE, 0);
         int s = findConfigAttrib(egl, display, config, EGL10.EGL_STENCIL_SIZE, 0);

             /*
              * 
              * EGL_CONFIG_CAVEAT value 
              
         #define EGL_NONE		       0x3038	
         #define EGL_SLOW_CONFIG		       0x3050	
         #define EGL_NON_CONFORMANT_CONFIG      0x3051	
			*/
         
		 return String.format("EGLConfig %d: rgba=%d%d%d%d depth=%d stencil=%d", id, r,g,b,a,d,s)
				+ " EGL_ALPHA_MASK_SIZE=" + findConfigAttrib(egl, display, config, EGL10.EGL_ALPHA_MASK_SIZE, 0)
				//+ " EGL_BIND_TO_TEXTURE_RGB=" + findConfigAttrib(egl, display, config, EGL10.EGL_BIND_TO_TEXTURE_RGB, 0)
				//+ " EGL_BIND_TO_TEXTURE_RGBA=" + findConfigAttrib(egl, display, config, EGL10.EGL_BIND_TO_TEXTURE_RGBA, 0)
				+ " EGL_BUFFER_SIZE=" + findConfigAttrib(egl, display, config, EGL10.EGL_BUFFER_SIZE, 0)
				+ " EGL_COLOR_BUFFER_TYPE=" + findConfigAttrib(egl, display, config, EGL10.EGL_COLOR_BUFFER_TYPE, 0)
				+ String.format(" EGL_CONFIG_CAVEAT=0x%04x" , findConfigAttrib(egl, display, config, EGL10.EGL_CONFIG_CAVEAT, 0))
				//+ String.format(" EGL_CONFORMANT=0x%04x" , findConfigAttrib(egl, display, config, EGL10.EGL_CONFORMANT, 0))
				+ " EGL_LEVEL=" + findConfigAttrib(egl, display, config, EGL10.EGL_LEVEL, 0)
				+ " EGL_LUMINANCE_SIZE=" + findConfigAttrib(egl, display, config, EGL10.EGL_LUMINANCE_SIZE, 0)
				+ " EGL_MAX_PBUFFER_WIDTH=" + findConfigAttrib(egl, display, config, EGL10.EGL_MAX_PBUFFER_WIDTH, 0)
				+ " EGL_MAX_PBUFFER_HEIGHT=" + findConfigAttrib(egl, display, config, EGL10.EGL_MAX_PBUFFER_HEIGHT, 0)
				+ " EGL_MAX_PBUFFER_PIXELS=" + findConfigAttrib(egl, display, config, EGL10.EGL_MAX_PBUFFER_PIXELS, 0)
				//+ " EGL_MAX_SWAP_INTERVAL=" + findConfigAttrib(egl, display, config, EGL10.EGL_MAX_SWAP_INTERVAL, 0)
				//+ " EGL_MIN_SWAP_INTERVAL=" + findConfigAttrib(egl, display, config, EGL10.EGL_MIN_SWAP_INTERVAL, 0)
				+ " EGL_MAX_PBUFFER_HEIGHT=" + findConfigAttrib(egl, display, config, EGL10.EGL_MAX_PBUFFER_HEIGHT, 0)
				+ " EGL_MAX_PBUFFER_HEIGHT=" + findConfigAttrib(egl, display, config, EGL10.EGL_MAX_PBUFFER_HEIGHT, 0)
        		+ " EGL_NATIVE_RENDERABLE=" + findConfigAttrib(egl, display, config, EGL10.EGL_NATIVE_RENDERABLE, 0)         	    								
				+ " EGL_NATIVE_VISUAL_TYPE=" + findConfigAttrib(egl, display, config, EGL10.EGL_NATIVE_VISUAL_TYPE, 0)
				+ " EGL_RENDERABLE_TYPE=" + findConfigAttrib(egl, display, config, EGL10.EGL_RENDERABLE_TYPE, 0)
				+ " EGL_SAMPLE_BUFFERS=" + findConfigAttrib(egl, display, config, EGL10.EGL_SAMPLE_BUFFERS, 0)
				+ " EGL_SAMPLES=" + findConfigAttrib(egl, display, config, EGL10.EGL_SAMPLES, 0)
				+ " EGL_SURFACE_TYPE=" + findConfigAttrib(egl, display, config, EGL10.EGL_SURFACE_TYPE, 0)
				+ " EGL_TRANSPARENT_TYPE=" + findConfigAttrib(egl, display, config, EGL10.EGL_TRANSPARENT_TYPE, 0)
				+ " EGL_TRANSPARENT_RED_VALUE=" + findConfigAttrib(egl, display, config, EGL10.EGL_TRANSPARENT_RED_VALUE, 0)								
				+ " EGL_TRANSPARENT_GREEN_VALUE=" + findConfigAttrib(egl, display, config, EGL10.EGL_TRANSPARENT_GREEN_VALUE, 0)
				+ " EGL_TRANSPARENT_BLUE_VALUE=" + findConfigAttrib(egl, display, config, EGL10.EGL_TRANSPARENT_BLUE_VALUE, 0)
         		;
    }
    
    private int findConfigAttrib(EGL10 egl, EGLDisplay display,
            EGLConfig config, int attribute, int defaultValue) {
    	
    	int[] mValue = new int[1];
        if (egl.eglGetConfigAttrib(display, config, attribute, mValue)) {
            return mValue[0];
        }
        return defaultValue;
    }    

	private int generateConfigSortKey(EGL10 egl, EGLDisplay display, EGLConfig config, boolean only16Bit) {
    	
    	 int colourSize = findConfigAttrib(egl, display, config,	EGL10.EGL_BUFFER_SIZE, 0);
         int depthSize = findConfigAttrib(egl, display, config, EGL10.EGL_DEPTH_SIZE, 0);
         int stencilSize = findConfigAttrib(egl, display, config, EGL10.EGL_STENCIL_SIZE, 0);
		 int caveat = findConfigAttrib(egl, display, config, EGL10.EGL_CONFIG_CAVEAT, 0);
		 int numSampleBuffers = findConfigAttrib(egl, display, config, EGL10.EGL_SAMPLE_BUFFERS, 0);		 

		 int caveatValue = 0;
		 switch(caveat)
		 {
			case EGL10.EGL_NONE:					caveatValue = 2; break;
			case EGL10.EGL_SLOW_CONFIG:				caveatValue = 1; break;
			case EGL10.EGL_NON_CONFORMANT_CONFIG:	caveatValue = 0; break;
			default:								caveatValue = 0; break;
		 } 

		 // Favour colour over depth over stencil (allow for values up to 32 for depth and stencil (though they shouldn't need as much as that) )		 
         //int sortKey = (caveatValue << 18) | (colourSize << 12) | (depthSize << 6) | stencilSize;		 		 
		 int sortKey = (caveatValue << 24) | ((32 - numSampleBuffers) << 18) | (colourSize << 12) | (depthSize << 6) | (stencilSize);

		 if (only16Bit == true)
		 {
			if (colourSize > 16)
			{
				sortKey = -1;
			}
		 }

         return sortKey;
    }
	
    public DemoGLSurfaceView(Context context, AttributeSet attrs)
	{    
        super(context, attrs);

		RunnerActivity.CurrentActivity.setupIniFile();

        //setEGLConfigChooser(true);
        Log.i("yoyo", "DemoGLSurfaceView: CREATED");
        m_context = context;
        m_prev = 0;
		m_fpsTime = 1000 / 60;
        mRenderer = new DemoRenderer( context );
		//setDebugFlags(DEBUG_CHECK_GL_ERROR | DEBUG_LOG_GL_CALLS);
        setEGLConfigChooser( this );
        //setEGLConfigChooser(8,8,8,8,16,0);
        setRenderer(mRenderer);
        //setRenderMode(RENDERMODE_WHEN_DIRTY);
		//m_refreshHandler.postDelayed( m_refreshTick, m_fpsTime);
    }
    
    /** Show an event in the LogCat view, for debugging */
    private void dumpEvent(MotionEvent event) {
       String names[] = { "DOWN" , "UP" , "MOVE" , "CANCEL" , "OUTSIDE" ,
          "POINTER_DOWN" , "POINTER_UP" , "7?" , "8?" , "9?" };
       StringBuilder sb = new StringBuilder();
       int action = event.getAction();
       int actionCode = action & MotionEvent.ACTION_MASK;
       sb.append("event ACTION_" ).append(names[actionCode]);
       if (actionCode == MotionEvent.ACTION_POINTER_DOWN
             || actionCode == MotionEvent.ACTION_POINTER_UP) {
          sb.append("(pid " ).append(
          action >> MotionEvent.ACTION_POINTER_ID_SHIFT);
          sb.append(")" );
       }
       sb.append("[" );
       for (int i = 0; i < event.getPointerCount(); i++) {
          sb.append("#" ).append(i);
          sb.append("(pid " ).append(event.getPointerId(i));
          sb.append(")=" ).append((int) event.getX(i));
          sb.append("," ).append((int) event.getY(i));
          if (i + 1 < event.getPointerCount())
             sb.append(";" );
       }
       sb.append("]" );
       Log.i("yoyo", sb.toString());
    }
    
    public boolean onTouchEvent(final MotionEvent event) {

    	//dumpEvent(event);
    	
    	//let the Native code know about the touch event
    	int action = event.getAction();
        int actionCode = action & MotionEvent.ACTION_MASK;
    	for( int i=0; i < event.getPointerCount(); ++i ) 
    	{
    		int id = event.getPointerId(i);
	    	if ((actionCode == MotionEvent.ACTION_POINTER_DOWN) || (actionCode == MotionEvent.ACTION_POINTER_UP))  
	       	{
	    		int index = ((action & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT);
    			//Log.i( "yoyo", i + ":: pointer index - " + index );
	       		// Ensure pointer up/down actions are correctly associated with a given touch
	    		if (index == i)
	    		{
	    			RunnerJNILib.TouchEvent( actionCode, id, event.getX(i), event.getY(i) );
	    		} 
	    		else 
	    		{
	    			RunnerJNILib.TouchEvent( MotionEvent.ACTION_MOVE, id, event.getX(i), event.getY(i) );    		
	    		}
    		} // end if
	    	else {
    			RunnerJNILib.TouchEvent( actionCode, id, event.getX(i), event.getY(i) );    			    		
	    	} // end else
    	} // end for
    	
    	try {
			Thread.sleep(16);
		} 
		catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        return true;
    }
}