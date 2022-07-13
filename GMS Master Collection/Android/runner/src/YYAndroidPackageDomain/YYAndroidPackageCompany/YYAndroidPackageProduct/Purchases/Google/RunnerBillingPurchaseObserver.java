// Copyright 2010 Google Inc. All Rights Reserved.
package ${YYAndroidPackageName};

import android.app.Activity;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Method;

import ${YYAndroidPackageName}.RunnerBillingConsts.PurchaseState;
import ${YYAndroidPackageName}.RunnerBillingConsts.ResponseCode;
import ${YYAndroidPackageName}.BillingRequest;
import ${YYAndroidPackageName}.CheckBillingSupported;
import ${YYAndroidPackageName}.ConfirmNotifications;
import ${YYAndroidPackageName}.GetPurchaseInformation;
import ${YYAndroidPackageName}.RequestPurchase;
import ${YYAndroidPackageName}.RestoreTransactions;

/**
 * A class for observing changes related to purchases.
 * {@link ResponseHandler}. The main application implements the callbacks
 * {@link #onBillingSupported(boolean)} and
 * {@link #onPurchaseStateChange(PurchaseState, String, int, long)}.  These methods
 * are used to update the UI.
 */
public class RunnerBillingPurchaseObserver 
{	
    private final Activity mActivity;
    private final Handler mHandler;
    private Method mStartIntentSender;
    private Object[] mStartIntentSenderArgs = new Object[5];
	private RunnerBilling mRunnerBilling;

    private static final Class[] START_INTENT_SENDER_SIG = new Class[] {
        IntentSender.class, Intent.class, int.class, int.class, int.class
    };
	
	/**
     * The SharedPreferences key for recording whether we initialized the
     * database.  If false, then we perform a RestoreTransactions request
     * to get all the purchases for this user.
     */
    private static final String PURCHASES_INITIALISED = "purchases_initialised";

	/**
	 * Constructor
	 */
    public RunnerBillingPurchaseObserver(Activity activity, Handler handler, RunnerBilling billing) 
    {
        mActivity = activity;
        mHandler = handler;
		mRunnerBilling = billing;
        initCompatibilityLayer();
    }    
    
    private void initCompatibilityLayer() 
    {
        try 
        {
            mStartIntentSender = mActivity.getClass().getMethod("startIntentSender",
                    START_INTENT_SENDER_SIG);
        } 
        catch (SecurityException e) {
            mStartIntentSender = null;
        } 
        catch (NoSuchMethodException e) {
            mStartIntentSender = null;
        }
    }

    void startBuyPageActivity(PendingIntent pendingIntent, Intent intent) 
    {
        if (mStartIntentSender != null) 
        {
            // This is on Android 2.0 and beyond.  The in-app buy page activity
            // must be on the activity stack of the application.
            try 
            {
                // This implements the method call:
                // mActivity.startIntentSender(pendingIntent.getIntentSender(), intent, 0, 0, 0);
                mStartIntentSenderArgs[0] = pendingIntent.getIntentSender();
                mStartIntentSenderArgs[1] = intent;
                mStartIntentSenderArgs[2] = Integer.valueOf(0);
                mStartIntentSenderArgs[3] = Integer.valueOf(0);
                mStartIntentSenderArgs[4] = Integer.valueOf(0);
                mStartIntentSender.invoke(mActivity, mStartIntentSenderArgs);
            } 
            catch (Exception e) 
            {
                Log.i("yoyo", "error starting activity", e);
            }
        } 
        else 
        {
            // This is on Android version 1.6. The in-app buy page activity must be on its
            // own separate activity stack instead of on the activity stack of
            // the application.
            try 
            {
                pendingIntent.send(mActivity, 0/* code */, intent);
            } 
            catch (CanceledException e) 
            {
                Log.i("yoyo", "error starting activity", e);
            }
        }
    }

    /**
     * Updates the UI after the database has been updated.  This method runs
     * in a background thread so it has to post a Runnable to run on the UI
     * thread.
     * @param purchaseState the purchase state of the item
     * @param itemId a string identifying the item
     * @param quantity the quantity of items in this purchase
     */
    void postPurchaseStateChange(
    	final PurchaseState purchaseState, 
	    final String itemId,
        final int quantity, 
        final long purchaseTime, 
        final String developerPayload) 
    {		
        mHandler.post(new Runnable() 
        {
            public void run() 
            {				
                onPurchaseStateChange(purchaseState, itemId, quantity, purchaseTime, developerPayload);
            }
        });
    }

	/**
     * This is the callback that is invoked when an item is purchased,
     * refunded, or canceled.  It is the callback invoked in response to
     * calling {@link BillingService#requestPurchase(String)}.  It may also
     * be invoked asynchronously when a purchase is made on another device
     * (if the purchase was for a Market-managed item), or if the purchase
     * was refunded, or the charge was canceled.  This handles the UI
     * update.  The database update is handled in
     * {@link ResponseHandler#purchaseResponse(Context, PurchaseState,
     * String, String, long)}.
     * @param purchaseState the purchase state of the item
     * @param itemId a string identifying the item (the "SKU")
     * @param quantity the current quantity of this item after the purchase
     * @param purchaseTime the time the product was purchased, in
     * milliseconds since the epoch (Jan 1, 1970)
     */    
    public void onBillingSupported(boolean supported) 
    {
		Log.i("yoyo", "BILLING: Supported = " + supported);
        if (!supported) {
            mActivity.showDialog(RunnerActivity.DIALOG_BILLING_NOT_SUPPORTED_ID);            
        }
        else {
	        restorePurchaseState();
        }
        mRunnerBilling.setBillingAvailable(supported);
    }

