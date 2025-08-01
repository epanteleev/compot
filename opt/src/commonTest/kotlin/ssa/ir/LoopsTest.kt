package ssa.ir

import ir.instruction.*
import ir.module.SSAModule
import ir.module.block.BlockViewer
import ir.module.builder.impl.ModuleBuilder
import ir.pass.analysis.LoopDetectionPassFabric
import ir.types.I64Type
import ir.value.constant.I32Value
import ir.value.constant.I64Value
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class LoopsTest {
    private fun makeLoop(): SSAModule {
        val moduleBuilder = ModuleBuilder.create()
        val builder = moduleBuilder.createFunction("test", I64Type, arrayListOf())

        val b1 = builder.createLabel()
        val b2 = builder.createLabel()
        val b3 = builder.createLabel()
        val b4 = builder.createLabel()
        val b5 = builder.createLabel()
        val b6 = builder.createLabel()
        val b7 = builder.createLabel()

        builder.branch(b1)
        builder.switchLabel(b1)
        val cmp1 = builder.icmp(
            I32Value.of(12), IntPredicate.Ne,
            I32Value.of(43)
        )
        builder.branchCond(cmp1, b3, b2)

        builder.switchLabel(b3)
        builder.branch(b1)

        builder.switchLabel(b2)
        val cmp2 = builder.icmp(
            I32Value.of(12), IntPredicate.Ne,
            I32Value.of(43)
        )
        builder.branchCond(cmp2, b5, b4)

        builder.switchLabel(b5)
        builder.ret(I64Type, arrayOf(I64Value.of(0)))

        builder.switchLabel(b4)
        val cmp3 = builder.icmp(
            I32Value.of(12), IntPredicate.Ne,
            I32Value.of(43)
        )
        builder.branchCond(cmp3, b7, b6)

        builder.switchLabel(b6)
        builder.branch(b4)

        builder.switchLabel(b7)
        builder.branch(b1)

        return moduleBuilder.build()
    }

    @Test
    fun testLoopDetection() {
        val module = makeLoop()
        val df = module.findFunction("test")
        val loopInfo = df.analysis(LoopDetectionPassFabric)
        assertEquals(3, loopInfo.size)
        assertTrue { loopInfo[BlockViewer(1)] != null }
        assertTrue { loopInfo[BlockViewer(4)] != null }
    }
}