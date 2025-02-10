package ir.module

import ir.module.block.Block
import ir.module.block.Label

sealed interface LabelResolver {
    fun findBlock(label: Label): Block
}