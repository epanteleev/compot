
extern void printf(const char* str, ...);

int main() {
    int arr[3] = {1, 2, 3};

    for (int i = 0; i < 3; i++) {
        printf("%d", arr[i]);
    }
    return 0;
}