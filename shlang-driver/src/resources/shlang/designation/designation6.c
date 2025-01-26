
extern void printf(char format[], ...);

int main() {
    int arr[] = { [1] = 1, [0] = 2 };
    printf("arr[0]: %d, arr[1]: %d\n", arr[0], arr[1]);
    return 0;
}