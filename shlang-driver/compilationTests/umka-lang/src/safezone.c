#define __USE_MINGW_ANSI_STDIO 1

#ifdef _MSC_VER  // MSVC++ only
    #define FORCE_INLINE __forceinline
#else
    #define FORCE_INLINE __attribute__((always_inline)) inline
#endif

//#define UMKA_STR_DEBUG
//#define UMKA_REF_CNT_DEBUG
//#define UMKA_DETAILED_LEAK_INFO


#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <stdarg.h>
#include <math.h>
#include <limits.h>
#include <ctype.h>
#include <inttypes.h>

#include "umka_vm.h"


/*
Virtual machine stack layout (64-bit slots):

0   ...
    ...                                              <- Stack origin
    ...
    Temporary value 1                                <- Stack top
    Temporary value 0
    ...
    Local variable 1
    ...
    Local variable 1
    Local variable 0
    ...
    Local variable 0
    Parameter layout table pointer
    Stack frame ref count
    Caller's stack frame base pointer                <- Stack frame base pointer
    Return address
    Parameter N
    ...
    Parameter N
    Parameter N - 1
    ...
    Parameter N - 1
    ...                                              <- Stack origin + size
*/


static const char *opcodeSpelling [] =
{
    "NOP",
    "PUSH",
    "PUSH_ZERO",
    "PUSH_LOCAL_PTR",
    "PUSH_LOCAL_PTR_ZERO",
    "PUSH_LOCAL",
    "PUSH_REG",
    "PUSH_UPVALUE",
    "POP",
    "POP_REG",
    "DUP",
    "SWAP",
    "ZERO",
    "DEREF",
    "ASSIGN",
    "ASSIGN_PARAM",
    "CHANGE_REF_CNT",
    "CHANGE_REF_CNT_GLOBAL",
    "CHANGE_REF_CNT_LOCAL",
    "CHANGE_REF_CNT_ASSIGN",
    "UNARY",
    "BINARY",
    "GET_ARRAY_PTR",
    "GET_DYNARRAY_PTR",
    "GET_MAP_PTR",
    "GET_FIELD_PTR",
    "ASSERT_TYPE",
    "ASSERT_RANGE",
    "WEAKEN_PTR",
    "STRENGTHEN_PTR",
    "GOTO",
    "GOTO_IF",
    "GOTO_IF_NOT",
    "CALL",
    "CALL_INDIRECT",
    "CALL_EXTERN",
    "CALL_BUILTIN",
    "RETURN",
    "ENTER_FRAME",
    "LEAVE_FRAME",
    "HALT"
};


static const char *builtinSpelling [] =
{
    "printf",
    "fprintf",
    "sprintf",
    "scanf",
    "fscanf",
    "sscanf",
    "real",
    "real_lhs",
    "round",
    "trunc",
    "ceil",
    "floor",
    "abs",
    "fabs",
    "sqrt",
    "sin",
    "cos",
    "atan",
    "atan2",
    "exp",
    "log",
    "new",
    "make",
    "makefromarr",
    "makefromstr",
    "maketoarr",
    "maketostr",
    "copy",
    "append",
    "insert",
    "delete",
    "slice",
    "sort",
    "sortfast",
    "len",
    "cap",
    "sizeof",
    "sizeofself",
    "selfhasptr",
    "selftypeeq",
    "typeptr",
    "valid",
    "validkey",
    "keys",
    "resume",
    "memusage",
    "exit"
};


static const char *regSpelling [] =
{
    "RESULT",
    "SELF",
    "HEAP_COPY",
    "SWITCH_EXPR",
    "EXPR_LIST"
};


// Memory management

void pageInit(HeapPages *pages, Fiber *fiber, Error *error)
{
    pages->first = pages->last = NULL;
    pages->freeId = 1;
    pages->totalSize = 0;
    pages->fiber = fiber;
    pages->error = error;
}


