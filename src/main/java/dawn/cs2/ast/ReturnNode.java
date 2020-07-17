package dawn.cs2.ast;

import dawn.cs2.CodePrinter;

public class ReturnNode extends AbstractCodeNode {

    /**
     * Contains return expression.
     */
    private ExpressionNode expression;
        
    
    public ReturnNode(ExpressionNode expr) {
    	this.expression = expr;
    }

	public ExpressionNode getExpression() {
		return expression;
	}

	@Override
	public void print(CodePrinter printer) {
		if (this.expression != null) {
			printer.print("return ");
			expression.print(printer);
			printer.print(';');
		}
		else {
			printer.print("return;");
		}
	}

}
