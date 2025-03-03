int main() {
    int arr[2] = {1, 2};
    int* ptr = arr;
    int* last = 1 + ptr;
    return *last;
}