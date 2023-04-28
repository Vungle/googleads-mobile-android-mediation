package com.google.android.vungle;

import android.os.Parcel;
import android.os.Parcelable;

public class AdUnit implements Parcelable {
    private final String id;
    private final AdType type;
    private final boolean isOpenBidding;
    private final String description;
    private UnitsAdapter.AdSizeSpinnerWrapper adSizeWrapper;

    public AdUnit(String id, AdType type, boolean isOpenBidding, String description) {
        this.id = id;
        this.type = type;
        this.isOpenBidding = isOpenBidding;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public UnitsAdapter.AdSizeSpinnerWrapper getAdSizeWrapper() {
        return adSizeWrapper;
    }

    public void setAdSizeWrapper(UnitsAdapter.AdSizeSpinnerWrapper adSizeWrapper) {
        this.adSizeWrapper = adSizeWrapper;
    }

    public AdType getType() {
        return type;
    }

    public boolean isOpenBidding() {
        return isOpenBidding;
    }

    public String getDescription() {
        return description;
    }

    public enum AdType {
        RewardedAd, Interstitial, Banner, MREC, Native, RewardedInterstitial
    }

    @Override
    public String toString() {
        return "AdUnit{" +
                "id='" + id + '\'' +
                ", type=" + type +
                ", isOpenBidding=" + isOpenBidding +
                ", description=" + description +
                ", adSize=" + adSizeWrapper +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.description);
        dest.writeInt(this.isOpenBidding ? 1 : 0);
        dest.writeInt(this.type == null ? -1 : this.type.ordinal());
        dest.writeParcelable(adSizeWrapper, 0);
    }

    protected AdUnit(Parcel in) {
        this.id = in.readString();
        this.description = in.readString();
        this.isOpenBidding = in.readInt() == 1;
        int tmpType = in.readInt();
        this.type = tmpType == -1 ? null : AdType.values()[tmpType];
        this.adSizeWrapper = in.readParcelable(UnitsAdapter.AdSizeSpinnerWrapper.class.getClassLoader());
    }

    public static final Parcelable.Creator<AdUnit> CREATOR = new Parcelable.Creator<AdUnit>() {
        @Override
        public AdUnit createFromParcel(Parcel source) {
            return new AdUnit(source);
        }

        @Override
        public AdUnit[] newArray(int size) {
            return new AdUnit[size];
        }
    };
}
