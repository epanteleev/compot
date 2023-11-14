#include <stdio.h>

void printLong(long value) {
	printf("%ld\n", value);	
}

void printUlong(unsigned long value) {
        printf("%lu\n", value); 
}


void printInt(int value) {
        printf("%d\n", value); 
}

void printLongArray(long* array, long size) {
        for (long i = 0; i < size; i++) {
                printf("%ld", array[i]);
        }
        printf("\n");
}

void printIntArray(int* array, int size) {
	for (int i = 0; i < size; i++) {
		printf("%d", array[i]);
	}
	printf("\n");
}
