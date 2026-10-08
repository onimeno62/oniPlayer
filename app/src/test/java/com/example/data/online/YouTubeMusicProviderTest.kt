package com.example.data.online

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class YouTubeMusicProviderTest {
    @Test
    fun blankApiKeyFailsWithoutNetwork() = runBlocking {
        val result = YouTubeMusicProvider("").search("test")
        val failure = result as ProviderResult.Failure
        assertEquals(ProviderFailureKind.Unauthorized, failure.kind)
    }

    @Test
    fun providerDoesNotClaimUnsupportedPlaybackCapabilities() {
        val capabilities = YouTubeMusicProvider("test").capabilities
        assertEquals(false, capabilities.supports(ProviderCapability.STREAM))
        assertEquals(false, capabilities.supports(ProviderCapability.DOWNLOAD))
    }
}
