extern int printf(const char *, ...);

char* lit = "hello";

char* select(int bl) {
    return bl ? lit : "Success";
}

int main() {
    printf("%s\n", select(0));
    return 0;
}