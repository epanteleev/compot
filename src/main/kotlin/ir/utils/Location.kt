package ir.utils

import ir.BasicBlock

data class Location(val block: BasicBlock, val index: Int) {
    override fun toString(): String {
        return "[${block}:${index}]"
    }
}