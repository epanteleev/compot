#include <stdlib.h>

typedef struct internal_hooks
{
    void *(*allocate)(size_t size);
    void (*deallocate)(void *pointer);
    void *(*reallocate)(void *pointer, size_t size);
} internal_hooks;

typedef struct Holder_ {
    const unsigned char *content;
    long length;
    long offset;
    long depth;
    internal_hooks hooks;
} Holder;

int main() {
    Holder h = { 0, 1, 2, 3, { 0, 0, 0 } };
    return h.length + h.offset + h.depth;
}