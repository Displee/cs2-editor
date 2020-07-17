package dawn.cs2.ast;

import dawn.cs2.CS2Type;
import dawn.cs2.CodePrinter;

public class NewColorNode extends ExpressionNode {

    private ExpressionNode expression;

    public NewColorNode(ExpressionNode expr) {
        this.expression = expr;
    }

    @Override
    public CS2Type getType() {
        return CS2Type.COLOR;
    }

    @Override
    public ExpressionNode copy() {
        return new NewColorNode(this.expression.copy());
    }

    public ExpressionNode getExpression() {
        return expression;
    }

    @Override
    public void print(CodePrinter printer) {
        if (this.expression instanceof IntExpressionNode) {
            int data = ((IntExpressionNode) expression).getData();
//            if (data == -1) {
//                printer.print("null");
//            } else {
                printer.print(String.format("0x%06X", (0xFFFFFF & data)));
//            }
        } else {
            printer.print("color(");
            expression.print(printer);
            printer.print(")");
        }
    }

}
