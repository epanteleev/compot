

int ptr_cmp(char* ptr) {
    return ptr != (char*) -1;
}

int main() {
    long v = -1;
    char* ptr = (char*) v;
    return ptr_cmp(ptr);
}