package ${YYAndroidPackageName};

import android.util.Log;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Vector;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.yoyogames.runner.RunnerJNILib;

public abstract class IRunnerBilling
{
	// Enums for the state of communication with the Android market, required to match up with the definition of enum eProductStoreState
	public static int eStoreUninitialised = -2;		// EnableInAppPurchases() has yet to be called
	public static int eStoreUnavailable = -1;		// The store is not available for some reason
	public static int eStoreLoading = 0;			// Currently loading up the store
	public static int eStoreAvailable = 1;			// The store has been loaded up and is available
	public static int eStoreProcessingOrder = 2;	// An order is currently being processed (effectively order debouncing)	

	// The state our communications with the billing service are in
	public static int mBillingServiceStatus = eStoreUninitialised;
	public static void setBillingServiceStatus(int status)
	{
		mBillingServiceStatus = status;				
		RunnerJNILib.IAPEvent(-1);		
	}

	// The different states an individual in-app purchase can occupy, required to match up with the definition of enum ePurchaseState
	public static int eContentUnavailable = -1;
	public static int eContentPurchased = 0;
	public static int eContentAvailable = 1;
	public static int eContentDownloading = 2;

	// Ugly global hacking for in-app purchases developer server support    
    public static String PurchasesBaseURL = null;    
	// Product ID for querying proprietary server URL
    public static String PurchasesProductID = null;

	// Object handling querying proprietary server for list of available purchases
	protected RunnerAvailablePurchases mAvailablePurchases = null; 

	// Purchase catalog is common to all sub-classes
	protected CatalogEntry[] mPurchaseCatalog = null;

	// list of RunnerDownloadPurchaseTasks currently executing
    protected Vector<RunnerDownloadPurchaseTask> mBillingActiveDownloads = new Vector<RunnerDownloadPurchaseTask>();
    // If a restore asks for content to be downloaded before we've got a response from our server for the available products then store in here
    protected Vector<String> mPendingDownloads = new Vector<String>();
	

	// All routines that need to be supplied by implementations of this class			
	public abstract void Destroy();
	public abstract void loadStore();	
	public abstract void restorePurchasedItems();
	public abstract void purchaseCatalogItem(int purchaseIndex);				
    protected abstract String getContentPurchasedKey(String contentId);
    protected abstract String getContentDownloadedKey(String contentId);
	protected abstract String getDownloadedPurchasesFileName();

	/**
     * Facility to encode a string using MD5
     */
    public static String md5encode(String in)
    {
       	// MD5 this into a string
	    MessageDigest digest;
	    try {
	        digest = MessageDigest.getInstance("MD5");
	        digest.reset();
	        digest.update(in.getBytes());
	        byte[] a = digest.digest();
	        int len = a.length;
	        StringBuilder sb = new StringBuilder(len << 1);
	        for (int i = 0; i < len; i++)
	        {
	        	sb.append(Character.forDigit((a[i] & 0xf0) >> 4, 16));
	        	sb.append(Character.forDigit(a[i] & 0x0f, 16));
	        }
	        	
	        return sb.toString();
	    } 
	    catch (NoSuchAlgorithmException e) {
	        e.printStackTrace();
	    }
	    return "not_encoded";
	}

	/*
	 * iap_activate() leads here to enable the store and related services
	 */
	public void enableInAppPurchases(String purchaseIndex)
	{
		// Look for a Base URL for a proprietary server				
		try {
			ApplicationInfo ai = RunnerActivity.CurrentActivity.getPackageManager().getApplicationInfo(
				RunnerActivity.CurrentActivity.getComponentName().getPackageName(), 
				PackageManager.GET_META_DATA);
        	Bundle bundle = ai.metaData;
        				
	        String value = bundle.getString("YYInAppPurchaseServerURL");        	 
			Log.d("yoyo", "BILLING: Inapp Purchasing proprietary URL set to: " + value);

	        PurchasesBaseURL = value;
		}
		catch (Exception e) {
		    e.printStackTrace();
		}
    
		// Store off the data used to query the developer server		
		PurchasesProductID = purchaseIndex;
    
		// And load the store up
	    RunnerActivity.ViewHandler.post(new Runnable() {
    		 public void run() 
    		 {
				 if ((mBillingServiceStatus == eStoreUnavailable) ||
		    		 (mBillingServiceStatus == eStoreUninitialised))
		    	{
					Log.i("yoyo", "BILLING: Loading services");
					setBillingServiceStatus(eStoreLoading);
	    
					// Create a new object for getting available purchases (these can only execute once)
					mAvailablePurchases = new RunnerAvailablePurchases();

					// Perform store specific loading operations
			    	loadStore();
			    }		
			}
		});
	}

