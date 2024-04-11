package parser

import tokenizer.*
import parser.nodes.*
import tokenizer.Specifiers.keywords
import types.PointerQualifier
import types.StorageClass
import types.TypeProperty
import kotlin.math.E


data class ParserException(val info: ProgramMessage) : Exception(info.message)


// https://cs.wmich.edu/~gupta/teaching/cs4850/sumII06/The%20syntax%20of%20C%20in%20Backus-Naur%20form.htm
class ProgramParser(firstToken: AnyToken) {
    private var current: AnyToken = firstToken

    private fun eat() {
        if (current is Eof) {
            throw ParserException(ProgramMessage("Unexpected EOF", current))
        }

        current = (current as CToken).next
    }

    private inline fun<reified T: CToken> peak(): T {
        return current as T
    }

    private fun check(s: String): Boolean {
        return current is CToken && (current as CToken).str() == s
    }

    private inline fun<reified T> check(): Boolean {
        return current is T
    }

    private inline fun<reified T> rule(fn: () -> T?): T? {
        val saved = current
        val result = fn()
        if (result == null) {
            current = saved
        }
        return result
    }

    // translation_unit
    //	: external_declaration
    //	| translation_unit external_declaration
    //	;
    fun program(): ProgramNode {//TODO
        val nodes = mutableListOf<Node>()
        while (current !is Eof) {
            val node = external_declaration()?:
                throw ParserException(ProgramMessage("Expected external declaration", current))
            nodes.add(node)
        }

        return ProgramNode(nodes)
    }

    // function_definition
    //	: declaration_specifiers declarator declaration_list compound_statement
    //	| declaration_specifiers declarator compound_statement
    //	| declarator declaration_list compound_statement
    //	| declarator compound_statement
    //	;
    fun function_definition(): Node? = rule {
        val declspec = declaration_specifiers()
        if (declspec == null) {
            val declarator = declarator()?: return@rule null
            val body = compound_stmt() ?: throw ParserException(ProgramMessage("Expected compound statement", current))
            return@rule FunctionNode(DeclarationSpecifier.EMPTY, declarator, body)
        }
        val declarator = declarator()?: return@rule null
        val body = compound_stmt() ?: throw ParserException(ProgramMessage("Expected compound statement", current))
        return@rule FunctionNode(declspec, declarator, body)
    }

    // stmt = "return" expr? ";"
    //      | "if" "(" expr ")" stmt ("else" stmt)?
    //      | "switch" "(" expr ")" stmt
    //      | "case" const-expr ("..." const-expr)? ":" stmt
    //      | "default" ":" stmt
    //      | "for" "(" (declaration | expr-stmt) expr? ";" expr? ")" stmt
    //      | "while" "(" expr ")" stmt
    //      | "do" stmt "while" "(" expr ")" ";"
    //      | "asm" asm-stmt
    //      | "goto" (ident | "*" expr) ";"
    //      | "break" ";"
    //      | "continue" ";"
    //      | ident ":" stmt
    //      | "{" compound-stmt
    //      | expr-stmt
    fun stmt(): Statement {
        while (check<CToken>()) {
            val token = peak<CToken>()
            if (check("return")) {
                eat()
                if (check(";")) {
                    eat()
                    return ReturnStatement(EmptyExpression())
                }
                val expr = conditional_expression()?: throw ParserException(ProgramMessage("Expected conditional expression", current))
                if (check(";")) {
                    eat()
                    return ReturnStatement(expr)
                } else {
                    throw ParserException(ProgramMessage("Expected ';'", current))
                }
            }
            if (check("if")) {
                eat()
                if (check("(")) {
                    eat()
                    val condition = conditional_expression()?: throw ParserException(ProgramMessage("Expected conditional expression", current))
                    if (check(")")) {
                        eat()
                        val then = stmt()
                        if (check("else")) {
                            eat()
                            val els = stmt()
                            return IfStatement(condition, then, els)
                        }
                        return IfStatement(condition, then, EmptyStatement())
                    } else {
                        throw ParserException(ProgramMessage("Expected ')'", current))
                    }
                } else {
                    throw ParserException(ProgramMessage("Expected '('", current))
                }
            }
            if (check("while")) {
                eat()
                if (check("(")) {
                    eat()
                    val condition = conditional_expression()?: throw ParserException(ProgramMessage("Expected conditional expression", current))
                    if (check(")")) {
                        eat()
                        val body = stmt()
                        return WhileStatement(condition, body)
                    } else {
                        throw ParserException(ProgramMessage("Expected ')'", current))
                    }
                } else {
                    throw ParserException(ProgramMessage("Expected '('", current))
                }
            }
            if (check("for")) {
                eat()
                if (check("(")) {
                    eat()

                    val init = declaration() ?: expr_stmt()?: DummyNode

                    val condition = if (!check(";")) {
                        expression()?: throw ParserException(ProgramMessage("Expected expression", current))
                    } else {
                        DummyNode
                    }

                    if (check(";")) {
                        eat()

                        val update = if (!check(")")) {
                            expression()?: throw ParserException(ProgramMessage("Expected expression", current))
                        } else {
                            DummyNode
                        }
                        if (check(")")) {
                            eat()
                            val body = stmt()
                            return ForStatement(init, condition, update, body)
                        } else {
                            throw ParserException(ProgramMessage("Expected ')'", current))
                        }
                    } else {
                        throw ParserException(ProgramMessage("Expected ';'", current))
                    }
                } else {
                    throw ParserException(ProgramMessage("Expected '('", current))
                }
            }
            if (check("do")) {
                eat()
                val body = stmt()
                if (check("while")) {
                    eat()
                    if (check("(")) {
                        eat()
                        val condition = conditional_expression()?: throw ParserException(ProgramMessage("Expected conditional expression", current))
                        if (check(")")) {
                            eat()
                            if (check(";")) {
                                eat()
                                return DoWhileStatement(body, condition)
                            } else {
                                throw ParserException(ProgramMessage("Expected ';'", current))
                            }
                        } else {
                            throw ParserException(ProgramMessage("Expected ')'", current))
                        }
                    } else {
                        throw ParserException(ProgramMessage("Expected '('", current))
                    }
                } else {
                    throw ParserException(ProgramMessage("Expected 'while'", current))
                }
            }
            if (token.str() == "switch") {
                eat()
                if (check("(")) {
                    eat()
                    val condition = conditional_expression()?: throw ParserException(ProgramMessage("Expected conditional expression", current))
                    if (check(")")) {
                        eat()
                        val body = stmt()
                        return SwitchStatement(condition, body)
                    } else {
                        throw ParserException(ProgramMessage("Expected ')'", current))
                    }
                } else {
                    throw ParserException(ProgramMessage("Expected '('", current))
                }
            }
            if (token.str() == "case") {
                eat()
                val expr = conditional_expression()?: throw ParserException(ProgramMessage("Expected conditional expression", current))
                if (check(":")) {
                    eat()
                    val stmt = stmt()
                    return CaseStatement(expr, stmt)
                } else {
                    throw ParserException(ProgramMessage("Expected ':'", current))
                }
            }
            if (token.str() == "default") {
                eat()
                if (check(":")) {
                    eat()
                    val stmt = stmt()
                    return DefaultStatement(stmt)
                } else {
                    throw ParserException(ProgramMessage("Expected ':'", current))
                }
            }
            if (token.str() == "break") {
                eat()
                if (check(";")) {
                    eat()
                    return BreakStatement()
                } else {
                    throw ParserException(ProgramMessage("Expected ';'", current))
                }
            }
            if (token.str() == "continue") {
                eat()
                if (check(";")) {
                    eat()
                    return ContinueStatement()
                } else {
                    throw ParserException(ProgramMessage("Expected ';'", current))
                }
            }
            if (token.str() == "goto") {
                eat()
                val label = peak<Ident>()
                eat()
                if (check(";")) {
                    eat()
                    return GotoStatement(label)
                } else {
                    throw ParserException(ProgramMessage("Expected ';'", current))
                }
            }
            if (current is Ident && (current as Ident).next is CToken && ((current as Ident).next as CToken).str() == ":"){
                val label = peak<Ident>()
                current = label.next
                eat()
                val stmt = stmt()
                return LabeledStatement(label, stmt)
            }
            if (token.str() == ";") {
                eat()
                return EmptyStatement()
            }
            return compound_stmt()?: expr_stmt() ?: EmptyStatement()
        }
        // TODO unreachable
        return EmptyStatement()
    }

