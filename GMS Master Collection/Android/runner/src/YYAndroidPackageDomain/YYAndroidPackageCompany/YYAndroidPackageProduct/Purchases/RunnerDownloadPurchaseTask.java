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

import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.HttpURLConnection;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import com.yoyogames.runner.RunnerJNILib;

/**
 * This class used to retrieve the set of in-app products available for the application
 */
public class RunnerDownloadPurchaseTask extends AsyncTask<String, Void, String[]>
{
	public boolean mSuccess;
	public String mContentId;
	public String[] mExtractedFileNames;
	
	// Since multiple files could be downloading in the background at once we need difference temporary files to download to
	private static String GetTempDownloadFilename(String url)
    {
    	// Let's not assume the url contains a filename, it could be a url that the server uses to look up a DB    
    	String md5fileName = IRunnerBilling.md5encode(url);
     	String fileName = RunnerJNILib.GetSaveFileName(md5fileName + ".zip");
    	
    	return fileName;
    }

    @Override
    protected void onPostExecute(String[] _extractedFileNames)
    {
    	// Inform the runner activity that the task is complete
    	RunnerActivity.CurrentActivity.RunnerBilling().purchaseContentDownloadedCallback(mSuccess, this);
    }

	// Parameters are _contentURL and _localDestination
	@Override
    protected String[] doInBackground(String... vals)
    {
		// Assume we're going to succeed and set on failure
		mSuccess = true;

    	// Grab the content ID for later use
    	mContentId = vals[2];
    
    	Log.i("yoyo", "Downloading compressed content from: " + vals[0] + " to: " + vals[1] + " for content: " + vals[2]);
    	try {
        	URL url = new URL(vals[0]);
        	HttpURLConnection c = (HttpURLConnection) url.openConnection();
        	c.setRequestMethod("GET");
        	c.setDoOutput(true);
        	c.connect();

			String tempFilename = GetTempDownloadFilename(vals[0]);
			File outputFile = new File(tempFilename);	
        	Log.i("yoyo", "Downloading to temp file " + tempFilename);
        	FileOutputStream fos = new FileOutputStream(outputFile);

        	InputStream is = c.getInputStream();

        	byte[] buffer = new byte[1024];
        	int len1 = 0;
        	while ((len1 = is.read(buffer)) != -1) {
            	fos.write(buffer, 0, len1);
        	}
        	fos.close();
        	is.close();
        	
        	// Unzip the contents to the local destination path, creating the relevant folders if necessary
        	String delimiter = "/";
        	String[] folders = vals[1].split(delimiter);
        	
        	String currentFolder = new String();
        	for (String folder : folders) 
        	{
        		currentFolder = currentFolder.concat(folder + "/");
        		
	        	String fullDestinationPath = RunnerJNILib.GetSaveFileName(currentFolder);
	        	
	        	File destPath = new File(fullDestinationPath); 	        	
	        	if (!destPath.isDirectory()) {
        			if (!destPath.mkdir()) {
        				Log.i("yoyo", "Failed to create destination directory: " + fullDestinationPath);
        			}
        		}
	        }
			// Actually do the unzipping
        	mExtractedFileNames = RunnerJNILib.ExpandCompressedFile(vals[1], tempFilename);
        	
        	// Delete the temporary local file
        	outputFile.delete();

			Log.i("yoyo", "Compressed content download complete!");
    	} 
    	catch (IOException e) {
        	Log.i("yoyo", "Error: " + e);
			mSuccess = false;
    	}
    	catch (Exception e) {
    		Log.i("yoyo", "Error: Failed to download compressed content :(");
			mSuccess = false;
    	}    	
    	
    	return mExtractedFileNames;
    }
}

