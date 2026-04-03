package com.nosferatu.launcher.reader

/**
 * Simple test injector used to toggle test-friendly behavior in `ReaderActivity`.
 * Production behavior is unchanged unless `useFakeNavigator` is set to true
 * by instrumented tests prior to launching the activity.
 */
object TestReaderInjector {
    /** When true, `ReaderActivity` will not create real Readium navigators/publications. */
    @Volatile
    var useFakeNavigator: Boolean = false

    /** Optional fake metadata that tests can set (used when `useFakeNavigator` is enabled). */
    var fakeTitle: String? = null
    var fakeAuthor: String? = null
}
