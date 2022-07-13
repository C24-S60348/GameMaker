package ${YYAndroidPackageName};

import android.os.Bundle;
import android.os.RemoteException;
import ${YYAndroidPackageName}.BillingRequest;
import ${YYAndroidPackageName}.RunnerBillingConsts;
import com.android.vending.billing.IMarketBillingService;

/**
 * Wrapper class that confirms a list of notifications to the server.
 */
class ConfirmNotifications extends BillingRequest 
{
    final String[] mNotifyIds;

    public ConfirmNotifications(int startId, String[] notifyIds) 
    {
        super(startId);
        mNotifyIds = notifyIds;
    }

    @Override
    protected long run(IMarketBillingService marketBillingService) throws RemoteException 
    {
        Bundle request = makeRequestBundle("CONFIRM_NOTIFICATIONS");
        request.putStringArray(RunnerBillingConsts.BILLING_REQUEST_NOTIFY_IDS, mNotifyIds);
        Bundle response = marketBillingService.sendBillingRequest(request);
        logResponseCode("confirmNotifications", response);
        return response.getLong(RunnerBillingConsts.BILLING_RESPONSE_REQUEST_ID, RunnerBillingConsts.BILLING_RESPONSE_INVALID_REQUEST_ID);
    }

	@Override
	public void onRemoteException(RemoteException e) 
	{
	}
}
