#include <stdarg.h>
#include <stdint.h>
#include <stdio.h>

char *format(char *fmt, ...) {
  char *buf;
  size_t buflen;
  FILE *out = open_memstream(&buf, &buflen);

  va_list ap;
  va_start(ap, fmt);
  vfprintf(out, fmt, ap);
  va_end(ap);
  fclose(out);
  return buf;
}

int main() {
    char* buf = format("print");
    printf("%s", buf);
    return 0;
}