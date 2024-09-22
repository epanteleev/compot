
struct Point {
    int x;
    int y;
};

int inc() {
    static struct Point i;
    return i.y++;
}

int main() {
    inc();
    inc();
    int k = inc();
    return k;
}