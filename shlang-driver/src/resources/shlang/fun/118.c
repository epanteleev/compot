#define xmlIsChar_ch(c)		(((0x9 <= (c)) && ((c) <= 0xa)) || \
				 ((c) == 0xd) || \
				  (0x20 <= (c)))

#define xmlIsCharQ(c)		(((c) < 0x100) ? \
				 xmlIsChar_ch((c)) :\
				(((0x100 <= (c)) && ((c) <= 0xd7ff)) || \
				 ((0xe000 <= (c)) && ((c) <= 0xfffd)) || \
				 ((0x10000 <= (c)) && ((c) <= 0x10ffff))))

int IS_CHAR(int c) { return  xmlIsCharQ(c); }

int main() {
    return IS_CHAR('\0');
}