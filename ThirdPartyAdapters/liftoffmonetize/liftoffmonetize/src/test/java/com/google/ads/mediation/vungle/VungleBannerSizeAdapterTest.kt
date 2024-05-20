package com.google.ads.mediation.vungle

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.ads.AdSize
import com.google.common.truth.Truth.assertThat
import com.vungle.ads.VungleAdSize
import com.vungle.mediation.VungleInterstitialAdapter
import org.junit.Test
import org.junit.runner.RunWith

/** Unit tests for the utility function getVungleBannerAdSizeFromGoogleAdSize() */
@RunWith(AndroidJUnit4::class)
class VungleBannerSizeAdapterTest {

  private val context = ApplicationProvider.getApplicationContext<Context>()

  @Test
  fun getVungleBannerAdSize_forGoogleSize300By50_returnsLiftoffSizeBannerShort() {
    val adSize = AdSize(300, 50)
    val liftoffBannerSize =
      VungleInterstitialAdapter.getVungleBannerAdSizeFromGoogleAdSize(context, adSize)

    assertThat(liftoffBannerSize.width).isEqualTo(adSize.width)
    assertThat(liftoffBannerSize.height).isEqualTo(adSize.height)
  }

  @Test
  fun getVungleBannerAdSize_forGoogleSizeRegularBanner_returnsLiftoffSizeRegularBanner() {
    val liftoffBannerSize =
      VungleInterstitialAdapter.getVungleBannerAdSizeFromGoogleAdSize(context, AdSize.BANNER)

    assertThat(liftoffBannerSize.width).isEqualTo(AdSize.BANNER.width)
    assertThat(liftoffBannerSize.height).isEqualTo(AdSize.BANNER.height)
  }

  @Test
  fun getVungleBannerAdSize_forGoogleSizeLeaderboard_returnsLiftoffSizeLeaderboard() {
    val liftoffBannerSize =
      VungleInterstitialAdapter.getVungleBannerAdSizeFromGoogleAdSize(context, AdSize.LEADERBOARD)

    assertThat(liftoffBannerSize.width).isEqualTo(AdSize.LEADERBOARD.width)
    assertThat(liftoffBannerSize.height).isEqualTo(AdSize.LEADERBOARD.height)
  }

  @Test
  fun getVungleBannerAdSize_forGoogleSizeMediumRectangle_returnsLiftoffSizeMediumRectangle() {
    val liftoffBannerSize =
      VungleInterstitialAdapter.getVungleBannerAdSizeFromGoogleAdSize(
        context,
        AdSize.MEDIUM_RECTANGLE,
      )

    assertThat(liftoffBannerSize.width).isEqualTo(AdSize.MEDIUM_RECTANGLE.width)
    assertThat(liftoffBannerSize.height).isEqualTo(AdSize.MEDIUM_RECTANGLE.height)
  }

  @Test
  fun getVungleBannerAdSize_forUnsupportedGoogleBannerSize_returnsNull() {
    val liftoffBannerSize =
      VungleInterstitialAdapter.getVungleBannerAdSizeFromGoogleAdSize(
        context,
        AdSize.WIDE_SKYSCRAPER,
      )

    assertThat(liftoffBannerSize).isNotNull()
    assertThat(liftoffBannerSize.width).isEqualTo(AdSize.WIDE_SKYSCRAPER.width)
    assertThat(liftoffBannerSize.height).isEqualTo(AdSize.WIDE_SKYSCRAPER.height)
  }
}
