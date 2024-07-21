
extern void printf(char format[], ...);

typedef struct Vect_ {
    float x;
    float y;
    float z;
} Vect3;

Vect3 getVect() {
    Vect3 v;
    v.x = 1;
    v.y = 2;
    v.z = 3;
    return v;
}

int main() {
    Vect3 v = getVect();
    printf("x: %f, y: %f, z: %f\n", v.x, v.y, v.z);
    return 0;
}