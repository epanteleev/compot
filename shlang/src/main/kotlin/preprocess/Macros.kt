package preprocess

import tokenizer.CToken

data class Macros(val name: String, val value: List<CToken>)