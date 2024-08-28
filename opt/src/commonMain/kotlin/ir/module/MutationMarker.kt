package ir.module


enum class Sensitivity {
    CONTROL_FLOW,
    DATA_FLOW,
    CONTROL_AND_DATA_FLOW;

    fun isIntersection(other: Sensitivity): Boolean {
        if (other == this) {
            return true
        }

        return when (this) {
            CONTROL_FLOW -> other == CONTROL_AND_DATA_FLOW
            DATA_FLOW    -> other == CONTROL_AND_DATA_FLOW
            else -> false
        }
    }
}

data class MutationMarker(private val cf: Long, private val df: Long) {
    fun mutationType(other: MutationMarker): Sensitivity? = when {
        cf != other.cf && df != other.df -> Sensitivity.CONTROL_AND_DATA_FLOW
        cf != other.cf                   -> Sensitivity.CONTROL_FLOW
        df != other.df                   -> Sensitivity.DATA_FLOW
        else -> null
    }
}