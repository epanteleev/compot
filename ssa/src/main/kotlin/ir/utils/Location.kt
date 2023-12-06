package ir.utils

import ir.module.block.Block

interface AbstractLocation {
    val index: Int
    val block: Block

    fun thisPlace(bb: Block, idx: Int): Boolean {
        return bb == block && idx == index
    }
}

data class Location(override val block: Block, override val index: Int): AbstractLocation {
    override fun toString(): String {
        return "[${block}:${index}]"
    }
}

data class OrderedLocation(override val block: Block, override val index: Int, val order: Int): AbstractLocation {
    override fun toString(): String {
        return "[${block}:$index order=$order]"
    }

    operator fun compareTo(other: OrderedLocation): Int {
        return order.compareTo(other.order)
    }
}