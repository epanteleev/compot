struct Mem {
    int a;
    int b;
};

struct Mem mem = {1, 2};

int main() {
    long m = (long)&mem;
    return (void*)m == &mem && &mem == (void*)m;
}