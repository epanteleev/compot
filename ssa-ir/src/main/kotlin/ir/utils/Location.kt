package ir.utils

import ir.BasicBlock

interface AbstractLocation {
    val index: Int
}

data class Location(val block: BasicBlock, override val index: Int): AbstractLocation {
    override fun toString(): String {
        return "[${block}:${index}]"
    }
}

data class OrderedLocation(val block: BasicBlock, val order: Int, override val index: Int): AbstractLocation {
    override fun toString(): String {
        return "[${block}:${index} {$order} ]"
    }

    operator fun compareTo(other: OrderedLocation): Int {
        return order.compareTo(other.order)
    }
}