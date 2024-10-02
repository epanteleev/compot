package asm.x64

interface ObjBuilder {
    fun byte(value: Byte)
    fun short(value: Short)
    fun long(value: Int)
    fun quad(label: ObjLabel)
    fun quad(value: Long)
    fun string(value: String)
    fun ascii(value: String)
}