void pageFree(HeapPages *pages, bool warnLeak)
{
    HeapPage *page = pages->first;
    while (page)
    {
        HeapPage *next = page->next;
        if (page->ptr)
        {
            // Report memory leaks
            if (warnLeak)
            {
                fprintf(stderr, "Warning: Memory leak at %p (%d refs)\n", page->ptr, page->refCnt);

#ifdef UMKA_DETAILED_LEAK_INFO
                for (int i = 0; i < page->numOccupiedChunks; i++)
                {
                    HeapChunkHeader *chunk = (HeapChunkHeader *)((char *)page->ptr + i * page->chunkSize);
                    if (chunk->refCnt == 0)
                        continue;

                    DebugInfo *debug = &pages->fiber->debugPerInstr[chunk->ip];
                    fprintf(stderr, "    Chunk allocated in %s: %s (%d)\n", debug->fnName, debug->fileName, debug->line);
                }
 #endif
            }

            // Call custom deallocators, if any
            for (int i = 0; i < page->numOccupiedChunks && page->numChunksWithOnFree > 0; i++)
            {
                HeapChunkHeader *chunk = (HeapChunkHeader *)((char *)page->ptr + i * page->chunkSize);
                if (chunk->refCnt == 0 || !chunk->onFree)
                    continue;

                Slot param = {.ptrVal = (char *)chunk + sizeof(HeapChunkHeader)};
                chunk->onFree(&param, NULL);
                page->numChunksWithOnFree--;
            }

            free(page->ptr);
        }
        free(page);
        page = next;
    }
}

HeapPage *pageAdd(HeapPages *pages, int numChunks, int chunkSize)
{
    HeapPage *page = malloc(sizeof(HeapPage));

    page->id = pages->freeId++;

    const int size = numChunks * chunkSize;
    page->ptr = malloc(size);
    if (!page->ptr)
        return NULL;

    page->numChunks = numChunks;
    page->numOccupiedChunks = 0;
    page->numChunksWithOnFree = 0;
    page->chunkSize = chunkSize;
    page->refCnt = 0;
    page->prev = pages->last;
    page->next = NULL;

    // Add to list
    if (!pages->first)
        pages->first = pages->last = page;
    else
    {
        pages->last->next = page;
        pages->last = page;
    }

    pages->totalSize += page->numChunks * page->chunkSize;

#ifdef UMKA_REF_CNT_DEBUG
    printf("Add page at %p\n", page->ptr);
#endif

    return pages->last;
}


void pageRemove(HeapPages *pages, HeapPage *page)
{
#ifdef UMKA_REF_CNT_DEBUG
    printf("Remove page at %p\n", page->ptr);
#endif

    pages->totalSize -= page->numChunks * page->chunkSize;

    if (page == pages->first)
        pages->first = page->next;

    if (page == pages->last)
        pages->last = page->prev;

    if (page->prev)
        page->prev->next = page->next;

    if (page->next)
        page->next->prev = page->prev;

    free(page->ptr);
    free(page);
}

HeapChunkHeader *pageGetChunkHeader(HeapPage *page, void *ptr)
{
    const int chunkOffset = ((char *)ptr - (char *)page->ptr) % page->chunkSize;
    return (HeapChunkHeader *)((char *)ptr - chunkOffset);
}

HeapPage *pageFind(HeapPages *pages, void *ptr, bool warnDangling)
{
    for (HeapPage *page = pages->first; page; page = page->next)
        if (ptr >= page->ptr && ptr < (void *)((char *)page->ptr + page->numChunks * page->chunkSize))
        {
            HeapChunkHeader *chunk = pageGetChunkHeader(page, ptr);

            if (warnDangling && chunk->refCnt == 0)
                pages->error->runtimeHandler(pages->error->context, ERR_RUNTIME, "Dangling pointer at %p", ptr);

            if (chunk->refCnt > 0)
                return page;
            return NULL;
        }
    return NULL;
}


HeapPage *pageFindForAlloc(HeapPages *pages, int size)
{
    HeapPage *bestPage = NULL;
    int bestSize = 1 << 30;

    for (HeapPage *page = pages->first; page; page = page->next)
        if (page->numOccupiedChunks < page->numChunks && page->chunkSize >= size && page->chunkSize < bestSize)
        {
            bestPage = page;
            bestSize = page->chunkSize;
        }
    return bestPage;
}

