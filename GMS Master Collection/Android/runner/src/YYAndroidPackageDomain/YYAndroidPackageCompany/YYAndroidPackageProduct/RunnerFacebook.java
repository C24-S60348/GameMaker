package ${YYAndroidPackageName};

import android.app.Application;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;

import java.lang.NullPointerException;
import java.net.URLConnection;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.facebook.android.*;
import com.facebook.android.Facebook.*;
import com.yoyogames.runner.RunnerJNILib;
	
public class RunnerFacebook {

	// Class for listening to the results of a Facebook-sdk dialog call
	abstract static class dialogListener implements Facebook.DialogListener {

    	public abstract void onComplete(Bundle values);
        public void onFacebookError(FacebookError e) {
        	Log.i("yoyo", "Facebook Error: " + e.getMessage());
        }
        public void onError(DialogError e) {
        	Log.i("yoyo", "Facebook Dialog Error: " + e.getMessage());
        }
        public void onCancel() {
        	Log.i("yoyo", "User cancelled the Facebook Dialog");
        }
	};
	
	class permissionsRequestListener implements AsyncFacebookRunner.RequestListener {
				
		private void onFailure() {
		
			Log.e("yoyo", "Permissions request failure. Current status: " + RunnerFacebook.msLoginStatus);
			ClearStoredAccessTokenData();
			FacebookLogin();
		}
				
		public void onComplete(String response, final Object state) {									
				
			Log.i("yoyo", "permissionsRequestListener: onComplete()");											
			try {
				JSONObject obj = Util.parseJson(response);
				RunnerFacebook.msLoginStatus = "AUTHORISED";			
			} 
			catch (JSONException e) {
				e.printStackTrace();
				Log.e("facebook-stream", "CheckFBPermissions: JSON Error: " + e.getMessage());
			} 
			catch (FacebookError e) {			
				Log.e("yoyo", "CheckFBPermissions: FacebookError: " + e.getMessage());
				onFailure();
			}
		}
								
		public void onFacebookError(FacebookError e, final Object state) {				
				
			Log.i("yoyo", "CheckFBPermissions: onFacebookError occurred...");
			onFailure();
		}
		
    	public void onFileNotFoundException(FileNotFoundException e, final Object state) {
	        Log.e("yoyo", "permissionsRequestListener: onFileNotFoundException():" + e.getMessage());      
	    }

    	public void onIOException(IOException e, final Object state) {
        	Log.e("yoyo", "permissionsRequestListener: onIOException():" + e.getMessage());      
	    }

    	public void onMalformedURLException(MalformedURLException e, final Object state) {
	        Log.e("yoyo", "permissionsRequestListener: onMalformedURLException():" + e.getMessage());            
    	}
	};


	class userInfoRequestListener implements AsyncFacebookRunner.RequestListener {
				
		private void onFailure() {
		
			Log.e("yoyo", "User Info request failure. Current status: " + RunnerFacebook.msLoginStatus);
		}
				
		public void onComplete(String response, final Object state) {									
				
			Log.i("yoyo", "userInfoRequestListener: onComplete()");											
			try {
				JSONObject obj = Util.parseJson(response);
				String id = obj.getString("id");
				
				msUserId = new String(id);
				Log.i("yoyo","received fb user id " + id);
						
			} 
			catch (JSONException e) {
				e.printStackTrace();
				Log.e("facebook-stream", "userInfoRequest(FB): JSON Error: " + e.getMessage());
			} 
			catch (FacebookError e) {			
				Log.e("yoyo", "userInfoRequest(FB): FacebookError: " + e.getMessage());
				onFailure();
			}
		}
								
		public void onFacebookError(FacebookError e, final Object state) {				
				
			Log.i("yoyo", "userInfoRequest(FB): onFacebookError occurred...");
			onFailure();
		}
		
    	public void onFileNotFoundException(FileNotFoundException e, final Object state) {
	        Log.e("yoyo", "userInfoRequest(FB): onFileNotFoundException():" + e.getMessage());      
	    }

    	public void onIOException(IOException e, final Object state) {
        	Log.e("yoyo", "userInfoRequest(FB): onIOException():" + e.getMessage());      
	    }

