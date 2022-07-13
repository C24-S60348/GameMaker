package ${YYAndroidPackageName};


import com.millennialmedia.android.RequestListener;
import com.millennialmedia.android.MMAdView;
import com.millennialmedia.android.MMAd;
import com.millennialmedia.android.MMException;
import com.millennialmedia.android.MMInterstitial;
import com.millennialmedia.android.MMSDK;
import android.app.Activity;
import android.view.ViewGroup;
import android.view.View;
import android.view.Display;
import android.widget.AbsoluteLayout;
import android.widget.RelativeLayout;
import android.widget.FrameLayout;
import java.util.Hashtable;
import android.view.Gravity;
import android.graphics.Color;
import android.util.Log;
import android.os.Build;





class AdvertisingMillenialMedia extends AdvertisingBase
{
	public class Listener implements RequestListener
	{
	
		@Override
		public void MMAdOverlayLaunched(MMAd mmAd)
		{
			Log.i("yoyo", "Millennial Media Ad (" + mmAd.getApid() + ") overlay launched");
		}

		@Override
		public void MMAdRequestIsCaching(MMAd mmAd)
		{
			Log.i("yoyo", "Millennial Media Ad (" + mmAd.getApid() + ") caching started");
		}

		@Override
		public void requestCompleted(MMAd mmAd)
		{
			Log.i("yoyo","Millennial Media Ad (" + mmAd.getApid() + ") request succeeded");
		}

		@Override
		public void requestFailed(MMAd mmAd, MMException exception)
		{
			Log.i("yoyo",String.format("Millennial Media Ad (" + mmAd.getApid() + ") request failed with error: %d %s.", exception.getCode(), exception.getMessage()));
		}
	}

	

	MMInterstitial m_InterstitialView = null;

	
	View[] m_views;
	Listener m_listener;
	public AdvertisingMillenialMedia( Activity _activity, ViewGroup _viewGroup,boolean _usetestads) {
		super( _activity, _viewGroup,_usetestads );
		
		m_listener = new Listener();
		m_views = new View[ m_adDefinitions.length ];
		
		if(_usetestads)
		{
			MMSDK.setLogLevel(MMSDK.LOG_LEVEL_DEBUG);
		}
		
	} // end AdvertisingMopub

	@Override
	public void setView( int _index )
	{
		m_view = m_views[ _index ];
	} // end setView

    @Override
	public void refresh(int _index)
	{
		MMAdView v  = ((MMAdView)m_view);
		v.getAd();
	//	v.setBackgroundColor(Color.argb(255,0,0,255));
	} // end refresh

	@Override
	public boolean interstitial_available()
	{
		return true;	
	}
	@Override
	public void onResume()
	{
		Log.i("yoyo","Millennial Media onResume() called");
	}

	

	@Override
	public boolean interstitial_display()
	{
		//Log.i("yoyo","interstitial display called");
	
    		RunnerActivity.ViewHandler.post( new Runnable() {
    			 public void run() {
    	    		if(m_InterstitialView.isAdAvailable())
					{
						m_InterstitialView.display();
						m_InterstitialView.fetch();
					}
    			 }
    		});
	
		
		
		return true;
	}
	@Override
	public void enable_interstitial(int _index)
	{
		
	}

    @Override
	public void define( int _index, String _key, AdTypes _type )
	{
		super.define( _index, _key, _type );
		// create the view
		View v = null;
		Hashtable<String, String> map = new Hashtable<String, String>();
		switch( _type ) {
		case INTERSTITIAL:
						Log.i("yoyo","about to create adview");	
						m_InterstitialView = new MMInterstitial( m_activity);
						m_InterstitialView.setApid(_key);
						(m_InterstitialView).setListener( m_listener );
						
					//	m_InterstitialView.setId(MMSDK.DEFAULT_VIEWID+1);
						
					//	m_InterstitialView.setVisibility(View.INVISIBLE);
						if(m_InterstitialView!=null)
						{
							m_InterstitialView.fetch();
						}
						else
						{
							Log.i("yoyo","failed to create adview");	
						}
					
					
						
					//	m_InterstitialView.requestLayout();
						
						return;
		
		case BANNER:		v = new MMAdView( m_activity);//, _key, MMAdView.BANNER_AD_RECTANGLE, 30, map ); break;
		case MRECT:			v = new MMAdView( m_activity);//, _key, MMAdView.BANNER_AD_RECTANGLE, 30, map );break;
		case FULL_BANNER:	v = new MMAdView( m_activity);//, _key, MMAdView.BANNER_AD_TOP, 30, map ); break;
		case LEADERBOARD:	v = new MMAdView( m_activity);//, _key, MMAdView.BANNER_AD_RECTANGLE, 30, map );break;
		case SKYSCRAPER:	v = new MMAdView( m_activity);//, _key, MMAdView.BANNER_AD_RECTANGLE, 30, map );break;
		} // end switch
		
		((MMAdView)v).setApid(_key);
		
		//String widthString = Integer.toString(getAdWidth( _type ));
       // String heightString = Integer.toString(getAdHeight( _type ));
		
		((MMAdView)v).setWidth(getAdWidth( _type ));
        ((MMAdView)v).setHeight(getAdHeight( _type ));
		
		int sdkVersion =Build.VERSION.SDK_INT;
		if (sdkVersion > 10)
			v.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
		//v.setId( MMAdViewSDK.DEFAULT_VIEWID );
		((MMAdView)v).setListener( m_listener );
		AbsoluteLayout.LayoutParams params = new AbsoluteLayout.LayoutParams(  getAdWidth( _type ), getAdHeight( _type ),0,0);
		
		Log.i("yoyo","Creating ad with width:"+ getAdWidth( _type ) + " height:"+getAdHeight( _type ));
		v.setVisibility( View.GONE ); 
	//	m_viewGroup.addView( v, params );
	//	v.requestLayout();
		m_views[ _index ] = v;
	} // end define	
}