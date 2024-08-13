package ir.module


enum class Sensitivity {
    CONTROL_FLOW,
    VALUE,
    CONTROL_FLOW_AND_VALUE
}

data class MutationMarker(private val cf: Long, private val df: Long) {
    fun mutationType(other: MutationMarker): Sensitivity? = when {
        cf != other.cf && df != other.df -> Sensitivity.CONTROL_FLOW_AND_VALUE
        cf != other.cf                   -> Sensitivity.CONTROL_FLOW
        df != other.df                   -> Sensitivity.VALUE
        else -> null
    }
}