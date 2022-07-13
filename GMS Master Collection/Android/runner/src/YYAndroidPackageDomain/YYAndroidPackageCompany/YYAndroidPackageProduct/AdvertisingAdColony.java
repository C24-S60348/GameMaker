package ${YYAndroidPackageName};

import com.jirbo.adcolony.*;
import android.app.Activity;
import android.view.ViewGroup;
import android.view.Display;
import ${YYAndroidPackageName}.RunnerActivity;
import android.util.Log;
import android.content.Intent;






class AdvertisingAdColony extends AdvertisingBase
implements AdColonyV4VCListener
{

	String m_zoneid;
	String m_key;
	boolean m_setupcomplete = false;
	boolean use_v4vc = false;

	public void onV4VCResult(boolean success, String name, int amount)
	{
		if(success)
		{
			Log.i("yoyo","skulls awarded ok");
		}
		else
		{
			Log.i("yoyo","skulls failed to be awarded "+ name);
		}
	}
	
	public AdvertisingAdColony( Activity _activity, ViewGroup _viewGroup,boolean _usetestads, boolean useV4VC) {
		super( _activity, _viewGroup,_usetestads );
			
		Log.i("yoyo","Creating New AdColony!!!!!!" + useV4VC);
		use_v4vc = useV4VC;
	} // end AdvertisingAdColony


	@Override 
	public void setup(String _userid)
	{
		

		final String sUserid = _userid;
		final AdvertisingAdColony sThis = this;
    	
    	RunnerActivity.ViewHandler.post( new Runnable() {
    		 public void run() {
    	    	//Log.i("yoyo","Setting Ad Colony user id "+sUserid);
				//AdColony.setDeviceID(sUserid);
				Log.i("yoyo", "!!!!##### Created Ad Colony #####!!!!!! " + m_key);
				AdColony.configure(m_activity,"1.0", m_key,m_zoneid);
				Log.i("yoyo", "!!!!##### configured Ad Colony #####!!!!!! " + m_zoneid);
				
				if(use_v4vc)
				{
					AdColony.addV4VCListener(sThis);
					Log.i("yoyo", "!!!!##### Added Ad Colony listener#####!!!!!! ");
				}
				m_setupcomplete = true;
    		 }
    	});
		
	}

	@Override
	public boolean engagement_available()
	{
		if(m_setupcomplete)
		{
			AdColonyVideoAd Ad = new AdColonyVideoAd(m_zoneid);
			
			if(Ad!=null)
			{
				boolean avail = Ad.getV4VCAvailable();
	
				if(!use_v4vc)
					avail = true;
				
				if(avail )
				{
					if(Ad.isReady())
					{
						return true;
					}
					else
					{
						return false;
					}
				}
			}
		}
	
		return false;
	}	

	@Override
	public void engagement_launch()
	{
		if(m_setupcomplete)
		{
			
			AdColonyVideoAd Ad = new AdColonyVideoAd(m_zoneid);
			if(Ad!=null)
			{
				if(!use_v4vc)
				{
					Ad.show(null);
				}
				else
				{
					Ad.showV4VC( null,true );
				}
			}
		}
	}	


	@Override
	public void enable(int _x,int _y,int _num)
	{
	//	Log.i("yoyo","Enable called "
	//	new AdColonyVideoAd().show( null );
	}


	@Override
	public void onPause()
	{
	Log.i("yoyo","Ad colony on pause ");
		AdColony.pause();
	}
	
	@Override
	public void onResume()
	{
	Log.i("yoyo","Ad colony on resume ");
		AdColony.resume(m_activity);
	}
   

    @Override
	public void define( int _index, String _key, AdTypes _type )
	{
		super.define( _index, _key, _type );
		// create the view
		if(_index == 0)
		{
			m_key = _key;
		
		}
		else if(_index ==1)
		{
			m_zoneid = _key;
		}

	} // end define


}