    // initializer
    //	: assignment_expression
    //	| '{' initializer_list '}'
    //	| '{' initializer_list ',' '}'
    //	;
    fun initializer(): Expression? = rule {
        if (check("{")) {
            eat()
            val list = initializerList()
            if (check("}")) {
                eat()
                return@rule list
            } else {
                throw ParserException(ProgramMessage("Expected '}'", current))
            }
        }
        if (check<StringLiteral>()) {
            val str = peak<StringLiteral>()
            eat()
            return@rule StringNode(str)
        }
        return@rule expression()
    }


    // compound_statement
    //	: '{' '}'
    //	| '{' statement_list '}'
    //	| '{' declaration_list '}'
    //	| '{' declaration_list statement_list '}'
    //	;
    fun compound_stmt(): Statement? = rule {
        if (!check("{")) {
            return@rule null
        }
        eat()
        if (check("}")) {
            eat()
            return@rule EmptyStatement()
        }
        val statements = mutableListOf<Node>()
        while (!check("}")) {
            statements.add(declaration()?: stmt())
        }
        eat()
        return@rule CompoundStatement(statements)
    }

    // expression
    //	: assignment_expression
    //	| expression ',' assignment_expression
    //	;
    fun expression(): Expression? = rule {
        val assign = assignment_expression()?: return@rule null
        if (check(",")) {
            eat()
            val expr = expression()?: throw ParserException(ProgramMessage("Expected expression", current))
            return@rule BinaryOp(assign, expr, BinaryOpType.COMMA)
        }
        return@rule assign
    }

    // assign    = conditional (assign-op assign)?
    // assign-op = "=" | "+=" | "-=" | "*=" | "/=" | "%=" | "&=" | "|=" | "^="
    //           | "<<=" | ">>="
    fun assignment_expression(): Expression? = rule {
        val cond = conditional_expression()?: return@rule null
        if (check("=")) {
            eat()
            val assign = assignment_expression()?: throw ParserException(ProgramMessage("Expected assignment expression", current))
            return@rule BinaryOp(cond, assign, BinaryOpType.ASSIGN)
        }
        if (check("+=")) {
            eat()
            val assign = assignment_expression()?: throw ParserException(ProgramMessage("Expected assignment expression", current))
            return@rule BinaryOp(cond, assign, BinaryOpType.ADD_ASSIGN)
        }
        if (check("-=")) {
            eat()
            val assign = assignment_expression()?: throw ParserException(ProgramMessage("Expected assignment expression", current))
            return@rule BinaryOp(cond, assign, BinaryOpType.SUB_ASSIGN)
        }
        if (check("*=")) {
            eat()
            val assign = assignment_expression()?: throw ParserException(ProgramMessage("Expected assignment expression", current))
            return@rule BinaryOp(cond, assign, BinaryOpType.MUL_ASSIGN)
        }
        if (check("/=")) {
            eat()
            val assign = assignment_expression()?: throw ParserException(ProgramMessage("Expected assignment expression", current))
            return@rule BinaryOp(cond, assign, BinaryOpType.DIV_ASSIGN)
        }
        if (check("%=")) {
            eat()
            val assign = assignment_expression()?: throw ParserException(ProgramMessage("Expected assignment expression", current))
            return@rule BinaryOp(cond, assign, BinaryOpType.MOD_ASSIGN)
        }
        if (check("&=")) {
            eat()
            val assign = assignment_expression()?: throw ParserException(ProgramMessage("Expected assignment expression", current))
            return@rule BinaryOp(cond, assign, BinaryOpType.BIT_AND_ASSIGN)
        }
        if (check("|=")) {
            eat()
            val assign = assignment_expression()?: throw ParserException(ProgramMessage("Expected assignment expression", current))
            return@rule BinaryOp(cond, assign, BinaryOpType.BIT_OR_ASSIGN)
        }
        if (check("^=")) {
            eat()
            val assign = assignment_expression()?: throw ParserException(ProgramMessage("Expected assignment expression", current))
            return@rule BinaryOp(cond, assign, BinaryOpType.BIT_XOR_ASSIGN)
        }
        if (check("<<=")) {
            eat()
            val assign = assignment_expression()?: throw ParserException(ProgramMessage("Expected assignment expression", current))
            return@rule BinaryOp(cond, assign, BinaryOpType.SHL_ASSIGN)
        }
        if (check(">>=")) {
            eat()
            val assign = assignment_expression()?: throw ParserException(ProgramMessage("Expected assignment expression", current))
            return@rule BinaryOp(cond, assign, BinaryOpType.SHR_ASSIGN)
        }
        return@rule cond
    }

