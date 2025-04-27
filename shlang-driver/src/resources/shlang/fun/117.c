
void init_array(void *buff, long size) {
    int *arr = (int *)buff;
    arr[0] = 1;
    arr[1] = 2;
}

unsigned arr[2];

int main() {
    init_array(arr, sizeof(arr));
    return arr[0] + arr[1];
}