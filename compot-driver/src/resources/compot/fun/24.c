
int inc() {
    static int i;
    return i++;
}

int main() {
    inc();
    inc();
    int k = inc();
    return k;
}