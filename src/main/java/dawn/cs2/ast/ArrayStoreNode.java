package dawn.cs2.ast;

import dawn.cs2.CS2Type;
import dawn.cs2.CodePrinter;

public class ArrayStoreNode extends ExpressionNode {


    public final int arrayId;
    private ExpressionNode index;
    private ExpressionNode value;
    
    public ArrayStoreNode(int arrayId, ExpressionNode index,ExpressionNode value) {
    	this.arrayId = arrayId;
    	this.index = index;
    	this.value = value;
    }

    @Override
    public CS2Type getType() {
    	return value.getType();
    }

	public ExpressionNode getIndex() {
		return index;
	}

    public ExpressionNode getValue() {
    	return value;
	}

	@Override
	public ExpressionNode copy() {
		return new ArrayStoreNode(this.arrayId, this.index.copy(), this.value.copy());
	}

	@Override
	public void print(CodePrinter printer) {
		printer.print("ARRAY"+ arrayId +"[");
		index.print(printer);
		printer.print(']');
		printer.print(" = ");
		value.print(printer);
	}

}
