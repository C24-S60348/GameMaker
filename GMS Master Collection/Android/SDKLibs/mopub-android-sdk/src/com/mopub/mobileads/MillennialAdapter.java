/*
 * Copyright (c) 2011, MoPub Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'MoPub Inc.' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.mopub.mobileads;

import android.location.Location;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import com.millennialmedia.android.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Compatible with version 5.0.0 of the Millennial Media SDK.
 */

public class MillennialAdapter extends BaseAdapter {

    private MMAdView mMillennialAdView;

    @Override
    public void init(MoPubView view, String jsonParams) {
        super.init(view, jsonParams);
        MMSDK.initialize(view.getContext());
    }

    @Override
    public void loadAd() {
        if (isInvalidated()) return;

        // The following parameters are required. Fail if they aren't set.
        JSONObject object;
        String pubId;
        int adWidth, adHeight;
        try {
            object = (JSONObject) new JSONTokener(mJsonParams).nextValue();
            pubId = object.getString("adUnitID");
            adWidth = object.getInt("adWidth");
            adHeight = object.getInt("adHeight");
        } catch (JSONException e) {
            mMoPubView.loadFailUrl(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        mMillennialAdView = new MMAdView(mMoPubView.getContext());

        mMillennialAdView.setApid(pubId);

        mMillennialAdView.setWidth(adWidth);
        mMillennialAdView.setHeight(adHeight);

        MMRequest request = new MMRequest();
        Location location = mMoPubView.getLocation();
        if (location != null) request.setUserLocation(location);

        mMillennialAdView.setMMRequest(request);
        mMillennialAdView.setId(MMSDK.getDefaultAdId());
        RequestListener requestListener = new MillennialRequestListener();
        mMillennialAdView.getAd(requestListener);
    }

    @Override
    public void invalidate() {
        if (mMillennialAdView != null) {
            mMillennialAdView.removeAllViews();
            mMillennialAdView.setListener(null);
            mMoPubView.removeView(mMillennialAdView);
        }
        super.invalidate();
    }

    class MillennialRequestListener implements RequestListener {
        @Override
        public void MMAdOverlayLaunched(MMAd mmAd) {
            if (isInvalidated()) return;

            Log.d("MoPub", "Millennial banner ad clicked.");
            mMoPubView.registerClick();
        }

        @Override
        public void MMAdRequestIsCaching(MMAd mmAd) {}

        @Override
        public void requestCompleted(MMAd mmAd) {
            if (isInvalidated()) return;

            mMoPubView.removeAllViews();
            mMillennialAdView.setHorizontalScrollBarEnabled(false);
            mMillennialAdView.setVerticalScrollBarEnabled(false);
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.FILL_PARENT,
                    FrameLayout.LayoutParams.FILL_PARENT);
            layoutParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL;
            mMoPubView.addView(mMillennialAdView, layoutParams);

            Log.d("MoPub", "Millennial banner ad loaded successfully. Showing ad...");
            mMoPubView.nativeAdLoaded();
            mMoPubView.trackNativeImpression();
        }

        @Override
        public void requestFailed(MMAd mmAd, MMException e) {
            if (isInvalidated()) return;

            Log.d("MoPub", "Millennial banner ad failed to load.");
            mMoPubView.loadFailUrl(MoPubErrorCode.NETWORK_NO_FILL);
        }
    }
}
