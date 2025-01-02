
extern void* malloc(int);

struct Array {
    int* a;
    long len;
};

struct Array new_array(int len) {
    struct Array arr;
    arr.a = (int*)malloc(len * sizeof(int));
    arr.len = len;
    return arr;
}

int main() {
    struct Array a;
    struct Array (*a_ptr)(int) = new_array;
    if (a_ptr(10).len != 0) {}
    return 10;
}