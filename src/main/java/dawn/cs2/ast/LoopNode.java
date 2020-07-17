package dawn.cs2.ast;

import dawn.cs2.CodePrinter;

public class LoopNode extends AbstractCodeNode {// implements IBreakableNode,IContinueableNode {

    public static final int LOOPTYPE_WHILE = 0;
    public static final int LOOPTYPE_DOWHILE = 1;
    public static final int LOOPTYPE_FOR = 2;

    /**
     * Contains expression which type is boolean.
     */
    private ExpressionNode expression;
    /**
     * Contains scope which should be executed if
     * expression results in true.
     */
    private ScopeNode scope;
    /**
     * Contains type of this loop node.
     */
    private int type;

    public LoopNode(int type, ScopeNode scope, ExpressionNode expr) {
        this.type = type;
        this.expression = expr;
        this.scope = scope;
    }

    public ScopeNode getScope() {
        return scope;
    }


    public ExpressionNode getExpression() {
        return expression;
    }

    @Override
    public void print(CodePrinter printer) {
//        if (this.labelName != null)
//            printer.print(labelName + " ");
        if (this.type == LOOPTYPE_WHILE) {
            printer.print("while (");
            expression.print(printer);
            printer.print(") ");
            scope.print(printer);
        } else if (this.type == LOOPTYPE_DOWHILE) {
            printer.print("do ");
            scope.print(printer);
            printer.print(" while (");
            expression.print(printer);
            printer.print(");");
        } else if (this.type == LOOPTYPE_FOR) { // TODO variable declarations
            if (expression instanceof BooleanExpressionNode && ((BooleanExpressionNode) expression).getData()) {
                printer.print("for (;;) ");
                scope.print(printer);
            } else {
                printer.print("for (;");
                expression.print(printer);
                printer.print(";) ");
                scope.print(printer);
            }
        } else {
            throw new RuntimeException("Unknown loop type:" + this.type);
        }
    }

//	@Override
//	public boolean canContinue() {
//		return start != null;
//	}
//
//	@Override
//	public boolean canBreak() {
//		return end != null;
//	}
//
//	@Override
//	public FlowBlock getStart() {
//		return start;
//	}
//
//	@Override
//	public FlowBlock getEnd() {
//		return end;
//	}
//
//	@Override
//	public void enableLabelName() {
//		labelName = "loop_" + this.hashCode() + ":";
//	}
//
//	@Override
//	public String getLabelName() {
//		return labelName;
//	}

    public void setType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }


}
