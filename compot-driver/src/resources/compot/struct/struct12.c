
extern void printf(char format[], ...);

typedef struct Vect_ {
    long x;
    long y;
    long z;
} Vect3;

void printVect(Vect3 v) {
    printf("x: %ld, y: %ld, z: %ld\n", v.x, v.y, v.z);
}

int main() {
    Vect3 v = {1, 2, 3};
    Vect3 v1;
    v1 = v;
    printVect(v1);
    return 0;
}