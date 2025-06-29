

extern int printf(const char* fmt, ...);

int main() {
    char types[][6] = {
		 "cof", "cob", "cif", "cib", "rof", "rob"
	};


	return printf("%s %s %s %s %s %s\n", types[0],
         types[1], types[2], types[3], types[4], types[5]);
}