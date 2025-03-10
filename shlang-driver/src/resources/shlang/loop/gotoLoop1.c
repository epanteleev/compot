

int loop(int c) {
    int i = c;
    while ((c = i) < 10) {
        if (c == 5) {
            c += 2;
            goto add2;
        } else if (c == 6) {

            goto add3;
        } else {
            goto add1;
        }
        add1: {
            i = i + 1;
            continue;
        }
        add2: {
            i = i + 2;
            continue;
        }
        add3: {
            c += 1;
            continue;
        }
    }
    return i;
}

int main() {
    int i = 0;
    return loop(i);
}