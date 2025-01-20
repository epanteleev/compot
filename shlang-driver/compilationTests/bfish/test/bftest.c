/**
 * This is free and unencumbered software released into the public domain.
 * 
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 * 
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <https://unlicense.org>
 *
 * Written and made public by C.J.Wagenius - 2021-03-06
 **/

#include <assert.h>
#include <stdlib.h>

#define BFISH_IMPL
#include "../bfish.h"
#include "test_vectors.c"

void tst_variable_key(void)
{
	int i;
	bfish_t bf;
	bfblk_t blk;

	for (i = 0; i < NUM_VARIABLE_KEY_TESTS; i ++) {
		blk.hi = plaintext_vl[i];
		blk.lo = plaintext_vr[i];
		bfish_init(&bf, variable_key[i], 8);
		bfish_enblock(&bf, &blk);
		assert(blk.hi == ciphertext_vl[i]);
		assert(blk.lo == ciphertext_vr[i]);
		bfish_init(&bf, variable_key[i], 8);
		bfish_deblock(&bf, &blk);
		assert(blk.hi == plaintext_vl[i]);
		assert(blk.lo == plaintext_vr[i]);
	}
}
void tst_set_key(void)
{
	int i;
	bfish_t bf;
	bfblk_t blk;

	for (i = 0; i < NUM_SET_KEY_TESTS; i ++) {
		blk.hi = 0xFEDCBA98;
		blk.lo = 0x76543210;
		bfish_init(&bf, set_key, i + 4);
		bfish_enblock(&bf, &blk);
		assert(blk.hi == ciphertext_sl[i]);
		assert(blk.lo == ciphertext_sr[i]);
		bfish_init(&bf, set_key, i + 4);
		bfish_deblock(&bf, &blk);
		assert(blk.hi == 0xFEDCBA98);
		assert(blk.lo == 0x76543210);
	}
}
void tst_crypt(void)
{
	char buf[16];
	bfish_t bf;
	char key[] = "secret";
	char text[] = "blowfish_test";
	unsigned char ciph[] = { 0x64, 0x94, 0xfb, 0xd0, 0xaa, 0xf6, 0xcb,
	    0xe6, 0xf9, 0xf3, 0xd8, 0xaf, 0x98, 0xba, 0x51, 0x80 };

	bfish_init(&bf, key, -1);
	strcpy(buf, text);
	bfish_encrypt(&bf, buf, -1);
	assert(!memcmp(buf, ciph, sizeof(buf)));
	bfish_init(&bf, "secret", -1);
	bfish_decrypt(&bf, buf, sizeof(buf));
	assert(!strcmp(buf, "blowfish_test"));
}
int main(int argc, char **argv)
{
	tst_variable_key();
	tst_set_key();
	tst_crypt();

	return EXIT_SUCCESS;
}