	/**
     * Called from RunnerAvailablePurchases (onPostExecute) when we've downloaded the set of available purchases from the YoYo server
     */
    public void availablePurchasesAcquired(CatalogEntry[] availablePurchases)
    {
    	mPurchaseCatalog = availablePurchases;
    	if (mBillingServiceStatus == eStoreLoading) 
    	{
		    if (mPurchaseCatalog != null) 
		    {
			    setBillingServiceStatus(eStoreAvailable);
				processPendingDownloads();
			    initialiseCatalogState();			    
			}
			else {
				setBillingServiceStatus(eStoreUnavailable);
			}
		}
    }

	/*
	 * Sets the availability of a catalog item and kicks off an event to let the GM user know
	 */
	public void setPurchaseAvailability(int entryIndex, CatalogEntry.Availability availability)
	{
		mPurchaseCatalog[entryIndex].availability = availability;		
		RunnerJNILib.IAPEvent(entryIndex);
	}

	 /**
      * Restore purchases may have kicked in before we received the set of available products 
	  * from a proprietary server so any requested downloads should now be safe to kick off
      */
	private void processPendingDownloads()
	{
		if (mPendingDownloads.size() > 0) {

			Log.i("yoyo", "BILLING: Processing pending downloads");
			for (String pendingDownload : mPendingDownloads)
			{
				for (int n = 0; n < mPurchaseCatalog.length; n++) 
				{
					if (mPurchaseCatalog[n].purchaseID == pendingDownload) {
						downloadPurchaseContent(n);
					}
				}
			}
			mPendingDownloads.removeAllElements();
		}
	}
    
    /**
     * When the user activates purchasing with a hand built list of available purchases we record it via this function
     */
    public void setGMLPurchasesList(CatalogEntry[] availablePurchases)
    {
		mPurchaseCatalog = availablePurchases;
		initialiseCatalogState();
    }

	/** 
	 * Make sure the purchase states are correctly initialised and any pending downloads are processed
	 */ 
	private void initialiseCatalogState()
	{		
		// Now make sure the purchase states for all catalog items are correctly reflected		
		for (int n = 0; n < mPurchaseCatalog.length; ++n)
		{			
            if (productPurchased(mPurchaseCatalog[n].purchaseID))
            {
				if (productDownloaded(mPurchaseCatalog[n].purchaseID)) {

					setPurchaseAvailability(n, CatalogEntry.Availability.eContentPurchased);            		
				}
				else if (mPurchaseCatalog[n].availability != CatalogEntry.Availability.eContentDownloading) {

					// Product was purchased but did not complete downloading last time out, and isn't already
					// being downloaded from a restore purchases, then try and download the content again now
					setPurchaseAvailability(n, CatalogEntry.Availability.eContentDownloading);					
					downloadPurchaseContent(n);
            	}
			}
		}
	}

	/** 
     * Called to find out how many in-app purchases are available for selection
     */
    public int availablePurchasesCount()
    {
    	if (eStoreAvailable == eStoreAvailable)
    	{
   			if (mPurchaseCatalog != null)
    		{
	    		return mPurchaseCatalog.length;	    		
	    	}
		}
		return 0;
    }
    
    /**
     * Retrieve the set of available purchases
     */ 
    public CatalogEntry[] getAvailablePurchases()
    {
    	return mPurchaseCatalog;
    }	

	/**
	 * Stores in shared preferences whether or not a product has been successfully purchased
	 */
	protected void registerContentPurchased(String purchaseId, boolean flag)
	{
		SharedPreferences prefs = RunnerActivity.CurrentActivity.getPreferences(Context.MODE_PRIVATE);
		SharedPreferences.Editor edit = prefs.edit();
            
        String contentKey = getContentPurchasedKey(purchaseId);
        edit.putBoolean(contentKey, flag);
        edit.commit();
	}

