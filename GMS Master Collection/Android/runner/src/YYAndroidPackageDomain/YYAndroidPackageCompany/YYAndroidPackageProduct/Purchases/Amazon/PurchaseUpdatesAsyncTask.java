/*
 * Copyright (C) 2012 YoYo Games Ltd 
 */
package ${YYAndroidPackageName};

import java.util.Date;
import java.util.LinkedList;
import java.util.Map;

import com.amazon.inapp.purchasing.Offset;
import com.amazon.inapp.purchasing.PurchaseResponse;
import com.amazon.inapp.purchasing.PurchaseUpdatesResponse;
import com.amazon.inapp.purchasing.PurchasingManager;
import com.amazon.inapp.purchasing.Receipt;
import com.amazon.inapp.purchasing.SubscriptionPeriod;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

/*
 * Started when the observer receives a Purchase Updates Response Once the AsyncTask returns successfully, we'll
 * update the UI.
 */
public class PurchaseUpdatesAsyncTask extends AsyncTask<PurchaseUpdatesResponse, Void, Boolean> {

    @Override
    protected Boolean doInBackground(final PurchaseUpdatesResponse... params) {

        final PurchaseUpdatesResponse purchaseUpdatesResponse = params[0];
        final SharedPreferences.Editor editor = AmazonBilling.getSharedPreferencesEditor();
        final String userId = AmazonBilling.getCurrentUser();
        if (!purchaseUpdatesResponse.getUserId().equals(userId)) {
            return false;
        }

        /*
         * If the customer for some reason had items revoked, the skus for these items will be contained in the
         * revoked skus set.
         */
        for (final String sku : purchaseUpdatesResponse.getRevokedSkus()) {            
			AmazonBilling.Instance().revokeCatalogItem(sku);
        }

        switch (purchaseUpdatesResponse.getPurchaseUpdatesRequestStatus()) 
		{
        	case SUCCESSFUL:
        	    for (final Receipt receipt : purchaseUpdatesResponse.getReceipts()) 
				{
					AmazonBilling.Instance().processReceiptUpdate(receipt);					        	        
        	    }
        	    
        	    // Store the offset into shared preferences. If there has been more purchases since the last time 
				// our application updated, another initiatePurchaseUpdatesRequest is called with the new offset.        	    
        	    final Offset newOffset = purchaseUpdatesResponse.getOffset();
        	    editor.putString(AmazonBilling.OFFSET, newOffset.toString());
        	    editor.commit();

        	    if (purchaseUpdatesResponse.isMore()) {
        	        Log.i("yoyo", "AMAZON-BILLING: Initiating Another Purchase Updates with offset " + newOffset.toString());
        	        PurchasingManager.initiatePurchaseUpdatesRequest(newOffset);
        	    }
        	    return true;
        	case FAILED:
        	     // On failed responses the application will ignore the request.
        	    return false;
        }
        return false;
    }

	@Override
    protected void onPostExecute(final Boolean success) {
        super.onPostExecute(success);
    }
}