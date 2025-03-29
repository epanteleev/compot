struct Mem {
    int a;
    int b;
};

struct Mem mem = {1, 2};

int main() {
    struct Mem *m = &mem;
    return m == &mem && &mem == m;
}