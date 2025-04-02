int printf(const char *, ...);

struct Struct {
    int x;
    int y;
};

union {
	long i;
	struct Struct s;
} un;

int main(void) {
	un.i = 0x7FFFEFFEEF;
	un.s.x = 1;
	return printf("%ld\n", un.i);
}