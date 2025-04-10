package tokenizer.tokens

import preprocess.Hideset

sealed interface MacrosName {
    fun hideset(): Hideset

    fun contains(name: String): Boolean {
        return hideset().contains(name)
    }

    fun add(name: String) {
        hideset().add(name)
    }
}