	/**
     * This is the callback that is invoked when an item is purchased,
     * refunded, or cancelled.  It is the callback invoked in response to
     * calling {@link BillingService#requestPurchase(String)}.  It may also
     * be invoked asynchronously when a purchase is made on another device
     * (if the purchase was for a Market-managed item), or if the purchase
     * was refunded, or the charge was canceled.  This handles the UI
     * update.  The database update is handled in
     * {@link ResponseHandler#purchaseResponse(Context, PurchaseState,
     * String, String, long)}.
     * @param purchaseState the purchase state of the item
     * @param itemId a string identifying the item (the "SKU")
     * @param quantity the current quantity of this item after the purchase
     * @param purchaseTime the time the product was purchased, in
     * milliseconds since the epoch (Jan 1, 1970)
     */    
    public void onPurchaseStateChange(PurchaseState purchaseState, String itemId, int quantity, long purchaseTime, String developerPayload) 
    {
		Log.i("yoyo", "BILLING: onPurchaseStateChange() itemId = " + itemId + " " + purchaseState);
        if (purchaseState == PurchaseState.PURCHASED) 
        {
    		Log.i("yoyo", "BILLING: Item " + itemId + " has been purchased");
    		mRunnerBilling.purchaseSucceeded(itemId);
    	}
    }

	/**
     * This is called when we receive a response code from Market for a
     * RequestPurchase request that we made.  This is NOT used for any
     * purchase state changes.  All purchase state changes are received in
     * {@link #onPurchaseStateChange(PurchaseState, String, int, long)}.
     * This is used for reporting various errors, or if the user backed out
     * and didn't purchase the item.  The possible response codes are:
     *   RESULT_OK means that the order was sent successfully to the server.
     *       The onPurchaseStateChange() will be invoked later (with a
     *       purchase state of PURCHASED or CANCELED) when the order is
     *       charged or canceled.  This response code can also happen if an
     *       order for a Market-managed item was already sent to the server.
     *   RESULT_USER_CANCELED means that the user didn't buy the item.
     *   RESULT_SERVICE_UNAVAILABLE means that we couldn't connect to the
     *       Android Market server (for example if the data connection is down).
     *   RESULT_BILLING_UNAVAILABLE means that in-app billing is not
     *       supported yet.
     *   RESULT_ITEM_UNAVAILABLE means that the item this app offered for
     *       sale does not exist (or is not published) in the server-side
     *       catalog.
     *   RESULT_ERROR is used for any other errors (such as a server error).
     */    
    public void onRequestPurchaseResponse(RequestPurchase request, ResponseCode responseCode) 
    {
        Log.i("yoyo", "BILLING: Request purchase response for " + request.mProductId + ": " + responseCode);        
        if (responseCode == ResponseCode.RESULT_OK) {
            Log.i("yoyo", "BILLING: purchase was successfully sent to server");
        } 
        else if (responseCode == ResponseCode.RESULT_USER_CANCELED) {
            Log.i("yoyo", "BILLING: user canceled purchase");
        } 
        else {
            Log.i("yoyo", "BILLING: purchase failed");
        }
    }
    
    public void onRestoreTransactionsResponse(RestoreTransactions request, ResponseCode responseCode) 
    {		
        if (responseCode == ResponseCode.RESULT_OK) 
        {
            Log.i("yoyo", "BILLING: completed RestoreTransactions request");

            // Update the shared preferences so that we don't perform an automatic RestoreTransactions again.
            SharedPreferences prefs = RunnerActivity.CurrentActivity.getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putBoolean(PURCHASES_INITIALISED, true);
            edit.commit();
        } 
        else {
        	Log.i("yoyo", "BILLING: RestoreTransactions error: " + responseCode);
        }
    }
    
    /**
     * If we have no record of having performed transactions we send a
     * RESTORE_TRANSACTIONS request to Android Market to get the list of purchased items
     * for this user. This happens if the application has just been installed
     * or the user wiped data. We do not want to do this on every startup, rather, we want to do
     * only when the app needs to be initialized.
	 *
	 * It's worth noting that during development we default to adb install -r which doesn't
	 * clear the SharedPreferences for the game/app and therefore if the developer hasn't
	 * got their settings quite right (e.g. content_url was missing) they will need restore
	 * purchases to happen automatically to allow content to be re-delivered.	
     */
    private void restorePurchaseState()
    {
		SharedPreferences prefs = RunnerActivity.CurrentActivity.getPreferences(Context.MODE_PRIVATE);
        boolean initialized = prefs.getBoolean(PURCHASES_INITIALISED, false);
        if (!initialized) {		                

			Log.i("yoyo", "BILLING: No prior activity noted: restoring previous purchases");
            mRunnerBilling.restorePurchasedItems();

			// Quickly notify the user that transactions are being restored, if required
			boolean value = false;
			try {
				ApplicationInfo ai = RunnerActivity.CurrentActivity.getPackageManager().getApplicationInfo(
						RunnerActivity.CurrentActivity.getComponentName().getPackageName(), 
						PackageManager.GET_META_DATA);
        		Bundle bundle = ai.metaData;
        					
				value = bundle.getBoolean("YYSuppressIAPToast");        	 
				Log.i("yoyo", "BILLING: Suppress Toast notification set to " + value);
			}
			catch (NameNotFoundException e) {
				Log.i("yoyo", "BILLING: " + e.getMessage());	
			}
			if (!value) 
			{
				Toast.makeText(RunnerActivity.CurrentActivity, "Restoring Market Transactions", Toast.LENGTH_LONG).show();
			}
        }
        else {
			Log.i("yoyo", "BILLING: Earlier activity found. NOT restoring previous purchases");
        }
    }
}
