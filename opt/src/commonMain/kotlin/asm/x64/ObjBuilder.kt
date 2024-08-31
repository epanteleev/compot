package asm.x64

interface ObjBuilder {
    fun byte(value: String)
    fun short(value: String)
    fun long(value: String)
    fun quad(value: String)
    fun string(value: String)
    fun ascii(value: String)
}