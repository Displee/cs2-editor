package dawn.cs2.ast;

import dawn.cs2.CS2Type;
import dawn.cs2.CodePrinter;

public class NullableIntExpressionNode extends ExpressionNode implements IIntConstantNode {

    /**
     * Contains int which this expression holds.
     */
    private int data;

    public NullableIntExpressionNode(int data) {
        this.data = data;
    }

    public int getData() {
        return data;
    }

    @Override
    public CS2Type getType() {
        return CS2Type.INT;
    }

    @Override
    public Integer getConst() {
        return this.data;
    }

    @Override
    public ExpressionNode copy() {
        return new NullableIntExpressionNode(this.data);
    }

    @Override
    public void print(CodePrinter printer) {
        printer.print("" + (this.data == -1 ? "null" : this.data));
    }

}
