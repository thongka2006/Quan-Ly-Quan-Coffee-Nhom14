package util;

public class Node {
    public int col;
    public int row;
    public boolean solid;

    public int gCost;
    public int hCost;
    public int fCost;

    public Node parent;

    public Node(int col, int row) {
        this.col = col;
        this.row = row;
        this.solid = false;
    }

    public void setAsSolid() {
        this.solid = true;
    }
}