    // storage_class_specifier
    //	: TYPEDEF
    //	| EXTERN
    //	| STATIC
    //	| AUTO
    //	| REGISTER
    //	;
    fun storage_class_specifier(): StorageClassSpecifier? = rule {
        if (check("typedef")) {
            val tok = peak<Ident>()
            eat()
            return@rule StorageClassSpecifier(tok)
        }
        if (check("extern")) {
            val tok = peak<Ident>()
            eat()
            return@rule StorageClassSpecifier(tok)
        }
        if (check("static")) {
            val tok = peak<Ident>()
            eat()
            return@rule StorageClassSpecifier(tok)
        }
        if (check("auto")) {
            val tok = peak<Ident>()
            eat()
            return@rule StorageClassSpecifier(tok)
        }
        if (check("register")) {
            val tok = peak<Ident>()
            eat()
            return@rule StorageClassSpecifier(tok)
        }
        return@rule null
    }

    // expression_statement
    //	: ';'
    //	| expression ';'
    //	;
    fun expr_stmt(): Statement? = rule {
        if (check(";")) {
            eat()
            return@rule EmptyStatement()
        }
        val expr = expression()?: return@rule null
        if (check(";")) {
            eat()
            return@rule ExprStatement(expr)
        }
        throw ParserException(ProgramMessage("Expected ';'", current))
    }

    //init_declarator = declarator
    //	              | declarator '=' initializer
    //	              ;
    fun init_declarator(): AnyDeclarator? = rule {
        val declarator = declarator()?: return@rule null
        if (check("=")) {
            eat()
            val initializer = initializer()?: throw ParserException(ProgramMessage("Expected initializer", current))
            return@rule AssignmentDeclarator(declarator, initializer) //TODO
        }
        return@rule declarator
    }

    // init_declarator_list
    //	: init_declarator
    //	| init_declarator_list ',' init_declarator
    //	;
    fun init_declarator_list(): List<AnyDeclarator> {
        val initDeclarators = mutableListOf<AnyDeclarator>()
        while (check<CToken>()) {
            val initDeclarator = init_declarator() ?: return initDeclarators
            initDeclarators.add(initDeclarator)
            if (check(",")) {
                eat()
            } else {
                return initDeclarators
            }
        }
        throw ParserException(ProgramMessage("Expected ';'", current))
    }


    // declaration
    //	: declaration_specifiers ';'
    //	| declaration_specifiers init_declarator_list ';'
    //	;
    fun declaration(): Declaration? = rule {
        val declspec = declaration_specifiers() ?: return@rule null
        if (check(";")) {
            eat()
            return@rule Declaration(declspec, listOf())
        }
        val initDeclaratorList = init_declarator_list()
        if (check(";")) {
            eat()
            return@rule Declaration(declspec, initDeclaratorList)
        }
        return@rule null
    }

    // parameter_type_list
    //	: parameter_list
    //	| parameter_list ',' '...' <- skip it here
    //	;
    fun parameter_type_list(): List<AnyParameter>? = rule {
        val parameters = parameter_list()
        if (parameters.isEmpty()) {
            return@rule null
        }

        return@rule parameters
    }

    // parameter_list
    //	: parameter_declaration
    //	| parameter_list ',' parameter_declaration
    //	;
    fun parameter_list(): List<AnyParameter> {
        val parameters = mutableListOf<AnyParameter>()
        val param = parameter_declaration()
        if (param != null) {
            parameters.add(param)
        }
        while (true) {
            val parameter = rule {
                if (!check(",")) {
                    return@rule null
                }
                eat()
                if (check("...")) {
                    eat()
                    return@rule ParameterVarArg()
                }
                parameter_declaration() ?: throw ParserException(ProgramMessage("Expected parameter declaration", current))
            }
            if (parameter == null) {
                return parameters
            }
            parameters.add(parameter)
        }
    }

    // struct_declarator
    //	: declarator
    //	| ':' constant_expression
    //	| declarator ':' constant_expression
    //	;
    fun struct_declarator(): Node? = rule {
        if (check(":")) {
            eat()
            val expr = constant_expression()
            return@rule expr
        }
        val declarator = declarator()?: return@rule null
        if (check(":")) {
            eat()
            val expr = constant_expression()
            return@rule expr
        }
        return@rule declarator
    }

    // struct_declarator_list
    //	: struct_declarator
    //	| struct_declarator_list ',' struct_declarator
    //	;
    fun struct_declarator_list(): List<Node> {
        val declarators = mutableListOf<Node>()
        while (true) {
            val declarator = struct_declarator()?: return declarators
            declarators.add(declarator)
            if (check(",")) {
                eat()
            } else {
                return declarators
            }
        }
    }

    // struct_declaration
    //	: specifier_qualifier_list struct_declarator_list ';'
    //	;
    fun struct_declaration(): StructField? = rule {
        val declspec = specifier_qualifier_list()?: return@rule null
        val declarators = struct_declarator_list()
        if (check(";")) {
            eat()
            return@rule StructField(declspec, declarators)
        }
        throw ParserException(ProgramMessage("Expected ';'", current))
    }

