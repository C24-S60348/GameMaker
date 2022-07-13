package ${YYAndroidPackageName};

import com.mopub.mobileads.MoPubView;
import android.app.Activity;
import android.view.ViewGroup;
import android.view.Display;

import android.util.Log;
import android.view.View;
import android.graphics.Color;
import ${YYAndroidPackageName}.RunnerActivity;
import android.view.Gravity;
import android.os.Build;
import com.yoyogames.runner.RunnerJNILib;
import android.os.Handler;

import com.mopub.mobileads.MoPubInterstitial;
import com.mopub.mobileads.MoPubInterstitial.MoPubInterstitialListener;


class AdvertisingMopub extends AdvertisingBase implements MoPubInterstitialListener
{

	private MoPubInterstitial interstitialAd=null;
	
	View[] m_views;
	public AdvertisingMopub( Activity _activity, ViewGroup _viewGroup,boolean _usetestads) {
		super( _activity, _viewGroup,_usetestads );
	
		Log.i("yoyo","*******Creating mopub*******");
		m_views = new View[ m_adDefinitions.length ];
	} // end AdvertisingMopub


	@Override
	public void setView( int _index )
	{
//	Log.i("yoyo","setting view to "+_index);
		m_view = m_views[ _index ];
	} // end setView
    @Override
	public void refresh(int _index)
	{
		Log.i("yoyo","refreshing "+_index);
		MoPubView v  = ((MoPubView)m_view);
		v.setAdUnitId( m_adDefinitions[_index] );
		v.loadAd();
	//	v.setBackgroundColor(Color.argb(255,0,0,255));
	} // end refresh
	
	
	@Override
	public boolean interstitial_available()
	{
		//Log.i("yoyo","interstitial Available called");
		if(interstitialAd!= null)
			return interstitialAd.isReady();
			
		//Log.i("yoyo","No Mo Pub interstitial available");
		return false;
	}


	@Override
	public void OnInterstitialLoaded() {
    	//Log.i("yoyo","Mo Pub interstitial loaded");
    	Log.i("yoyo","Mo Pub interstitial loaded");
    }

	@Override
	public void OnInterstitialFailed() {
    	//Log.i("yoyo","Mo Pub interstitial failed to load");
    	
    	 Handler handler = new Handler(); 
			handler.postDelayed(new Runnable() { 
				public void run() { 
				
				Log.i("yoyo","Mo Pub interstitial failed to load - requesting another on delay");
				if(interstitialAd!= null)
				{
					interstitialAd.load();
				}
         } 
    }, 60000); 
    	
    	
    }

	@Override
	public boolean interstitial_display()
	{
		//Log.i("yoyo","interstitial display called");
		
			RunnerActivity.ViewHandler.post( new Runnable() {
    			 public void run() {
					if(interstitialAd!= null)
					{
						if (interstitialAd.isReady()) {
							interstitialAd.show();
							
							
						}		
					}
					
				}
		}
		);
		return true;
	}
	
	
	
	@Override
	public void enable_interstitial(int _index)
	{
	
	}
	
	@Override
	public void onResume()
	{
		Log.i("yoyo","Mo Pub onResume");
		if(interstitialAd!=null)
		{
			interstitialAd.load(); // Kick it off to load another as there is no OnInterstitialClosed
		}
	}
	
	
	
	@Override
	public void define( int _index, String _key, AdTypes _type )
	{
		//Log.i("yoyo","defining unit "+_index);
	
		super.define( _index, _key, _type );
		
		switch( _type ) {
		default:
		//Log.i("yoyo","Mo Pub define called for "+ _type);
		break;
		case INTERSTITIAL:  
			{
			//	Log.i("yoyo","Mo Pub interstitial initialised");
				interstitialAd = new MoPubInterstitial(m_activity,m_adDefinitions[ _index ]);
				
				
				if(m_usetestads)
				{
				//	Log.i("yoyo","Mo Pub interstitial testing set to true");
					interstitialAd.setTesting(true);
				}
				//interstitialAd.setAutoRefreshEnabled(false);
				interstitialAd.setListener(this);
				interstitialAd.load();
				
				
				return;
				//request_new_interstitial();			
			}
		} // end switch
		
		// create the view
		MoPubView v = null;
	
		v = new MoPubView( RunnerJNILib.ms_context);
		if(m_usetestads)
		{
			v.setTesting(true);
		}
		
	
		int sdkVersion =Build.VERSION.SDK_INT;
		if (sdkVersion > 10)
			v.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
	
		v.setId( _index );
		v.setAdUnitId( m_adDefinitions[_index] );
		
		//m_viewGroup.addView( v);
		m_views[ _index ] = v;
		if(m_view==null)
		{
			m_view = m_views[_index];
		}
		v.setVisibility( View.GONE );     	
	} // end define
	
	


}