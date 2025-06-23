#include <string.h>
#include <stdio.h>

int main() {
    static char buff[200] = "";
    char *p = buff;
    strcat(buff, "Hello, ");
    strcat(buff, "world!");
    fprintf(stdout, "%s\n", buff);

    return strlen(buff) - strlen(p);
}