package ${YYAndroidPackageName};

import com.pocketchange.android.PocketChange;
import com.pocketchange.android.R;
import com.pocketchange.android.util.CollectionUtils;
import android.app.Activity;
import android.view.ViewGroup;
import android.view.Display;
import android.widget.FrameLayout;
import ${YYAndroidPackageName}.RunnerActivity;
import android.util.Log;
import android.content.Intent;
import com.yoyogames.runner.RunnerJNILib;

public class AdvertisingPocketChange 
{
	

	public AdvertisingPocketChange( Activity _activity,String id,boolean UseTest) {
			
	//	Log.i("yoyo", "XXXXXX Initialising Pocketchange XXXXX ");
	
		PocketChange.initialize(_activity, id,UseTest);
	} // end AdvertisingPocketChange


	public void displayshop()
	{
	//	Log.i("yoyo", "XXXXXX About to call openShop   XXXXXX ");
	
		PocketChange.openShop();
	}
	
	public void display()
	{
		Intent intent = PocketChange.getDisplayRewardIntent();
	
	//	Log.i("yoyo", "XXXXXX About to call getDisplayRewardIntent XXXXXX ");
		
		
		 
		if(intent!=null)
		{
		 intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_SINGLE_TOP |
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            );
			if (RunnerJNILib.ms_context != null) {	
				RunnerJNILib.ms_context.startActivity(intent);
			}
		}
	//	else
	//		Log.i("yoyo", "No PocketChange intent found! ");
	
	}

}