package dawn.cs2.ast;

import dawn.cs2.CS2Type;
import dawn.cs2.CodePrinter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class VariableAssignationNode extends ExpressionNode {

    public LinkedList<Variable> variables;
    private ExpressionNode expression;

    public VariableAssignationNode(Variable variable, ExpressionNode expr) {
        this(new LinkedList<>(), expr);
        variables.add(variable);
    }

    public VariableAssignationNode(LinkedList<Variable> variables, ExpressionNode expr) {
        this.expression = expr;
        this.variables = variables;
    }

    @Override
    public int getPriority() {
        return PRIORITY_ASSIGNMENT;
    }

    @Override
    public CS2Type getType() {
        return CS2Type.of(variables.stream().map(Variable::getType).collect(Collectors.toList()));
    }

    @Override
    public ExpressionNode copy() {
        return new VariableAssignationNode(new LinkedList<>(variables), expression == null ? null : this.expression.copy());
    }


    public ExpressionNode getExpression() {
        return expression;
    }

    @Override
    public void print(CodePrinter printer) {
        boolean first = true;
        for (Variable var : variables) {
            if (!first) {
                printer.print(", ");
            }
            first = false;
            printer.print(var.getName());
        }
        if (expression != null) {
            printer.print(" = ");
            if (variables.size() == 1 && variables.get(0).getType() != CS2Type.INT && expression instanceof IntExpressionNode && ((IntExpressionNode) expression).getData() == -1) {
                printer.print("null");
            } else {
                if (variables.size() == 1) {
                    CS2Type.cast(expression, variables.get(0).getType()).print(printer);
                } else {
                    expression.print(printer);
                }
            }
        }
    }

}
