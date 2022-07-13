package ${YYAndroidPackageName};

import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import ${YYAndroidPackageName}.RequestPurchase;
import ${YYAndroidPackageName}.RestoreTransactions;
import ${YYAndroidPackageName}.RunnerBillingConsts.PurchaseState;
import ${YYAndroidPackageName}.RunnerBillingConsts.ResponseCode;

import com.android.vending.billing.IMarketBillingService;

//----------------------------------------------------------------------------------------------------

public class RunnerBilling extends IRunnerBilling
{		
	/**
     * This is a static instance of {@link PurchaseObserver}. 
	 * The PurchaseObserver is used for updating the UI if the UI is visible.
     */
    private RunnerBillingPurchaseObserver mPurchaseObserver = null;
    private RunnerBillingService mBillingService = null;    
    private Handler mBillingHandler;    
        

	// The developer payload that is sent with subsequent purchase requests.
    private String mBillingPayloadContents = null;

	/*
	 * Constructor
	 */
	public RunnerBilling(Context context)
	{
	    // Create the billing handler
    	mBillingHandler = new Handler();

		// Create the static purchase observer
        mPurchaseObserver = new RunnerBillingPurchaseObserver(RunnerActivity.CurrentActivity, mBillingHandler, this);
        
    	// And the billing service (NB: This can't happen on the UI thread)
	    mBillingService = new RunnerBillingService();
        mBillingService.setContext(context);

		// Propogate this object to the classes needing to know about it
		BillingRequest.RegisterRunnerBilling(this);
		RunnerBillingService.RegisterRunnerBilling(this);
	}

	/*
	 * Called when onDestroy() is hit for the main Activity
	 */
	public void Destroy()
	{		
	    mBillingService.unbind();	    
	}	

	/*
	 * Called whilst enabling the store at the IRunnerBilling level
	 */
	public void loadStore()
	{ 			       		       		
		// Check if billing is supported, the callback from this executes the available purchases query
		if (!mBillingService.checkBillingSupported()) 
		{
		    RunnerActivity.CurrentActivity.showDialog(RunnerActivity.DIALOG_CANNOT_CONNECT_ID);
		    setBillingServiceStatus(eStoreUnavailable);
		}
    }

	/**
     * Called by the Billing Service when we have a remote Market status available
     */
    public void setBillingAvailable(boolean billingAvailable)
    {    
		Log.i("yoyo", "BILLING: setBillingAvailable(" + billingAvailable + ")");
    	if (!billingAvailable) 					
		{
			setBillingServiceStatus(eStoreUnavailable);
		}
		else
    	{		    		
			// Run the query to get the list of available purchases
			try
			{				
				if ((PurchasesBaseURL != null) && (PurchasesProductID != null)) {									
					Log.i("yoyo", "BILLING: Trying to obtain list of available purchases from Developer Server " + PurchasesBaseURL + " for product " + PurchasesProductID);
		    		mAvailablePurchases.execute(PurchasesBaseURL + "/products/purchases?product=" + PurchasesProductID);
				}								
				else if (mPurchaseCatalog == null) {
					// User has not supplied a purchase catalog via GML during store activation
					Log.i("yoyo", "BILLING: Store is not available, no purchase catalog supplied");
					setBillingServiceStatus(eStoreUnavailable);
				}
				else {
					Log.i("yoyo", "BILLING: Store is available!");
					setBillingServiceStatus(eStoreAvailable);
				}
		    }
		    catch (Exception e)
		    {				
		    	e.printStackTrace();
				setBillingServiceStatus(eStoreUnavailable);
		    }
		}
	}	
    
    /**
     * Called from the VC_Runner when the current application decides it wants to purchase an item
     */
    public void purchaseCatalogItem(int purchaseIndex) 
    {
    	if (mPurchaseCatalog != null)
    	{
    		if (mBillingServiceStatus == eStoreAvailable)
    		{
    			CatalogEntry[] availablePurchases = mPurchaseCatalog;    		
			
				if ((purchaseIndex >= 0) && (purchaseIndex < availablePurchases.length))
				{					
					setBillingServiceStatus(eStoreProcessingOrder);
					
        			if (!mBillingService.requestPurchase(availablePurchases[purchaseIndex].purchaseID, mBillingPayloadContents)) {
        				// We can't create the dialog on the GL thread that this is called from
        				Log.i("yoyo", "BILLING: Billing failed!");
       				}
       				
       				setBillingServiceStatus(eStoreAvailable);
       			}
       			else {
       				Log.i("yoyo","BILLING: Invalid purchaseIndex passed in " + purchaseIndex + " of " +availablePurchases.length);
       			}
       			
       		}
       		else {
	       		Log.i("yoyo", "BILLING: General billing error!");
       		}
       	}
       	else {
       		Log.i("yoyo", "BILLING: Billing is not supported!");
       	}
    }

