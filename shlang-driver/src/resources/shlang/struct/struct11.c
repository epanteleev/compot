
struct Point {
    int a;
    int b;
};

int main(void) {
	struct Point arr = {1, 2};
	char *ptr = (char *) arr;

	return (int) *ptr;
}
