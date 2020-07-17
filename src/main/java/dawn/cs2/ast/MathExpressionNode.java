package dawn.cs2.ast;

import dawn.cs2.CS2Type;
import dawn.cs2.CodePrinter;
import dawn.cs2.DecompilerException;

public class MathExpressionNode extends ExpressionNode {

    /**
     * Contains left expression node.
     */
    private ExpressionNode left;
    /**
     * Contains right expression node.
     */
    private ExpressionNode right;

    public final Operator operator;

    public MathExpressionNode(ExpressionNode left, ExpressionNode right, Operator operator) {
        this.left = left;
        this.right = right;
        assert left.getType().isCompatible(right.getType());
        this.operator = operator;
    }

    @Override
    public CS2Type getType() {
        return left.getType();
    }

    @Override
    public int getPriority() {
        return operator.priority;
    }

    @Override
    public void print(CodePrinter printer) {
        boolean needsLeftParen = left.getPriority() > getPriority();
        boolean needsRightParen = right.getPriority() >= getPriority();

//        boolean needsRightParen = right.getPriority() > getPriority();
//        if (right.getPriority() == getPriority() && (operator. operator == 1 || operator == 3 || operator == 4)) {
//            subtract and rem and divide is not associative!! 0 - (1 +/- 2) requires paren on right side
//            needsRightParen = true;
//        }

        if (needsLeftParen)
            printer.print("(");
        this.left.print(printer);
        if (needsLeftParen)
            printer.print(")");
        printer.print(" " + this.operator.text + " ");
        if (needsRightParen)
            printer.print("(");
        this.right.print(printer);
        if (needsRightParen)
            printer.print(")");
    }

    @Override
    public ExpressionNode copy() {
        return new MathExpressionNode(left.copy(), right.copy(), operator);
    }

    public ExpressionNode getLeft() {
        return left;
    }

    public ExpressionNode getRight() {
        return right;
    }

    public ExpressionNode simplify() {
        //Compute the expression if both sides are constants, this wont work on things like though ((2 + int) + 2) because both expressions are variable
        if (left instanceof IntExpressionNode && right instanceof IntExpressionNode) {
            int l = ((IntExpressionNode) left).getData();
            int r = ((IntExpressionNode) right).getData();
            switch (operator) {
                case PLUS:
                    return new IntExpressionNode(l + r);
                case MINUS:
                    return new IntExpressionNode(l - r);
                case MUL:
                    return new IntExpressionNode(l * r);
                case DIV:
                    return new IntExpressionNode(l / r);
                case REM:
                    return new IntExpressionNode(l % r);
                default:
                    throw new DecompilerException("Unknown operator? " + this);
            }
        }
        return this;
    }

}