HeapPage *pageFindById(HeapPages *pages, int id)
{
    for (HeapPage *page = pages->first; page; page = page->next)
        if (page->id == id)
            return page;
    return NULL;
}


bool stackUnwind(Fiber *fiber, Slot **base, int *ip)
{
    if (*base == fiber->stack + fiber->stackSize - 1)
        return false;

    int returnOffset = (*base + 1)->intVal;
    if (returnOffset == RETURN_FROM_FIBER || returnOffset == RETURN_FROM_VM)
        return false;

    *base = (Slot *)((*base)->ptrVal);
    if (ip)
        *ip = returnOffset;
    return true;
}


void stackChangeFrameRefCnt(Fiber *fiber, HeapPages *pages, void *ptr, int delta)
{
    if (ptr >= (void *)fiber->top && ptr < (void *)(fiber->stack + fiber->stackSize))
    {
        Slot *base = fiber->base;
        const ParamLayout *paramLayout = base[-2].ptrVal;

        while (ptr > (void *)(base + 1 + paramLayout->numParamSlots))   // + 1 for return address
        {
            if (!stackUnwind(fiber, &base, NULL))
                pages->error->runtimeHandler(pages->error->context, ERR_RUNTIME, "Illegal stack pointer");

            paramLayout = base[-2].ptrVal;
        }

        int64_t *stackFrameRefCnt = &base[-1].intVal;
        *stackFrameRefCnt += delta;
    }
}


void *chunkAlloc(HeapPages *pages, int64_t size, Type *type, ExternFunc onFree, bool isStack, Error *error)
{
    // Page layout: header, data, footer (char), padding, header, data, footer (char), padding...
    int64_t chunkSize = align(sizeof(HeapChunkHeader) + align(size + 1, sizeof(int64_t)), MEM_MIN_HEAP_CHUNK);

    if (size < 0 || chunkSize > INT_MAX)
        error->runtimeHandler(error->context, ERR_RUNTIME, "Illegal block size");

    HeapPage *page = pageFindForAlloc(pages, chunkSize);
    if (!page)
    {
        int numChunks = MEM_MIN_HEAP_PAGE / chunkSize;
        if (numChunks == 0)
            numChunks = 1;

        page = pageAdd(pages, numChunks, chunkSize);
        if (!page)
            error->runtimeHandler(error->context, ERR_RUNTIME, "Out of memory");
    }

    HeapChunkHeader *chunk = (HeapChunkHeader *)((char *)page->ptr + page->numOccupiedChunks * page->chunkSize);

    memset(chunk, 0, page->chunkSize);
    chunk->refCnt = 1;
    chunk->size = size;
    chunk->type = type;
    chunk->onFree = onFree;
    chunk->ip = pages->fiber->ip;
    chunk->isStack = isStack;

    page->numOccupiedChunks++;
    if (onFree)
        page->numChunksWithOnFree++;

    page->refCnt++;

#ifdef UMKA_REF_CNT_DEBUG
    printf("Add chunk at %p\n", (void *)chunk + sizeof(HeapChunkHeader));
#endif

    return (char *)chunk + sizeof(HeapChunkHeader);
}

int chunkChangeRefCnt(HeapPages *pages, HeapPage *page, void *ptr, int delta)
{
    HeapChunkHeader *chunk = pageGetChunkHeader(page, ptr);

    if (chunk->refCnt <= 0 || page->refCnt < chunk->refCnt)
        pages->error->runtimeHandler(pages->error->context, ERR_RUNTIME, "Wrong reference count for pointer at %p", ptr);

    if (chunk->onFree && chunk->refCnt == 1 && delta == -1)
    {
        Slot param = {.ptrVal = ptr};
        chunk->onFree(&param, NULL);
        page->numChunksWithOnFree--;
    }

    chunk->refCnt += delta;
    page->refCnt += delta;

    // Additional ref counts for a user-defined address interval (used for stack frames to detect escaping refs)
    stackChangeFrameRefCnt(pages->fiber, pages, ptr, delta);

#ifdef UMKA_REF_CNT_DEBUG
    printf("%p: delta: %d  chunk: %d  page: %d\n", ptr, delta, chunk->refCnt, page->refCnt);
#endif

    if (page->refCnt == 0)
    {
        pageRemove(pages, page);
        return 0;
    }

    return chunk->refCnt;
}

