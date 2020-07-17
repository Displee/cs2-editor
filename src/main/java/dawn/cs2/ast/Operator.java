package dawn.cs2.ast;

import static dawn.cs2.ast.ExpressionNode.*;

public enum Operator {

    PLUS("+", PRIORITY_ADDSUB, MathExpressionNode.class, 2),
    MINUS("-", PRIORITY_ADDSUB, MathExpressionNode.class, 2),
    MUL("*", PRIORITY_MULDIVREM, MathExpressionNode.class, 2),
    DIV("/", PRIORITY_MULDIVREM, MathExpressionNode.class, 2),
    REM("%", PRIORITY_MULDIVREM, MathExpressionNode.class, 2),
    //TODO:
    PLUSPREFIX("++", PRIORITY_PLUSMINUSPREFIXPOSTFIX, ExpressionNode.class, 1, true, false),
    PLUSPOSTFIX("++", PRIORITY_PLUSMINUSPREFIXPOSTFIX, ExpressionNode.class, 1),
    MINUSPREFIX("--", PRIORITY_PLUSMINUSPREFIXPOSTFIX, ExpressionNode.class, 1, true, false),
    MINUSPOSTFIX("--", PRIORITY_PLUSMINUSPREFIXPOSTFIX, ExpressionNode.class, 1),
    UNARYMINUS("-", PRIORITY_UNARYPLUSMINUS, ExpressionNode.class, 1, true, false),
    UNARYPLUS("+", PRIORITY_UNARYPLUSMINUS, ExpressionNode.class, 1, true, false),
    UNARYNOT("!", PRIORITY_UNARYLOGICALNOT, ExpressionNode.class, 1, true, false),
    EQ("==", PRIORITY_EQNE, ConditionalExpressionNode.class, 2),
    NEQ("!=", PRIORITY_EQNE, ConditionalExpressionNode.class, 2),
    GT(">", PRIORITY_LELTGEGTINSTANCEOF, ConditionalExpressionNode.class, 2),
    GE(">=", PRIORITY_LELTGEGTINSTANCEOF, ConditionalExpressionNode.class, 2),
    LT("<", PRIORITY_LELTGEGTINSTANCEOF, ConditionalExpressionNode.class, 2),
    LE("<=", PRIORITY_LELTGEGTINSTANCEOF, ConditionalExpressionNode.class, 2),
    OR("||", PRIORITY_LOGICALOR, ConditionalExpressionNode.class, 2),
    AND("&&", PRIORITY_LOGICALAND, ConditionalExpressionNode.class, 2),
    ASSIGN("=", PRIORITY_ASSIGNMENT, VariableAssignationNode.class, 2, false, true),
    //TODO: += etc

    DUMMY_OP("", 99, ExpressionNode.class, 0);


    public final String text;
    public final int priority;
    public final Class<? extends ExpressionNode> type;
    public final int operands;
    public boolean prefix;
    public boolean assocRight;

    Operator(String text, int prio, Class<? extends ExpressionNode> type, int operands) {
        this(text, prio, type, operands, false, false);
    }

    Operator(String text, int prio, Class<? extends ExpressionNode> type, int operands, boolean prefix, boolean assocRight) {
        this.text = text;
        this.priority = prio;
        this.type = type;
        this.operands = operands;
        this.prefix = prefix;
        this.assocRight = assocRight;
    }

    @Override
    public String toString() {
        return text;
    }
}
