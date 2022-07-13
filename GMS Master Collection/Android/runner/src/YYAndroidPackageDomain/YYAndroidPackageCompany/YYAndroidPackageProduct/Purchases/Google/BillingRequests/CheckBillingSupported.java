package ${YYAndroidPackageName};

import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import ${YYAndroidPackageName}.RunnerBilling;
import ${YYAndroidPackageName}.RunnerBillingConsts;
import ${YYAndroidPackageName}.RunnerBillingConsts.ResponseCode;
import com.android.vending.billing.IMarketBillingService;

/**
 * Wrapper class that checks if in-app billing is supported.
 */
class CheckBillingSupported extends BillingRequest 
{
    public CheckBillingSupported() 
    {
        // This object is never created as a side effect of starting this
        // service so we pass -1 as the startId to indicate that we should
        // not stop this service after executing this request.
        super(-1);
    }

    @Override
    protected long run(IMarketBillingService marketBillingService) throws RemoteException 
    {
		Log.i("yoyo", "BILLING: Checking billing supported");
    
        Bundle request = makeRequestBundle("CHECK_BILLING_SUPPORTED");
        Bundle response = marketBillingService.sendBillingRequest(request);
        int responseCode = response.getInt(RunnerBillingConsts.BILLING_RESPONSE_RESPONSE_CODE);
        Log.i("yoyo", "BILLING: CheckBillingSupported response code: " + ResponseCode.valueOf(responseCode));
        
        boolean billingSupported = (responseCode == ResponseCode.RESULT_OK.ordinal());
        msRunnerBilling.checkBillingSupportedResponse(billingSupported);
        
        return RunnerBillingConsts.BILLING_RESPONSE_INVALID_REQUEST_ID;
    }

	@Override
	public void onRemoteException(RemoteException e) 
	{
	}
}
