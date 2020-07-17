package dawn.cs2.ast;

import dawn.cs2.CodePrinter;

import java.util.List;

public class SwitchFlowBlockJump extends AbstractCodeNode {


    private ExpressionNode expression;
    private List<Integer> cases;
    private List<FlowBlock> targets;

    public SwitchFlowBlockJump(ExpressionNode expr, List<Integer> cases, List<FlowBlock> targets) {
        this.expression = expr;
        this.cases = cases;
        this.targets = targets;
    }


    public ExpressionNode getExpression() {
        return expression;
    }

    public List<Integer> getCases() {
        return cases;
    }

    public List<FlowBlock> getTargets() {
        return targets;
    }

    @Override
    public void print(CodePrinter printer) {
        printer.print("SWITCH (");
        expression.print(printer);
        printer.print(") {");
        printer.tab();
        for (int i = 0; i < cases.size(); i++) {
            printer.print("\r\nCASE " + cases.get(i) + ":\t GOTO flow_" + targets.get(i).getBlockID());
        }
        printer.untab();
        printer.print("\r\n}");

    }
}
