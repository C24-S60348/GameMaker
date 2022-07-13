package ${YYAndroidPackageName};

import android.os.Bundle;
import android.os.RemoteException;
import ${YYAndroidPackageName}.BillingRequest;
import ${YYAndroidPackageName}.RunnerBillingConsts;
import ${YYAndroidPackageName}.RunnerBillingSecurity;
import com.android.vending.billing.IMarketBillingService;

/**
 * Wrapper class that sends a GET_PURCHASE_INFORMATION message to the server.
 */
class GetPurchaseInformation extends BillingRequest 
{
    long mNonce;
    final String[] mNotifyIds;

    public GetPurchaseInformation(int startId, String[] notifyIds) 
    {
        super(startId);
        mNotifyIds = notifyIds;
    }

    @Override
    protected long run(IMarketBillingService marketBillingService) throws RemoteException 
    {
        mNonce = RunnerBillingSecurity.generateNonce();

        Bundle request = makeRequestBundle("GET_PURCHASE_INFORMATION");
        request.putLong(RunnerBillingConsts.BILLING_REQUEST_NONCE, mNonce);
        request.putStringArray(RunnerBillingConsts.BILLING_REQUEST_NOTIFY_IDS, mNotifyIds);
        Bundle response = marketBillingService.sendBillingRequest(request);
        logResponseCode("getPurchaseInformation", response);
        return response.getLong(RunnerBillingConsts.BILLING_RESPONSE_REQUEST_ID, RunnerBillingConsts.BILLING_RESPONSE_INVALID_REQUEST_ID);
    }

    @Override
    public void onRemoteException(RemoteException e) 
    {        
        RunnerBillingSecurity.removeNonce(mNonce);
    }
}