    	public void onMalformedURLException(MalformedURLException e, final Object state) {
	        Log.e("yoyo", "userInfoRequest(FB): onMalformedURLException():" + e.getMessage());            
    	}
	};
	// Class for listening to the results of a Facebook-sdk graph call
	abstract static class asyncRequestListener implements AsyncFacebookRunner.RequestListener {
	
		public int mUserData;
		
	 	public abstract void onComplete(JSONObject obj, final Object state);

		public asyncRequestListener(int userData) {
			mUserData = userData;
		}
		
	    public void onComplete(String response, final Object state) {
    	    try {
        	    JSONObject obj = Util.parseJson(response);
	            onComplete(obj, state);
    	    } 
    	    catch (JSONException e) {
        	    e.printStackTrace();
            	Log.e("facebook-stream", "JSON Error:" + e.getMessage());
	        } 
	        catch (FacebookError e) {
    	        Log.e("facebook-stream", "Facebook Error:" + e.getMessage());
	        }
    	}	   

    	public void onFacebookError(FacebookError e, final Object state) {
        	Log.e("yoyo", "This was a Facebook Error:" + e.getMessage());
	    }

    	public void onFileNotFoundException(FileNotFoundException e,
    	                                    final Object state) {
	        Log.e("yoyo", "Resource not found:" + e.getMessage());      
	    }

    	public void onIOException(IOException e, final Object state) {
        	Log.e("yoyo", "Network Error:" + e.getMessage());      
	    }

    	public void onMalformedURLException(MalformedURLException e,
        	                                final Object state) {
	        Log.e("yoyo", "Invalid URL:" + e.getMessage());            
    	}
	};


	public static AsyncFacebookRunner ms_asyncFacebookRunner;

	// Facebook communication settings
	// public static final String FACEBOOK_APP_ID = "fbappidxxxxxxxx";	
    public static Facebook msFacebook = null;
    public static String msLoginStatus = "IDLE"; 
    public static String msUserId = "";       
    
    // The context for maintaining app integrity
    private Context mContext;
    
    // C-tor
    public RunnerFacebook(Context context) {
    	mContext = context;    	
    }

	public void initFacebook(String appID) {
	
		Log.i("yoyo", "RunnerFacebook.initFacebook: Facebook initialisation for " + appID);
		if (msFacebook == null) {
		
			msFacebook = new Facebook(appID);
			ms_asyncFacebookRunner = new AsyncFacebookRunner(msFacebook);
		}
	}
	
	public String getUserId(){
		return msUserId;
	}
	
	public String facebookLoginStatus() {
		return msLoginStatus;
	}
	
	private void ClearStoredAccessTokenData() {
	
		final RunnerActivity activity = RunnerActivity.CurrentActivity;
		SharedPreferences.Editor editor = activity.getPreferences(Context.MODE_PRIVATE).edit();
	    editor.remove("access_token");
    	editor.remove("access_expires");
        editor.commit();
	}

	// Handle FB authorisation
	static String[] ms_FacebookPermissions;
    public void setupFacebook(String[] permissions) {    
    
		if (msFacebook == null) {
			Log.i("yoyo", "Facebook has not been initialised!");
		}
	    	    
	    // If we have our custom Facebook permission then setup the necessary parts
	    final RunnerActivity activity = RunnerActivity.CurrentActivity;
	    
	    // Get existing access_token if already stored and hasn't expired
    	SharedPreferences prefs = activity.getPreferences(Context.MODE_PRIVATE);        
        String access_token = prefs.getString("access_token", null);
	    long expires = prefs.getLong("access_expires", 0);
    	if(access_token != null) {
		
        	Log.i("yoyo", "Access token is already available: " + access_token);
        	msFacebook.setAccessToken(access_token);
	    }
    	if (expires != 0) {
            msFacebook.setAccessExpires(expires);
	    }
 
		msLoginStatus = "PROCESSING";
		
		// Store permissions for potential later use
		ms_FacebookPermissions = permissions;
		
		if (msFacebook.isSessionValid()) {	
			// Do a permissions check at this stage to ensure the user hasn't uninstalled the app
			CheckFBPermissions();
			GetFBUserId();
		}
		else {
			// Actually attempt to do the login
			FacebookLogin();
    	}
    }	
	
