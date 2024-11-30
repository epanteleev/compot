extern void printf(const char *format, ...);
extern long strlen(const char *s);
const char* fmt = "\\Hello World!\n";

int main() {
    printf(fmt);
    return strlen(fmt);
}