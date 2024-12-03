struct Point {
    int x;
    int y;
};

int main() {
    struct Point p;
    struct Point p1;
    if (&p == &p1) {
        return 1;
    } else {
        return 2;
    }
}