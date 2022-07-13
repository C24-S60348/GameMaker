package ${YYAndroidPackageName};

import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import ${YYAndroidPackageName}.RunnerBillingConsts.PurchaseState;
import ${YYAndroidPackageName}.RunnerBillingConsts.ResponseCode;
import ${YYAndroidPackageName}.RunnerBillingSecurity.VerifiedPurchase;
import ${YYAndroidPackageName}.BillingRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import com.android.vending.billing.IMarketBillingService;

//----------------------------------------------------------------------------------------------------

public class RunnerBillingService extends Service implements ServiceConnection 
{
    /** The service connection to the remote MarketBillingService. */
    private static IMarketBillingService mMarketBillingService = null;

    /**
     * The list of requests that are pending while we are waiting for the
     * connection to the MarketBillingService to be established.
     */
    private static LinkedList<BillingRequest> mPendingRequests = new LinkedList<BillingRequest>();

    /**
     * The list of requests that we have sent to Android Market but for which we have
     * not yet received a response code. The HashMap is indexed by the
     * request Id that each request receives when it executes.
     */
    private static HashMap<Long, BillingRequest> mSentRequests = new HashMap<Long, BillingRequest>();

	private static RunnerBilling msRunnerBilling;
	public static void RegisterRunnerBilling(RunnerBilling runnerBilling)
	{
		msRunnerBilling = runnerBilling;
	}

    public RunnerBillingService() 
    {		
		super();
    }	

    public void setContext(Context context) 
    {
        attachBaseContext(context);
    }

    /**
     * We don't support binding to this service, only starting the service.
     */
    @Override
    public IBinder onBind(Intent intent) 
    {
        return null;
    }

    @Override
    public void onStart(Intent intent, int startId) 
    {
        handleCommand(intent, startId);
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
    	Log.i("yoyo", "BILLING: onStartCommand()");
    	handleCommand(intent, startId);
    	return START_NOT_STICKY;
    }

    /**
     * The {@link BillingReceiver} sends messages to this service using intents.
     * Each intent has an action and some extra arguments specific to that action.
     * @param intent the intent containing one of the supported actions
     * @param startId an identifier for the invocation instance of this service
     */
    public void handleCommand(Intent intent, int startId) {
    
    	try {
	        String action = intent.getAction();
    	    Log.i("yoyo", "BILLING: handleCommand() action: " + action);
        
	        if (RunnerBillingConsts.ACTION_CONFIRM_NOTIFICATION.equals(action)) 
    	    {
        	    String[] notifyIds = intent.getStringArrayExtra(RunnerBillingConsts.NOTIFICATION_ID);
	            confirmNotifications(startId, notifyIds);
    	    } 
	        else if (RunnerBillingConsts.ACTION_GET_PURCHASE_INFORMATION.equals(action)) 
    	    {
	            String notifyId = intent.getStringExtra(RunnerBillingConsts.NOTIFICATION_ID);
    	        getPurchaseInformation(startId, new String[] { notifyId });
	        } 
    	    else if (RunnerBillingConsts.ACTION_PURCHASE_STATE_CHANGED.equals(action)) 
	        {
    	        String signedData = intent.getStringExtra(RunnerBillingConsts.INAPP_SIGNED_DATA);
        	    String signature = intent.getStringExtra(RunnerBillingConsts.INAPP_SIGNATURE);
	            purchaseStateChanged(startId, signedData, signature);
    	    } 
	        else if (RunnerBillingConsts.ACTION_RESPONSE_CODE.equals(action)) 
    	    {
        	    long requestId = intent.getLongExtra(RunnerBillingConsts.INAPP_REQUEST_ID, -1);
	            int responseCodeIndex = intent.getIntExtra(
    	        	RunnerBillingConsts.INAPP_RESPONSE_CODE, ResponseCode.RESULT_ERROR.ordinal());
        	    ResponseCode responseCode = ResponseCode.valueOf(responseCodeIndex);
	            checkResponseCode(requestId, responseCode);
    	    }
    	}
    	catch (Exception e) {
	    	e.printStackTrace();
    	}
    }

    /**
     * Binds to the MarketBillingService and returns true if the bind
     * succeeded.
     * @return true if the bind succeeded; false otherwise
     */
    public boolean bindToMarketBillingService() 
    {
        try {        
            Log.i("yoyo", "BILLING: Binding to Market Billing Service");            
            boolean bindResult = bindService(new Intent(RunnerBillingConsts.MARKET_BILLING_SERVICE_ACTION), this, Context.BIND_AUTO_CREATE);

            if (bindResult) {
                return true;
            } 
            else {
                Log.i("yoyo", "BILLING: Could not bind to service");
            }
        } 
        catch (SecurityException e) {
            Log.i("yoyo", "BILLING: Security exception: " + e);
        }
        catch (Exception e) {
        	Log.i("yoyo", "BILLING: Exception: " + e);
        }
        return false;
    }
    
