
extern void printf(const char* format, ...);

char* hello_str() {
    return "Hello, World!";
}

int main() {
    printf("%s\n", hello_str());
    return 0;
}