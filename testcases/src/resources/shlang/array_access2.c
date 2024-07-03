

extern void* malloc(int);
extern void free(void*);
extern void printf(char*, ...);

int main () {
    int* p = (int*)malloc(sizeof(int) * 4);
    int* orig = p;
    *p = 42;
    p++;
    *p = 43;
    p++;
    *p = 44;
    p++;
    *p = 45;
    for(int i = 0; i < 4; i++) {
        printf("%d ", *(orig + i));
    }

    free(p);
    return 0;
}