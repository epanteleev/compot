

typedef struct {
    long x;
    long y;
    long z;
} Vec3;

Vec3 copy0(Vec3 v) {
    return v;
}

Vec3 copy(Vec3* v) {
    return copy0(*v);
}

int main() {
    Vec3 v = {1, 2, 3};
    Vec3 v1 = copy(&v);
    return v1.x + v1.y + v1.z;
}