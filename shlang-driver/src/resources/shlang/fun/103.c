struct P {
    int v:1;
    int:32;
};

int main() {
    struct P p;
    return sizeof(struct P);
}