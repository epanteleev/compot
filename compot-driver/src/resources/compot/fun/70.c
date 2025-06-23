
#define NULL 0
#define OPCODE(name, _) TYPE_##name,

enum TYPES {
    OPCODE(NULL, 0)
    OPCODE(ADD, 1)
};

int main() {
    return TYPE_NULL;
}