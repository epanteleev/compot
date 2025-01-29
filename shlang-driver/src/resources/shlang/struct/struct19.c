extern int printf(const char*, ...);

typedef union {
    long v;
    double d;
} SS;

int get(SS* ptr, SS p) {
    return ptr->v + p.d;
}


int main() {
    SS s = { .v = 0};
    SS* ptr = &s;
    printf("here");

    long corrupt = 99;
    if (corrupt == 99) {
        printf("here");
    } else {
        printf("here");
    }
    SS corrupt9 = {.d = 99};
    printf("here");
    return get(ptr, corrupt9);
}