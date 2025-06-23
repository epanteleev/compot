int main() {
    int arr[5];
    int* ptr = &arr[2];
    ptr[-2] = 99;
    return arr[0];
}