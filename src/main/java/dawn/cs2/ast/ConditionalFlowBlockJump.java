package dawn.cs2.ast;

import dawn.cs2.CodePrinter;

public class ConditionalFlowBlockJump extends AbstractCodeNode {

    /**
     * Contains expression which type is boolean.
     */
    private ExpressionNode expression;
    /**
     * Contains target flow block.
     */
    private FlowBlock target;
    
    public ConditionalFlowBlockJump(ExpressionNode expr,FlowBlock target) {
    	this.expression = expr;
    	this.target = target;
    }
    

	public ExpressionNode getExpression() {
		return expression;
	}
	
	public FlowBlock getTarget() {
		return target;
	}

	@Override
	public void print(CodePrinter printer) {
		printer.print("IF (");
		expression.print(printer);
		printer.print(") ");
		printer.tab();
		printer.print("\r\nGOTO\t" + "flow_" + target.getBlockID());
		printer.untab();
	}

	public void setTarget(FlowBlock target) {
		this.target = target;
	}
	public void setExpression(ExpressionNode expression) {
		this.expression = expression;
	}
}
