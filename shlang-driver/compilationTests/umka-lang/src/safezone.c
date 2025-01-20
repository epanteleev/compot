#define __USE_MINGW_ANSI_STDIO 1

#include <stdlib.h>
#include <string.h>
#include <limits.h>
#include <math.h>
#include <inttypes.h>

#include "umka_const.h"


void constAssign(Consts *consts, void *lhs, Const *rhs, TypeKind typeKind, int size)
{
    if (typeOverflow(typeKind, *rhs))
        consts->error->handler(consts->error->context, "Overflow in assignment to %s", typeKindSpelling(typeKind));

    switch (typeKind)
    {
        case TYPE_INT8:         *(int8_t        *)lhs = rhs->intVal;         break;
        case TYPE_INT16:        *(int16_t       *)lhs = rhs->intVal;         break;
        case TYPE_INT32:        *(int32_t       *)lhs = rhs->intVal;         break;
        case TYPE_INT:          *(int64_t       *)lhs = rhs->intVal;         break;
        case TYPE_UINT8:        *(uint8_t       *)lhs = rhs->intVal;         break;
        case TYPE_UINT16:       *(uint16_t      *)lhs = rhs->intVal;         break;
        case TYPE_UINT32:       *(uint32_t      *)lhs = rhs->intVal;         break;
        case TYPE_UINT:         *(uint64_t      *)lhs = rhs->uintVal;        break;
        case TYPE_BOOL:         *(bool          *)lhs = rhs->intVal;         break;
        case TYPE_CHAR:         *(unsigned char *)lhs = rhs->intVal;         break;
        case TYPE_REAL32:       *(float         *)lhs = rhs->realVal;        break;
        case TYPE_REAL:         *(double        *)lhs = rhs->realVal;        break;
        case TYPE_PTR:          *(void *        *)lhs = rhs->ptrVal;         break;
        case TYPE_WEAKPTR:      *(uint64_t      *)lhs = rhs->weakPtrVal;     break;
        case TYPE_STR:          *(void *        *)lhs = rhs->ptrVal;         break;
        case TYPE_ARRAY:
        case TYPE_DYNARRAY:
        case TYPE_STRUCT:
        case TYPE_INTERFACE:
        case TYPE_CLOSURE:      memcpy(lhs, rhs->ptrVal, size);              break;
        case TYPE_FIBER:        *(void *        *)lhs = rhs->ptrVal;         break;
        case TYPE_FN:           *(int64_t       *)lhs = rhs->intVal;         break;

        default:          consts->error->handler(consts->error->context, "Illegal type"); return;
    }
}