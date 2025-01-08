CC?=gcc
.PHONY: clean

test: utf8.o test.o
	$(CC) -o test utf8.o test.o

utf8.o: utf8.c utf8.h
	$(CC) -c utf8.c

test.o: test.c utf8.h
	$(CC) -c test.c

clean:
	rm -f test *.o
