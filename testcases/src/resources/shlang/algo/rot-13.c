/*********************************************************************
* Copyright:
* Disclaimer: This code is presented "as is" without any guarantees.
* Details:    Performs known-answer tests on the corresponding ROT-13
	          implementation. These tests do not encompass the full
	          range of available test vectors, however, if the tests
	          pass it is very, very likely that the code is correct
	          and was compiled properly. This code also serves as
	          example usage of the functions.
*********************************************************************/

/*************************** HEADER FILES ***************************/
#include <stdio.h>
#include <string.h>

////////////////////////////////////////////////////////////////////// rot-13.c

/*********************************************************************
* Filename:   rot-13.c
* Author:     Brad Conte (brad AT bradconte.com)
* Copyright:
* Disclaimer: This code is presented "as is" without any guarantees.
* Details:    Implementation of the ROT-13 encryption algorithm.
				  Algorithm specification can be found here:
				   *
				  This implementation uses little endian byte order.
*********************************************************************/

/*************************** HEADER FILES ***************************/
#include <string.h>

/*********************** FUNCTION DEFINITIONS ***********************/
void rot13(char str[])
{
   int case_type, idx, len;

   for (idx = 0, len = strlen(str); idx < len; idx++) {
      // Only process alphabetic characters.
      if (str[idx] < 'A' || (str[idx] > 'Z' && str[idx] < 'a') || str[idx] > 'z')
         continue;
      // Determine if the char is upper or lower case.
      if (str[idx] >= 'a')
         case_type = 'a';
      else
         case_type = 'A';
      // Rotate the char's value, ensuring it doesn't accidentally "fall off" the end.
      str[idx] = (str[idx] + 13) % (case_type + 26);
      if (str[idx] < 26)
         str[idx] += case_type;
   }
}

////////////////////////////////////////////////////////////////////// rot-13-test.c

/*********************** FUNCTION DEFINITIONS ***********************/
int rot13_test()
{
	char text[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	char code[] = "NOPQRSTUVWXYZABCDEFGHIJKLMnopqrstuvwxyzabcdefghijklm";
	char buf[1024];
	int pass = 1;

	// To encode, just apply ROT-13.
	strcpy(buf, text);
	rot13(buf);
	pass = pass && !strcmp(code, buf);

	// To decode, just re-apply ROT-13.
	rot13(buf);
	pass = pass && !strcmp(text, buf);

	return(pass);
}

int main()
{
	printf("ROT-13 tests: %s\n", rot13_test() ? "SUCCEEDED" : "FAILED");

	return(0);
}