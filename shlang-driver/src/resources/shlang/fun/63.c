

typedef struct {
    long x;
    long y;
    long z;
} Vec3;

Vec3 assign(Vec3 v) {
    Vec3 v1;
    v1 = v;
    return v1;
}

int main() {
    Vec3 v = {1, 2, 3};
    Vec3 v1 = assign(v);
    return v1.x + v1.y + v1.z;
}