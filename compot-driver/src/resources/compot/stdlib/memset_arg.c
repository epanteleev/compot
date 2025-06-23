#include <stdio.h>
#include <string.h>


typedef struct date_s {
    char mday;
    char mon;
    char d;
    long tm_mday; /* day of the month - [1,31] */
    long tm_mon;  /* months since January - [0,11] */
    long tm_year; /* years - [1980..2044] */
} date;


date set_1(date d) {
    memset(&d, 1, sizeof(date));
    date res;
    memcpy(&res, &d, sizeof(date));

    return res;
}

int main() {
    date d;
    date d1 = set_1(d);

    // Print the date in a specific format
    printf("Date: %d/%d/%d\n", d1.mday, d1.mon, d1.d);

    return 0;
}