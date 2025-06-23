
extern int printf(const char *, ...);

typedef enum {
    RED,
    GREEN,
    BLUE,
} Color;

typedef enum {
    A, B, C
} Unused;

int main() {
    Color color = RED;
    switch (color) {
        case RED:
            printf("Red\n");
            return 0;
        case GREEN:
            return 1;
        case BLUE:
            return 2;
    }
    return 3;
}