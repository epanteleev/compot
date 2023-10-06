package ir.utils

import ir.block.Block

interface AbstractLocation {
    val index: Int
}

data class Location(val block: Block, override val index: Int): AbstractLocation {
    override fun toString(): String {
        return "[${block}:${index}]"
    }
}

data class OrderedLocation(val block: Block, override val index: Int): AbstractLocation {
    override fun toString(): String {
        return "[${block}: order=${index} ]"
    }

    operator fun compareTo(other: OrderedLocation): Int {
        return index.compareTo(other.index)
    }
}