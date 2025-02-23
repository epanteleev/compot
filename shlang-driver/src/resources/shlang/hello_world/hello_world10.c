#include <stdio.h>

#define WRAP(x) #x

int main() {
    printf(WRAP("\"Hello\\\"World\""));
    return 0;
}