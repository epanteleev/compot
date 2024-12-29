
struct S {
    _Bool bl;
};

int main() {
    struct S s = { .bl = 1 };
    return s.bl;
}