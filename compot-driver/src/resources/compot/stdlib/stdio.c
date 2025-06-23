#include <stdio.h>
#include <stdlib.h>

struct S {
    FILE *file;
};

int main() {
    struct S* s = malloc(sizeof(struct S));
    s->file = stdout;
    fprintf(s->file, "done\n");
    return 0;
}