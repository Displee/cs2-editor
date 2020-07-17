package dawn.cs2.ast;

import dawn.cs2.CS2Type;
import dawn.cs2.CodePrinter;

import java.util.ArrayList;
import java.util.List;

public class ExpressionList extends ExpressionNode {

    public List<ExpressionNode> arguments;
    public CS2Type type;

    public ExpressionList(List<ExpressionNode> expressions) {
        this.arguments = expressions;
    }

    public ExpressionList(CS2Type type, List<ExpressionNode> expressions) {
        this.type = type;
        this.arguments = expressions;
    }

    @Override
    public CS2Type getType() {
        //recalculate because types can be inferred during decompilation
        return type == null ? CS2Type.typeFor(arguments) : type;
    }


    @Override
    public ExpressionList copy() {
        List<ExpressionNode> copy = new ArrayList<>(arguments.size());
        for (ExpressionNode argument : arguments) {
            copy.add(argument.copy());
        }
        return new ExpressionList(copy);
    }

    @Override
    public void print(CodePrinter printer) {
        for (int i = 0; i < arguments.size(); i++) {
            arguments.get(i).print(printer);
            if ((i + 1) < arguments.size())
                printer.print(", ");
        }
    }

}
