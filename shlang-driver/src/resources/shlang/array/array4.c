extern int printf(const char *, ...);

struct Array {
    int *data;
    int size;
};

int main() {
    struct Array arr[3];
    arr->size = 3;
    arr->data = (int[3]){1, 2, 3};
    printf("len=%d {%d, %d, %d}\n", arr->size, arr->data[0], arr->data[1], arr->data[2]);
    return 0;
}