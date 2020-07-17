package dawn.cs2.ast;

import dawn.cs2.CodePrinter;

public class ContinueNode extends AbstractCodeNode implements IFlowControlNode {

	private IContinueableNode node;
	private ScopeNode selfScope;
	
	
    public ContinueNode(ScopeNode selfScope, IContinueableNode node) {
    	this.node = node;
    	this.selfScope = selfScope;
    	// don't need to write self scope because it's just for label checks. (Not expressed).
		if (this.getSelfScope().getParent() != node && node.getLabelName() == null)
			node.enableLabelName();
    }
    
    @Override
	public IContinueableNode getNode() {
		return node;
	}

	public ScopeNode getSelfScope() {
		return selfScope;
	}

	@Override
	public void print(CodePrinter printer) {
		if (this.getSelfScope().getParent() == node)
			printer.print("continue;");
		else
			printer.print("continue " + node.getLabelName() + ";");
	}

}