    // struct_declaration_list
    //	: struct_declaration
    //	| struct_declaration_list struct_declaration
    //	;
    fun struct_declaration_list(): List<StructField> {
        val fields = mutableListOf<StructField>()
        while (true) {
            val field = struct_declaration() ?: return fields
            fields.add(field)
            if (check("}")) {
                return fields
            }
        }
    }

    // struct_or_union_specifier
    //	: struct_or_union IDENTIFIER '{' struct_declaration_list '}'
    //	| struct_or_union '{' struct_declaration_list '}'
    //	| struct_or_union IDENTIFIER
    //	;
    fun struct_or_union_specifier(): AnyTypeNode? = rule {
        if (check("struct")) {
            eat()
            if (check("{")) {
                eat()
                val fields = struct_declaration_list()
                if (check("}")) {
                    eat()
                    return@rule StructSpecifier(Ident.UNKNOWN, fields)
                }
                throw ParserException(ProgramMessage("Expected '}'", current))
            }
            if (check<Ident>()) {
                val name = peak<Ident>()
                eat()
                if (check("{")) {
                    eat()
                    val fields = struct_declaration_list()
                    if (check("}")) {
                        eat()
                        return@rule StructSpecifier(name, fields)
                    }
                    throw ParserException(ProgramMessage("Expected '}'", current))
                }
                return@rule StructDeclaration(name)
            }
            throw ParserException(ProgramMessage("Expected identifier", current))
        }
        if (check("union")) {
            eat()
            if (check("{")) {
                eat()
                val fields = struct_declaration_list()
                if (check("}")) {
                    eat()
                    return@rule UnionSpecifier(Ident.UNKNOWN, fields)
                }
                throw ParserException(ProgramMessage("Expected '}'", current))
            }
            if (check<Ident>()) {
                val name = peak<Ident>()
                eat()
                if (check("{")) {
                    eat()
                    val fields = struct_declaration_list()
                    if (check("}")) {
                        eat()
                        return@rule UnionSpecifier(name, fields)
                    }
                    throw ParserException(ProgramMessage("Expected '}'", current))
                }
                return@rule UnionDeclaration(name)
            }
            throw ParserException(ProgramMessage("Expected identifier", current))
        }
        return@rule null
    }

    // enumerator
    //	: IDENTIFIER
    //	| IDENTIFIER '=' constant_expression
    //	;
    fun enumerator(): Enumerator? = rule {
        if (check<Ident>()) {
            val name = peak<Ident>()
            eat()
            if (check("=")) {
                eat()
                val expr = constant_expression()?: throw ParserException(ProgramMessage("Expected constant expression", current))
                return@rule Enumerator(name, expr)
            }
            return@rule Enumerator(name, DummyNode)
        }
        return@rule null
    }

    // enumerator_list
    //	: enumerator
    //	| enumerator_list ',' enumerator
    //	;
    fun enumerator_list(): List<Enumerator> {
        val enumerators = mutableListOf<Enumerator>()
        while (true) {
            val enumerator = enumerator()?: return enumerators
            enumerators.add(enumerator)
            if (check(",")) {
                eat()
            } else {
                return enumerators
            }
        }
    }

    // enum_specifier
    //	: 'enum' '{' enumerator_list '}'
    //	| 'enum' IDENTIFIER '{' enumerator_list '}'
    //	| 'enum' IDENTIFIER
    //	;
    fun enum_specifier(): AnyTypeNode? = rule {
        if (!check("enum")) {
            return@rule null
        }
        eat()
        if (check("{")) {
            eat()
            val enumerators = enumerator_list()
            if (check("}")) {
                eat()
                return@rule EnumSpecifier(Ident.UNKNOWN, enumerators)
            }
            throw ParserException(ProgramMessage("Expected '}'", current))
        }
        if (check<Ident>()) {
            val name = peak<Ident>()
            eat()
            if (check("{")) {
                eat()
                val enumerators = enumerator_list()
                if (check("}")) {
                    eat()
                    return@rule EnumSpecifier(name, enumerators)
                }
                throw ParserException(ProgramMessage("Expected '}'", current))
            }
            return@rule EnumDeclaration(name)
        }
        throw ParserException(ProgramMessage("Expected identifier", current))
    }

    // type_specifier
    //	: VOID
    //	| CHAR
    //	| SHORT
    //	| INT
    //	| LONG
    //	| FLOAT
    //	| DOUBLE
    //	| SIGNED
    //	| UNSIGNED
    //	| struct_or_union_specifier
    //	| enum_specifier
    //	| TYPE_NAME
    //	;
    fun type_specifier(): AnyTypeNode? = rule {
        if (check("int")) {
            val tok = peak<Ident>()
            eat()
            return@rule TypeNode(tok)
        }
        if (check("char")) {
            val tok = peak<Ident>()
            eat()
            return@rule TypeNode(tok)
        }
        if (check("short")) {
            val tok = peak<Ident>()
            eat()
            return@rule TypeNode(tok)
        }
        if (check("long")) {
            val tok = peak<Ident>()
            eat()
            return@rule TypeNode(tok)
        }
        if (check("float")) {
            val tok = peak<Ident>()
            eat()
            return@rule TypeNode(tok)
        }
        if (check("double")) {
            val tok = peak<Ident>()
            eat()
            return@rule TypeNode(tok)
        }
        if (check("void")) {
            val tok = peak<Ident>()
            eat()
            return@rule TypeNode(tok)
        }
        return@rule struct_or_union_specifier() ?: enum_specifier()
    }

    // type_qualifier
    //	: CONST
    //	| VOLATILE
    //  | RESTRICT
    //	;
    fun type_qualifier(): TypeQualifier? = rule {
        if (check("const")) {
            val tok = peak<Ident>()
            eat()
            return@rule TypeQualifier(tok)
        }
        if (check("volatile")) {
            val tok = peak<Ident>()
            eat()
            return@rule TypeQualifier(tok)
        }
        if (check("restrict")) {
            val tok = peak<Ident>()
            eat()
            return@rule TypeQualifier(tok)
        }
        return@rule null
    }

