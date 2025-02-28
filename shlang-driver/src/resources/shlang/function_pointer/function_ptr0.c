
extern int printf(char format[], ...);

void fun() { printf("Hello"); }

int main() {
    void (*fun_ptr)() = &fun;
    (*fun_ptr)();
    return 0;
}
