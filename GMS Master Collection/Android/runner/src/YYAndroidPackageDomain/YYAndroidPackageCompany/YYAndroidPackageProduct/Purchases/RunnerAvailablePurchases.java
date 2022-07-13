/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ${YYAndroidPackageName};

import android.os.AsyncTask;
import android.util.Log;
import android.content.SharedPreferences;
import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class used to retrieve the set of in-app products available for the application
 */
public class RunnerAvailablePurchases extends AsyncTask<String, Void, CatalogEntry[]>
{
 	private static String convertStreamToString(InputStream is) 
 	{
        /*
         * To convert the InputStream to String we use the BufferedReader.readLine()
         * method. We iterate until the BufferedReader return null which means
         * there's no more data to read. Each line will appended to a StringBuilder
         * and returned as String.
         */
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
 
        String line = null;
        try 
        {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } 
        catch (IOException e) {
            e.printStackTrace();
        } 
        finally 
        {
            try {
                is.close();
            } 
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
    
    @Override
    protected void onPostExecute(CatalogEntry[] availablePurchases)
    {
   		// Make sure the runner activity knows that we've got the available purchases
   		RunnerActivity.CurrentActivity.RunnerBilling().availablePurchasesAcquired(availablePurchases);
    }

	// Executes a web request to pull down the set of in-app purchases available to the current product
	// The results are communicated via onPostExecute
	@Override
    protected CatalogEntry[] doInBackground(String... url)
    {       
	    Log.i("yoyo", "Retrieving available purchases from " + url[0]);
    
    	CatalogEntry[] availablePurchases = null;
    
	    HttpClient httpclient = new DefaultHttpClient();
 
        // Prepare a request object        
        HttpGet httpget = new HttpGet(url[0]);
 
        // Execute the request
        HttpResponse response;
        try 
        {
            response = httpclient.execute(httpget);
            
            // Examine the response status and log it
            Log.i("yoyo", "Acquired available purchases response: " + response.getStatusLine().toString());
 
            // Get hold of the response entity
            HttpEntity entity = response.getEntity();
            
            // If the response does not enclose an entity, there is no need
            // to worry about connection release
            if (entity != null)
            {
                // A Simple JSON Response Read
                InputStream instream = entity.getContent();
                String result= convertStreamToString(instream);
                Log.i("yoyo",result);
 
                // We're returned an array containing one or more possible purchases
                JSONArray jsonArray = new JSONArray(result);
                
                // Create storage for the set of available purchases
                availablePurchases = new CatalogEntry[jsonArray.length()];
                for (int arrayIndex = 0; arrayIndex < jsonArray.length(); ++arrayIndex)
                {                
                	JSONObject jsonObject = jsonArray.getJSONObject(arrayIndex);
                	Log.i("yoyo","<jsonobject>\n" + jsonObject.toString() + "\n</jsonobject>");
                	
                	try
                	{
                		availablePurchases[arrayIndex] = new CatalogEntry(
                			jsonObject.getString("name"),
                			jsonObject.getString("description"),
                			jsonObject.getString("purchase_id"),
                			jsonObject.getString("url"),
                			jsonObject.getString("price"),
                			jsonObject.getString("path"));						            			
                			
                		Log.i("yoyo", availablePurchases[arrayIndex].purchaseID + " is available for purchase");
	                }
	                catch (JSONException e) {
	                	availablePurchases[arrayIndex] = new CatalogEntry();
	                }
                }
 
                // Closing the input stream will trigger connection release
                instream.close();
            }
        } 
        catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } 
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (Exception e) {
        	e.printStackTrace();
        }
        
        return availablePurchases;
    }
}
