static int read_string (char ls) {
  int a = 0;
  switch (ls) {
    case ' ':
      a = 90;
      break;  /* to avoid warnings */
    case '\n':
    case '\r':
      a = 90;
      break;  /* to avoid warnings */
    case '\\': {  /* escape sequences */
      int c;  /* final character to be saved */
      a = 90;
      switch (ls) {
        case 'a': c = '\a'; goto read_save;
        case 'b': c = '\b'; goto read_save;
        case 'f': c = '\f'; goto read_save;
        case 'n': c = '\n'; goto read_save;
        case 'r': c = '\r'; goto read_save;
        case 't': c = '\t'; goto read_save;
        case 'v': c = '\v'; goto read_save;
        case 'x': a = 90; goto read_save;
        case 'u': a = 90;  goto no_save;
        case '\n': case '\r':
          a = 90; c = '\n'; goto only_save;
        case '\\': case '\"': case '\'':
          c = ls; goto read_save;
        case ' ': goto no_save;  /* will raise an error next loop */
        case 'z': {  /* zap following span of spaces */
          a = 90;

          goto no_save;
        }
        default: {
          goto only_save;
        }
      }
     read_save:
       a = 90;
       /* go through */
     only_save:
       a = 90;
       a = 90;
       /* go through */
     no_save: break;
    }
    default:
      a = 90;
  }
  return a;
}

int main() {
  char ls = 'a';  /* Example input */
  return read_string(ls);
}