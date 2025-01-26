struct V {
    int (*getInteger)(int);
};

struct S {
    struct V v;
};

int getInteger(int x);

void set(struct S* s) {
    s->v.getInteger = getInteger;
}

int getInteger(int x) {
    return x;
}

int get(struct S* s) {
    return s->v.getInteger(1);
}

int main() {
    struct S s;
    set(&s);
    return get(&s);
}