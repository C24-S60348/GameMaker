/*
 * Copyright (C) 2012 YoYo Games Ltd 
 */

package ${YYAndroidPackageName};

import java.util.Date;
import java.util.LinkedList;
import java.util.Map;

import com.amazon.inapp.purchasing.Offset;
import com.amazon.inapp.purchasing.GetUserIdResponse;
import com.amazon.inapp.purchasing.GetUserIdResponse.GetUserIdRequestStatus;
import com.amazon.inapp.purchasing.PurchasingManager;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

/*
 * Started when the AmazonBillingObserver receives a GetUserIdResponse. The Shared Preferences file for the returned user id is
 * accessed.
 */
public class GetUserIdAsyncTask extends AsyncTask<GetUserIdResponse, Void, Boolean> {

    @Override
    protected Boolean doInBackground(final GetUserIdResponse... params) {

        GetUserIdResponse getUserIdResponse = params[0];
        if (getUserIdResponse.getUserIdRequestStatus() == GetUserIdRequestStatus.SUCCESSFUL) {

            final String userId = getUserIdResponse.getUserId();

            // Each UserID has their own shared preferences file, and we'll load that file when a new user logs in.
            AmazonBilling.setCurrentUser(userId);
            return true;
        } 
		else {
            Log.i("yoyo", "AmazonBilling: onGetUserIdResponse, unable to get user ID.");
            return false;
        }
    }

    /*
     * Call initiatePurchaseUpdatesRequest for the returned user to sync purchases that are not yet fulfilled.
     */
    @Override
    protected void onPostExecute(final Boolean result) {

        super.onPostExecute(result);
        if (result) {
            PurchasingManager.initiatePurchaseUpdatesRequest(Offset.fromString(RunnerActivity.CurrentActivity.getApplicationContext()
                .getSharedPreferences(AmazonBilling.getCurrentUser(), Context.MODE_PRIVATE)
                .getString(AmazonBilling.OFFSET, Offset.BEGINNING.toString())));
        }
    }
}