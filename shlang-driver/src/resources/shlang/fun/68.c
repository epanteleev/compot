int main() {
    int arr[5];
    int* ptr = arr;
    int* a = &arr[2];
    ptr[-2] = 99;
    return ptr[0];
}