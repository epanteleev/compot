#include <assert.h>

extern int printf(const char *, ...);

typedef enum {
    RED,
    GREEN,
    BLUE,
} Color;

typedef enum {
    A, B, C
} Unused;


int visitColor(Color color, int ch) {
    switch (color) {
        case RED:
            if (ch == '(') {
                return 1;
            }
            break;
        case GREEN:
            if (ch == ')') {
                return 2;
            }
            if (ch == '(') {
                return 1;
            }
            break;
        case BLUE:
            if (ch == '[') {
                return 3;
            }
            break;
    }
    return -1;
}

int main() {
    assert(visitColor(RED, '(') == 1);
    assert(visitColor(GREEN, ')') == 2);
    assert(visitColor(BLUE, '[') == 3);
    assert(visitColor(RED, ')') == -1);
    return 0;
}