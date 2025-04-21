
const char str[];

int main() {
    char* p = (char *)str;
    return p[0];
}

const char str[] = "Hello, world!";