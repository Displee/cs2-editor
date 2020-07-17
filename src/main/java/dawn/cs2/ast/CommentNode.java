package dawn.cs2.ast;

import dawn.cs2.CodePrinter;

public class CommentNode extends AbstractCodeNode {

	private String comment;
    
    public CommentNode(String comment) {
    	this.comment = comment;
    }
    
    public int numLines() {
    	int total = 0;
    	for (int i = 0; i < comment.length(); i++)
    		if (comment.charAt(i) == '\n')
    			total++;
    	return total;
    }

	public String getComment() {
		return comment;
	}

	@Override
	public void print(CodePrinter printer) {
		if (numLines() > 0) {
			printer.tab();
			printer.print("/* \r\n");
			printer.print(comment);
			printer.untab();
			printer.print("\r\n */");
		}
		else {
			printer.print("// " + comment);
		}
	}




}