void candidateInit(RefCntChangeCandidates *candidates)
{
    candidates->capacity = 100;
    candidates->stack = malloc(candidates->capacity * sizeof(RefCntChangeCandidate));
    candidates->top = -1;
}

void candidateFree(RefCntChangeCandidates *candidates)
{
    free(candidates->stack);
}

void candidateReset(RefCntChangeCandidates *candidates)
{
    candidates->top = -1;
}

void candidatePush(RefCntChangeCandidates *candidates, void *ptr, Type *type)
{
    if (candidates->top >= candidates->capacity - 1)
    {
        candidates->capacity *= 2;
        candidates->stack = realloc(candidates->stack, candidates->capacity * sizeof(RefCntChangeCandidate));
    }

    RefCntChangeCandidate *candidate = &candidates->stack[++candidates->top];
    candidate->ptr = ptr;
    candidate->type = type;
    candidate->pageForDeferred = NULL;
}

void candidatePushDeferred(RefCntChangeCandidates *candidates, void *ptr, Type *type, HeapPage *page)
{
    candidatePush(candidates, ptr, type);
    candidates->stack[candidates->top].pageForDeferred = page;
}

void candidatePop(RefCntChangeCandidates *candidates, void **ptr, Type **type, HeapPage **page)
{
    RefCntChangeCandidate *candidate = &candidates->stack[candidates->top--];
    *ptr = candidate->ptr;
    *type = candidate->type;
    *page = candidate->pageForDeferred;
}

// Helper functions

int fsgetc(bool string, void *stream, int *len)
{
    int ch = string ? ((char *)stream)[*len] : fgetc((FILE *)stream);
    (*len)++;
    return ch;
}

int fsnprintf(bool string, void *stream, int size, const char *format, ...)
{
    va_list args;
    va_start(args, format);

    int res = string ? vsnprintf((char *)stream, size, format, args) : vfprintf((FILE *)stream, format, args);

    va_end(args);
    return res;
}


int fsscanf(bool string, void *stream, const char *format, ...)
{
    va_list args;
    va_start(args, format);

    int res = string ? vsscanf((char *)stream, format, args) : vfscanf((FILE *)stream, format, args);

    va_end(args);
    return res;
}


char *fsscanfString(bool string, void *stream, int *len)
{
    int capacity = 8;
    char *str = malloc(capacity);

    *len = 0;
    int writtenLen = 0;
    int ch = ' ';

    // Skip whitespace
    while (isspace(ch))
        ch = fsgetc(string, stream, len);

    // Read string
    while (ch && ch != EOF && !isspace(ch))
    {
        str[writtenLen++] = ch;
        if (writtenLen == capacity - 1)
        {
            capacity *= 2;
            str = realloc(str, capacity);
        }
        ch = fsgetc(string, stream, len);
    }

    str[writtenLen] = '\0';
    return str;
}


void qsortSwap(void *a, void *b, void *temp, int itemSize)
{
    memcpy(temp, a, itemSize);
    memcpy(a, b, itemSize);
    memcpy(b, temp, itemSize);
}

char *qsortPartition(char *first, char *last, int itemSize, QSortCompareFn compare, void *context, void *temp)
{
    char *i = first;
    char *j = last;

    char *pivot = first;

    while (i < j)
    {
        while (compare(i, pivot, context) <= 0 && i < last)
            i += itemSize;

        while (compare(j, pivot, context) > 0 && j > first)
            j -= itemSize;

        if (i < j)
            qsortSwap(i, j, temp, itemSize);
    }

    qsortSwap(pivot, j, temp, itemSize);

    return j;
}

void qsortEx(char *first, char *last, int itemSize, QSortCompareFn compare, void *context, void *temp)
{
    if (first >= last)
        return;

    char *partition = qsortPartition(first, last, itemSize, compare, context, temp);

    qsortEx(first, partition - itemSize, itemSize, compare, context, temp);
    qsortEx(partition + itemSize, last, itemSize, compare, context, temp);
}

