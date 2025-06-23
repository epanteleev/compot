
int arr[2] = {1, 2};

int* array() {
    return arr;
}

int main() {
    return array()[1];
}