int main() {
	start:
		goto next;
		return 1;
	success:
		return 220;
	next:
		goto success;
		return 1;
}
