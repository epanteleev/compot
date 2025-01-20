# bfish.h - A ANSI C blowfish ECB API

## Structures:
<pre>
    <b>typedef struct bfish bfish_t</b>

	A blowfish-structure which must be initiated with bfish_init()
	before use.


    <b>typedef struct bfblk bfblk_t</b>

	A blowfish-block that is used for en-/decrypting data.
</pre>
## Macros:
<pre>
	<b>BFISH_BLOCKSZ		8
	BFISH_MAX_KEY_LEN	56
	BFISH_MIN_KEY_LEN	 4</b>
</pre>
## Functions:
<pre>
    <b>size_t bfish_buflen(len)</b>

	Returns the required buffer length for encryption of data. In
	other words, this macro returns the closest upper multiple of
	eight.


    <b>void bfish_deblock(bfish_t *bf, bfblk_t *blk)</b>

	Decrypts a blowfish-block of eight bytes. [bf] is a initiated
	blowfish-structure and [blk] is a blowfish-block of bytes.


    <b>void bfish_enblock(bfish_t *bf, bfblk_t *blk)</b>

	Encrypts a blowfish-block of eight bytes. [bf] is a initiated
	blowfish-structure and [blk] is a blowfish-block of bytes.


    <b>void bfish_decrypt(bfish_t *bf, void *buf, size_t len)</b>

	This is a convenience-function that decrypts a buffer of [len]
	number of bytes. [bf] is a pointer to a initiated blowfish-
	structure and [buf] is a pointer to the data to be encrypted.

	[len] must be a multipler of eight, if [buf] is a correct blow-
	fish encrypted string of bytes.

	This functions decrypts to the buffer in place.


    <b>void bfish_encrypt(bfish_t *bf, const void *buf, size_t len)</b>
	
	This is a convenience-function that encrypts a buffer of [len]
	number of bytes. [bf] is a pointer to a initiated blowfish-
	structure and [buf] is a pointer to the data to be encrypted.

	If [len] is -1, buf is assumed to be a null-terminated string,
	and strlen() will be used to calculate the length of [buf].

	This function encrypts to the buffer in place, so it MUST be of
	enough space to hold the encrypted data (which might be more
	than [len]).  If encrypting a string of thirteen bytes, the
	buffer needs to be at least sixteen bytes long, (see
	the function bfish_buflen).


    <b>void bfish_init(bfish_t *bf, const void *key, size_t len)</b>

	Initiates a bluefish-structure for de-/encoding. [bf] is a
	pointer to a allocated blowfish-structure. [key] is the byte-
	string which will be used to do the encryption with. [len] is
	the number of bytes at [key] that should be used.
	If [len] is -1, [key] is assumed to be a null-terminated
	string, and strlen() will be used to calculate its length.


    <b>size_t bfish_read(bfblk_t *blk, const void *buf, size_t len)</b>

	Reads max eight bytes (lesser if [len] < 8) from [buf] and puts
	it in the blowfish-buffer at [blk]. The block are appended with
	zeroes if [len] < 8.


    <b>void bfish_write(const bfblk_t *blk, void *buf)</b>

	Writes the eight byte long blowfish-block [blk] to the buffer
	at [buf].
</pre>

