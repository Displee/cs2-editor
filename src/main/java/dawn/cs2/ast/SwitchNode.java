package dawn.cs2.ast;

import dawn.cs2.CodePrinter;

import java.util.LinkedList;
import java.util.List;

public class SwitchNode extends AbstractCodeNode implements IBreakableNode {

    /**
     * Contains expression which type is boolean.
     */
    private ExpressionNode expression;
    /**
     * Contains scope which should be executed if
     * expression finds valid case.
     */
    private ScopeNode scope;
    /**
     * Contains cases of this switch node.
     */
    private Case[] cases;
    /**
     * Contains end block of this switch node.
     */
    private FlowBlock end;
    /**
     * Contains label name.
     */
    private String labelName;

    public SwitchNode(ScopeNode scope, ExpressionNode expr) {
        this.expression = expr;
        this.scope = scope;
        scope.setParent(this);
    }

//
//    public SwitchNode(Case[] cases, FlowBlock end, ScopeNode scope, ExpressionNode expr) {
//        this.cases = cases;
//        this.end = end;
//        this.expression = expr;
//        this.scope = scope;
//        this.write(expr);
//        this.write(scope);
//        expr.setParent(this);
//        scope.setParent(this);
//    }


    @Override
    public void print(CodePrinter printer) {
        if (this.labelName != null)
            printer.print(this.labelName + " ");
        printer.print("switch (");
        expression.print(printer);
        printer.tab();
        printer.print(") {\r\n");
        printer.tab();
        scope.printInline(printer);
        printer.untab();
        printer.untab();
        printer.print("\r\n}");

//        printer.tab();
//        scope.print(printer);
//        printer.untab();
    }

    public ScopeNode getScope() {
        return scope;
    }

    public Case[] getCases() {
        return cases;
    }

    public ExpressionNode getExpression() {
        return expression;
    }

    public static class Case {
        public List<CaseAnnotation> annotations = new LinkedList<>();
        private FlowBlock block;

//        public Case(CaseAnnotation[] annotations, FlowBlock block) {
//            this.annotations = annotations;
//            this.block = block;
//        }
//
//        public CaseAnnotation[] getAnnotations() {
//            return annotations;
//        }

        public FlowBlock getBlock() {
            return block;
        }

        @Override
        public String toString() {
            StringBuilder bld = new StringBuilder();
//            for (int i = 0; i < annotations.length; i++) {
//                bld.append(annotations[i]);
//                if ((i + 1) < annotations.length)
//                    bld.append(" AND ");
//            }
            bld.append("\tcase ... GOTO flow_" + block.getBlockID());
            return bld.toString();
        }

    }

    @Override
    public boolean canBreak() {
        return this.end != null;
    }

    @Override
    public FlowBlock getEnd() {
        return this.end;
    }

    @Override
    public void enableLabelName() {
        this.labelName = "switch_" + this.hashCode() + ":";
    }

    @Override
    public String getLabelName() {
        return this.labelName;
    }


}
