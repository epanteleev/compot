package gen.builder

import gen.VarStack
import ir.ArgumentValue
import ir.FunctionPrototype
import ir.LocalValue
import ir.module.BasicBlocks
import ir.module.FunctionData
import ir.module.builder.AnyFunctionDataBuilder


class CFunctionModuleBuilder(prototype: FunctionPrototype,
                             argumentValues: List<ArgumentValue>,
                             blocks: BasicBlocks
) : AnyFunctionDataBuilder(prototype, argumentValues, blocks) {
    val varStack = VarStack<LocalValue>()

    override fun build(): FunctionData {
        TODO("Not yet implemented")
    }
}