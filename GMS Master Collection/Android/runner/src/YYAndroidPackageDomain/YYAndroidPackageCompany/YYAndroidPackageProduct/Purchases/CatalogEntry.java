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

public class CatalogEntry 
{
	public enum Availability
	{
		eContentUnavailable, eContentDownloading, eContentPurchased, eContentAvailable, eContentFailed
	}

	public Availability availability;
    public String title;		// marketplace title
    public String description;	// marketplace description
    public String purchaseID;	// marketplace identifier
    public String contentURL;	// url for the content 
    public String price;		// price of the item
    public String localPath;	// local game path of the content once downloaded
	public int index;			// the index into the master list of purchases

    public CatalogEntry(String title, String description, String purchaseID, String contentURL, String price, String localPath)
    {
        this.title = title;
        this.description = description;
        this.purchaseID = purchaseID;
        this.contentURL = contentURL;
        this.price = price;
        this.localPath = localPath;
        this.availability = Availability.eContentAvailable;		
    }
    
    public CatalogEntry() 
    {
    	this.title = "";
    	this.description = "";
        this.purchaseID = "";
        this.contentURL = "";
        this.price = "FREE";
        this.localPath = "";
        this.availability = Availability.eContentAvailable;
		this.index = 0;
    }
    
    public String purchaseStateString()
    {
    	switch (this.availability)
    	{
			case eContentAvailable:
    			return "Available";
			case eContentDownloading:
				return "Downloading";
    		case eContentPurchased:
    			return "Purchased";
			case eContentFailed:
				return "Failed";
    	}
    	return "Unavailable";
    }
}