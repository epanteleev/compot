package types

import typedesc.TypeDesc

sealed class Member {
    abstract fun cType(): CType
    abstract fun typeDesc(): TypeDesc
    abstract fun size(): Int
}

class AnonMember(private val typeDesc: TypeDesc): Member() {
    override fun cType(): AnyCStructType = typeDesc.cType().asType()
    override fun typeDesc(): TypeDesc   = typeDesc
    override fun size(): Int            = typeDesc.size()
    override fun toString(): String     = typeDesc.toString()
}

class FieldMember(val name: String, private val typeDesc: TypeDesc): Member() {
    override fun cType(): CType       = typeDesc.cType()
    override fun typeDesc(): TypeDesc = typeDesc
    override fun size(): Int          = typeDesc.size()
    override fun toString(): String   = "$typeDesc $name;"
}

class FieldDesc(private val nameToAccess: String, val index: Int, val member: Member) {
    fun cType(): CType = when (member) {
        is AnonMember  -> member.cType().fieldByIndex(nameToAccess).member.cType()
        is FieldMember -> member.cType()
    }

    fun typeDesc(): TypeDesc = member.typeDesc()
    fun size(): Int = member.size()
    fun name(): String = nameToAccess
}