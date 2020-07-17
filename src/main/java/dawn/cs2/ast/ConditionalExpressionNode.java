package dawn.cs2.ast;

import dawn.cs2.CS2Type;
import dawn.cs2.CodePrinter;

public class ConditionalExpressionNode extends ExpressionNode {

    /**
     * Contains left expression node.
     */
    private ExpressionNode left;
    /**
     * Contains right expression node.
     */
    private ExpressionNode right;
    public final Operator conditional;

    public ConditionalExpressionNode(ExpressionNode left, ExpressionNode right, Operator conditional) {
        this.left = left;
        this.right = right;
        this.conditional = conditional;
        solveMultiReturnType();
    }

    private void solveMultiReturnType() {
        ExpressionNode l = null;
        ExpressionNode r = null;
        if (left.getType() != right.getType()) {
            if (left.getType() != CS2Type.INT) {
                l = left;
                r = right;
            } else {
                l = right;
                r = left;
            }
        }
        if (l == null) {
            return;
        }
        int count = 1; //skip first return type
        while (!tryCast() && count < l.getCS2Types().length) {
            l.solveCS2Type(r, count++);
        }
    }

    public boolean tryCast() {
        try {
            if (left.getType() != right.getType()) {
                if (left.getType() != CS2Type.INT) {
                    CS2Type.cast(right, left.getType());
                } else {
                    CS2Type.cast(left, right.getType());
                }
            }
            return true;
        } catch(Exception e) {
            return false;
        }
    }

    @Override
    public CS2Type getType() {
        return CS2Type.BOOLEAN;
    }

    @Override
    public int getPriority() {
        return conditional.priority;
    }

    @Override
    public void print(CodePrinter printer) {
        if (left.getType() != right.getType()) {
            if (left.getType() != CS2Type.INT) {
                right = CS2Type.cast(right, left.getType());
            } else {
                left = CS2Type.cast(left, right.getType());
            }
        }

        boolean needsLeftParen = left.getPriority() > this.getPriority();
        boolean needsRightParen = right.getPriority() > this.getPriority();

        if (needsLeftParen)
            printer.print("(");


        this.left.print(printer);
        if (needsLeftParen)
            printer.print(")");
        printer.print(" " + this.conditional.text + " ");

        if (needsRightParen)
            printer.print("(");
        this.right.print(printer);

        if (needsRightParen)
            printer.print(")");
    }

    @Override
    public ExpressionNode copy() {
        return new ConditionalExpressionNode(left.copy(), right.copy(), conditional);
    }

    public ExpressionNode getLeft() {
        return left;
    }

    public ExpressionNode getRight() {
        return right;
    }

}
