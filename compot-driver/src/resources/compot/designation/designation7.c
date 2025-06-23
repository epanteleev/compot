extern int printf(char format[], ...);

union Data {
    long data;
    int* ptr;
};

typedef struct {
    int len;
    union Data data;
} Array;

int main() {
    Array arr = { .len = 2, .data = { .ptr = (int[]){1, 2} } };
    printf("arr.data.ptr[0]: %d, arr.data.ptr[1]: %d\n", arr.data.ptr[0], arr.data.ptr[1]);
    return 0;
}
