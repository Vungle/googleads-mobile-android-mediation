package com.google.ads.mediation.vungle

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.vungle.VungleInitializer.getInstance
import com.google.android.gms.ads.RequestConfiguration
import com.google.common.truth.Truth.assertThat
import com.vungle.ads.InitializationListener
import com.vungle.ads.VunglePrivacySettings
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock

/** Tests for [VungleInitializer]. */
@RunWith(AndroidJUnit4::class)
class VungleInitializerTest {

  private lateinit var initializer: VungleInitializer

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val mockVungleInitializationListener =
    mock<InitializationListener>()
  private val mockSdkWrapper = mock<SdkWrapper>() { on { isInitialized() } doReturn false }

  @Before
  fun setUp() {
    VungleSdkWrapper.delegate = mockSdkWrapper
    initializer = getInstance()
  }

  @Test
  fun multipleCallsToGetInstanceReturnsTheSameInstance() {
    assertThat(initializer).isEqualTo(getInstance())
  }

  @Test
  fun initialize_callsInit() {
    initializer.initialize(TEST_APP_ID_1, context, mockVungleInitializationListener)

    verify(mockSdkWrapper).init(eq(context), eq(TEST_APP_ID_1), eq(mockVungleInitializationListener))

    // Call onSuccess to clear the init listeners.
    mockVungleInitializationListener.onSuccess()
  }

  @Test
  fun updateCoppaStatus_whenChildDirectedIsTrue_setsCoppaStatusTrue() {
    Mockito.mockStatic(VunglePrivacySettings::class.java).use {
      initializer.updateCoppaStatus(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)

      it.verify { VunglePrivacySettings.setCOPPAStatus(true) }
      it.verify({ VunglePrivacySettings.setCOPPAStatus(false) }, never())
    }
  }

  @Test
  fun updateCoppaStatus_whenChildDirectedIsFalse_setsCoppaStatusFalse() {
    Mockito.mockStatic(VunglePrivacySettings::class.java).use {
      initializer.updateCoppaStatus(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE)

      it.verify { VunglePrivacySettings.setCOPPAStatus(false) }
      it.verify({ VunglePrivacySettings.setCOPPAStatus(true) }, never())
    }
  }

  @Test
  fun updateCoppaStatus_whenChildDirectedTagIsUnspecified_doesntSetCoppaStatus() {
    Mockito.mockStatic(VunglePrivacySettings::class.java).use {
      initializer.updateCoppaStatus(
        RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
      )

      it.verify({ VunglePrivacySettings.setCOPPAStatus(any()) }, never())
    }
  }

  private companion object {
    const val TEST_APP_ID_1 = "testAppId1"
  }
}
