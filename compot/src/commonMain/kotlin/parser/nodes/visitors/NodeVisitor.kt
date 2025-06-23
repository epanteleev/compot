package parser.nodes.visitors

interface NodeVisitor<T> : ExpressionVisitor<T>,
    DeclaratorVisitor<T>,
    StatementVisitor<T>,
    TypeNodeVisitor<T>,
    TypeSpecifierVisitor<T>,
    ParameterVisitor<T>,
    DirectDeclaratorParamVisitor<T>