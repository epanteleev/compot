package types

import tokenizer.Position
import typedesc.TypeDesc

sealed class Member(protected val typeDesc: TypeDesc) {
    abstract fun cType(): CompletedType
    fun typeDesc(): TypeDesc   = typeDesc
    fun size(): Int            = typeDesc.asType<CompletedType>().size()
    fun alignmentOf(): Int     = typeDesc.asType<CompletedType>().alignmentOf()
}

class AnonMember(typeDesc: TypeDesc): Member(typeDesc) {
    override fun cType(): AnyCStructType = typeDesc.cType().asType(Position.UNKNOWN)
    override fun toString(): String = typeDesc.toString()
}

class FieldMember(val name: String, typeDesc: TypeDesc): Member(typeDesc) {
    override fun cType(): CompletedType = typeDesc.cType().asType(Position.UNKNOWN)
    override fun toString(): String   = "$typeDesc $name;"
}

class FieldDesc(private val nameToAccess: String, val index: Int, private val member: Member) {
    fun cType(): CType = when (member) {
        is AnonMember  -> member.cType().fieldByName(nameToAccess).member.cType()
        is FieldMember -> member.cType()
    }

    fun member(): Member = member
    fun typeDesc(): TypeDesc = member.typeDesc()
    fun size(): Int = member.size()
    fun name(): String = nameToAccess
}