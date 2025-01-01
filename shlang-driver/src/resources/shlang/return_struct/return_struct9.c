
extern int printf(char format[], ...);

typedef struct Vec_ {
    long x;
    long y;
    long z;
} Vec3;

Vec3 getVec3() {
    Vec3 p;
    p.x = 1;
    p.y = 2;
    p.z = 3;
    return p;
}

int main() {
    Vec3 (*fn)() = getVec3;
    Vec3 p = fn();
    printf("x: %d, y: %d, z: %d\n", p.x, p.y, p.z);
    return 0;
}