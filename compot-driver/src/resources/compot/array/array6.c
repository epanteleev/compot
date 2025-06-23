#include <stdio.h>
#include <string.h>
#include <stdbool.h>


bool startswith(char *p, char *q) {
  return strncmp(p, q, strlen(q)) == 0;
}

int main(void) {
    const char *p = "1u'Hello, world!\"";
    if (startswith(p + 1, "u'")) {
        printf("startswith\n");
    }
    return 0;
}