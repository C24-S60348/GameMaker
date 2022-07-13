/*
 * Copyright (C) 2012 YoYo Games Ltd 
 */
package ${YYAndroidPackageName};

import java.util.Date;
import java.util.LinkedList;
import java.util.Map;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.amazon.inapp.purchasing.BasePurchasingObserver;
import com.amazon.inapp.purchasing.Offset;
import com.amazon.inapp.purchasing.PurchaseResponse;
import com.amazon.inapp.purchasing.PurchaseUpdatesResponse;
import com.amazon.inapp.purchasing.PurchasingManager;

import com.amazon.inapp.purchasing.GetUserIdResponse;
import com.amazon.inapp.purchasing.GetUserIdResponse.GetUserIdRequestStatus;
import com.amazon.inapp.purchasing.Item;
import com.amazon.inapp.purchasing.ItemDataResponse;

/**
 * Purchasing Observer will be called on by the Purchasing Manager asynchronously.
 * Since the methods on the UI thread of the application, all fulfillment logic is done via an AsyncTask. This way, any
 * intensive processes will not hang the UI thread and cause the application to become
 * unresponsive.
 */
public class AmazonBillingObserver extends BasePurchasingObserver 
{
	/**
     * Creates new instance of the ButtonClickerObserver class.
     * 
     * @param buttonClickerActivity Activity context
     */
    public AmazonBillingObserver() {

        super(RunnerActivity.CurrentActivity);		
    }

	/**
     * Invoked once the observer is registered with the Purchasing Manager If the boolean is false, the application is
     * receiving responses from the SDK Tester. If the boolean is true, the application is live in production.
     * 
     * @param isSandboxMode
     *            Boolean value that shows if the app is live or not.
     */
    @Override
    public void onSdkAvailable(final boolean isSandboxMode) {

		Log.i("yoyo", "AMAZON-BILLING: onSdkAvailable received, response - " + isSandboxMode);
		RunnerActivity.ViewHandler.post(new Runnable() {
    		public void run() {
    			PurchasingManager.initiateGetUserIdRequest();
    		}
    	});        
    }

	/**
     * Invoked once the call from initiateGetUserIdRequest is completed.
     * On a successful response, a response object is passed which contains the request id, request status, and the
     * userid generated for your application.
     * 
     * @param getUserIdResponse
     *            Response object containing the UserID
     */
    @Override
    public void onGetUserIdResponse(final GetUserIdResponse getUserIdResponse) {
		Log.i("yoyo", "AMAZON-BILLING: onGetUserIdResponse recieved. Response - " + getUserIdResponse);
        Log.i("yoyo", "AMAZON-BILLING: RequestId " + getUserIdResponse.getRequestId());
        Log.i("yoyo", "AMAZON-BILLING: IdRequestStatus " + getUserIdResponse.getUserIdRequestStatus());
        new GetUserIdAsyncTask().execute(getUserIdResponse);
    }

	/**
     * Invoked once the call from initiateItemDataRequest is completed.
     * On a successful response, a response object is passed which contains the request id, request status, and a set of
     * item data for the requested skus. Items that have been suppressed or are unavailable will be returned in a
     * set of unavailable skus.
     * 
     * @param itemDataResponse
     *            Response object containing a set of purchasable/non-purchasable items
     */
    @Override
    public void onItemDataResponse(final ItemDataResponse itemDataResponse) {
        Log.i("yoyo", "AMAZON-BILLING: onItemDataResponse recieved");
        Log.i("yoyo", "AMAZON-BILLING: ItemDataRequestStatus" + itemDataResponse.getItemDataRequestStatus());
        Log.i("yoyo", "AMAZON-BILLING: ItemDataRequestId" + itemDataResponse.getRequestId());
        new ItemDataAsyncTask().execute(itemDataResponse);
    }

    /**
     * Is invoked once the call from initiatePurchaseRequest is completed.
     * On a successful response, a response object is passed which contains the request id, request status, and the
     * receipt of the purchase.
     * 
     * @param purchaseResponse
     *            Response object containing a receipt of a purchase
     */
    @Override
    public void onPurchaseResponse(final PurchaseResponse purchaseResponse) {
        Log.i("yoyo", "AMAZON-BILLING: onPurchaseResponse recieved");
        Log.i("yoyo", "AMAZON-BILLING: PurchaseRequestStatus - " + purchaseResponse.getPurchaseRequestStatus());
        new PurchaseAsyncTask().execute(purchaseResponse);
    }

    /**
     * Is invoked once the call from initiatePurchaseUpdatesRequest is completed.
     * On a successful response, a response object is passed which contains the request id, request status, a set of
     * previously purchased receipts, a set of revoked skus, and the next offset if applicable. If a user downloads your
     * application to another device, this call is used to sync up this device with all the user's purchases.
     * 
     * @param purchaseUpdatesResponse
     *            Response object containing the user's recent purchases.
     */
    @Override
    public void onPurchaseUpdatesResponse(final PurchaseUpdatesResponse purchaseUpdatesResponse) {
        Log.i("yoyo", "AMAZON-BILLING: onPurchaseUpdatesRecived recieved: Response -" + purchaseUpdatesResponse);
        Log.i("yoyo", "AMAZON-BILLING: PurchaseUpdatesRequestStatus " + purchaseUpdatesResponse.getPurchaseUpdatesRequestStatus());
        Log.i("yoyo", "AMAZON-BILLING: RequestID " + purchaseUpdatesResponse.getRequestId());
        new PurchaseUpdatesAsyncTask().execute(purchaseUpdatesResponse);
    }               
}