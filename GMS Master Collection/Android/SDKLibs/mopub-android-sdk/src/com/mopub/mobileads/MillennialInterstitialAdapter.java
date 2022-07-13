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

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;
import com.millennialmedia.android.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Compatible with version 5.0.0 of the Millennial Media SDK.
 *
 * NOTE: The Millennial Media SDK does not provide any interstitial dismissal callbacks, so the
 * MoPub SDK will be unable to notify you when Millennial Media interstitials have been dismissed.
 * However, your Activity will still receive onResume; make sure that any relevant logic in your
 * dismissal callback is replicated in onResume.
 */

public class MillennialInterstitialAdapter extends BaseInterstitialAdapter {

    private MMInterstitial mMillennialInterstitial;
    private String mApid;
    private MMBroadcastReceiver mBroadcastReceiver;

    @Override
    public void init(MoPubInterstitial interstitial, String jsonParams) {
        super.init(interstitial, jsonParams);

        Context context = interstitial.getActivity();

        // The following parameters are required. Fail if they aren't set. 
        JSONObject object;
        try {
            object = (JSONObject) new JSONTokener(mJsonParams).nextValue();
            mApid = object.getString("adUnitID");
        } catch (JSONException e) {
            if (mAdapterListener != null) mAdapterListener.onNativeInterstitialFailed(this,
                    MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        MMSDK.initialize(context);
        MMSDK.setBroadcastEvents(true);

        mMillennialInterstitial = new MMInterstitial(context);
        mMillennialInterstitial.setListener(new MillennialRequestListener());

        mBroadcastReceiver = new MillennialBroadcastReceiver();
        context.registerReceiver(mBroadcastReceiver, MMBroadcastReceiver.createIntentFilter());
    }

    @Override
    public void loadInterstitial() {
        if (isInvalidated()) return;

        Log.d("MoPub", "Loading Millennial interstitial ad.");

        MMRequest request = new MMRequest();
        Location location = mInterstitial.getLocation();
        if (location != null) request.setUserLocation(location);

        mMillennialInterstitial.setMMRequest(request);
        mMillennialInterstitial.setApid(mApid);
        mMillennialInterstitial.fetch();
    }

    @Override
    public void showInterstitial() {
        if (isInvalidated()) return;

        if (mMillennialInterstitial.isAdAvailable()) {
            mMillennialInterstitial.display();
        } else {
            Log.d("MoPub", "Tried to show a Millennial interstitial ad before it finished loading. Please try again.");
        }
    }

    @Override
    public void invalidate() {
        if (mMillennialInterstitial != null) {
            mMillennialInterstitial.setListener(null);
        }
        try {
            mInterstitial.getActivity().unregisterReceiver(mBroadcastReceiver);
        } catch (Exception exception) {
            Log.d("MoPub", "Unable to unregister MMBroadcastReceiver", exception);
        }
        super.invalidate();
    }

    class MillennialRequestListener implements RequestListener {
        @Override
        public void MMAdOverlayLaunched(MMAd mmAd) {
            if (isInvalidated()) return;

            Log.d("MoPub", "Showing Millennial interstitial ad.");
            if (mAdapterListener != null) {
                mAdapterListener.onNativeInterstitialShown(MillennialInterstitialAdapter.this);
                mAdapterListener.onNativeInterstitialExpired(MillennialInterstitialAdapter.this);
            }
        }

        @Override
        public void MMAdRequestIsCaching(MMAd mmAd) {}

        @Override
        public void requestCompleted(MMAd mmAd) {
            if (isInvalidated()) return;

            if (mAdapterListener != null) {
                if (mMillennialInterstitial.isAdAvailable()) {
                    Log.d("MoPub", "Millennial interstitial ad loaded successfully.");
                    mAdapterListener.onNativeInterstitialLoaded(MillennialInterstitialAdapter.this);
                } else {
                    Log.d("MoPub", "Millennial interstitial ad failed to load.");
                    mAdapterListener.onNativeInterstitialFailed(
                            MillennialInterstitialAdapter.this,
                            MoPubErrorCode.NETWORK_INVALID_STATE);
                }
            }
        }

        @Override
        public void requestFailed(MMAd mmAd, MMException e) {
            if (isInvalidated()) return;

            Log.d("MoPub", "Millennial interstitial ad failed to load.");
            if (mAdapterListener != null) {
                mAdapterListener.onNativeInterstitialFailed(
                        MillennialInterstitialAdapter.this,
                        MoPubErrorCode.NETWORK_NO_FILL);
            }
        }
    }

    private class MillennialBroadcastReceiver extends MMBroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_OVERLAY_TAP.equals(intent.getAction())) {
                Log.d("MoPub", "Millennial interstitial ad clicked.");
                mAdapterListener.onNativeInterstitialClicked(MillennialInterstitialAdapter.this);
                // Prevent later version of Millennial from double counting clicks. Not tested.
                return;
            }

            try {
                super.onReceive(context, intent);
            } catch (NullPointerException e) {
                // Ignored.
            }
        }
    }
}
