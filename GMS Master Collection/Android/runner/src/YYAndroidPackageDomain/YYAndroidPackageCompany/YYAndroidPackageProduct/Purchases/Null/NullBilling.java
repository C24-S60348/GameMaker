package ${YYAndroidPackageName};

import android.util.Log;
import android.content.Context;

public class NullBilling extends IRunnerBilling
{	
	public static final String EmptyString = "";

	public NullBilling(Context context)
	{		
	}

	public void Destroy()
	{
	}

	/**
	 * Called whilst enabling the store at the IRunnerBilling level
	 */
	public void loadStore()
	{
		Log.i("yoyo", "NULL-BILLING: Store is not available");
		setBillingServiceStatus(eStoreUnavailable);
	}


	/**
	 * I'm not entirely sure this is good functionality and purchase updates is handled via user ID updates
	 */
	public void restorePurchasedItems()
	{
		Log.i("yoyo", "NULL-BILLING: Store is not available");
	}	

	/**
	 * Actually go ahead and purchase an item
	 */
	public void purchaseCatalogItem(int purchaseIndex)
	{
		Log.i("yoyo", "NULL-BILLING: Store is not available");
	}  

	/**
	 * User has had a catlog item revoked, look it up and consume it
	 */ 
	public void revokeCatalogItem(String purchaseId)
	{
		Log.i("yoyo", "NULL-BILLING: Store is not available");
	}

	protected String getContentPurchasedKey(String contentId)
	{
		return EmptyString;
	}

    protected String getContentDownloadedKey(String contentId)
	{
		return EmptyString;
	}

	protected String getDownloadedPurchasesFileName()
	{
		return EmptyString;
	}
}