	private void GetFBUserId()
	{
		Bundle parameters = new Bundle();  
		ms_asyncFacebookRunner.request("me", parameters, "GET", new userInfoRequestListener(), null);
	}
	
	private void CheckFBPermissions() {
	
		Log.i("yoyo", "Sanity checking FB permissions");
		
		Bundle parameters = new Bundle();  
		ms_asyncFacebookRunner.request("me/permissions", parameters, "GET", new permissionsRequestListener(), null);
	}
	
	private boolean isNetworkAvailable() {	
        
        ConnectivityManager conMan = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetwork = conMan.getActiveNetworkInfo();
		return activeNetwork != null && activeNetwork.isConnected();
    }
	
	private void FacebookLogin() {
	
		for(int i=0;i<ms_FacebookPermissions.length;i++)
			Log.i("yoyo", "Setting up Facebook for permissions " + ms_FacebookPermissions[i]);	
		RunnerActivity.ViewHandler.post(new Runnable() {
			public void run() {			
				msFacebook.authorize(RunnerActivity.CurrentActivity, ms_FacebookPermissions, new DialogListener() {			
					@Override
					public void onComplete(Bundle values) {
			
						Log.i("yoyo", "Facebook authorisation complete with access token: " + msFacebook.getAccessToken());
						
						final RunnerActivity activity = RunnerActivity.CurrentActivity;
						SharedPreferences.Editor editor = activity.getPreferences(Context.MODE_PRIVATE).edit();
						editor.putString("access_token", msFacebook.getAccessToken());
						editor.putLong("access_expires", msFacebook.getAccessExpires());
						editor.commit();
						
						RunnerFacebook.msLoginStatus = "AUTHORISED";
						GetFBUserId();
					}
		
					@Override
					public void onFacebookError(FacebookError error) {
						Log.i("yoyo", "ERROR: Facebook authorisation in error");
						Log.i("yoyo", "ERROR: Facebook authorisation in error");
						Log.i("yoyo", "ERROR: Facebook authorisation in error");
						Log.i("yoyo", "ERROR: Facebook authorisation in error");
						Log.i("yoyo", "error = " + error.getMessage() );
						RunnerFacebook.msLoginStatus = "DENIED";
					}
		
					@Override
					public void onError(DialogError e) {
						Log.i("yoyo", "Facebook authorisation onError()");
						RunnerFacebook.msLoginStatus = "FAILED";
						
						// If the internet connection being down is the reason for failure then display an error to the user as such
						if (!isNetworkAvailable()) {
						
							AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
							builder.setMessage("Connection Failed: Please check your internet connection")
								.setCancelable(false)
								.setPositiveButton("Ok", null);
							AlertDialog alert  = builder.create();
							alert.show();
						}
					}
		
					@Override
					public void onCancel() {
						Log.i("yoyo", "Facebook authorisation onCancel()");
						RunnerFacebook.msLoginStatus = "DENIED";
					}
				});
			}
		});
	}
    
    // Log the user out from Facebook
    public void logout() {
    
    	ms_asyncFacebookRunner.logout(mContext, new asyncRequestListener(-1) {
	    	public void onComplete(JSONObject response, final Object state) {
	    	   		Log.i("yoyo", "The user has been logged out of Facebook: " + response.toString());
	    	   		msLoginStatus = "IDLE";
    		   	}
    		}, null);
    		
    	// The stored access_token from the shared preferences is no longer valid
    	ClearStoredAccessTokenData();
    }
    