    // declaration_specifiers
    //	: storage_class_specifier
    //	| storage_class_specifier declaration_specifiers
    //	| type_specifier
    //	| type_specifier declaration_specifiers
    //	| type_qualifier
    //	| type_qualifier declaration_specifiers
    //	;
    fun declaration_specifiers(): DeclarationSpecifier? = rule {
        val specifiers = mutableListOf<AnyTypeNode>()
        while (true) {
            val storageClass = storage_class_specifier()
            if (storageClass != null) {
                specifiers.add(storageClass)
                continue
            }
            val type = type_specifier()
            if (type != null) {
                specifiers.add(type)
                continue
            }
            val typeQualifier = type_qualifier()
            if (typeQualifier != null) {
                specifiers.add(typeQualifier)
                continue
            }
            break
        }

        return@rule if (specifiers.isEmpty()) {
            null
        } else {
            DeclarationSpecifier(specifiers)
        }
    }

    // parameter_declaration
    //	: declaration_specifiers declarator
    //	| declaration_specifiers abstract_declarator
    //	| declaration_specifiers
    //	;
    fun parameter_declaration(): Parameter? = rule {
        val declspec = declaration_specifiers() ?: return@rule null

        val declarator = declarator()
        if (declarator == null) {
            val abstractDeclarator = abstract_declarator()?: return@rule Parameter(declspec, EmptyDeclarator)
            return@rule Parameter(declspec, abstractDeclarator)
        }
        return@rule Parameter(declspec, declarator)
    }

    // direct_declarator
    //	: IDENTIFIER
    //	| '(' declarator ')'
    //	| direct_declarator '[' constant_expression ']'
    //	| direct_declarator '[' ']'
    //	| direct_declarator '(' parameter_type_list ')'
    //	| direct_declarator '(' identifier_list ')'
    //	| direct_declarator '(' ')'
    fun direct_declarator(): DirectDeclarator? = rule {
        fun declarator_list(): List<Node> {
            val declarators = mutableListOf<Node>()
            while (true) {
                if (check("(")) {
                    eat()
                    if (check(")")) {
                        eat()
                        declarators.add(FunctionDeclarator(listOf()))
                        continue
                    }
                    val declspec = parameter_type_list()
                    if (declspec != null) {
                        if (check(")")) {
                            eat()
                            declarators.add(FunctionDeclarator(declspec))
                            continue
                        }
                        throw ParserException(ProgramMessage("Expected ')'", current))
                    }
                    val identifiers = identifier_list()
                    if (check(")")) {
                        eat()
                        declarators.add(FunctionPointerDeclarator(identifiers))
                        continue
                    }

                    throw ParserException(ProgramMessage("Expected ')'", current))
                }
                if (check("[")) {
                    eat()
                    if (check("]")) {
                        eat()
                        declarators.add(ArrayDeclarator(DummyNode))
                        continue
                    }
                    val size = constant_expression()?: throw ParserException(ProgramMessage("Expected constant expression", current))
                    if (check("]")) {
                        eat()
                        declarators.add(ArrayDeclarator(size))
                        continue
                    }
                    throw ParserException(ProgramMessage("Expected ']'", current))
                }
                break
            }
            return declarators
        }

        if (check("(")) {
            eat()
            val declarator = declarator()
            if (declarator != null) {
                if (check(")")) {
                    eat()
                    val declarators = declarator_list()
                    return@rule DirectDeclarator(FunctionPointerDeclarator(listOf(declarator)), declarators)
                }
                throw ParserException(ProgramMessage("Expected ')'", current))
            }
        }
        if (check<Ident>()) {
            val ident = peak<Ident>()
            eat()
            return@rule DirectDeclarator(VarDeclarator(ident), declarator_list())
        }
        return@rule null
    }

    // constant_expression
    //	: conditional_expression
    //	;
    fun constant_expression(): Node? = rule {
        return@rule conditional_expression()
    }

    // identifier_list
    //	: IDENTIFIER
    //	| identifier_list ',' IDENTIFIER
    //	;
    fun identifier_list(): List<IdentNode> {
        val identifiers = mutableListOf<IdentNode>()
        while (true) {
            if (check<Ident>()) {
                val ident = peak<Ident>()
                eat()
                identifiers.add(IdentNode(ident))
                if (check(",")) {
                    eat()
                } else {
                    return identifiers
                }
            }
        }
    }

    // pointers = ("*" ("const" | "volatile" | "restrict")*)*
    fun pointers(): List<NodePointer>? = rule {
        val pointers = mutableListOf<NodePointer>()
        while (check("*")) {
            eat()
            val qualifiers = mutableListOf<PointerQualifier>()
            while (check<CToken>()) {
                val token = peak<CToken>()
                if (token.str() == "const") { //TODO
                    qualifiers.add(PointerQualifier.CONST)
                    eat()
                } else if (token.str() == "volatile") {
                    qualifiers.add(PointerQualifier.VOLATILE)
                    eat()
                } else if (token.str() == "restrict") {
                    qualifiers.add(PointerQualifier.RESTRICT)
                    eat()
                } else {
                    break
                }
            }
            if (qualifiers.isEmpty()) {
                qualifiers.add(PointerQualifier.EMPTY)
            }
            pointers.add(NodePointer(qualifiers))
        }

        return@rule if (pointers.isEmpty()) {
            null
        } else {
            pointers
        }
    }

    // declarator
    //	: pointer direct_declarator
    //	| direct_declarator
    //	;
    fun declarator(): Declarator? = rule {
        val pointers = pointers()
        if (pointers != null) {
            val directDeclarator = direct_declarator()?: throw ParserException(ProgramMessage("Expected direct declarator", current))
            return@rule Declarator(directDeclarator, pointers)
        }
        val directDeclarator = direct_declarator()?: return@rule null
        return@rule Declarator(directDeclarator, listOf())
    }

