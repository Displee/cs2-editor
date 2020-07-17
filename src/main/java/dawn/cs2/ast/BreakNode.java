package dawn.cs2.ast;

import dawn.cs2.CodePrinter;

public class BreakNode extends AbstractCodeNode {//implements IFlowControlNode {

    private IBreakableNode node;
    private ScopeNode selfScope;

    public BreakNode() {

    }

    public BreakNode(ScopeNode selfScope, IBreakableNode node) {
        this.node = node;
        this.selfScope = selfScope;
        // don't need to write self scope because it's just for label checks. (Not expressed).
//		if (this.getSelfScope().getParent() != node && node.getLabelName() == null)
//			node.enableLabelName();
    }


//    @Override
//	public IBreakableNode getNode() {
//		return node;
//	}
//
//	public ScopeNode getSelfScope() {
//		return selfScope;
//	}

    @Override
    public void print(CodePrinter printer) {
//    	if (this.getSelfScope().getParent() == node)
        printer.print("break;");
//    	else
//    		printer.print("break " + node.getLabelName() + ";");
    }

}
