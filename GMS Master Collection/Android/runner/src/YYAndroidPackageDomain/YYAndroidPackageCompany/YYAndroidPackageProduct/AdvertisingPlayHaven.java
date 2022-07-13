package ${YYAndroidPackageName};


//import com.playhaven.*;
//import com.playhaven.src.common.PHConfig;
import com.playhaven.src.common.*;
import com.playhaven.src.publishersdk.content.*;
import com.playhaven.src.publishersdk.*;
import com.playhaven.src.publishersdk.open.*;
import com.playhaven.src.publishersdk.metadata.*;
import com.playhaven.src.publishersdk.content.adapters.*;
import android.app.Activity;
import android.view.ViewGroup;
import android.view.Display;
import android.view.View;
import android.view.ViewParent;
import android.widget.FrameLayout;
import ${YYAndroidPackageName}.RunnerActivity;
import android.util.Log;
import android.content.Intent;
import com.yoyogames.runner.RunnerJNILib;
import android.provider.Settings.Secure;
import android.content.Context;
import org.json.JSONObject;
import com.yoyogames.runner.RunnerJNILib;
import android.widget.AbsoluteLayout;

public class AdvertisingPlayHaven  extends AdvertisingBase implements PHPublisherContentRequest.FailureDelegate,PHPublisherContentRequest.ContentDelegate,
																	PHPublisherContentRequest.RewardDelegate,PHPublisherContentRequest.PurchaseDelegate
{
	
	PHNotificationView notifyView=null;
	int reward_callback=-1;
	
	
	public AdvertisingPlayHaven( Activity _activity, ViewGroup _viewGroup,boolean _usetestads) {
		super( _activity, _viewGroup,_usetestads );
			
		//PHConfig.token = "14448708a4084a9bb98310468ce91b39";
		//PHConfig.secret = "88883ef230ab433aa7adb4e1996276fb";
			
		String android_id = Secure.getString(RunnerJNILib.ms_context.getContentResolver(),
                                                        Secure.ANDROID_ID); 
		Log.i("yoyo","***********************************************************");	
		Log.i("yoyo","creating playhaven");	
		Log.i("yoyo","creating playhaven for device "+ android_id);
		Log.i("yoyo","!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");		
			
	
	} // end AdvertisingPocketChange

	@Override
	public void define( int _index, String _key, AdTypes _type )
	{
		//Log.i("yoyo","defining unit "+_index);
	
		super.define( _index, _key, _type );
		
		if(_index==0)
		{
			PHConfig.token = _key;
		}
			
		if(_index==1)
		{
			PHConfig.secret = _key;
			
			PHPublisherOpenRequest request = new PHPublisherOpenRequest(m_activity);
			
			request.send();
			
		}
	}
	
	
	
	@Override
	public void enable( int _x, int _y, int _index)
	{
	
	}
	
	@Override
	public void enable_interstitial( int _index )
	{
	}
	
	@Override
	public boolean interstitial_available()
	{
	Log.i("yoyo","checking interstitial available for playhaven");
		return true;
	}
	
	@Override
	public boolean interstitial_display()
	{
		Log.i("yoyo","sending new request for playhaven");
		PHPublisherContentRequest request = new PHPublisherContentRequest(m_activity, "level_complete");

		request.send();
		return true;
	}
	
	public void unlockedReward(PHPublisherContentRequest request, PHReward reward)
	{
		Log.i("yoyo","PlayHaven reward unlocked: " + reward.quantity + " " + reward.name); 
		if(reward_callback!=-1)
		{
			RunnerJNILib.callreward(reward_callback,reward.quantity,reward.name);
		}
	}
	
	
	public void requestSucceeded(PHAPIRequest request, JSONObject obj)
	{
		Log.i("yoyo","Playhaven request succeeded with obj:"+obj.toString());	
	}
	public void requestFailed(PHAPIRequest request, Exception e)
	{
		Log.i("yoyo","Playhaven request failed");	
	}
	
	public void didFail(PHPublisherContentRequest request, String error) {
		Log.i("yoyo","Playhaven event fail " + error);
	}
	public void contentDidFail(PHPublisherContentRequest request, Exception e) {
		Log.i("yoyo","Playhaven event fail (exception)");
	}
	
	public void willGetContent		(PHPublisherContentRequest request					  )
	{
		Log.i("yoyo","Playhaven willGetContent");	
	}
    public void willDisplayContent	(PHPublisherContentRequest request, PHContent content )
    {
		Log.i("yoyo","Playhaven willDisplayContent");	
    }
    public void didDisplayContent	(PHPublisherContentRequest request, PHContent content )
    {
		Log.i("yoyo","Playhaven didDisplayContent");	
    }
    public void didDismissContent	(PHPublisherContentRequest request, PHPublisherContentRequest.PHDismissType type)
    {
		Log.i("yoyo","Playhaven didDismissContent + type:" + type);	
    }
    
    
    public void shouldMakePurchase(PHPublisherContentRequest request, PHPurchase content)
    {
		
    
		Log.i("yoyo","Playhaven shouldMakePurchase of name:"+content.name + " product:" + content.product + " marketplace:" + content.marketplace);	
		//Need to do the equivalent of iap_acquire(content.product);
		String product = content.product;
		RunnerJNILib.CallInappPurchase(product);	
		
    }
    
    @Override
    public void pc_badge_move(int _x, int _y, int _width, int _height)
    {
    	final int x = _x;
		final int y = _y;
		final int width = _width;
		final int height = _height;
		
		RunnerActivity.ViewHandler.post( new Runnable() 
		{
    		public void run() 
    		{
		
				if(notifyView!=null)
				{
					AbsoluteLayout.LayoutParams params = new AbsoluteLayout.LayoutParams(  width, height,x,y);	
					notifyView.setLayoutParams(params);
					notifyView.requestLayout();
				}
			
			}
		}
		);
    }
    
    @Override
    public void pc_badge_add(int _x, int _y, int _width, int _height, String _ident)
    {
    
		final int x = _x;
		final int y = _y;
		final int width = _width;
		final int height = _height;
		final String ident = _ident;
		
    
    	RunnerActivity.ViewHandler.post( new Runnable() {
    		public void run() {
			notifyView = new PHNotificationView(RunnerJNILib.ms_context, ident);
		
			AbsoluteLayout.LayoutParams params = new AbsoluteLayout.LayoutParams(  width, height,x,y);	
			notifyView.setLayoutParams(params);
			
			m_viewGroup.addView(notifyView);
			notifyView.refresh();
			notifyView.requestLayout();
			
			}
			}
		);
    
    }
    @Override
    public void pc_badge_hide()
    {
		RunnerActivity.ViewHandler.post( new Runnable() {
	    		public void run() {
	    			if(notifyView!=null)
	    			{
						notifyView.setVisibility( View.GONE );  
						ViewParent vp = notifyView.getParent();
						
						if(vp!=null) 
						{ 			
							m_viewGroup.removeView(notifyView);
						}
					}
    			} // end run()
    		});
    }
    @Override
    public void pc_badge_update()
    {
		if(notifyView!=null)
		{
			notifyView.refresh();
		}
    }
    
    @Override
    public void reward_callback(int funcid)
    {
		Log.i("yoyo", "######################## setting reward callback to "+funcid);
		reward_callback=funcid;
    }
	
	
	@Override
	public void event(String _ident)
	{	
	
		Log.i("yoyo","sending new request for playhaven");
		PHPublisherContentRequest request = new PHPublisherContentRequest(m_activity, _ident);
		
		request.setOnFailureListener(this);
		request.setOnContentListener(this);
		request.setOnRewardListener(this);
		request.setOnPurchaseListener(this);
		
		request.send();
		
	}
	
	@Override
	public void event_preload(String _ident)
	{	
	
		Log.i("yoyo","preloading new request for playhaven");
		PHPublisherContentRequest request = new PHPublisherContentRequest(m_activity, _ident);
		request.preload();
		
	}

}