    /**
     * This is called when we are connected to the MarketBillingService.
     * This runs in the main UI thread.
     */
    public void onServiceConnected(ComponentName name, IBinder service) {        

        Log.i("yoyo", "BILLING: Service connected");       
        mMarketBillingService = IMarketBillingService.Stub.asInterface(service);
        runPendingRequests();
    }

    /**
     * This is called when we are disconnected from the MarketBillingService.
     */
    public void onServiceDisconnected(ComponentName name) {

        Log.i("yoyo", "BILLING: Service disconnected");
        mMarketBillingService = null;
    }

	/**
     * Run the request, starting the connection if necessary.
     * @return true if the request was executed or queued; false if there
     * was an error starting the connection
     */
    private boolean runBillingRequest(BillingRequest request) 
    {
        if (runRequestIfConnected(request)) 
        {
            return true;
        }

        if (bindToMarketBillingService()) 
        {	                           
			// Between the time taken to bind the the billing service and returning to this point
			// in execution we may have already managed to bind to the service
            if (!runRequestIfConnected(request)) 
            {
                // Add a pending request to run when the service is connected
                Log.i("yoyo", "BILLING: Adding request to pending queue..."); 
				mPendingRequests.add(request);                
            }
            return true;
        }
        return false;
    }

	/**
     * Called when a remote exception occurs while trying to execute the
     * {@link #run()} method.  The derived class can override this to
     * execute exception-handling code.
     * @param e the exception
     */
    protected void onRemoteException(RemoteException e, BillingRequest request) 
    {
		Log.i("yoyo", "BILLING: Remote billing service crashed");
		request.onRemoteException(e);
        mMarketBillingService = null;
    }

    /**
     * Try running the request directly if the service is already connected.
     * @return true if the request ran successfully; false if the service
     * is not connected or there was an error when trying to use it
     */
    private boolean runRequestIfConnected(BillingRequest request) 
    {        
        if (mMarketBillingService != null)
        {
            try 
            {
                long requestId = request.run(mMarketBillingService);
                if (requestId >= 0) {
                    mSentRequests.put(requestId, request);
                }
				Log.i("yoyo", "BILLING: Request id: " + requestId);
                return true;
            } 
            catch (RemoteException e) {
                onRemoteException(e, request);
            }
        }
        return false;
    }

    /**
     * Checks if in-app billing is supported.
     * @return true if supported; false otherwise
     */
    public boolean checkBillingSupported() 
    {
		Log.i("yoyo", "BILLING: Checking billing supported");
		return runBillingRequest(new CheckBillingSupported());
    }

    /**
     * Requests that the given item be offered to the user for purchase. When
     * the purchase succeeds (or is canceled) the {@link BillingReceiver}
     * receives an intent with the action {@link Consts#ACTION_NOTIFY}.
     * Returns false if there was an error trying to connect to Android Market.
     * @param productId an identifier for the item being offered for purchase
     * @param developerPayload a payload that is associated with a given
     * purchase, if null, no payload is sent
     * @return false if there was an error connecting to Android Market
     */
    public boolean requestPurchase(String productId, String developerPayload) 
    {
    	Log.i("yoyo", "BILLING: Requesting " + productId + " for purchase");
		return runBillingRequest(new RequestPurchase(productId, developerPayload));
    }

    /**
     * Requests transaction information for all managed items. Call this only when the
     * application is first installed or after a database wipe. Do NOT call this
     * every time the application starts up.
     * @return false if there was an error connecting to Android Market
     */
    public boolean restoreTransactions() 
    {
		Log.i("yoyo", "BILLING: Restoring transactions");
		return runBillingRequest(new RestoreTransactions());        
    }

    /**
     * Confirms receipt of a purchase state change. Each {@code notifyId} is
     * an opaque identifier that came from the server. This method sends those
     * identifiers back to the MarketBillingService, which ACKs them to the
     * server. Returns false if there was an error trying to connect to the
     * MarketBillingService.
     * @param startId an identifier for the invocation instance of this service
     * @param notifyIds a list of opaque identifiers associated with purchase
     * state changes.
     * @return false if there was an error connecting to Market
     */
    private boolean confirmNotifications(int startId, String[] notifyIds) 
    {        
		Log.i("yoyo", "BILLING: Confirm notifications");
		return runBillingRequest(new ConfirmNotifications(startId, notifyIds));
    }

