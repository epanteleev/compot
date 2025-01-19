extern int printf(const char *format, ...);

static const char array[] = { '/' };

int main(void) {
    printf("%c\n", array[0]);
    return 0;
}