package ${YYAndroidPackageName};

import android.os.Bundle;
import android.os.RemoteException;
import ${YYAndroidPackageName}.BillingRequest;
import ${YYAndroidPackageName}.RunnerBillingConsts.ResponseCode;
import ${YYAndroidPackageName}.RunnerBilling;
import com.android.vending.billing.IMarketBillingService;

/**
 * Wrapper class that sends a RESTORE_TRANSACTIONS message to the server.
 */
class RestoreTransactions extends BillingRequest 
{
	long mNonce;	

    public RestoreTransactions() 
    {
        // This object is never created as a side effect of starting this
        // service so we pass -1 as the startId to indicate that we should
        // not stop this service after executing this request.
        super(-1);
    }

    @Override
    protected long run(IMarketBillingService marketBillingService) throws RemoteException 
    {		
        mNonce = RunnerBillingSecurity.generateNonce();

        Bundle request = makeRequestBundle("RESTORE_TRANSACTIONS");
        request.putLong(RunnerBillingConsts.BILLING_REQUEST_NONCE, mNonce);
        Bundle response = marketBillingService.sendBillingRequest(request);
        logResponseCode("restoreTransactions", response);

        return response.getLong(RunnerBillingConsts.BILLING_RESPONSE_REQUEST_ID, RunnerBillingConsts.BILLING_RESPONSE_INVALID_REQUEST_ID);
    }

    @Override
    public void onRemoteException(RemoteException e) 
    {        
        RunnerBillingSecurity.removeNonce(mNonce);
    }

    @Override
    public void responseCodeReceived(ResponseCode responseCode) 
    {		
        msRunnerBilling.responseCodeReceived(this, responseCode);
    }
}