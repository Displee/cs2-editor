package dawn.cs2.ast;

import dawn.cs2.CodePrinter;

public class UnconditionalFlowBlockJump extends AbstractCodeNode {

    /**
     * Contains target flow block.
     */
    private FlowBlock target;
    
    public UnconditionalFlowBlockJump(FlowBlock target) {
    	this.target = target;
    }
 
	
	public FlowBlock getTarget() {
		return target;
	}

	@Override
	public void print(CodePrinter printer) {
		printer.print("GOTO\t" + "flow_" + target.getBlockID());
	}

    public void setTarget(FlowBlock target) {
        this.target = target;
    }
}