	/**
	  * A successfully completed response has been received from the Android Market
	  */
	public void purchaseSucceeded(String purchaseId)
	{		
		registerContentPurchased(purchaseId, true);
		if (mPurchaseCatalog == null)
		{
			// User hasn't provided a local catalog or we're still waiting for a response 
			// for the set of available purchases from the proprietary server
			deferContentDownload(purchaseId);
		}
		else
    	{
			for (int n = 0; n < mPurchaseCatalog.length; n++)   			
   			{
   				if (mPurchaseCatalog[n].purchaseID.equals(purchaseId))
   				{					
					downloadPurchaseContent(n);
					break;
				}
			}
		}
	}        	   
    
    /**
     * Gets a consistent key for the content ID when checking/registering purchased state
     */
    public String getContentPurchasedKey(String contentId)
    {
    	return md5encode("yoyo_purchase_" + contentId + "_punky_juular");
    }

	/**
     * Gets a consistent key for the content ID when checking/registering purchased state
     */
    public String getContentDownloadedKey(String contentId)
    {
    	return md5encode("yoyo_purchase_" + contentId + "_ziltoid_pandemic");
    }
    
    /**
     * Gets the filename used for the file containing the files downloaded for each purchase
     */
    public String getDownloadedPurchasesFileName()
    {
    	return md5encode("purchases_files_hysteria") + ".json";
    }    
    
    /**
     * Called from the VC_Runner when the current game decides it wants all previously purchased items restored
     */
    public void restorePurchasedItems()
    {				
    	mBillingService.restoreTransactions();
    }        

    /**
     * Notifies the application of the availability of the MarketBillingService.
     * This method is called in response to the application calling
     * {@link BillingService#checkBillingSupported()}.
     * @param supported true if in-app billing is supported.
     */
    public void checkBillingSupportedResponse(boolean supported) 
    {
		Log.i("yoyo", "BILLING: checkBillingSupportedResponse()");        
        mPurchaseObserver.onBillingSupported(supported);        
    }

    /**
     * Starts a new activity for the user to buy an item for sale. This method
     * forwards the intent on to the PurchaseObserver (if it exists) because
     * we need to start the activity on the activity stack of the application.
     *
     * @param pendingIntent a PendingIntent that we received from Android Market that
     *     will create the new buy page activity
     * @param intent an intent containing a request id in an extra field that
     *     will be passed to the buy page activity when it is created
     */
    public void buyPageIntentResponse(PendingIntent pendingIntent, Intent intent) 
    {
        mPurchaseObserver.startBuyPageActivity(pendingIntent, intent);
    }

    /**
     * Notifies the application of purchase state changes. The application
     * can offer an item for sale to the user via
     * {@link BillingService#requestPurchase(String)}. The BillingService
     * calls this method after it gets the response. Another way this method
     * can be called is if the user bought something on another device running
     * this same app. Then Android Market notifies the other devices that
     * the user has purchased an item, in which case the BillingService will
     * also call this method. Finally, this method can be called if the item
     * was refunded.
     * @param purchaseState the state of the purchase request (PURCHASED,
     *     CANCELED, or REFUNDED)
     * @param productId a string identifying a product for sale
     * @param orderId a string identifying the order
     * @param purchaseTime the time the product was purchased, in milliseconds
     *     since the epoch (Jan 1, 1970)
     * @param developerPayload the developer provided "payload" associated with
     *     the order
     */
    public void purchaseResponse(
            final Context context, 
            final PurchaseState purchaseState, 
            final String productId,
            final String orderId, 
            final long purchaseTime, 
            final String developerPayload) 
    {                		
        new Thread(new Runnable() 
        {
            public void run() 
            {				
                mPurchaseObserver.postPurchaseStateChange(purchaseState, productId, 1, purchaseTime, developerPayload);                
            }
        }).start();
    }

    /**
     * This is called when we receive a response code from Android Market for a
     * RequestPurchase request that we made.  This is used for reporting various
     * errors and also for acknowledging that an order was sent successfully to
     * the server. This is NOT used for any purchase state changes. All
     * purchase state changes are received in the {@link BillingReceiver} and
     * are handled in {@link Security#verifyPurchase(String, String)}.     
     * @param request the RequestPurchase request for which we received a
     *     response code
     * @param responseCode a response code from Market to indicate the state
     * of the request
     */
    public void responseCodeReceived(RequestPurchase request, ResponseCode responseCode) 
    {        
        mPurchaseObserver.onRequestPurchaseResponse(request, responseCode);        			        
    }

    /**
     * This is called when we receive a response code from Android Market for a
     * RestoreTransactions request.     
     * @param request the RestoreTransactions request for which we received a
     *     response code
     * @param responseCode a response code from Market to indicate the state
     *     of the request
     */
    public void responseCodeReceived(RestoreTransactions request, ResponseCode responseCode) 
    {        
		mPurchaseObserver.onRestoreTransactionsResponse(request, responseCode);
    }
}