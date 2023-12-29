package ir.platform.x64.codegen

import asm.x64.*
import ir.types.*
import ir.platform.x64.CallConvention
import ir.platform.x64.utils.Utils.case


object StoreCodegen {
    operator fun invoke(type: PrimitiveType, objFunc: ObjFunction, value: AnyOperand, pointer: AnyOperand) {
        when (type) {
            is FloatingPointType -> generateForFp(objFunc, value, pointer, type.size())
            is IntegerType, is PointerType -> generate(objFunc, value, pointer, type.size())
            else -> throw RuntimeException("Unknown type=$type, value=$value, pointer=$pointer")
        }
    }

    private fun generate(objFunc: ObjFunction, value: AnyOperand, pointer: AnyOperand, size: Int) {
        when {
            case<GPRegister, GPRegister>(value, pointer) -> {
                value   as GPRegister
                pointer as GPRegister
                objFunc.mov(size, value, Address.mem(pointer, 0))
            }

            case<Imm32, GPRegister>(value, pointer) -> {
                value   as Imm32
                pointer as GPRegister
                objFunc.mov(size, value, Address.mem(pointer, 0))
            }

            case<GPRegister, Address>(value, pointer) -> {
                value   as GPRegister
                pointer as Address
                objFunc.mov(size, value, pointer)
            }

            case<Address, Address>(value, pointer) -> {
                value   as Address
                pointer as Address
                objFunc.mov(size, value, CallConvention.temp1)
                objFunc.mov(size, CallConvention.temp1, pointer)
            }

            case<Imm32, Address>(value, pointer) -> {
                value   as Imm32
                pointer as Address
                objFunc.mov(size, value, pointer)
            }

            else -> throw RuntimeException("Unimplemented: value=$value, pointer=$pointer")
        }
    }

    private fun generateForFp(objFunc: ObjFunction, value: AnyOperand, pointer: AnyOperand, size: Int) {
        when {
            case<XmmRegister, GPRegister>(value, pointer) -> {
                value   as XmmRegister
                pointer as GPRegister
                objFunc.movf(size, value, Address.mem(pointer, 0))
            }

            case<XmmRegister, Address>(value, pointer) -> {
                value   as XmmRegister
                pointer as Address
                objFunc.movf(size, value, pointer)
            }

            case<Address, Address>(value, pointer) -> {
                value   as Address
                pointer as Address
                objFunc.movf(size, value, CallConvention.xmmTemp1)
                objFunc.movf(size, CallConvention.xmmTemp1, pointer)
            }

            else -> throw RuntimeException("Unimplemented: value=$value, pointer=$pointer")
        }
    }
}