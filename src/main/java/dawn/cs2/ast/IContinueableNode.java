package dawn.cs2.ast;


public interface IContinueableNode extends IControllableFlowNode {
	public FlowBlock getStart();
	public boolean canContinue();
}
