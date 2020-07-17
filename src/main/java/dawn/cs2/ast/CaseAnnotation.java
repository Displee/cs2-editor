package dawn.cs2.ast;

import dawn.cs2.CS2Type;
import dawn.cs2.CodePrinter;


public class CaseAnnotation extends AbstractCodeNode {

    private int caseNumber;
    private ExpressionNode switchExpr;
    private boolean isDefault;

    public CaseAnnotation(int caseNumber, ExpressionNode switchExpr) {
        this.caseNumber = caseNumber;
        this.switchExpr = switchExpr;
        this.isDefault = false;
    }

    public CaseAnnotation() {
        this.caseNumber = 0;
        this.isDefault = true;
    }

    public int getCaseNumber() {
        return caseNumber;
    }

    public boolean isDefault() {
        return isDefault;
    }

    @Override
    public void print(CodePrinter printer) {
        if (isDefault)
            printer.print("default:");
        else {
            if(switchExpr != null) {
                printer.print("case " + CS2Type.cast(new IntExpressionNode(caseNumber), switchExpr.getType()) + ":");
            } else {
                printer.print("case " + caseNumber + ":");
            }
        }
    }

}
