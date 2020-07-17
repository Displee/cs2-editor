package dawn.cs2.ast;

import dawn.cs2.CS2Type;
import dawn.cs2.CodePrinter;

public class NewArrayNode extends ExpressionNode {

    public int arrayId;
    private ExpressionNode expression;
    private CS2Type type;

    public NewArrayNode(int arrayId, ExpressionNode expr, CS2Type type) {
        this.arrayId = arrayId;
        this.expression = expr;
        this.type = type;
    }

    @Override
    public CS2Type getType() {
        return this.type;
    }

    @Override
    public ExpressionNode copy() {
        return new NewArrayNode(arrayId, this.expression.copy(), this.type);
    }

    public ExpressionNode getExpression() {
        return expression;
    }

    @Override
    public void print(CodePrinter printer) {
        printer.print("ARRAY" + arrayId + " = new " + type.getName());
        printer.print('[');
        expression.print(printer);
        printer.print(']');
    }

}
