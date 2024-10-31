package ir.platform

import ir.platform.common.TargetPlatform

interface MacroAssembler {
    fun platform(): TargetPlatform
}