    // direct_abstract_declarator
    //	: '(' abstract_declarator ')'
    //	| '[' ']'
    //	| '[' constant_expression ']'
    //	| direct_abstract_declarator '[' ']'
    //	| direct_abstract_declarator '[' constant_expression ']'
    //	| '(' ')'
    //	| '(' parameter_type_list ')'
    //	| direct_abstract_declarator '(' ')'
    //	| direct_abstract_declarator '(' parameter_type_list ')'
    //	;
    fun direct_abstract_declarator(): List<Node>? = rule {
        if (!check("(") && !check("[")) {
            return@rule null
        }

        val abstractDeclarators = mutableListOf<Node>()
        while (true) {
            if (check("(")) {
                eat()
                if (check(")")) {
                    eat()
                    abstractDeclarators.add(DirectFunctionDeclarator(listOf()))
                    continue
                }

                val parameters = parameter_type_list()
                if (parameters != null) {
                    if (check(")")) {
                        eat()
                        abstractDeclarators.add(DirectFunctionDeclarator(parameters))
                        continue
                    }
                    throw ParserException(ProgramMessage("Expected ')'", current))
                }
                throw ParserException(ProgramMessage("Expected ')'", current))
            }
            if (check("[")) {
                eat()
                if (check("]")) {
                    eat()
                    abstractDeclarators.add(DirectArrayDeclarator(DummyNode))
                    continue
                }
                val size = constant_expression()?: throw ParserException(ProgramMessage("Expected constant expression", current))
                if (check("]")) {
                    eat()
                    abstractDeclarators.add(DirectArrayDeclarator(size))
                    continue
                }
                throw ParserException(ProgramMessage("Expected ']'", current))
            }
            break
        }
        return@rule abstractDeclarators
    }

    // conditional_expression
    //	: logical_or_expression
    //	| logical_or_expression '?' expression ':' conditional_expression
    //	;
    fun conditional_expression(): Expression? = rule {
        val logor = logical_or_expression()?: return@rule null
        if (check("?")) {
            eat()
            val expr = expression()?: throw ParserException(ProgramMessage("Expected expression", current))
            if (check(":")) {
                eat()
                val conditional = conditional_expression()?: throw ParserException(ProgramMessage("Expected conditional expression", current))
                return@rule Conditional(logor, expr, conditional)
            } else {
                throw ParserException(ProgramMessage("Expected ':'", current))
            }
        }
        return@rule logor
    }

    // logical_or_expression
    //	: logical_and_expression
    //	| logical_or_expression '||' logical_and_expression
    //	;
    fun logical_or_expression(): Expression? = rule {
        val logand = logical_and_expression()?: return@rule null
        if (check("||")) {
            eat()
            val logor = logical_or_expression()?: throw ParserException(ProgramMessage("Expected logical expression", current))
            return@rule BinaryOp(logand, logor, BinaryOpType.OR)
        }
        return@rule logand
    }

    // logical_and_expression
    //	: inclusive_or_expression
    //	| logical_and_expression '&&' inclusive_or_expression
    //	;
    fun logical_and_expression(): Expression? = rule {
        val bitor = inclusive_or_expression()?: return@rule null
        if (check("&&")) {
            eat()
            val logand = logical_and_expression()?: throw ParserException(ProgramMessage("Expected logical expression", current))
            return@rule BinaryOp(bitor, logand, BinaryOpType.AND)
        }
        return@rule bitor
    }

    // inclusive_or_expression
    //	: exclusive_or_expression
    //	| inclusive_or_expression '|' exclusive_or_expression
    //	;
    fun inclusive_or_expression(): Expression? = rule {
        val bitxor = exclusive_or_expression()?: return@rule null
        if (check("|")) {
            eat()
            val bitor = inclusive_or_expression()?: throw ParserException(ProgramMessage("Expected inclusive expression", current))
            return@rule BinaryOp(bitxor, bitor, BinaryOpType.BIT_OR)
        }
        return@rule bitxor
    }

    // exclusive_or_expression
    //	: and_expression
    //	| exclusive_or_expression '^' and_expression
    //	;
    fun exclusive_or_expression(): Expression? = rule {
        val bitand = and_expression()?: return@rule null
        if (check("^")) {
            eat()
            val bitxor = exclusive_or_expression()?: throw ParserException(ProgramMessage("Expected exclusive expression", current))
            return@rule BinaryOp(bitand, bitxor, BinaryOpType.BIT_XOR)
        }
        return@rule bitand
    }

    // and_expression
    //	: equality_expression
    //	| and_expression '&' equality_expression
    //	;
    fun and_expression(): Expression? = rule {
        val equality = equality_expression()?: return@rule null
        if (check("&")) {
            eat()
            val bitand = and_expression()?: throw ParserException(ProgramMessage("Expected 'and' expression", current))
            return@rule BinaryOp(equality, bitand, BinaryOpType.BIT_AND)
        }
        return@rule equality
    }

    // equality_expression
    //	: relational_expression
    //	| equality_expression '==' relational_expression
    //	| equality_expression '!=' relational_expression
    //	;
    fun equality_expression(): Expression? = rule {
        val relational = relational_expression()?: return@rule null
        if (check("==")) {
            eat()
            val equality = equality_expression()?: throw ParserException(ProgramMessage("Expected equality expression", current))
            return@rule BinaryOp(relational, equality, BinaryOpType.EQ)
        }
        if (check("!=")) {
            eat()
            val equality = equality_expression()?: throw ParserException(ProgramMessage("Expected equality expression", current))
            return@rule BinaryOp(relational, equality, BinaryOpType.NE)
        }
        return@rule relational
    }

