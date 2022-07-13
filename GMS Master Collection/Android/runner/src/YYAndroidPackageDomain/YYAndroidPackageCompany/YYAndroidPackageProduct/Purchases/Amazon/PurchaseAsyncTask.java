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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

/*
 * Started when the observer receives a Purchase Response
 * Once the AsyncTask returns successfully, the UI is updated.
 */
public class PurchaseAsyncTask extends AsyncTask<PurchaseResponse, Void, Boolean> 
{
    @Override
    protected Boolean doInBackground(final PurchaseResponse... params) {

        final PurchaseResponse purchaseResponse = params[0];            
        final String userId = AmazonBilling.getCurrentUser();
        
        if (!purchaseResponse.getUserId().equals(userId)) {

            // currently logged in user is different than what we have so update the state
            AmazonBilling.setCurrentUser(purchaseResponse.getUserId());

            PurchasingManager.initiatePurchaseUpdatesRequest(
				Offset.fromString(RunnerActivity.CurrentActivity
					.getSharedPreferences(AmazonBilling.getCurrentUser(), Context.MODE_PRIVATE)
						.getString(AmazonBilling.OFFSET, Offset.BEGINNING.toString())));                
        }
		AmazonBilling.Instance().onPurchaseResponse(purchaseResponse);               
        return false;
    }

    @Override
    protected void onPostExecute(final Boolean success) {
        super.onPostExecute(success);
    }
}