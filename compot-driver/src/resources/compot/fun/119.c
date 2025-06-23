

struct Data {
    long a;
    long b;
    long c;
};

void* getAddress(struct Data data) {
    return (void*)&data;
}


int main() {
    struct Data data;
    return (long)(getAddress(data) != (void*)0);
}