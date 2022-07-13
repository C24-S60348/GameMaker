package ${YYAndroidPackageName};

import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import ${YYAndroidPackageName}.RunnerActivity;
import ${YYAndroidPackageName}.RunnerBillingConsts;
import ${YYAndroidPackageName}.RunnerBillingConsts.ResponseCode;
import com.android.vending.billing.IMarketBillingService;

/**
* The base class for all requests that use the MarketBillingService.
* Each derived class overrides the run() method to call the appropriate
* service interface.  If we are already connected to the MarketBillingService,
* then we call the run() method directly. Otherwise, we bind
* to the service and save the request on a queue to be run later when
* the service is connected.
*/
abstract class BillingRequest 
{	
	protected static RunnerBilling msRunnerBilling;
	public static void RegisterRunnerBilling(RunnerBilling runnerBilling)
	{
		msRunnerBilling = runnerBilling;
	}


    private final int mStartId;		
    protected long mRequestId;	

    public BillingRequest(int startId) 
    {		
        mStartId = startId;		
    }

    public int getStartId() 
    {
        return mStartId;
    }

    abstract protected long run(IMarketBillingService marketBillingService) throws RemoteException;
	abstract public void onRemoteException(RemoteException e);	

    /**
     * This is called when Android Market sends a response code for this
     * request.
     * @param responseCode the response code
     */
    protected void responseCodeReceived(ResponseCode responseCode) 
    {
    }

    protected Bundle makeRequestBundle(String method) 
    {		
		/*
		// For debugging purposes make use the appID that's sent on from the INI file
		String packageName = RunnerActivity.AppID;
		if (packageName == null) {
			packageName = getPackageName();
		}
		*/
        Bundle request = new Bundle();
        request.putString(RunnerBillingConsts.BILLING_REQUEST_METHOD, method);
        request.putInt(RunnerBillingConsts.BILLING_REQUEST_API_VERSION, 1);
        request.putString(RunnerBillingConsts.BILLING_REQUEST_PACKAGE_NAME, RunnerActivity.CurrentActivity.getPackageName());
        return request;
    }

    protected void logResponseCode(String method, Bundle response) 
    {
        ResponseCode responseCode = ResponseCode.valueOf(response.getInt(RunnerBillingConsts.BILLING_RESPONSE_RESPONSE_CODE));
		Log.i("yoyo", "BILLING: " + method + " received " + responseCode.toString());
    }
}
