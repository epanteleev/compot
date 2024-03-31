package types

interface TypeResolver {
    fun resolve(type: Type): Type
}

object UncachedTypeResolver : TypeResolver {
    override fun resolve(type: Type): Type = TODO()


}

data class ResolveCache(val unused: Int) : TypeResolver {
    private val resolveCache = LinkedHashMap<Type, Type>()

    override fun resolve(type: Type): Type {
        if (type !in resolveCache) {
            resolveCache[type] = UncachedTypeResolver.resolve(type)
        }
        return resolveCache[type]!!
    }
}