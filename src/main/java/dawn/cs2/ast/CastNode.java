package dawn.cs2.ast;

import dawn.cs2.CS2Type;
import dawn.cs2.CodePrinter;

public class CastNode extends ExpressionNode {


    private CS2Type type;
    private ExpressionNode expression;

    public CastNode(CS2Type type, ExpressionNode expr) {
        this.type = type;
        this.expression = expr;
    }

    @Override
    public CS2Type getType() {
        return type;
    }

    @Override
    public ExpressionNode copy() {
        return new CastNode(this.type, this.expression.copy());
    }

    public ExpressionNode getExpression() {
        return expression;
    }

    @Override
    public int getPriority() {
        return Math.min(expression.getPriority(), PRIORITY_CAST);
    }

    @Override
    public void print(CodePrinter printer) {
        if (type != CS2Type.INT && expression instanceof IntExpressionNode && ((IntExpressionNode) expression).getData() == -1) {
            printer.print("null");
        } else {
            if (expression.getPriority() >= PRIORITY_CAST) {
                printer.print("((" + type + ")(");
                expression.print(printer);
                printer.print("))");
            } else {
                printer.print("(" + type + ")");
                expression.print(printer);
            }
        }
    }

}
