package common

actual inline fun assertion(value: Boolean, lazyMessage: () -> Any) {
    assert(value, lazyMessage)
}