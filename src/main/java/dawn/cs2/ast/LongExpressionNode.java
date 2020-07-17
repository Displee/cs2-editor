package dawn.cs2.ast;

import dawn.cs2.CS2Type;
import dawn.cs2.CodePrinter;

public class LongExpressionNode extends ExpressionNode /*implements IIntConstantNode*/ {

    /**
     * Contains long which this expression holds.
     */
    public long data;
    
    public LongExpressionNode(long data) {
    	this.data = data;
    }
    
    public long getData() {
    	return data;
    }

    @Override
    public CS2Type getType() {
    	return CS2Type.LONG;
    }
	/*
	@Override
	public Long getConst() {
		return this.data;
	}
*/
	@Override
	public ExpressionNode copy() {
		return new LongExpressionNode(data);
	}

	@Override
	public void print(CodePrinter printer) {
		printer.print("" + this.data + "L");
	}

}
