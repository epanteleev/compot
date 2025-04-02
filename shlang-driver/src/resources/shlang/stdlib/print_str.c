
extern int printf(char* fmt, ...);

int print(char* argv[]) {
    printf("%s", *argv);
    argv++;
    printf("%s", *argv);
    return 0;
}

int main(int argc, char *argv[]) {
    char* args[] = {"Hello, ", "world!\n"};
    print(args);
    return 0;
}