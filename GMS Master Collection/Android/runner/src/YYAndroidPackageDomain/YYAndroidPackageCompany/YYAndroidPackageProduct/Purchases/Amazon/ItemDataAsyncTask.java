/*
 * Copyright (C) 2012 YoYo Games Ltd 
 */

package ${YYAndroidPackageName};

import java.util.Date;
import java.util.LinkedList;
import java.util.Map;

import com.amazon.inapp.purchasing.Item;
import com.amazon.inapp.purchasing.ItemDataResponse;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

/*
 * Started when the observer receives an Item Data Response.
 * Takes the items and display them in the logs. You can use this information to display an in game
 * storefront for your IAP items.
 */
public class ItemDataAsyncTask extends AsyncTask<ItemDataResponse, Void, Void> {

    @Override
    protected Void doInBackground(final ItemDataResponse... params) {
        final ItemDataResponse itemDataResponse = params[0];

        switch (itemDataResponse.getItemDataRequestStatus()) {
        case SUCCESSFUL_WITH_UNAVAILABLE_SKUS:
            // Skus that you can not purchase will be here.
            for (final String s : itemDataResponse.getUnavailableSkus()) {
                Log.i("yoyo", "AMAZON-BILLING: Unavailable SKU " + s);
            }
        case SUCCESSFUL:
            // Information you'll want to display about your IAP items is here
            // In this example we'll simply log them.
            final Map<String, Item> items = itemDataResponse.getItemData();
            for (final String key : items.keySet()) {
                Item i = items.get(key);
                Log.i("yoyo", String.format("AMAZON-BILLING: Item: %s\n Type: %s\n SKU: %s\n Price: %s\n Description: %s\n", i.getTitle(), i.getItemType(), i.getSku(), i.getPrice(), i.getDescription()));
            }
            break;
        case FAILED:
            // On failed responses will fail gracefully.
            break;
        }
        return null;
    }
}