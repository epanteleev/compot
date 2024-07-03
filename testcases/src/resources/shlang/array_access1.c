

extern void* malloc(int);
extern void free(void*);
extern void printf(char*, ...);

int main () {
    int* p = (int*)malloc(sizeof(int) * 4);
    *p = 42;
    *(p + 1) = 43;
    *(p + 2) = 44;
    *(p + 3) = 45;
    for(int i = 0; i < 4; i++) {
        printf("%d ", *(p + i));
    }

    free(p);
    return 0;
}