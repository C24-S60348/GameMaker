package ${YYAndroidPackageName};

import android.util.Log;
import android.content.Context;
import android.content.SharedPreferences;

import com.amazon.inapp.purchasing.Offset;
import com.amazon.inapp.purchasing.PurchasingManager;
import com.amazon.inapp.purchasing.PurchaseResponse;
import com.amazon.inapp.purchasing.Receipt;

import java.lang.RuntimeException;
import java.util.HashMap;
import java.util.Map;

public class AmazonBilling extends IRunnerBilling
{	
	public static final String OFFSET = "amazon-offset";    

	private static AmazonBilling msInstance = null;	
	private static String msCurrentUser;

	private AmazonBillingObserver mPurchaseObserver;
	private Map<String, String> mRequestIds;

	public AmazonBilling(Context context)
	{
		if (msInstance != null) {			
			throw new RuntimeException("AmazonBilling instance already exists");
		}
		msInstance = this;

		mPurchaseObserver = new AmazonBillingObserver();		
		mRequestIds = new HashMap<String, String>();

		Log.i("yoyo", "AMAZON-BILLING: Registering observer");
		PurchasingManager.registerObserver(mPurchaseObserver);		
	}

	public void Destroy()
	{
	}

	/**
	 * Gets the static instance of this class
	 */
	public static AmazonBilling Instance()
	{
		return msInstance;
	}

	/**
     * Gets current logged in user
     * @return current user
     */
    public static String getCurrentUser(){
        return msCurrentUser;
    }
    
    /**
     * Sets current logged in user
     * @param currentUser current user to set
     */
    public static void setCurrentUser(final String currentUser) {
        msCurrentUser = currentUser;
    }

    public static SharedPreferences getSharedPreferencesForCurrentUser() {

        final SharedPreferences settings = RunnerActivity.CurrentActivity.getSharedPreferences(AmazonBilling.getCurrentUser(), Context.MODE_PRIVATE);
        return settings;
    }
    
    public static SharedPreferences.Editor getSharedPreferencesEditor(){
        return getSharedPreferencesForCurrentUser().edit();
    }

	/**
	 * Called whilst enabling the store at the IRunnerBilling level
	 */
	public void loadStore()
	{
		// Make sure we're hooked up to the correct user
		PurchasingManager.initiateGetUserIdRequest();

		if ((PurchasesBaseURL != null) && (PurchasesProductID != null)) {
			Log.i("yoyo", "AMAZON-BILLING: Trying to obtain list of available purchases from Developer Server " + PurchasesBaseURL + " for product " + PurchasesProductID);
			mAvailablePurchases.execute(PurchasesBaseURL + "/products/purchases?product=" + PurchasesProductID);
		}
		else if (mPurchaseCatalog == null) {
			// User has not supplied a purchase catalog via GML during store activation
			Log.i("yoyo", "AMAZON-BILLING: Store is not available, no purchase catalog supplied");
			setBillingServiceStatus(eStoreUnavailable);
		}
		else {
			Log.i("yoyo", "AMAZON-BILLING: Store is available!");
			setBillingServiceStatus(eStoreAvailable);
		}
	}


	/**
	 * I'm not entirely sure this is good functionality and purchase updates is handled via user ID updates
	 */
	public void restorePurchasedItems()
	{		
		PurchasingManager.initiatePurchaseUpdatesRequest(Offset
			.fromString(RunnerActivity.CurrentActivity.getApplicationContext()
				.getSharedPreferences(getCurrentUser(), Context.MODE_PRIVATE)
					.getString(OFFSET, Offset.BEGINNING.toString())));
	}	

    /**
     * Gets a consistent key for the content ID when checking/registering purchased state
     */
    public String getContentPurchasedKey(String contentId)
    {
    	return md5encode("yoyo_purchase_" + contentId + "_wrathchild_" + getCurrentUser());
    }

	/**
     * Gets a consistent key for the content ID when checking/registering purchased state
     */
    public String getContentDownloadedKey(String contentId)
    {
    	return md5encode("yoyo_purchase_" + contentId + "_paschendale_" + getCurrentUser());
    }
    
    /**
     * Gets the filename used for the file containing the files downloaded for each purchase
     */
    public String getDownloadedPurchasesFileName()
    {
    	return md5encode("purchases_files_mindcrime_" + getCurrentUser()) + ".json";
    }

	/**
	 * Actually go ahead and purchase an item
	 */
	public void purchaseCatalogItem(int purchaseIndex)
	{
		if (mPurchaseCatalog != null)
    	{
    		if (mBillingServiceStatus == eStoreAvailable)
    		{    						
				if (purchaseIndex >= 0 && purchaseIndex < mPurchaseCatalog.length) 
				{
					setBillingServiceStatus(eStoreProcessingOrder);

					Log.i("yoyo", "AMAZON-BILLING: Initiating purchase request");
					String requestId = PurchasingManager.initiatePurchaseRequest(mPurchaseCatalog[purchaseIndex].purchaseID);
					// Store the request off locally
					mRequestIds.put(requestId, mPurchaseCatalog[purchaseIndex].purchaseID);
				}
			}
		}
		else {
			Log.i("yoyo", "AMAZON-BILLING: ERROR. Purchase index is out of range for item purchase");
		}
	}  