// Virtual machine

void vmInit(VM *vm, int stackSize, bool fileSystemEnabled, Error *error)
{
    vm->fiber = vm->mainFiber = malloc(sizeof(Fiber));
    vm->fiber->parent = NULL;
    vm->fiber->refCntChangeCandidates = &vm->refCntChangeCandidates;
    vm->fiber->vm = vm;
    vm->fiber->alive = true;
    vm->fiber->fileSystemEnabled = fileSystemEnabled;

    pageInit(&vm->pages, vm->fiber, error);

    vm->fiber->stack = chunkAlloc(&vm->pages, stackSize * sizeof(Slot), NULL, NULL, true, error);
    vm->fiber->stackSize = stackSize;

    candidateInit(&vm->refCntChangeCandidates);

    memset(&vm->hooks, 0, sizeof(vm->hooks));
    vm->terminatedNormally = false;
    vm->error = error;
}

void vmFree(VM *vm)
{
    HeapPage *page = pageFind(&vm->pages, vm->mainFiber->stack, true);
    if (!page)
       vm->error->runtimeHandler(vm->error->context, ERR_RUNTIME, "No fiber stack");

    chunkChangeRefCnt(&vm->pages, page, vm->mainFiber->stack, -1);

    candidateFree(&vm->refCntChangeCandidates);
    pageFree(&vm->pages, vm->terminatedNormally);

    free(vm->mainFiber);
}

void vmReset(VM *vm, Instruction *code, DebugInfo *debugPerInstr)
{
    vm->fiber = vm->pages.fiber = vm->mainFiber;
    vm->fiber->code = code;
    vm->fiber->debugPerInstr = debugPerInstr;
    vm->fiber->ip = 0;
    vm->fiber->top = vm->fiber->base = vm->fiber->stack + vm->fiber->stackSize - 1;
}

void doCheckStr(const char *str, Error *error)
{
#ifdef UMKA_STR_DEBUG
    if (!str)
        return;

    const StrDimensions *dims = getStrDims(str);
    if (dims->len != strlen(str) || dims->capacity < dims->len + 1)
        error->runtimeHandler(error->context, ERR_RUNTIME, "Invalid string: %s", str);
#endif
}

void doHook(Fiber *fiber, HookFunc *hooks, HookEvent event)
{
    if (!hooks || !hooks[event])
        return;

    const DebugInfo *debug = &fiber->debugPerInstr[fiber->ip];
    hooks[event](debug->fileName, debug->fnName, debug->line);
}

void doSwapImpl(Slot *slot)
{
    Slot val = slot[0];
    slot[0] = slot[1];
    slot[1] = val;
}

void doDerefImpl(Slot *slot, TypeKind typeKind, Error *error)
{
    if (!slot->ptrVal)
        error->runtimeHandler(error->context, ERR_RUNTIME, "Pointer is null");

    switch (typeKind)
    {
        case TYPE_INT8:         slot->intVal     = *(int8_t         *)slot->ptrVal; break;
        case TYPE_INT16:        slot->intVal     = *(int16_t        *)slot->ptrVal; break;
        case TYPE_INT32:        slot->intVal     = *(int32_t        *)slot->ptrVal; break;
        case TYPE_INT:          slot->intVal     = *(int64_t        *)slot->ptrVal; break;
        case TYPE_UINT8:        slot->intVal     = *(uint8_t        *)slot->ptrVal; break;
        case TYPE_UINT16:       slot->intVal     = *(uint16_t       *)slot->ptrVal; break;
        case TYPE_UINT32:       slot->intVal     = *(uint32_t       *)slot->ptrVal; break;
        case TYPE_UINT:         slot->uintVal    = *(uint64_t       *)slot->ptrVal; break;
        case TYPE_BOOL:         slot->intVal     = *(bool           *)slot->ptrVal; break;
        case TYPE_CHAR:         slot->intVal     = *(unsigned char  *)slot->ptrVal; break;
        case TYPE_REAL32:       slot->realVal    = *(float          *)slot->ptrVal; break;
        case TYPE_REAL:         slot->realVal    = *(double         *)slot->ptrVal; break;
        case TYPE_PTR:          slot->ptrVal     = *(void *         *)slot->ptrVal; break;
        case TYPE_WEAKPTR:      slot->weakPtrVal = *(uint64_t       *)slot->ptrVal; break;
        case TYPE_STR:
        {
            slot->ptrVal = *(void **)slot->ptrVal;
            doCheckStr((char *)slot->ptrVal, error);
            break;
        }
        case TYPE_ARRAY:
        case TYPE_DYNARRAY:
        case TYPE_MAP:
        case TYPE_STRUCT:
        case TYPE_INTERFACE:
        case TYPE_CLOSURE:      break;  // Always represented by pointer, not dereferenced
        case TYPE_FIBER:        slot->ptrVal     = *(void *         *)slot->ptrVal; break;
        case TYPE_FN:           slot->intVal     = *(int64_t        *)slot->ptrVal; break;

        default:                error->runtimeHandler(error->context, ERR_RUNTIME, "Illegal type"); return;
    }
}

