int main() {
	int x = 50;
	while (x != 10) {
	    x = x - 1;
	    if (x == 20) {
            break;
        }
	}

	return x;
}