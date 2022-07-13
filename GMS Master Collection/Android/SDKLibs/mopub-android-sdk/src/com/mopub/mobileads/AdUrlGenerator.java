package com.mopub.mobileads;

import android.content.Context;
import android.content.res.Configuration;
import android.location.Location;
import android.net.Uri;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import com.mopub.mobileads.util.DateAndTime;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;

public class AdUrlGenerator {
    public static final String DEVICE_ORIENTATION_PORTRAIT = "p";
    public static final String DEVICE_ORIENTATION_LANDSCAPE = "l";
    public static final String DEVICE_ORIENTATION_SQUARE = "s";
    public static final String DEVICE_ORIENTATION_UNKNOWN = "u";
    private Context mContext;
    private StringBuilder mStringBuilder;
    private boolean mFirstParam;
    private TelephonyManager mTelephonyManager;

    public AdUrlGenerator(Context context) {
        mContext = context;
        mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
    }

    public String generateUrlString(String serverHostname, String adUnitId, String originalKeywords, Location location) {
        mStringBuilder = new StringBuilder("http://" + serverHostname + MoPubView.AD_HANDLER);
        mFirstParam = true;

        setApiVersion("6");

        setAdUnitId(adUnitId);

        setSdkVersion(MoPub.SDK_VERSION);

        setUdid(Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID));

        String keywords = addKeyword(originalKeywords, getFacebookKeyword(mContext));
        setKeywords(keywords);

        setLocation(location);

        setTimezone(getTimeZoneOffsetString());

        setOrientation(mContext.getResources().getConfiguration().orientation);

        setDensity(mContext.getResources().getDisplayMetrics().density);

        setMraidFlag(detectIsMraidSupported());

        String networkOperator = mTelephonyManager.getNetworkOperator();
        if (mTelephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA &&
                mTelephonyManager.getSimState() == TelephonyManager.SIM_STATE_READY) {
            networkOperator = mTelephonyManager.getSimOperator();
        }
        setMccCode(networkOperator);
        setMncCode(networkOperator);

        setIsoCountryCode(mTelephonyManager.getNetworkCountryIso());
        setCarrierName(mTelephonyManager.getNetworkOperatorName());

        return mStringBuilder.toString();
    }

    private void setApiVersion(String apiVersion) {
        addParam("v", apiVersion);
    }

    private void setAdUnitId(String adUnitId) {
        addParam("id", adUnitId);
    }

    private void setSdkVersion(String sdkVersion) {
        addParam("nv", sdkVersion);
    }

    private void setUdid(String udid) {
        String udidDigest = (udid == null) ? "" : Utils.sha1(udid);
        addParam("udid", "sha:" + udidDigest);
    }

    private void setKeywords(String keywords) {
        if (keywords != null && keywords.length() > 0) {
            addParam("q", keywords);
        }
    }

    private void setLocation(Location location) {
        if (location != null) {
            addParam("ll", location.getLatitude() + "," + location.getLongitude());
        }
    }

    private void setTimezone(String timeZoneOffsetString) {
        addParam("z", timeZoneOffsetString);
    }

    private void setOrientation(int orientation) {
        String orString = DEVICE_ORIENTATION_UNKNOWN;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            orString = DEVICE_ORIENTATION_PORTRAIT;
        } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            orString = DEVICE_ORIENTATION_LANDSCAPE;
        } else if (orientation == Configuration.ORIENTATION_SQUARE) {
            orString = DEVICE_ORIENTATION_SQUARE;
        }
        addParam("o", orString);
    }

    private void setDensity(float density) {
        addParam("sc_a", "" + density);
    }

    private void setMraidFlag(boolean mraid) {
        if (mraid) addParam("mr", "1");
    }

    private void setMccCode(String networkOperator) {
        String mcc = networkOperator == null ? "" : networkOperator.substring(0, mncPortionLength(networkOperator));
        addParam("mcc", mcc);
    }

    private void setMncCode(String networkOperator) {
        String mnc = networkOperator == null ? "" : networkOperator.substring(mncPortionLength(networkOperator));
        addParam("mnc", mnc);
    }

    private void setIsoCountryCode(String networkCountryIso) {
        addParam("iso", networkCountryIso);
    }

    private void setCarrierName(String networkOperatorName) {
        addParam("cn", networkOperatorName);
    }

    private void addParam(String key, String value) {
        mStringBuilder.append(getParamDelimiter());
        mStringBuilder.append(key);
        mStringBuilder.append("=");
        String nonNullValue = value != null ? value : "";
        mStringBuilder.append(Uri.encode(nonNullValue));
    }

    private String getParamDelimiter() {
        if (mFirstParam) {
            mFirstParam = false;
            return "?";
        }
        return "&";
    }

    private boolean detectIsMraidSupported() {
        boolean mraid = true;
        try {
            Class.forName("com.mopub.mobileads.MraidView");
        } catch (ClassNotFoundException e) {
            mraid = false;
        }
        return mraid;
    }

    private int mncPortionLength(String networkOperator) {
        return Math.min(3, networkOperator.length());
    }

    private static String getTimeZoneOffsetString() {
        SimpleDateFormat format = new SimpleDateFormat("Z");
        format.setTimeZone(DateAndTime.localTimeZone());
        return format.format(DateAndTime.now());
    }

    private static String getFacebookKeyword(Context context) {
        try {
            Class<?> facebookKeywordProviderClass = Class.forName("com.mopub.mobileads.FacebookKeywordProvider");
            Method getKeywordMethod = facebookKeywordProviderClass.getMethod("getKeyword", Context.class);

            return (String) getKeywordMethod.invoke(facebookKeywordProviderClass, context);
        } catch (Exception exception) {
            return null;
        }
    }

    private static String addKeyword(String keywords, String addition) {
        if (addition == null || addition.length() == 0) {
            return keywords;
        } else if (keywords == null || keywords.length() == 0) {
            return addition;
        } else {
            return keywords + "," + addition;
        }
    }
}
