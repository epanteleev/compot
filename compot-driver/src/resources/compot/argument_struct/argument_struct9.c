
extern void printf(char format[], ...);

typedef struct Vect_ {
    char x;
    char y;
    char z;
} Vect;

void printVect(Vect v) {
    printf("x: %d y: %d z: %d\n", v.x, v.y, v.z);
}

int main() {
    Vect v;
    v.x = 1;
    v.y = 2;
    v.z = 3;
    printVect(v);
    return 0;
}