	/**
	 * Stores in shared preferences whether or not the related content for a purchase has been downloaded
	 */
	protected void registerContentDownloaded(String purchaseId, boolean flag)
	{
		SharedPreferences prefs = RunnerActivity.CurrentActivity.getPreferences(Context.MODE_PRIVATE);
		SharedPreferences.Editor edit = prefs.edit();
            
        String contentKey = getContentDownloadedKey(purchaseId);
        edit.putBoolean(contentKey, flag);
        edit.commit();
	}    

	/** 
     * Looks up the SharedPreferences to see if a product has been flagged as purchased
     */
    public boolean productPurchased(String contentId) {

    	String contentKey = getContentPurchasedKey(contentId);
        SharedPreferences prefs = RunnerActivity.CurrentActivity.getPreferences(Context.MODE_PRIVATE);
        return prefs.getBoolean(contentKey, false);
    }

	/** 
     * Looks up the SharedPreferences to see if a product has been flagged as downloaded
     */
	public boolean productDownloaded(String contentId) {

		String contentKey = getContentDownloadedKey(contentId);
        SharedPreferences prefs = RunnerActivity.CurrentActivity.getPreferences(Context.MODE_PRIVATE);
        return prefs.getBoolean(contentKey, false);
	}	

	/**
     * Store purchaseId in the set of downloads to be executed once the store is in a good state
     */
	protected void deferContentDownload(String purchaseId)
	{
		Log.i("yoyo", "BILLING: Deferring content download for " + purchaseId);
		if (!mPendingDownloads.contains(purchaseId)) {
   			mPendingDownloads.add(purchaseId);
		}
	}

	/**
     * Actually download content for the purchase from the CatalogEntry URL if available, otherwise mark as downloaded and complete
     */
	protected void downloadPurchaseContent(int entryIndex)
    {    	
		CatalogEntry catalogEntry = mPurchaseCatalog[entryIndex];

        // Prepare a request object if there is a content url to use        			
        if (catalogEntry.contentURL != null && !catalogEntry.contentURL.equals(""))
        {
			Log.i("yoyo", "BILLING: Retrieving content from " + catalogEntry.contentURL);
			setPurchaseAvailability(entryIndex, CatalogEntry.Availability.eContentDownloading);

			RunnerDownloadPurchaseTask downloadTask = new RunnerDownloadPurchaseTask();
			mBillingActiveDownloads.add(downloadTask);

			downloadTask.execute(catalogEntry.contentURL, catalogEntry.localPath, catalogEntry.purchaseID);
		}
		else {
			// Just mark it as downloaded and carry on as you were
			Log.i("yoyo", "BILLING: No associated content. Purchase complete for " + catalogEntry.purchaseID);
			setPurchaseAvailability(entryIndex, CatalogEntry.Availability.eContentPurchased);

			registerContentDownloaded(catalogEntry.purchaseID, true);
		}   		   		   		
    }


	/** 
     * Called (from RunnerDownloadPurchaseTask) when content has finished downloading
     */
    public void purchaseContentDownloadedCallback(boolean success, RunnerDownloadPurchaseTask downloadTask)
    {
		try {			
			int catalogEntryIndex = -1;
            for (int n = 0; n < mPurchaseCatalog.length; n++)
            {				
				if (mPurchaseCatalog[n].purchaseID.equals(downloadTask.mContentId)) {
					catalogEntryIndex = n;					
	            	break;
	            }
            }

			if (success) {
				setPurchaseAvailability(catalogEntryIndex, CatalogEntry.Availability.eContentPurchased);
	    		// Store that this content has been purchased in the shared preferences			
            	registerContentDownloaded(downloadTask.mContentId, true);            	            	
	    		// Store off the set of files downloaded
	    		storeDownloadedFileNames(downloadTask.mContentId, downloadTask.mExtractedFileNames);	    		
	    		// Remove from the active downloads list (and let GC clear it up once this routine has cleared)
	    		mBillingActiveDownloads.remove(downloadTask);

				Log.i("yoyo", "BILLING: Content delivery succeeded, product purchase is now complete for " + downloadTask.mContentId);
			}
			else {
				setPurchaseAvailability(catalogEntryIndex, CatalogEntry.Availability.eContentFailed);
				Log.i("yoyo", "BILLING: Content delivery failed for " + downloadTask.mContentId);
			}
	    }
	    catch (NullPointerException e) {
		    e.printStackTrace();
	    }
	    catch (ClassCastException e) {
		    e.printStackTrace();
	    }	    
    }