void doAssignImpl(void *lhs, Slot rhs, TypeKind typeKind, int structSize, Error *error)
{
    if (!lhs)
        error->runtimeHandler(error->context, ERR_RUNTIME, "Pointer is null");

    Const rhsConstant = {.intVal = rhs.intVal};
    if (typeOverflow(typeKind, rhsConstant))
        error->runtimeHandler(error->context, ERR_RUNTIME, "Overflow of %s", typeKindSpelling(typeKind));

    switch (typeKind)
    {
        case TYPE_INT8:         *(int8_t        *)lhs = rhs.intVal;  break;
        case TYPE_INT16:        *(int16_t       *)lhs = rhs.intVal;  break;
        case TYPE_INT32:        *(int32_t       *)lhs = rhs.intVal;  break;
        case TYPE_INT:          *(int64_t       *)lhs = rhs.intVal;  break;
        case TYPE_UINT8:        *(uint8_t       *)lhs = rhs.intVal;  break;
        case TYPE_UINT16:       *(uint16_t      *)lhs = rhs.intVal;  break;
        case TYPE_UINT32:       *(uint32_t      *)lhs = rhs.intVal;  break;
        case TYPE_UINT:         *(uint64_t      *)lhs = rhs.uintVal; break;
        case TYPE_BOOL:         *(bool          *)lhs = rhs.intVal;  break;
        case TYPE_CHAR:         *(unsigned char *)lhs = rhs.intVal;  break;
        case TYPE_REAL32:       *(float         *)lhs = rhs.realVal; break;
        case TYPE_REAL:         *(double        *)lhs = rhs.realVal; break;
        case TYPE_PTR:          *(void *        *)lhs = rhs.ptrVal;  break;
        case TYPE_WEAKPTR:      *(uint64_t      *)lhs = rhs.weakPtrVal; break;
        case TYPE_STR:
        {
            doCheckStr((char *)rhs.ptrVal, error);
            *(void **)lhs = rhs.ptrVal;
            break;
        }
        case TYPE_ARRAY:
        case TYPE_DYNARRAY:
        case TYPE_MAP:
        case TYPE_STRUCT:
        case TYPE_INTERFACE:
        case TYPE_CLOSURE:
        {
            if (!rhs.ptrVal)
                error->runtimeHandler(error->context, ERR_RUNTIME, "Pointer is null");
            memcpy(lhs, rhs.ptrVal, structSize);
            break;
        }
        case TYPE_FIBER:        *(void *        *)lhs = rhs.ptrVal;  break;
        case TYPE_FN:           *(int64_t       *)lhs = rhs.intVal;  break;

        default:                error->runtimeHandler(error->context, ERR_RUNTIME, "Illegal type"); return;
    }
}

void doAddPtrBaseRefCntCandidate(RefCntChangeCandidates *candidates, void *ptr, Type *type)
{
    if (typeKindGarbageCollected(type->base->kind))
    {
        void *data = ptr;
        if (type->base->kind == TYPE_PTR || type->base->kind == TYPE_STR || type->base->kind == TYPE_FIBER)
            data = *(void **)data;

        candidatePush(candidates, data, type->base);
    }
}

