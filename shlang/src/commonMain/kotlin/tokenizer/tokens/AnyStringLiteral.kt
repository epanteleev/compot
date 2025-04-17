package tokenizer.tokens

import tokenizer.Position


sealed class AnyStringLiteral(position: Position): CToken(position)