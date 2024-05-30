#ifndef RUNTIME_H
#define RUNTIME_H

extern int exit(int);
void reportError();

#define check(a, b) if (a != b) { reportError(); }

#define unreachable() reportError()

void printFloat(float);
void printInt(int a);
void printUByte(unsigned char value);

#endif