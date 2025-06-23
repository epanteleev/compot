

struct Array {
    union {
        int* ptr;
        long data;
    };
    int length;
};

int main() {
    int arr[2] = {1, 2};
    struct Array a = { .ptr = arr, .length = 2 };
    return a.ptr[0];
}