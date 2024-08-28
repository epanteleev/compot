
int inc() {
    static int i = 0;
    return i++;
}

int main() {
    inc();
    inc();
    int k = inc();
    return k;
}