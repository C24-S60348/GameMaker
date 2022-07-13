package ${YYAndroidPackageName};

import com.google.ads.AdView;
import com.google.ads.AdSize;
import com.google.ads.AdRequest;
import com.google.ads.InterstitialAd;
import com.google.ads.Ad;
import android.app.Activity;
import android.view.ViewGroup;
import android.view.View;
import android.os.Build;
import android.util.Log;
import android.provider.Settings.Secure;
import ${YYAndroidPackageName}.RunnerActivity;
import com.yoyogames.runner.RunnerJNILib;
import com.google.ads.AdListener;


class AdvertisingAdMob extends AdvertisingBase implements AdListener
{
	private InterstitialAd interstitialAd=null;
	
	View[] m_views;
	String m_deviceid;
	public AdvertisingAdMob( Activity _activity, ViewGroup _viewGroup,boolean _usetestads,String _deviceid) {
		super( _activity, _viewGroup,_usetestads );
		m_deviceid = _deviceid;
		m_views = new View[ m_adDefinitions.length ];
	} // end AdvertisingMopub

	@Override
	public void setView( int _index )
	{
		m_view = m_views[ _index ];
	} // end setView

    @Override
	public void refresh(int _index)
	{
		AdView v  = ((AdView)m_view);
		AdRequest a = new AdRequest();
		if(m_usetestads)
		{
			a.addTestDevice(m_deviceid);
			
			if(!a.isTestDevice(RunnerJNILib.ms_context))
			{
				Log.i("yoyo","Error setting AdMob device id.");
			}
			else
			{
				Log.i("yoyo","AdMob using test adverts");
			}
			
		}
		v.loadAd( a );
	} // end refresh

	private void request_new_interstitial()
	{
			final boolean usetest = m_usetestads;
			final String deviceid = m_deviceid; 
			
    		
    		RunnerActivity.ViewHandler.post( new Runnable() {
    			 public void run() {
    	    		AdRequest adRequest = new AdRequest();
					if(usetest)
					{
						adRequest.addTestDevice(deviceid);
					}
					interstitialAd.loadAd(adRequest);	
    			 }
    		});
	}
	
	@Override
	public boolean interstitial_available()
	{
		//Log.i("yoyo","interstitial Available called");
		if(interstitialAd!= null)
			return interstitialAd.isReady();
		return false;
	}

	@Override
	public boolean interstitial_display()
	{
		//Log.i("yoyo","interstitial display called");
		if(interstitialAd!= null)
		{
			if (interstitialAd.isReady()) {
				interstitialAd.show();
				return true;
			}		
		}
		return false;
	}
	@Override
	public void enable_interstitial(int _index)
	{
		//Log.i("yoyo","enabling AdMob InterstitialAd");
		if (interstitialAd.isReady()) {
		//	interstitialAd.show();
		}	
		else
      	{
      		Log.i("yoyo","AdMob interstitial not ready, requesting new ad");
      		request_new_interstitial();
      	} 
	}

    @Override
	public void define( int _index, String _key, AdTypes _type )
	{
	
		
		super.define( _index, _key, _type );
		// create the view
		View v = null;
		AdSize sz = null;
		switch( _type ) {
		case BANNER:		sz = AdSize.BANNER; break;
		case MRECT:			sz = AdSize.IAB_MRECT; break;
		case FULL_BANNER:	sz = AdSize.IAB_BANNER; break;
		case LEADERBOARD:	sz = AdSize.IAB_LEADERBOARD; break;
		case SKYSCRAPER:	sz = AdSize.IAB_WIDE_SKYSCRAPER; break;
		case INTERSTITIAL:  
			{
				interstitialAd = new InterstitialAd(m_activity,m_adDefinitions[ _index ]);
				interstitialAd.setAdListener(this);
				request_new_interstitial();			
			}
		} // end switch
		if (sz != null) {
			v = new AdView( m_activity, sz, _key );
			
			int sdkVersion =Build.VERSION.SDK_INT;
			if (sdkVersion > 10)
				v.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
				
			//Log.i("yoyo","Creating ad mob ad with width " + sz.getWidthInPixels( m_activity ) + " height " +sz.getHeightInPixels( m_activity ));	
			//FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(  sz.getWidthInPixels( m_activity ), sz.getHeightInPixels( m_activity ));
			m_viewGroup.addView( v );
			m_views[ _index ] = v;
		} // end if
	} // end define
     
    @Override
	public void onFailedToReceiveAd(Ad ad, AdRequest.ErrorCode error) {
		String message = "onFailedToReceiveAd (" + error + ")";
		Log.i("yoyo", message);
	}
	
	@Override
	public void onReceiveAd(Ad ad) {
	}
  
    /**
   * Called when an ad is clicked and going to start a new Activity that will
   * leave the application (e.g. breaking out to the Browser or Maps
   * application).
   */
  @Override
  public void onLeaveApplication(Ad ad) {
  }

  @Override
  public void onDismissScreen(Ad ad) {
   // Log.d(LOG_TAG, "onDismissScreen");
   // Toast.makeText(this, "onDismissScreen", Toast.LENGTH_SHORT).show();
   
	request_new_interstitial();
  
		
  }
  
 
  /**
   * Called when an Activity is created in front of the app (e.g. an
   * interstitial is shown, or an ad is clicked and launches a new Activity).
   */
  @Override
  public void onPresentScreen(Ad ad) {
  //  Log.d(LOG_TAG, "onPresentScreen");
  //  Toast.makeText(this, "onPresentScreen", Toast.LENGTH_SHORT).show();
  }
}