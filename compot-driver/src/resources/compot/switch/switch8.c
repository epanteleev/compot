#include <assert.h>
#include <string.h>

extern int printf(const char *, ...);

typedef enum {
    RED,
    GREEN,
    BLUE,
} Color;

typedef enum {
    ORANGE,
    APPLE,
    PEAR
} Fruit;

typedef enum {
    A, B, C
} Unused;


char* toString(Fruit f, Color color) {
    Fruit f1;
    switch (color) {
        case RED:
            switch (f) {
                case ORANGE:
                    return "red orange";
                case APPLE:
                    f1 = f;
                    return "red apple";
                case PEAR:
                    return "red pear";
            }
            f1 = f;
        case GREEN:
        case BLUE: {
            switch (f) {
                case ORANGE:
                    return "green orange";
                case APPLE:
                    f1 = f;
                    return "green apple";
                case PEAR:
                    return "green pear";
            }
            f1 = f;
        }
    }
    f1 = f;
    return "nothing";
}

int main() {
    assert(strcmp(toString(APPLE, RED), "red apple") == 0);
    assert(strcmp(toString(APPLE, GREEN), "green apple") == 0);
    return 0;
}