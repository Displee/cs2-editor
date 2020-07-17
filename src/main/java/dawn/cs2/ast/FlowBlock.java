package dawn.cs2.ast;

import dawn.cs2.CS2Stack;
import dawn.cs2.CodePrinter;

import java.util.ArrayList;
import java.util.List;

public class FlowBlock extends ParentAbstractCodeNode implements Comparable<FlowBlock> {

    /**
     * Contains ID of this flow block.
     * Main flow block ID is always 0.
     */
    private int blockID;
    /**
     * Contains all blocks to which this blocks
     * jumps with instructions.
     */
    public List<FlowBlock> children;
    /**
     * Contains all blocks which jumps to this block
     * with instructions.
     */
    public List<FlowBlock> parents;
    /**
     * Contains stack content before jumping/walking into this
     * block dumped to local variables.
     */
    private CS2Stack variableStack;
    /**
     * Contains start address of this flow block in instructions.
     * Not guaranteed to be correct.
     */
    private int startAddress;

    public FlowBlock(int blockID, int startAddress, CS2Stack stack) {
        this.blockID = blockID;
        this.startAddress = startAddress;
        this.variableStack = stack;
        this.children = new ArrayList<>();
        this.parents = new ArrayList<>();
    }




    @Override
    public void print(CodePrinter printer) {
        printer.print("flow_" + this.blockID + ": // (parents: " + parents.size() + " children: " + children.size() + ")");
        printer.tab();
        List<AbstractCodeNode> childs = this.listChilds();
        for (AbstractCodeNode node : childs) {
            printer.print("\r\n");
            node.print(printer);
        }
        printer.untab();
    }

    public CS2Stack getStack() {
        return variableStack;
    }

    public int getStartAddress() {
        return startAddress;
    }

    public int getBlockID() {
        return blockID;
    }

    @Override
    public int hashCode() {
        return blockID;
    }

    @Override
    public boolean equals(Object obj) {
        return ((FlowBlock) obj).blockID == blockID;
    }

    @Override
    public int compareTo(FlowBlock o) {
        return blockID - o.blockID;
    }
}
