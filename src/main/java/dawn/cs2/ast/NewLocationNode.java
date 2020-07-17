package dawn.cs2.ast;

import dawn.cs2.CS2Type;
import dawn.cs2.CodePrinter;

public class NewLocationNode extends ExpressionNode {

    private ExpressionNode expression;

    public NewLocationNode(ExpressionNode expr) {
        this.expression = expr;
    }

    @Override
    public CS2Type getType() {
        return CS2Type.LOCATION;
    }

    @Override
    public ExpressionNode copy() {
        return new NewLocationNode(this.expression.copy());
    }

    public ExpressionNode getExpression() {
        return expression;
    }

    @Override
    public void print(CodePrinter printer) {
        if (this.expression instanceof IntExpressionNode) {
            int data = ((IntExpressionNode) expression).getData();
            if (data == -1) {
                printer.print("null");
            } else {
                printer.print("location(");
                int z = data >> 28;
                int x = (data >> 14) & 0x3fff;
                int y = data & 0x3FFF;
                printer.print(x + ", " + y + ", " + z);
                printer.print(")");
            }
        } else {
            printer.print("location(");
            expression.print(printer);
            printer.print(")");
        }
    }

}
