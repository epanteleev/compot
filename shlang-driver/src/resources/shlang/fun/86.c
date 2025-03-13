
int get() {
    return 1;
}

extern int get();

int main() {
    return get();
}