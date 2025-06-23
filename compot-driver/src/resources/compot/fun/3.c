int main() {
	int x;
	int *p;
	
	x = 4;
	p = &x;
	*p = 20;
	return *p;
}
