
struct A {
    int a;
    int b;
};

int main() {
    struct A* a;
    char mem[sizeof(*a)];
    return sizeof(mem);
}