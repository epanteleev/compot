package preprocess

import tokenizer.Token

data class Macros(val name: String, val value: List<Token>)