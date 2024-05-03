#ifndef RUNTIME_H
#define RUNTIME_H

extern int exit(int);

#define check(a, b) if (a != b) { exit(1); }

void printFloat(float);

void printInt(int a);

#endif