    // relational_expression
    //	: shift_expression
    //	| relational_expression '<' shift_expression
    //	| relational_expression '>' shift_expression
    //	| relational_expression '<=' shift_expression
    //	| relational_expression '>=' shift_expression
    //	;
    fun relational_expression(): Expression? = rule {
        val shift = shift_expression()?: return@rule null
        if (check("<")) {
            eat()
            val relational = relational_expression()?: throw ParserException(ProgramMessage("Expected relational expression", current))
            return@rule BinaryOp(shift, relational, BinaryOpType.LT)
        }
        if (check(">")) {
            eat()
            val relational = relational_expression()?: throw ParserException(ProgramMessage("Expected relational expression", current))
            return@rule BinaryOp(shift, relational, BinaryOpType.GT)
        }
        if (check("<=")) {
            eat()
            val relational = relational_expression()?: throw ParserException(ProgramMessage("Expected relational expression", current))
            return@rule BinaryOp(shift, relational, BinaryOpType.LE)
        }
        if (check(">=")) {
            eat()
            val relational = relational_expression()?: throw ParserException(ProgramMessage("Expected relational expression", current))
            return@rule BinaryOp(shift, relational, BinaryOpType.GE)
        }
        return@rule shift
    }

    // shift_expression
    //	: additive_expression
    //	| shift_expression '<<' additive_expression
    //	| shift_expression '>>' additive_expression
    //	;
    fun shift_expression(): Expression? = rule {
        val additive = additive_expression()?: return@rule null
        if (check("<<")) {
            eat()
            val shift = shift_expression()?: throw ParserException(ProgramMessage("Expected shift expression", current))
            return@rule BinaryOp(additive, shift, BinaryOpType.SHL)
        }
        if (check(">>")) {
            eat()
            val shift = shift_expression()?: throw ParserException(ProgramMessage("Expected shift expression", current))
            return@rule BinaryOp(additive, shift, BinaryOpType.SHR)
        }
        return@rule additive
    }

    // additive_expression
    //	: multiplicative_expression
    //	| additive_expression '+' multiplicative_expression
    //	| additive_expression '-' multiplicative_expression
    //	;
    fun additive_expression(): Expression? = rule {
        val mult = multiplicative_expression()?: return@rule null
        if (check("+")) {
            eat()
            val additive = additive_expression()?: throw ParserException(ProgramMessage("Expected additive expression", current))
            return@rule BinaryOp(mult, additive, BinaryOpType.ADD)
        }
        if (check("-")) {
            eat()
            val additive = additive_expression()?: throw ParserException(ProgramMessage("Expected additive expression", current))
            return@rule BinaryOp(mult, additive, BinaryOpType.SUB)
        }
        return@rule mult
    }

    // multiplicative_expression
    //	: cast_expression
    //	| multiplicative_expression '*' cast_expression
    //	| multiplicative_expression '/' cast_expression
    //	| multiplicative_expression '%' cast_expression
    //	;
    fun multiplicative_expression(): Expression? = rule {
        val cast = cast_expression() ?: return@rule null
        if (check("*")) {
            eat()
            val mult = multiplicative_expression()?: throw ParserException(ProgramMessage("Expected multiplicative expression", current))
            return@rule BinaryOp(cast, mult, BinaryOpType.MUL)
        }
        if (check("/")) {
            eat()
            val mult = multiplicative_expression()?: throw ParserException(ProgramMessage("Expected multiplicative expression", current))
            return@rule BinaryOp(cast, mult, BinaryOpType.DIV)
        }
        if (check("%")) {
            eat()
            val mult = multiplicative_expression()?: throw ParserException(ProgramMessage("Expected multiplicative expression", current))
            return@rule BinaryOp(cast, mult, BinaryOpType.MOD)
        }
        return@rule cast
    }

    // cast_expression
    //	: unary_expression
    //	| '(' type_name ')' cast_expression
    //	;
    fun cast_expression(): Expression? = rule {
        val cast = rule castRule@ {
            if (!check("(")) {
                return@castRule null
            }
            eat()
            val declspec = type_name() ?: return@castRule null
            if (check(")")) {
                eat()
                val cast = cast_expression() ?: throw ParserException(ProgramMessage("Expected cast expression", current))
                return@castRule Cast(declspec, cast)
            }
            throw ParserException(ProgramMessage("Expected ')'", current))
        }

        return@rule cast?: unary_expression()
    }

    // type_name
    //	: specifier_qualifier_list
    //	| specifier_qualifier_list abstract_declarator
    //	;
    fun type_name(): TypeName? = rule {
        val specifierQualifierList = specifier_qualifier_list()?: return@rule null
        val abstractDeclarator = abstract_declarator()
        if (abstractDeclarator != null) {
            return@rule TypeName(specifierQualifierList, abstractDeclarator)
        }
        return@rule TypeName(specifierQualifierList, DummyNode)
    }

    // specifier_qualifier_list
    //	: type_specifier specifier_qualifier_list
    //	| type_specifier
    //	| type_qualifier specifier_qualifier_list
    //	| type_qualifier
    //	;
    fun specifier_qualifier_list(): List<Any>? = rule {
        val specifiers = mutableListOf<Any>()
        while (true) {
            val type = type_specifier()
            if (type != null) {
                specifiers.add(type)
                continue
            }
            val typeQualifier = type_qualifier()
            if (typeQualifier != null) {
                specifiers.add(typeQualifier)
                continue
            }
            break
        }
        return@rule if (specifiers.isEmpty()) {
            null
        } else {
            specifiers
        }
    }

