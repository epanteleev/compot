#define __USE_MINGW_ANSI_STDIO 1

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <limits.h>

#include "umka_expr.h"
#include "umka_stmt.h"
#include "umka_decl.h"


// exportMark = ["*"].
bool parseExportMark(Compiler *comp)
{
    if (comp->lex.tok.kind == TOK_MUL)
    {
        lexNextForcedSemicolon(&comp->lex);
        return true;
    }
    return false;
}