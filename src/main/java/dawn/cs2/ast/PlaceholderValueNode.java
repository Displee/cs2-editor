package dawn.cs2.ast;

import dawn.cs2.CS2Type;
import dawn.cs2.CodePrinter;

public class PlaceholderValueNode extends ExpressionNode {

    private String name;
    public final int magic;
    private CS2Type type;

    public PlaceholderValueNode(String name, int magic, CS2Type type) {
    	this.name = name;
    	this.magic = magic;
    	this.type = type;
    }
    
    @Override
    public CS2Type getType() {
    	return this.type;
    }

	@Override
	public ExpressionNode copy() {
		return new PlaceholderValueNode(this.name,magic,this.type);
	}

	public String getName() {
		return name;
	}

	@Override
	public void print(CodePrinter printer) {
		printer.print(name);
	}

}
