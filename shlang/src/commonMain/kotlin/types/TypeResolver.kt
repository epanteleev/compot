package types

interface TypeResolver {
    fun resolve(type: CType): CType
}

object UncachedTypeResolver : TypeResolver {
    override fun resolve(type: CType): CType = TODO()


}

data class ResolveCache(val unused: Int) : TypeResolver {
    private val resolveCache = LinkedHashMap<CType, CType>()

    override fun resolve(type: CType): CType {
        if (type !in resolveCache) {
            resolveCache[type] = UncachedTypeResolver.resolve(type)
        }
        return resolveCache[type]!!
    }
}