    // unary_expression
    //	: postfix_expression
    //	| '++' unary_expression
    //	| '--' unary_expression
    //	| unary_operator cast_expression
    //	| sizeof unary_expression
    //	| sizeof '(' type_name ')'
    //	;
    fun unary_expression(): Expression? = rule {
        if (check("sizeof")) {
            eat()
            if (check("(")) {
                eat()
                val type = type_name()?: throw ParserException(ProgramMessage("Expected type name", current))
                if (check(")")) {
                    eat()
                    return@rule SizeOf(type)
                } else {
                    throw ParserException(ProgramMessage("Expected ')'", current))
                }
            }
            val expr = unary_expression()?: throw ParserException(ProgramMessage("Expected unary expression", current))
            return@rule SizeOf(expr)
        }
        if (check("++")) {
            eat()
            val unary = unary_expression()?: throw ParserException(ProgramMessage("Expected unary expression", current))
            return@rule UnaryOp(unary, PrefixUnaryOpType.INC)
        }
        if (check("--")) {
            eat()
            val unary = unary_expression()?: throw ParserException(ProgramMessage("Expected unary expression", current))
            return@rule UnaryOp(unary, PrefixUnaryOpType.DEC)
        }
        if (check("&")) {
            eat()
            val unary = unary_expression()?: throw ParserException(ProgramMessage("Expected unary expression", current))
            return@rule UnaryOp(unary, PrefixUnaryOpType.ADDRESS)
        }
        if (check("*")) {
            eat()
            val unary = unary_expression()?: throw ParserException(ProgramMessage("Expected unary expression", current))
            return@rule UnaryOp(unary, PrefixUnaryOpType.DEREF)
        }
        if (check("+")) {
            eat()
            val unary = unary_expression()?: throw ParserException(ProgramMessage("Expected unary expression", current))
            return@rule UnaryOp(unary, PrefixUnaryOpType.PLUS)
        }
        if (check("-")) {
            eat()
            val unary = unary_expression()?: throw ParserException(ProgramMessage("Expected unary expression", current))
            return@rule UnaryOp(unary, PrefixUnaryOpType.NEG)
        }
        if (check("~")) {
            eat()
            val unary = unary_expression()?: throw ParserException(ProgramMessage("Expected unary expression", current))
            return@rule UnaryOp(unary, PrefixUnaryOpType.NOT)
        }
        if (check("!")) {
            eat()
            val unary = unary_expression()?: throw ParserException(ProgramMessage("Expected unary expression", current))
            return@rule UnaryOp(unary, PrefixUnaryOpType.NOT)
        }
        return@rule postfix_expression()
    }

    // postfix_expression
    //	: primary_expression
    //	| postfix_expression '[' expression ']'
    //	| postfix_expression '(' ')'
    //	| postfix_expression '(' argument_expression_list ')'
    //	| postfix_expression '.' IDENTIFIER
    //	| postfix_expression '->' IDENTIFIER
    //	| postfix_expression '--'
    //	| postfix_expression '++'
    //	;
    fun postfix_expression(): Expression? = rule {
        val primary_expr: Expression = primary_expression() ?: return@rule null
        var primary = primary_expr
        while (current is CToken) {
            val token = peak<CToken>()
            if (token.str() == "[") {
                eat()
                val expr = expression() ?: throw ParserException(ProgramMessage("Expected expression", current))
                if (check("]")) {
                    eat()
                    primary = ArrayAccess(primary, expr)
                } else {
                    throw ParserException(ProgramMessage("Expected ']'", current))
                }
            } else if (token.str() == "(") {
                eat()
                val args = argument_expression_list()
                if (check(")")) {
                    eat()
                    primary = FunctionCall(primary, args)
                } else {
                    throw ParserException(ProgramMessage("Expected ')'", current))
                }
            }
            else if (token.str() == ".") {
                eat()
                val ident = peak<Ident>()
                eat()
                primary = MemberAccess(primary, ident)
            } else if (token.str() == "->") {
                eat()
                val ident = peak<Ident>()
                eat()
                primary = ArrowMemberAccess(primary, ident)
            } else if (token.str() == "++") {
                eat()
                primary = UnaryOp(primary, PostfixUnaryOpType.INC)
            } else if (token.str() == "--") {
                eat()
                primary = UnaryOp(primary, PostfixUnaryOpType.DEC)
            } else {
                break
            }
        }

        return@rule primary
    }

    // argument_expression_list
    //	: assignment_expression
    //	| argument_expression_list ',' assignment_expression
    //	;
    fun argument_expression_list(): List<Expression> {
        val arguments = mutableListOf<Expression>()
        while (true) {
            val expr = assignment_expression()?: return arguments
            arguments.add(expr)
            if (check(",")) {
                eat()
            } else {
                return arguments
            }
        }
    }

    //initializer_list
    //	: initializer
    //	| initializer_list ',' initializer
    //	;
    fun initializerList(): Expression {
        val initializers = mutableListOf<Expression>()
        while (true) {
            val initializer = initializer() ?: return InitializerList(initializers)
            initializers.add(initializer)
            if (check(",")) {
                eat()
            } else {
                return InitializerList(initializers)
            }
        }
    }

    // abstract_declarator
    //	: pointer
    //	| direct_abstract_declarator
    //	| pointer direct_abstract_declarator
    fun abstract_declarator(): AbstractDeclarator? = rule {
        val pointers = pointers()
        if (pointers != null) {
            val declarator = direct_abstract_declarator()
            if (declarator != null) {
                return@rule AbstractDeclarator(pointers, declarator)
            }
            return@rule AbstractDeclarator(pointers, listOf(DummyNode))
        }

        val declarator = direct_abstract_declarator()?: return@rule null
        return@rule AbstractDeclarator(listOf(), declarator)
    }

    // primary_expression
    //	: IDENTIFIER
    //	| CONSTANT
    //	| STRING_LITERAL
    //	| '(' expression ')'
    //	;
    fun primary_expression(): Expression? = rule {
        if (check("(")) {
            eat()
            val expr = expression()
            if (check(")")) {
                eat()
                return@rule expr
            } else {
                throw ParserException(ProgramMessage("Expected ')'", current))
            }
        }
        if (check("_Generic")) {
            eat()
            val genericSelection = genericSelection()
            return@rule genericSelection
        }
        if (check<Ident>() && !keywords.contains(peak<Ident>().str())) { //TODO ??????????????????????????????????????????!!!!!!!!
            val ident = peak<Ident>()
            eat()
            return@rule VarNode(ident)
        }
        if (check<StringLiteral>()) {
            val str = peak<StringLiteral>()
            eat()
            return@rule StringNode(str)
        }
        if (check<Numeric>()) {
            val num = peak<Numeric>()
            eat()
            return@rule NumNode(num)
        }
        return@rule null
    }

    fun genericSelection(): Expression {
        TODO()
    }

    // external_declaration
    //	: function_definition
    //	| declaration
    //	;
    fun external_declaration(): Node? = rule {
        return@rule declaration() ?: function_definition()
    }
}