    /**
     * Gets the purchase information. This message includes a list of
     * notification IDs sent to us by Android Market, which we include in
     * our request. The server responds with the purchase information,
     * encoded as a JSON string, and sends that to the {@link BillingReceiver}
     * in an intent with the action {@link Consts#ACTION_PURCHASE_STATE_CHANGED}.
     * Returns false if there was an error trying to connect to the MarketBillingService.
     *
     * @param startId an identifier for the invocation instance of this service
     * @param notifyIds a list of opaque identifiers associated with purchase
     * state changes
     * @return false if there was an error connecting to Android Market
     */
    private boolean getPurchaseInformation(int startId, String[] notifyIds) 
    {
		Log.i("yoyo", "BILLING: Getting purchase information");
		return runBillingRequest(new GetPurchaseInformation(startId, notifyIds));
    }

    /**
     * Verifies that the data was signed with the given signature, and calls
     * {@link ResponseHandler#purchaseResponse(Context, PurchaseState, String, String, long)}
     * for each verified purchase.
     * @param startId an identifier for the invocation instance of this service
     * @param signedData the signed JSON string (signed, not encrypted)
     * @param signature the signature for the data, signed with the private key
     */
    private void purchaseStateChanged(int startId, String signedData, String signature) 
    {
        ArrayList<RunnerBillingSecurity.VerifiedPurchase> purchases;
        purchases = RunnerBillingSecurity.verifyPurchase(signedData, signature);
        if (purchases == null) {			
            return;
        }

        ArrayList<String> notifyList = new ArrayList<String>();
        for (VerifiedPurchase vp : purchases) 
        {
            if (vp.notificationId != null) {
                notifyList.add(vp.notificationId);
            }
			Log.i("yoyo", "BILLING: Verified purchase, purchaseResponse() ");
            msRunnerBilling.purchaseResponse(
            	this, 
            	vp.purchaseState, 
            	vp.productId,
                vp.orderId, 
                vp.purchaseTime, 
                vp.developerPayload);
        }
        if (!notifyList.isEmpty()) {
            String[] notifyIds = notifyList.toArray(new String[notifyList.size()]);
            confirmNotifications(startId, notifyIds);
        }
    }

    /**
     * This is called when we receive a response code from Android Market for a request
     * that we made. This is used for reporting various errors and for
     * acknowledging that an order was sent to the server. This is NOT used
     * for any purchase state changes.  All purchase state changes are received
     * in the {@link BillingReceiver} and passed to this service, where they are
     * handled in {@link #purchaseStateChanged(int, String, String)}.
     * @param requestId a number that identifies a request, assigned at the
     * time the request was made to Android Market
     * @param responseCode a response code from Android Market to indicate the state
     * of the request
     */
    private void checkResponseCode(long requestId, ResponseCode responseCode) {
    
        BillingRequest request = mSentRequests.get(requestId);
        if (request != null) {

            Log.i("yoyo", "BILLING: Response code checked for class " + request.getClass().getSimpleName() + ": " + responseCode);
            request.responseCodeReceived(responseCode);
        }
        mSentRequests.remove(requestId);
    }

    /**
     * Runs any pending requests that are waiting for a connection to the
     * service to be established.  This runs in the main UI thread.
     */
    private void runPendingRequests() {
    
    	Log.i("yoyo", "BILLING: runPendingRequests()");

        int maxStartId = -1;
        BillingRequest request;
        while ((request = mPendingRequests.peek()) != null) {

            if (runRequestIfConnected(request)) {
				
                // Remove the request
                mPendingRequests.remove();

                // Remember the largest startId, which is the most recent
                // request to start this service.
                if (maxStartId < request.getStartId()) {
                    maxStartId = request.getStartId();
                }
            } 
			else {
                // The service crashed, so restart it. Note that this leaves
                // the current request on the queue.
                bindToMarketBillingService();
                return;
            }
        }

        // If we get here then all the requests ran successfully.  If maxStartId
        // is not -1, then one of the requests started the service, so we can
        // stop it now.
        if (maxStartId >= 0) {

			Log.i("yoyo", "BILLING: Stopping service, startId: " + maxStartId);
            stopSelf(maxStartId);
        }
    }


    /**
     * Unbinds from the MarketBillingService. Call this when the application
     * terminates to avoid leaking a ServiceConnection.
     */
    public void unbind() {

        try {
            unbindService(this);
        } 
        catch (IllegalArgumentException e) {
            // This might happen if the service was disconnected
        }
    }
}