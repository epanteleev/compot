package gen.builder

import ir.ArgumentValue
import ir.FunctionPrototype
import ir.module.BasicBlocks
import ir.module.FunctionData
import ir.module.builder.AnyFunctionDataBuilder

class CFunctionModuleBuilder(prototype: FunctionPrototype,
                             argumentValues: List<ArgumentValue>, blocks: BasicBlocks
) : AnyFunctionDataBuilder(prototype, argumentValues, blocks) {
    override fun build(): FunctionData {
        TODO("Not yet implemented")
    }
}