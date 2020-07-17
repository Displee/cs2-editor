package dawn.cs2.ast;

import dawn.cs2.CodePrinter;

public class IfElseNode extends AbstractCodeNode {

    /**
     * Contains expressions for each scope.
     */
    public ExpressionNode condition;
    /**
     * Contains scopes for each expression.
     */
    public ScopeNode ifScope;
    /**
     * Contains else scope if all expressions conditions
     * were not met.
     */
    public ScopeNode elseScope;


    public IfElseNode(ExpressionNode expressions, ScopeNode ifScope, ScopeNode elseScope) {
        this.condition = expressions;
        this.ifScope = ifScope;
        this.elseScope = elseScope;
        elseScope.setParent(this);
    }


    public boolean hasElseScope() {
        return !this.elseScope.isEmpty();
    }

    public ScopeNode getElseScope() {
        return elseScope;
    }


    @Override
    public void print(CodePrinter printer) {
        printer.print("if (");
        condition.print(printer);
        printer.print(") ");
        ifScope.print(printer);
        if (hasElseScope()) {
            printer.print(" else ");
            if (elseScope.size() == 1 && elseScope.listChilds().get(0) instanceof IfElseNode) {
                elseScope.printInline(printer);
            } else {
                elseScope.print(printer);
            }
        }
    }
}
