package ir.pass.transform.auxiliary

import ir.types.*
import ir.instruction.*
import ir.instruction.matching.*
import ir.module.FunctionData
import ir.module.Module
import ir.module.block.Block
import ir.value.LocalValue
import ir.value.constant.UndefValue


internal class ReplaceByteOperands private constructor(private val cfg: FunctionData) {
    private fun transform(bb: Block, inst: Instruction): Instruction {
        inst.match(tupleDiv(value(i8()), value(i8()))) { tupleDiv: TupleDiv ->
            // Before:
            //  %resANDrem = div i8 %a, %b
            //
            // After:
            //  %extFirst  = sext %a to i16
            //  %extSecond = sext %b to i16
            //  %newDiv = div i16 %extFirst, %extSecond
            //  %projDiv = proj %newDiv, 0
            //  %projRem = proj %newDiv, 1
            //  %res = trunc %projDiv to i8
            //  %rem = trunc %projRem to i8

            val extFirst  = bb.putBefore(inst, SignExtend.sext(tupleDiv.first(), I16Type))
            val extSecond = bb.putBefore(inst, SignExtend.sext(tupleDiv.second(), I16Type))
            val newDiv    = bb.putBefore(inst, TupleDiv.div(extFirst, extSecond))


            val divProj = tupleDiv.quotient()
            if (divProj != null) {
                val proj = bb.putBefore(inst, Projection.proj(newDiv, 0))
                bb.updateUsages(divProj) {
                    bb.putBefore(inst, Truncate.trunc(proj, I8Type))
                }
                killOnDemand(bb, divProj)
            }

            val remProj  = tupleDiv.remainder() ?: throw IllegalStateException("Remainder projection is missing")
            val proj     = bb.putBefore(inst, Projection.proj(newDiv, 1))
            val truncate = bb.updateUsages(remProj) {
                bb.putBefore(inst, Truncate.trunc(proj, I8Type))
            }
            killOnDemand(bb, remProj)

            killOnDemand(bb, tupleDiv)
            return truncate
        }
        inst.match(tupleDiv(value(u8()), value(u8()))) { tupleDiv: TupleDiv ->
            // Before:
            //  %resANDrem = div u8 %a, %b
            //
            // After:
            //  %extFirst  = zext %a to u16
            //  %extSecond = zext %b to u16
            //  %newDiv = div u16 %extFirst, %extSecond
            //  %projDiv = proj %newDiv, 0
            //  %projRem = proj %newDiv, 1
            //  %res = trunc %projDiv to u8
            //  %rem = trunc %projRem to u8

            val extFirst  = bb.putBefore(inst, ZeroExtend.zext(tupleDiv.first(), U16Type))
            val extSecond = bb.putBefore(inst, ZeroExtend.zext(tupleDiv.second(), U16Type))
            val newDiv    = bb.putBefore(inst, TupleDiv.div(extFirst, extSecond))

            val divProj = tupleDiv.proj(0)
            if (divProj != null) {
                val proj = bb.putBefore(inst, Projection.proj(newDiv, 0))
                bb.updateUsages(divProj) { bb.putBefore(inst, Truncate.trunc(proj, U8Type)) }
                killOnDemand(bb, divProj)
            }

            val remProj  = tupleDiv.remainder() ?: throw IllegalStateException("Remainder projection is missing")
            val proj     = bb.putBefore(inst, Projection.proj(newDiv, 1))
            val truncate = bb.updateUsages(remProj) {
                bb.putBefore(inst, Truncate.trunc(proj, U8Type))
            }
            killOnDemand(bb, remProj)
            killOnDemand(bb, tupleDiv)
            return truncate
        }
        inst.match(tupleDiv(constant().not(), nop())) { tupleDiv: TupleDiv ->
            // TODO temporal
            val second = tupleDiv.second()
            val copy = bb.putBefore(inst, Copy.copy(second))
            bb.updateDF(inst, TupleDiv.SECOND, copy)
            return inst
        }
        inst.match(select(nop(), value(i8()), value(i8()))) { select: Select ->
            // Before:
            //  %cond = icmp <predicate> i8 %a, %b
            //  %res = select i1 %cond, i8 %onTrue, i8 %onFalse
            //
            // After:
            //  %extOnTrue  = sext %onTrue to i16
            //  %extOnFalse = sext %onFalse to i16
            //  %cond = icmp <predicate> i8 %a, %b
            //  %newSelect = select i1 %cond, i16 %extOnTrue, i16 %extOnFalse
            //  %res = trunc %newSelect to i8

            val insertPos = when(val selectCond = select.condition()) {
                is CompareInstruction -> selectCond
                else                  -> inst
            }

            val extOnTrue  = bb.putBefore(insertPos, SignExtend.sext(select.onTrue(), I16Type))
            val extOnFalse = bb.putBefore(insertPos, SignExtend.sext(select.onFalse(), I16Type))
            val newSelect  = bb.putBefore(inst, Select.select(select.condition(), I16Type, extOnTrue, extOnFalse))
            return bb.replace(inst, Truncate.trunc(newSelect, I8Type))
        }
        inst.match(select(nop(), value(u8()), value(u8()))) { select: Select ->
            // Before:
            //  %cond = icmp <predicate> u8 %a, %b
            //  %res = select i1 %cond, u8 %onTrue, u8 %onFalse
            //
            // After:
            //  %extOnTrue  = zext %onTrue to u16
            //  %extOnFalse = zext %onFalse to u16
            //  %cond = icmp <predicate> u8 %a, %b
            //  %newSelect = select i1 %cond, u16 %extOnTrue, u16 %extOnFalse
            //  %res = trunc %newSelect to u8

            val insertPos = when(val selectCond = select.condition()) {
                is CompareInstruction -> selectCond
                else                  -> inst
            }

            val extOnTrue  = bb.putBefore(insertPos, ZeroExtend.zext(select.onTrue(), U16Type))
            val extOnFalse = bb.putBefore(insertPos, ZeroExtend.zext(select.onFalse(), U16Type))
            val newSelect  = bb.putBefore(inst, Select.select(select.condition(), U16Type, extOnTrue, extOnFalse))
            return bb.replace(inst, Truncate.trunc(newSelect, U8Type))
        }
        return inst
    }

    private fun pass() {
        for (bb in cfg) {
            bb.transform { inst -> transform(bb, inst) }
        }
    }

    private fun killOnDemand(bb: Block, instruction: LocalValue) {
        instruction as Instruction
        if (instruction.usedIn().isEmpty()) { //TODO Need DCE
            bb.kill(instruction, UndefValue) // TODO bb may not contain pointer
        }
    }

    companion object {
        fun run(module: Module): Module {
            for (fn in module.functions()) {
                ReplaceByteOperands(fn).pass()
            }
            return module
        }
    }
}