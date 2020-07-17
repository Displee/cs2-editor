package dawn.cs2.ast;

import dawn.cs2.CS2Type;
import dawn.cs2.CodePrinter;

public class NewWidgetPointerNode extends ExpressionNode {


    private ExpressionNode expression;

    public NewWidgetPointerNode(ExpressionNode expr) {
        this.expression = expr;
    }

    @Override
    public CS2Type getType() {
        return CS2Type.WIDGET_PTR;
    }

    @Override
    public ExpressionNode copy() {
        return new NewWidgetPointerNode(this.expression.copy());
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
                printer.print("widget(");
                int interfaceID = data >> 16;
                int componentID = data & 0xFFFF;
                printer.print(interfaceID + ", " + componentID);
                printer.print(")");
            }
        } else {
            printer.print("widget(");
            expression.print(printer);
            printer.print(")");
        }
    }

}
