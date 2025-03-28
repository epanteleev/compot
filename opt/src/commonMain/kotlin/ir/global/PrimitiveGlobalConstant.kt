package ir.global

import ir.types.*
import ir.value.constant.*


sealed class PrimitiveGlobalConstant(override val name: String): GlobalConstant(name) {
    abstract override fun constant(): PrimitiveConstant
}

class U8ConstantValue(override val name: String, private val u8: UByte): PrimitiveGlobalConstant(name) {
    override fun dump(): String {
        return "@$name = constant ${type()} $u8"
    }

    override fun type(): U8Type = U8Type
    override fun constant(): U8Value = U8Value.of(u8.toByte())
}

class I8ConstantValue(override val name: String, private val i8: Byte): PrimitiveGlobalConstant(name) {
    override fun dump(): String {
        return "@$name = constant ${type()} $i8"
    }

    override fun type(): I8Type = I8Type
    override fun constant(): I8Value = I8Value.of(i8)
}

class U16ConstantValue(override val name: String, private val u16: UShort): PrimitiveGlobalConstant(name) {
    override fun dump(): String {
        return "@$name = constant ${type()} $u16"
    }

    override fun type(): U16Type = U16Type
    override fun constant(): U16Value = U16Value.of(u16.toShort())
}

class I16ConstantValue(override val name: String, private val i16: Short): PrimitiveGlobalConstant(name) {
    override fun dump(): String {
        return "@$name = constant ${type()} $i16"
    }

    override fun type(): NonTrivialType = I16Type
    override fun constant(): I16Value = I16Value.of(i16)
}

class U32ConstantValue(override val name: String, private val u32: UInt): PrimitiveGlobalConstant(name) {
    override fun dump(): String {
        return "@$name = constant ${type()} $u32"
    }

    override fun type(): U32Type = U32Type
    override fun constant(): U32Value = U32Value.of(u32.toInt())
}

class I32ConstantValue(override val name: String, private val i32: Int): PrimitiveGlobalConstant(name) {
    override fun dump(): String {
        return "@$name = constant ${type()} $i32"
    }

    override fun type(): NonTrivialType = I32Type
    override fun constant(): I32Value = I32Value.of(i32)
}

class U64ConstantValue(override val name: String, private val u64: ULong): PrimitiveGlobalConstant(name) {
    override fun dump(): String {
        return "@$name = constant ${type()} $u64"
    }

    override fun type(): NonTrivialType = U64Type
    override fun constant(): U64Value = U64Value.of(u64.toLong())
}

class I64ConstantValue(override val name: String, private val i64: Long): PrimitiveGlobalConstant(name) {
    override fun dump(): String {
        return "@$name = constant ${type()} $i64"
    }

    override fun type(): NonTrivialType = I64Type
    override fun constant(): I64Value = I64Value.of(i64)
}

class F32ConstantValue(override val name: String, private val f32: Float): PrimitiveGlobalConstant(name) {
    private fun data(): String {
        return f32.toBits().toString()
    }

    override fun dump(): String {
        return "@$name = constant ${type()} $f32; bits='${data()}'"
    }

    override fun type(): NonTrivialType = F32Type
    override fun constant(): F32Value = F32Value(f32)
}

class F64ConstantValue(override val name: String, private val f64: Double): PrimitiveGlobalConstant(name) {
    private fun data(): String {
        return f64.toBits().toString()
    }

    override fun dump(): String {
        return "@$name = constant ${type()} $f64; bits='${data()}'"
    }

    override fun type(): NonTrivialType = F64Type
    override fun constant(): F64Value = F64Value(f64)
}