    // Handles the situation where an array is found within the json data
	private void extractJSONDataArray(JSONArray objArray, int dsListIndex)
	{       
    	for (int arrayIndex = 0; arrayIndex < objArray.length(); ++arrayIndex) 
	    {
		    Object arrayObj;
	    	try {	    	
	    	    arrayObj = objArray.get(arrayIndex);
	    	}
	    	catch (org.json.JSONException e) {
	    		e.printStackTrace();
	    		continue;
	    	}
	    	
        	if (arrayObj instanceof JSONArray)
    	    {
	            // Create a new ds_list for the array
        	    int dsNewListIndex = RunnerJNILib.dsListCreate();
            
            	// Add this map index to the list
    	        RunnerJNILib.dsListAddInt(dsListIndex, dsNewListIndex);
        
 	           	Log.i("yoyo", "Added ds_list " + dsNewListIndex + " to ds_list " + dsListIndex);
            
    	        // Add the ds_list index to the ds_map for the current key                      
            	extractJSONDataArray((JSONArray)objArray, dsNewListIndex); 
	        }
    	    else if (arrayObj instanceof JSONObject) 
        	{                    
            	// Create a new ds_map and get the index for it...
    	        int subDsMap = RunnerJNILib.dsMapCreate();
	            
            	// Add this map index to the list
        	    RunnerJNILib.dsListAddInt(dsListIndex, subDsMap);
            
	            // Recurse to parse the new dictionary
        	    translateJSONResponse((JSONObject)arrayObj, subDsMap);
            
	            Log.i("yoyo", "Added ds_map " + subDsMap + " to ds_list " + dsListIndex);
    	    }
        	else if (arrayObj instanceof String) 
	        {                    
    	        // Add the string to the list        	                     
    	        RunnerJNILib.dsListAddString(dsListIndex, (String)arrayObj);
        	    
            	Log.i("yoyo", "Added " + (String)arrayObj + " to ds_list " + dsListIndex);
	        } 
	        else if(arrayObj instanceof Integer)
    	    {
    			RunnerJNILib.dsListAddInt(dsListIndex, (Integer)arrayObj);
    			
    			Log.i("yoyo", "Added " + (Integer)arrayObj + " to ds_list " + dsListIndex );
    		}               
    	}
    }
    
    // Translates a JSON response from Facebook into ds_map/ds_list data
    public void translateJSONResponse(JSONObject response, int dsMapResponse) {
    
    	JSONArray keys = response.names();    
	    for (int n = 0; n < response.length(); ++n) 
    	{
	    	String currentKey;
	    	Object currentObj;
        	try {
	        	currentKey = (String)keys.get(n);
		        currentObj = response.get(currentKey);
		    }
		    catch (org.json.JSONException e) {
	    		e.printStackTrace();
	    		continue;
	    	}
    	    if (currentObj instanceof JSONArray) 
        	{
	            // Create a new ds_list for the array
    	        int dsListIndex = RunnerJNILib.dsListCreate();
            
        	    // Add this map index to the list
            	RunnerJNILib.dsMapAddInt(dsMapResponse, currentKey, dsListIndex);
            
	            Log.i("yoyo", "Added " + dsListIndex + " to ds_map " + dsMapResponse + " for key " + currentKey);
            
    	        // Add the ds_list index to the ds_map for the current key                  
            	extractJSONDataArray((JSONArray)currentObj, dsListIndex);
	        }
    	    else if (currentObj instanceof JSONObject) 
        	{
            	// Create a new ds_map and get the index for it...
	            int subDsMap = RunnerJNILib.dsMapCreate();
            
    	        // Add this to the current ds_map
        	    RunnerJNILib.dsMapAddInt(dsMapResponse, currentKey, subDsMap);
            
            	// And recurse to parse the new dictionary
    	        translateJSONResponse((JSONObject)currentObj, subDsMap);
            
        	    Log.i("yoyo", "Added new ds_map " + subDsMap + " to ds_map " + dsMapResponse + " for key " + currentKey);
	        }
    	    else if (currentObj instanceof String) 
        	{
            	// Add the string to the map with the current key                  
            	RunnerJNILib.dsMapAddString(dsMapResponse, currentKey, (String)currentObj);
            
	            Log.i("yoyo", "Added " + (String)currentObj + " to ds_map " + dsMapResponse + " for key " + currentKey);
    	    }
    	    else if(currentObj instanceof Integer)
    	    {
    			RunnerJNILib.dsMapAddInt(dsMapResponse, currentKey, (Integer)currentObj);
    			Log.i("yoyo", "Added " + (Integer)currentObj + " to ds_map " + dsMapResponse + " for key " + currentKey);
    		}
    	}    
    }
    