void doAddArrayItemsRefCntCandidates(RefCntChangeCandidates *candidates, void *ptr, Type *type, int len)
{
    if (typeKindGarbageCollected(type->base->kind))
    {
        char *itemPtr = ptr;
        int itemSize = typeSizeNoCheck(type->base);

        for (int i = 0; i < len; i++)
        {
            void *item = itemPtr;
            if (type->base->kind == TYPE_PTR || type->base->kind == TYPE_STR || type->base->kind == TYPE_FIBER)
                item = *(void **)item;

            candidatePush(candidates, item, type->base);
            itemPtr += itemSize;
        }
    }
}

void doAddStructFieldsRefCntCandidates(RefCntChangeCandidates *candidates, void *ptr, Type *type)
{
    for (int i = 0; i < type->numItems; i++)
    {
        if (typeKindGarbageCollected(type->field[i]->type->kind))
        {
            void *field = (char *)ptr + type->field[i]->offset;
            if (type->field[i]->type->kind == TYPE_PTR || type->field[i]->type->kind == TYPE_STR || type->field[i]->type->kind == TYPE_FIBER)
                field = *(void **)field;

            candidatePush(candidates, field, type->field[i]->type);
        }
    }
}


void doChangeRefCntImpl(Fiber *fiber, HeapPages *pages, void *ptr, Type *type, TokenKind tokKind)
{
    // Update ref counts for pointers (including static/dynamic array items and structure/interface fields) if allocated dynamically
    // All garbage collected composite types are represented by pointers by default
    // RTTI is required for lists, trees, etc., since the propagation depth for the root ref count is unknown at compile time

    RefCntChangeCandidates *candidates = fiber->refCntChangeCandidates;
    candidateReset(candidates);
    candidatePush(candidates, ptr, type);

    while (candidates->top >= 0)
    {
        HeapPage *pageForDeferred = NULL;
        candidatePop(candidates, &ptr, &type, &pageForDeferred);

        // Process deferred ref count updates first (the heap page should have been memoized for them)
        if (pageForDeferred)
        {
            chunkChangeRefCnt(pages, pageForDeferred, ptr, (tokKind == TOK_PLUSPLUS) ? 1 : -1);
            continue;
        }

        // Process all other updates
        switch (type->kind)
        {
            case TYPE_PTR:
            {
                HeapPage *page = pageFind(pages, ptr, true);
                if (!page)
                    break;

                if (tokKind == TOK_PLUSPLUS)
                    chunkChangeRefCnt(pages, page, ptr, 1);
                else
                {
                    HeapChunkHeader *chunk = pageGetChunkHeader(page, ptr);
                    if (chunk->refCnt > 1)
                    {
                        chunkChangeRefCnt(pages, page, ptr, -1);
                        break;
                    }

                    // Only one ref is left. Defer processing the parent and traverse the children before removing the ref
                    candidatePushDeferred(candidates, ptr, type, page);

                    // Sometimes the last remaining ref to chunk data is a pointer to a single item of a composite type (interior pointer)
                    // In this case, we should traverse children as for the actual composite type, rather than for the pointer
                    if (chunk->type)
                    {
                        void *chunkDataPtr = (char *)chunk + sizeof(HeapChunkHeader);

                        switch (chunk->type->kind)
                        {
                            case TYPE_ARRAY:
                            {
                                doAddArrayItemsRefCntCandidates(candidates, chunkDataPtr, chunk->type, chunk->type->numItems);
                                break;
                            }
                            case TYPE_DYNARRAY:
                            {
                                DynArrayDimensions *dims = (DynArrayDimensions *)chunkDataPtr;
                                void *data = (char *)chunkDataPtr + sizeof(DynArrayDimensions);
                                doAddArrayItemsRefCntCandidates(candidates, data, chunk->type, dims->len);
                                break;
                            }
                            case TYPE_STRUCT:
                            {
                                doAddStructFieldsRefCntCandidates(candidates, chunkDataPtr, chunk->type);
                                break;
                            }
                            default:
                            {
                                doAddPtrBaseRefCntCandidate(candidates, ptr, type);
                                break;
                            }
                        }
                    }
                    else
                        doAddPtrBaseRefCntCandidate(candidates, ptr, type);
                }
                break;
            }

            case TYPE_STR:
            {
                doCheckStr((char *)ptr, pages->error);

                HeapPage *page = pageFind(pages, ptr, true);
                if (!page)
                    break;

                chunkChangeRefCnt(pages, page, ptr, (tokKind == TOK_PLUSPLUS) ? 1 : -1);
                break;
            }

            case TYPE_ARRAY:
            {
                doAddArrayItemsRefCntCandidates(candidates, ptr, type, type->numItems);
                break;
            }

            case TYPE_DYNARRAY:
            {
                DynArray *array = (DynArray *)ptr;
                HeapPage *page = pageFind(pages, array->data, true);
                if (!page)
                    break;

                if (tokKind == TOK_PLUSPLUS)
                    chunkChangeRefCnt(pages, page, array->data, 1);
                else
                {
                    HeapChunkHeader *chunk = pageGetChunkHeader(page, array->data);
                    if (chunk->refCnt > 1)
                    {
                        chunkChangeRefCnt(pages, page, array->data, -1);
                        break;
                    }

                    // Only one ref is left. Defer processing the parent and traverse the children before removing the ref
                    candidatePushDeferred(candidates, array->data, type, page);
                    doAddArrayItemsRefCntCandidates(candidates, array->data, type, getDims(array)->len);
                }
                break;
            }

            case TYPE_MAP:
            {
                Map *map = (Map *)ptr;
                candidatePush(candidates, map->root, typeMapNodePtr(type));
                break;
            }

            case TYPE_STRUCT:
            {
                doAddStructFieldsRefCntCandidates(candidates, ptr, type);
                break;
            }

            case TYPE_INTERFACE:
            {
                Interface *interface = (Interface *)ptr;
                if (interface->self)
                    candidatePush(candidates, interface->self, interface->selfType);
                break;
            }

            case TYPE_CLOSURE:
            {
                doAddStructFieldsRefCntCandidates(candidates, ptr, type);
                break;
            }

            case TYPE_FIBER:
            {
                HeapPage *page = pageFind(pages, ptr, true);
                if (!page)
                    break;

                if (tokKind == TOK_PLUSPLUS)
                    chunkChangeRefCnt(pages, page, ptr, 1);
                else
                {
                    HeapChunkHeader *chunk = pageGetChunkHeader(page, ptr);
                    if (chunk->refCnt > 1)
                    {
                        chunkChangeRefCnt(pages, page, ptr, -1);
                        break;
                    }

                    if (((Fiber *)ptr)->alive)
                        pages->error->runtimeHandler(pages->error->context, ERR_RUNTIME, "Cannot destroy a busy fiber");

                    // Only one ref is left. Defer processing the parent and traverse the children before removing the ref
                    HeapPage *stackPage = pageFind(pages, ((Fiber *)ptr)->stack, true);
                    if (!stackPage)
                        pages->error->runtimeHandler(pages->error->context, ERR_RUNTIME, "No fiber stack");

                    chunkChangeRefCnt(pages, stackPage, ((Fiber *)ptr)->stack, -1);
                    chunkChangeRefCnt(pages, page, ptr, -1);
                }
                break;
            }

            default: break;
        }
    }
}

char *doAllocStr(HeapPages *pages, int64_t len, Error *error)
{
    StrDimensions dims = {.len = len, .capacity = 2 * (len + 1)};

    char *dimsAndData = chunkAlloc(pages, sizeof(StrDimensions) + dims.capacity, NULL, NULL, false, error);
    *(StrDimensions *)dimsAndData = dims;

    char *data = dimsAndData + sizeof(StrDimensions);
    data[len] = 0;

    return data;
}

char *doGetEmptyStr()
{
    StrDimensions dims = {.len = 0, .capacity = 1};

    static char dimsAndData[sizeof(StrDimensions) + 1];
    *(DynArrayDimensions *)dimsAndData = dims;

    char *data = dimsAndData + sizeof(StrDimensions);
    data[0] = 0;

    return data;
}