#define __USE_MINGW_ANSI_STDIO 1

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>

#ifdef _WIN32
    #include <windows.h>
#else
    #include <dlfcn.h>
    #include <unistd.h>
#endif

#include "umka_common.h"
#include "umka_types.h"


// Errors

void errorReportInit(ErrorReport *report, const char *fileName, const char *fnName, int line, int pos, int code, const char *format, va_list args)
{
    errorReportFree(report);

    report->fileName = malloc(strlen(fileName) + 1);
    strcpy(report->fileName, fileName);

    report->fnName = malloc(strlen(fnName) + 1);
    strcpy(report->fnName, fnName);

    report->line = line;
    report->pos = pos;
    report->code = code;

    va_list argsCopy;
    va_copy(argsCopy, args);

    const int msgLen = vsnprintf(NULL, 0, format, args);
    report->msg = malloc(msgLen + 1);
    vsnprintf(report->msg, msgLen + 1, format, argsCopy);

    va_end(argsCopy);
}


void errorReportFree(ErrorReport *report)
{
    if (report->fileName)
    {
        free(report->fileName);
        report->fileName = NULL;
    }

    if (report->fnName)
    {
        free(report->fnName);
        report->fnName = NULL;
    }

    if (report->msg)
    {
        free(report->msg);
        report->msg = NULL;
    }
}

// Storage

void storageInit(Storage *storage)
{
    storage->first = storage->last = NULL;
}


void storageFree(Storage *storage)
{
    StorageChunk *chunk = storage->first;
    while (chunk)
    {
        StorageChunk *next = chunk->next;
        free(chunk->data);
        free(chunk);
        chunk = next;
    }
}


char *storageAdd(Storage *storage, int size)
{
    StorageChunk *chunk = malloc(sizeof(StorageChunk));

    chunk->data = malloc(size);
    memset(chunk->data, 0, size);
    chunk->next = NULL;

    // Add to list
    if (!storage->first)
        storage->first = storage->last = chunk;
    else
    {
        storage->last->next = chunk;
        storage->last = chunk;
    }
    return storage->last->data;
}


char *storageAddStr(Storage *storage, int len)
{
    StrDimensions dims = {.len = len, .capacity = len + 1};

    char *dimsAndData = storageAdd(storage, sizeof(StrDimensions) + dims.capacity);
    *(StrDimensions *)dimsAndData = dims;

    char *data = dimsAndData + sizeof(StrDimensions);
    data[len] = 0;

    return data;
}

DynArray *storageAddDynArray(Storage *storage, struct tagType *type, int len)
{
    DynArray *array = (DynArray *)storageAdd(storage, sizeof(DynArray));

    array->type     = type;
    array->itemSize = typeSizeNoCheck(array->type->base);

    DynArrayDimensions dims = {.len = len, .capacity = 2 * (len + 1)};

    char *dimsAndData = storageAdd(storage, sizeof(DynArrayDimensions) + dims.capacity * array->itemSize);
    *(DynArrayDimensions *)dimsAndData = dims;

    array->data = dimsAndData + sizeof(DynArrayDimensions);
    return array;
}

// Modules

const char *moduleImplLibSuffix()
{
#ifdef UMKA_EXT_LIBS
    #ifdef _WIN32
        return "_windows";
    #elif defined __EMSCRIPTEN__
        return "_wasm";
    #else
        return "_linux";
    #endif
#endif
    return "";
}


void *moduleLoadImplLib(const char *path)
{
#ifdef UMKA_EXT_LIBS
    #ifdef _WIN32
        return LoadLibrary(path);
    #else
        return dlopen(path, RTLD_LOCAL | RTLD_LAZY);
    #endif
#endif
    return NULL;
}


void moduleFreeImplLib(void *lib)
{
#ifdef UMKA_EXT_LIBS
    #ifdef _WIN32
        FreeLibrary(lib);
    #else
        dlclose(lib);
    #endif
#endif
}

void *moduleLoadImplLibFunc(void *lib, const char *name)
{
#ifdef UMKA_EXT_LIBS
    #ifdef _WIN32
        return GetProcAddress(lib, name);
    #else
        return dlsym(lib, name);
    #endif
#endif
    return NULL;
}

void moduleInit(Modules *modules, bool implLibsEnabled, Error *error)
{
    for (int i = 0; i < MAX_MODULES; i++)
    {
        modules->module[i] = NULL;
        modules->moduleSource[i] = NULL;
    }
    modules->numModules = 0;
    modules->numModuleSources = 0;
    modules->implLibsEnabled = implLibsEnabled;
    modules->error = error;

    if (!moduleCurFolder(modules->curFolder, DEFAULT_STR_LEN + 1))
        modules->error->handler(modules->error->context, "Cannot get current folder");
}


void moduleFree(Modules *modules)
{
    for (int i = 0; i < modules->numModules; i++)
    {
        if (modules->module[i]->implLib)
            moduleFreeImplLib(modules->module[i]->implLib);

        for (int j = 0; j < modules->numModules; j++)
            if (modules->module[i]->importAlias[j])
                free(modules->module[i]->importAlias[j]);

        free(modules->module[i]);
    }

    for (int i = 0; i < modules->numModuleSources; i++)
    {
        free(modules->moduleSource[i]->source);
        free(modules->moduleSource[i]);
    }
}


void moduleNameFromPath(Modules *modules, const char *path, char *folder, char *name, int size)
{
    const char *slash = strrchr(path, '/');
    const char *backslash = strrchr(path, '\\');

    if (backslash && (!slash || backslash > slash))
        slash = backslash;

    const char *start = slash ? (slash + 1) : path;

    const char *dot = strrchr(path, '.');
    const char *stop = dot ? dot : (path + strlen(path));

    if (stop <= start)
        modules->error->handler(modules->error->context, "Illegal module path %s", path);

    strncpy(folder, path, (start - path < size - 1) ? (start - path) : (size - 1));
    strncpy(name, start,  (stop - start < size - 1) ? (stop - start) : (size - 1));

    folder[size - 1] = 0;
    name[size - 1] = 0;
}

int moduleFind(Modules *modules, const char *path)
{
    unsigned int pathHash = hash(path);
    for (int i = 0; i < modules->numModules; i++)
        if (modules->module[i]->pathHash == pathHash && strcmp(modules->module[i]->path, path) == 0)
            return i;
    return -1;
}


int moduleFindImported(Modules *modules, Blocks *blocks, const char *alias)
{
    for (int i = 0; i < modules->numModules; i++)
    {
        const char *importAlias = modules->module[blocks->module]->importAlias[i];
        if (importAlias && strcmp(importAlias, alias) == 0)
            return i;
    }
    return -1;
}