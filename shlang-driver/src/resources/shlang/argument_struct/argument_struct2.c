
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
    Vect3 v;
    v.x = 1;
    v.y = 2;
    v.z = 3;
    printVect(v);
    return 0;
}