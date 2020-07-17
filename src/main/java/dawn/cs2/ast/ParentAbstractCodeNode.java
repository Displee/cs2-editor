package dawn.cs2.ast;

import dawn.cs2.CodePrinter;

import java.util.ArrayList;
import java.util.List;

public abstract class ParentAbstractCodeNode extends AbstractCodeNode {

    private static final int INITIAL_BUFFER_SIZE = 4;
    private AbstractCodeNode[] childs;
    private int codeAddress;

    public abstract void print(CodePrinter printer);

    public ParentAbstractCodeNode() {
        childs = new AbstractCodeNode[INITIAL_BUFFER_SIZE];
        codeAddress = 0;
    }


    public AbstractCodeNode read() {
        if (childs[codeAddress] == null)
            return null;
        return childs[codeAddress++];
    }

    public AbstractCodeNode read(int addr) {
        if (addr < 0 || addr >= childs.length || (addr > 0 && childs[addr - 1] == null))
            throw new IllegalArgumentException("Invalid address.");
        return childs[addr];
    }

    public void write(AbstractCodeNode node) {
        if (needsExpand())
            expand();
        if (childs[codeAddress] == null)
            childs[codeAddress++] = node;
        else {
            List<AbstractCodeNode> taken = new ArrayList<AbstractCodeNode>();
            for (int i = codeAddress; i < childs.length; i++) {
                if (childs[i] != null) {
                    taken.add(childs[i]);
                    childs[i] = null;
                }
            }
            childs[codeAddress] = node;
            int write = ++codeAddress;
            for (AbstractCodeNode n : taken)
                childs[write++] = n;
        }
    }

    public void delete() {
        delete(codeAddress);
    }

    public void delete(int address) {
        if (address < 0 || address >= childs.length || (address > 0 && childs[address - 1] == null))
            throw new IllegalArgumentException("Invalid address.");
        if (childs[address] == null)
            throw new RuntimeException("No element to delete.");
        if ((address + 1) < childs.length && childs[address + 1] == null)
            childs[address] = null;
        else {
            childs[address] = null;
            for (int i = address + 1; i < childs.length; i++) {
                childs[i - 1] = childs[i];
                childs[i] = null;
            }
        }
    }

    public int addressOf(AbstractCodeNode child) {
        for (int i = 0; i < childs.length; i++)
            if (childs[i] == child)
                return i;
        return -1;
    }

    public List<AbstractCodeNode> listChilds() {
        List<AbstractCodeNode> list = new ArrayList<AbstractCodeNode>();
        for (int i = 0; i < childs.length; i++)
            if (childs[i] != null)
                list.add(childs[i]);
        return list;
    }

    public int size() {
        int total = 0;
        for (int i = 0; i < childs.length; i++)
            if (childs[i] != null)
                total++;
        return total;
    }


    private boolean needsExpand() {
        double max = childs.length * 0.50;
        return (double) size() > max;
    }


    private void expand() {
        if (childs.length >= Integer.MAX_VALUE)
            throw new RuntimeException("Can't expand anymore.");
        long newSize = childs.length * 2;
        if (newSize > Integer.MAX_VALUE)
            newSize = Integer.MAX_VALUE;
        AbstractCodeNode[] newBuffer = new AbstractCodeNode[(int) newSize];
        System.arraycopy(childs, 0, newBuffer, 0, childs.length);
        childs = newBuffer;
    }

    public void setCodeAddress(int codeAddress) {
        if (codeAddress < 0 || codeAddress >= childs.length || (codeAddress > 0 && childs[codeAddress - 1] == null))
            throw new IllegalArgumentException("Invalid address.");
        this.codeAddress = codeAddress;
    }

    public int getCodeAddress() {
        return codeAddress;
    }
}
