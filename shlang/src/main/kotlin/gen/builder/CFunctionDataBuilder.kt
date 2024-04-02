package gen.builder

import ir.*
import ir.types.*
import ir.module.*
import gen.VarStack
import ir.module.block.Block
import ir.module.block.Label
import ir.module.builder.AnyFunctionDataBuilder


//class CFunctionDataBuilder(private val moduleBuilder: CModuleBuilder,
//                           prototype: FunctionPrototype,
//                           argumentValues: List<ArgumentValue>,
//                           blocks: BasicBlocks,
//                           private val varStack: VarStack<LocalValue>
//) : AnyFunctionDataBuilder(prototype, argumentValues, blocks) {
//    override fun build(): FunctionData {
//        TODO("Not yet implemented")
//    }
//
//    fun push() = varStack.push()
//    fun pop() = varStack.pop()
//
//
//
//    companion object {
//        fun create(moduleBuilder: CModuleBuilder, prototype: FunctionPrototype, argumentValueTokens: List<String>): CFunctionDataBuilder {
//            fun handleArguments(argumentTypeTokens: List<Type>): List<ArgumentValue> {
//                val argumentValues = arrayListOf<ArgumentValue>()
//                for ((idx, arg) in argumentTypeTokens.withIndex()) {
//                    if (arg !is NonTrivialType) {
//                        continue
//                    }
//
//                    argumentValues.add(ArgumentValue(idx, arg))
//                }
//
//                return argumentValues
//            }
//
//            fun setupNameMap(argument: List<ArgumentValue>, tokens: List<String>): VarStack<LocalValue> {
//                val nameToValue = VarStack<LocalValue>()
//                for ((arg, tok) in argument zip tokens) {
//                    nameToValue[tok] = arg
//                }
//
//                return nameToValue
//            }
//
//            val startBB     = Block.empty(Label.entry.index)
//            val basicBlocks = BasicBlocks.create(startBB)
//
//            val arguments = handleArguments(prototype.arguments())
//            val nameMap   = setupNameMap(arguments, argumentValueTokens)
//
//            return CFunctionDataBuilder(moduleBuilder, prototype, arguments, basicBlocks, nameMap)
//        }
//    }
//}