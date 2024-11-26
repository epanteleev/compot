int isNull(char* ptr1, char* ptr2) {
    if (ptr1 && ptr2) {
        return 10;
    } else {
        return 5;
    }
}

int main() {
    char* ptr = (char*) (1 << 24);
    char* ptr2 = (char*) (1L << 32);
    return isNull(ptr, ptr2);
}