package com.google.ads.mediation.vungle.util;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.vungle.ads.VungleException;

public class ErrorUtil {

    public static final String VUNGLE_SDK_ERROR_DOMAIN = "com.vungle.ads";

    /**
     * Convert the given Vungle exception into the appropriate custom error code.
     */
    @NonNull
    public static AdError getAdError(@NonNull VungleException exception) {
        return new AdError(
                exception.getExceptionCode(),
                exception.getLocalizedMessage(),
                VUNGLE_SDK_ERROR_DOMAIN
        );
    }
}
