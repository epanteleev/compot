package common

import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class)
actual inline fun assertion(value: Boolean, lazyMessage: () -> Any) {
    assert(value, lazyMessage)
}