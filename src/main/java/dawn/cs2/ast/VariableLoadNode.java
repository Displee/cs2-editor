package dawn.cs2.ast;

import dawn.cs2.CS2Type;
import dawn.cs2.CodePrinter;

public class VariableLoadNode extends ExpressionNode {


    private Variable variable;
    
    public VariableLoadNode(Variable variable) {
    	this.variable = variable;
    }

    @Override
    public CS2Type getType() {
    	return this.variable.getType();
    }

	@Override
	public ExpressionNode copy() {
		return new VariableLoadNode(this.variable);
	}

	public Variable getVariable() {
		return variable;
	}

	@Override
	public void print(CodePrinter printer) {
		printer.print(variable.getName());
	}

}
