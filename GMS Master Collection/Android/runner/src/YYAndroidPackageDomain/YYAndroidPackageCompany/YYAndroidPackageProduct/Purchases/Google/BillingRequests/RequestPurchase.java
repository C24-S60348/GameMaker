package ${YYAndroidPackageName};

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import ${YYAndroidPackageName}.BillingRequest;
import ${YYAndroidPackageName}.RunnerBillingConsts;
import ${YYAndroidPackageName}.RunnerBillingConsts.ResponseCode;
import ${YYAndroidPackageName}.RunnerBilling;
import com.android.vending.billing.IMarketBillingService;

/**
 * Wrapper class that requests a purchase.
 */
class RequestPurchase extends BillingRequest 
{
    public final String mProductId;
    public final String mDeveloperPayload;

    public RequestPurchase(String itemId) 
    {		
        this(itemId, null);
    }

    public RequestPurchase(String itemId, String developerPayload) 
    {
        // This object is never created as a side effect of starting this
        // service so we pass -1 as the startId to indicate that we should
        // not stop this service after executing this request.
        super(-1);
        mProductId = itemId;
        mDeveloperPayload = developerPayload;
    }

    @Override
    protected long run(IMarketBillingService marketBillingService) throws RemoteException 
    {
        Bundle request = makeRequestBundle("REQUEST_PURCHASE");
        request.putString(RunnerBillingConsts.BILLING_REQUEST_ITEM_ID, mProductId);
        // Note that the developer payload is optional.
        if (mDeveloperPayload != null) 
        {
            request.putString(RunnerBillingConsts.BILLING_REQUEST_DEVELOPER_PAYLOAD, mDeveloperPayload);
        }
        Bundle response = marketBillingService.sendBillingRequest(request);
        PendingIntent pendingIntent = response.getParcelable(RunnerBillingConsts.BILLING_RESPONSE_PURCHASE_INTENT);
        if (pendingIntent == null) 
        {
            Log.e("yoyo", "Error with requestPurchase");
            return RunnerBillingConsts.BILLING_RESPONSE_INVALID_REQUEST_ID;
        }

        Intent intent = new Intent();
        msRunnerBilling.buyPageIntentResponse(pendingIntent, intent);
        return response.getLong(RunnerBillingConsts.BILLING_RESPONSE_REQUEST_ID, RunnerBillingConsts.BILLING_RESPONSE_INVALID_REQUEST_ID);
    }

    @Override
    protected void responseCodeReceived(ResponseCode responseCode) 
    {
		Log.i("yoyo", "RequestPurchase responseCodeReceived()");
        msRunnerBilling.responseCodeReceived(this, responseCode);
    }

	@Override
	public void onRemoteException(RemoteException e) 
	{
	}
}