    // Posts a message to the users feed based on the set of key-value pairs found in the given array: ["key0", "value0", "key1", "value1"..."keyN", "valueN"]
	// See http://developers.facebook.com/docs/reference/api/user/#posts for the set of key-value pairs expected
    public void graphRequest(String graphPath, String httpMethod, String[] keyValuePairs, int dsMapResponse) {
    
	    if ((keyValuePairs.length & 0x1) != 0) {
			throw new IllegalArgumentException("There must be an even number of strings forming key-value pairs");
		}
		if (!httpMethod.equals("GET") && !httpMethod.equals("POST")  && !httpMethod.equals("DELETE")) {
			throw new IllegalArgumentException("The httpMethod for a Facebook graph request must be one of 'GET', 'POST' or 'DELETE', value supplied was: " + httpMethod);
		}
			
		Log.i("yoyo", "Making graph API request for path: " + graphPath + " with httpMethod: " + httpMethod);
		try {
    		Bundle parameters = new Bundle();    	        
    	        
    	    // Populate the Bundle parameters with the key-value pairs			
    	    for (int n = 0; n < keyValuePairs.length; n += 2) {
							
	           	parameters.putString(keyValuePairs[n], keyValuePairs[n+1]);
    	    }
    	    						
	    	ms_asyncFacebookRunner.request(graphPath, parameters, httpMethod, new asyncRequestListener(dsMapResponse) {
	    	  	public void onComplete(JSONObject response, final Object state) {
					try {
						Log.i("yoyo", "FacebookWallPost() COMPLETE: " + response.toString());
	    	   		
						// Translate the response into ds_map/ds_list
						if (mUserData != -1) {
							RunnerJNILib.m_runnerFacebook.translateJSONResponse(response, mUserData);
						}
					}
					catch(Exception e) {
						e.printStackTrace();
					}
    		   	}
    		}, null);			
		} 
	    catch(Exception e) {
    	    e.printStackTrace();
        }		
    }
    
    // Brings up a Facebook dialog that the user can interract with
    private static Bundle msDialogBundle = null;	
    private static String msDialogType = null;
	public void dialog(String dialogType, String[] keyValuePairs, int dsMapResponse) {
	
		if ((keyValuePairs.length & 0x1) != 0) {
			throw new IllegalArgumentException("There must be an even number of strings forming key-value pairs");
		}
		if (msDialogBundle != null) {
			throw new IllegalStateException("There cannot be more than one Facebook dialog being processed at a time");
		}
				
		Log.i("yoyo", "Testing graph API wall post");
		try {
    		Bundle parameters = new Bundle();    	        
    	        
    		// Populate the Bundle parameters with the key-value pairs
    	    for (int n = 0; n < keyValuePairs.length; n += 2) {
	        	parameters.putString(keyValuePairs[n], keyValuePairs[n+1]);
    	    }    	
    	        
    		// parameters.putString("to", "me");
    		msDialogBundle = parameters;
    		msDialogType = dialogType;
    		RunnerActivity.ViewHandler.post(new Runnable() {
	    		public void run() {
		    		msFacebook.dialog(mContext, msDialogType, msDialogBundle, new dialogListener() {    			
	    				public void onComplete(Bundle values) {
    						Log.i("yoyo", "FacebookWallPost() dialog completed: " + values.toString());
    						msDialogBundle = null;
		    			}		    			
		    			public void onFacebookError(FacebookError e) {
        					Log.i("yoyo", "FacebookWallPost() dialog onFacebookError " + e.getMessage());
        					msDialogBundle = null;
						}
						public void onError(DialogError e) {
        					Log.i("yoyo", "FacebookWallPost() dialog DialogError: " + e.getMessage());
        					msDialogBundle = null;
						}
						public void onCancel() {
        					Log.i("yoyo", "FacebookWallPost() dialog user cancelled");
        					msDialogBundle = null;
						}
    				});
    			}
    		});
	    }
	    catch(Exception e) {
    		e.printStackTrace();
        }
    }	
}
