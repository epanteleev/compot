
int main() {
    int i = 0;
    int y = 0;
    do {
        i++;
        if( i > 10 ) {
            continue;
        }
        y++;

    } while( i < 50  );
    return y + i;
}