package types

import typedesc.TypeDesc

sealed class Member(protected val typeDesc: TypeDesc) {
    abstract fun cType(): SizedType
    fun typeDesc(): TypeDesc   = typeDesc
    fun size(): Int            = typeDesc.asType<SizedType>().size()
    fun alignmentOf(): Int     = typeDesc.asType<SizedType>().alignmentOf()
}

class AnonMember(typeDesc: TypeDesc): Member(typeDesc) {
    override fun cType(): AnyCStructType = typeDesc.cType().asType()
    override fun toString(): String = typeDesc.toString()
}

class FieldMember(val name: String, typeDesc: TypeDesc): Member(typeDesc) {
    override fun cType(): SizedType = typeDesc.cType().asType()
    override fun toString(): String   = "$typeDesc $name;"
}

class FieldDesc(private val nameToAccess: String, val index: Int, private val member: Member) {
    fun cType(): CType = when (member) {
        is AnonMember  -> member.cType().fieldByName(nameToAccess).member.cType()
        is FieldMember -> member.cType()
    }

    fun typeDesc(): TypeDesc = member.typeDesc()
    fun size(): Int = member.size()
    fun name(): String = nameToAccess
}