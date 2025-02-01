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