int main(void) {
    long arr[4];
    long* last = &arr[3];
    long* first = &arr[0];
    return last - first < -1;
}