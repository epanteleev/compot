/** bfcrypt.c - A bfish.h API example
 *
 * 	En-/decrypts files with blowfish.
 *
 *	The encrypted data created with this example program, is appended with
 *	a blowfish-block (eight bytes). It stores the encrypted number of null-
 *	padding of the last data-block to be recalled when decrypting.
 *
 * USAGE:
 * 	bfcrypt +secret < file_to_encrypt > file_to_encrypt_to
 * 	bfcrypt -secret < file_to_decrypt > file_to_decrypt_to
 *
 * ---------------------------------------------------------------------------
 *
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
 * Written and made public by C.J.Wagenius - 2020-03-07
 *
 **/

#include <stdio.h>
#include <errno.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>

#define BFISH_IMPL
#include "../bfish.h"

char buf[4096];

void die(char* msg)
{
	if (!msg)
		msg = "USAGE: bfcrypt [+|-]secret < infile > outfile\n"
		      "\n"
		      "    En-/decrypts stdin to stdout. The secret must be "
		      "between 3 & 57 in length.\n\n"
		      "\t+ encrypt\n"
		      "\t- decrypt\n";
	fputs(msg, stderr);
	fputc('\n', stderr);

	exit(EXIT_FAILURE);
}
void decrypt(bfish_t* bf)
{
	int c;
	size_t rb;
	bfblk_t blk;
	
	while ((rb = fread(buf, 1, sizeof(buf), stdin))) {
		if (rb % 8)
			die("stream error");
		bfish_decrypt(bf, buf, rb);
		if ((c = fgetc(stdin)) == EOF) {
			rb -= BFISH_BLOCKSZ;
			bfish_read(&blk, buf + rb, BFISH_BLOCKSZ);
			if (blk.lo && blk.lo <= 8)
				rb -= BFISH_BLOCKSZ - blk.lo;
		} else {
			ungetc(c, stdin);
		}
		fwrite(buf, 1, rb, stdout);
	}
}
void encrypt(bfish_t* bf)
{
	size_t rb;
	bfblk_t blk;
	int mod = 0;

	while ((rb = fread(buf, 1, sizeof(buf), stdin))) {
		mod = rb % 8;
		bfish_encrypt(bf, buf, rb);
		fwrite(buf, 1, bfish_buflen(rb), stdout);
	}
	blk.lo = mod;
	bfish_enblock(bf, &blk);
	bfish_write(&blk, buf);
	fwrite(buf, BFISH_BLOCKSZ, 1, stdout);
}

int main(int argc, char* argv[])
{
	int m;
	bfish_t bf;

	if (argc < 2 || (m = strlen(argv[1] + 1)) < 4 ||
	    m > 56 || !strchr("+-", (m = argv[1][0])))
		die(NULL);
		
	bfish_init(&bf, argv[1] + 1, -1);
	if (m == '+')
		encrypt(&bf);
	else
		decrypt(&bf);
	
	return EXIT_SUCCESS;
}