	/**
     * Used after we've unzipped downloaded content for a purchase to store a list of the files retrieved
     */
    protected void storeDownloadedFileNames(String contentId, String[] downloadedFileNames)
    {
        JSONObject jsonObject = getDownloadedFilesJSON();
        try
        {
	        if (jsonObject != null)
    	    {
	            // If the key already exists within the JSON data then it needs to be removed
           		jsonObject.remove(contentId);
        	}
	        else {
    	    	jsonObject = new JSONObject();
	        }
	        // Turn the string array into something useful for JSON
	        jsonObject.put(contentId, new JSONArray(Arrays.asList(downloadedFileNames)));
	    }
	    catch (JSONException e) {
	    	e.printStackTrace();
	    }
        
        // Dump out the JSON to the output file
        String outputFileName = RunnerJNILib.GetSaveFileName(getDownloadedPurchasesFileName());
		Log.i("yoyo", "BILLING: Storing downloaded filenames extracted to file " + outputFileName);

		// We're essentially just overwriting the original file, so just delete it if it's already there (we have its contents from above)
        File outputFile = new File(outputFileName);
        outputFile.delete();
        
        try {
	        FileOutputStream fos = new FileOutputStream(outputFile);        
    	    String outputString = jsonObject.toString();
			Log.i("yoyo", "BILLING: Storing JSON data " + outputString);
	        byte[] outputStringBytes = outputString.getBytes();
    	   	fos.write(outputStringBytes, 0, outputStringBytes.length);
        	fos.close();
        }
        catch (IOException e) {
			Log.i("yoyo", "BILLING: Failed to store JSON data.");
        	e.printStackTrace();
        }
        catch (Exception e) {
        	Log.i("yoyo", "BILLING: Failed to store JSON data.");
        	e.printStackTrace();
        }
    }
    
    /**
     * Returns the JSON data for the set of files downloaded for purchases
     */
    public JSONObject getDownloadedFilesJSON()
    {	            
	    JSONObject jsonObject = null;
    
        // Check to see if the file already exists and if it does get the JSON that's already there        
        File outputFile = new File(RunnerJNILib.GetSaveFileName(getDownloadedPurchasesFileName()));
    	if (outputFile.exists())
    	{
	    	byte[] fileContents = null;
    		try {
			    FileInputStream fis = new FileInputStream(outputFile);
    		    fileContents = new byte[(int)outputFile.length()];
	    		fis.read(fileContents, 0, (int)outputFile.length());
	    		fis.close();
	    	}
	    	catch (IOException e) {
	    		e.printStackTrace();
	    	}
        	
    	    String fileString = new String(fileContents);
    	    // We should be returned a JSON object containing one or more possible purchases
    		try
    		{
		    	jsonObject = new JSONObject(fileString);
		    }
	    	catch (JSONException e) {
	    		e.printStackTrace();
		    }
		}
	    
	    return jsonObject;
    }

	/**
     * Called from the VC_Runner when the current game decides it wants to consume an item (i.e. set it to no longer be purchased...)
     */
    public void consumeCatalogItem(int purchaseIndex)
    {
		if (mPurchaseCatalog != null)
    	{
    		if (mBillingServiceStatus == eStoreAvailable)
    		{    			
				if ((purchaseIndex >= 0) && (purchaseIndex < mPurchaseCatalog.length))
				{
					Log.i("yoyo", "BILLING: Consuming content for purchase ID " + mPurchaseCatalog[purchaseIndex].purchaseID);

					registerContentPurchased(mPurchaseCatalog[purchaseIndex].purchaseID, false);
					registerContentDownloaded(mPurchaseCatalog[purchaseIndex].purchaseID, false);

					setPurchaseAvailability(purchaseIndex, CatalogEntry.Availability.eContentAvailable);
       			}
       		}
       		else {
	       		Log.i("yoyo", "BILLING: Store is not available for consuming content.");
       		}
       	}
       	else {
       		Log.i("yoyo", "BILLING: Billing is not supported!");
       	}
    }	
}