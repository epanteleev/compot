
extern int printf(const char *, ...);

struct Holder {
    int array[3];
};

int printArray(struct Holder holder) {
    return printf("{%d, %d, %d}\n", *holder.array, *(holder.array + 1), *(holder.array + 2));
}

int main(void) {
    struct Holder foo = {{1, 2, 3}};
    *(foo.array + 1) = 20;
    printArray(foo);
    return 0;
}