	/**
	 * User has had a catlog item revoked, look it up and consume it
	 */ 
	public void revokeCatalogItem(String purchaseId)
	{
		Log.i("yoyo", "AMAZON-BILLING: Revoked purchase " + purchaseId);
		int entryIndex = getCatalogEntryIndex(purchaseId);
		if (entryIndex >= 0) {
			consumeCatalogItem(entryIndex);
		}
	}

	/**
	 * Get the index into our set of purchases
	 */
	private int getCatalogEntryIndex(String purchaseId)
	{
		if (mPurchaseCatalog != null) 
		{
			for (int n = 0; n < mPurchaseCatalog.length; n++)
			{
				if (mPurchaseCatalog[n].purchaseID.equals(purchaseId))
				{
					return n;				
				}
			}
		}
		return -1;
	}

	/**
	 * Respond to a response to a purchase request
	 */ 
	public void onPurchaseResponse(PurchaseResponse purchaseResponse)
	{
		// The purchase has completed, put the store back to its available state
		setBillingServiceStatus(eStoreAvailable);

		switch (purchaseResponse.getPurchaseRequestStatus()) 
		{
        	case SUCCESSFUL:				
				{					
        	    	final Receipt receipt = purchaseResponse.getReceipt();
					Log.i("yoyo", "AMAZON-BILLING: Purchase successful for " + receipt.getSku());
					
        	    	switch (receipt.getItemType()) {
        	    		case CONSUMABLE:
						case ENTITLED:
							int entryIndex = getCatalogEntryIndex(receipt.getSku());
							if (entryIndex >= 0) {
								registerContentPurchased(mPurchaseCatalog[entryIndex].purchaseID, true);
								downloadPurchaseContent(entryIndex);
							}
        	    		    break;
        	    		case SUBSCRIPTION:
							Log.i("yoyo", "AMAZON-BILLING: Subscriptions not supported");
        	    		    break;
        	    	}        	    
				}
        	    break;
        	case ALREADY_ENTITLED:				
				/*
        	     * If the customer has already been entitled to the item, a receipt is not returned.
        	     * Fulfillment is done unconditionally, we determine which item should be fulfilled by matching the
        	     * request id returned from the initial request with the request id stored in the response.
        	     */
				{
					String purchaseId = mRequestIds.get(purchaseResponse.getRequestId());
					Log.i("yoyo", "AMAZON-BILLING: User is already entitled to " + purchaseId);

					int entryIndex = getCatalogEntryIndex(purchaseId);											
					if (entryIndex >= 0) {
						registerContentPurchased(mPurchaseCatalog[entryIndex].purchaseID, true);
						downloadPurchaseContent(entryIndex);
					}
				}
        	    break;
			case FAILED:
				{
					String purchaseId = mRequestIds.get(purchaseResponse.getRequestId());				

					int entryIndex = getCatalogEntryIndex(purchaseId);											
					if (entryIndex >= 0) {					
						setPurchaseAvailability(entryIndex, CatalogEntry.Availability.eContentFailed);
					}
				}
        	    break;
        	case INVALID_SKU:
        	    /*
        	     * If the sku that was purchased was invalid, the application ignores the request and logs the failure.
        	     * This can happen when there is a sku mismatch between what is sent from the application and what
        	     * currently exists on the dev portal.
        	     */
        	    Log.i("yoyo", "AMAZON-BILLING: Invalid product ID " + mRequestIds.get(purchaseResponse.getRequestId()));
        	    break;
        }
	}

	/**
	 * Receipt has been found during a PurchaseUpdates request
	 */ 
	public void processReceiptUpdate(Receipt receipt)
	{
        final String sku = receipt.getSku();
		Log.i("yoyo", "AMAZON-BILLING: Receipt update found for " + sku);

        switch (receipt.getItemType()) 
		{
        	case ENTITLED:
				if (!productPurchased(receipt.getSku())) {
					registerContentPurchased(receipt.getSku(), true);
				}

				if (!productDownloaded(receipt.getSku())) {

					int entryIndex = getCatalogEntryIndex(receipt.getSku());
					if (entryIndex >= 0) {						
						downloadPurchaseContent(entryIndex);
					}
					else {
						deferContentDownload(receipt.getSku());
					}
				}
        	    break;
        	case SUBSCRIPTION:
				Log.i("yoyo", "AMAZON-BILLING: Subscriptions not supported");
        	    break;
        }
	}
}