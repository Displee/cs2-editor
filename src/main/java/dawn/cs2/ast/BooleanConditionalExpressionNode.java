package dawn.cs2.ast;

import dawn.cs2.CS2Type;
import dawn.cs2.CodePrinter;

public class BooleanConditionalExpressionNode extends ExpressionNode {

    public boolean invert;

    private ExpressionNode condition;

    public BooleanConditionalExpressionNode(boolean invert, ExpressionNode condition) {
        this.invert = invert;
        this.condition = condition;
    }

    @Override
    public CS2Type getType() {
        return CS2Type.BOOLEAN;
    }

    @Override
    public int getPriority() {
        return PRIORITY_UNARYLOGICALNOT;
    }

    @Override
    public void print(CodePrinter printer) {
        if (invert) {
            printer.print("!");
        }
        boolean paren = this.condition.getPriority() > getPriority();
        if (paren) {
            printer.print("(");
        }
        this.condition.print(printer);
        if (paren) {
            printer.print(")");
        }
    }


    @Override
    public ExpressionNode copy() {
        return new BooleanConditionalExpressionNode(invert, condition.copy());
    }

    public ExpressionNode getCondition() {
        return condition;
    }


}
