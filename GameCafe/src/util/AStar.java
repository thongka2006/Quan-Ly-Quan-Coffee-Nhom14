package util;

import java.util.ArrayList;

public class AStar {

    Node[][] node;
    ArrayList<Node> openList = new ArrayList<>();
    public ArrayList<Node> pathList = new ArrayList<>();

    Node startNode, goalNode, currentNode;
    boolean goalReached = false;
    int step = 0;

    int maxCol;
    int maxRow;

    public AStar(int maxCol, int maxRow) {
        this.maxCol = maxCol;
        this.maxRow = maxRow;
        node = new Node[maxCol][maxRow];
        for (int col = 0; col < maxCol; col++) {
            for (int row = 0; row < maxRow; row++) {
                node[col][row] = new Node(col, row);
            }
        }
    }

    public void resetNodes() {
        for (int col = 0; col < maxCol; col++) {
            for (int row = 0; row < maxRow; row++) {
                node[col][row].solid = false;
                node[col][row].parent = null;
                node[col][row].gCost = 0;
                node[col][row].hCost = 0;
                node[col][row].fCost = 0;
            }
        }
        openList.clear();
        pathList.clear();
        goalReached = false;
        step = 0;
    }

    public void setSolidNode(int col, int row) {
        if (col >= 0 && col < maxCol && row >= 0 && row < maxRow) {
            node[col][row].setAsSolid();
        }
    }

    public void setNodes(int startCol, int startRow, int goalCol, int goalRow) {
        startCol = Math.max(0, Math.min(maxCol - 1, startCol));
        startRow = Math.max(0, Math.min(maxRow - 1, startRow));
        goalCol = Math.max(0, Math.min(maxCol - 1, goalCol));
        goalRow = Math.max(0, Math.min(maxRow - 1, goalRow));

        startNode = node[startCol][startRow];
        currentNode = startNode;
        goalNode = node[goalCol][goalRow];
        openList.add(currentNode);

        for (int col = 0; col < maxCol; col++) {
            for (int row = 0; row < maxRow; row++) {
                getCost(node[col][row]);
            }
        }
    }

    private void getCost(Node node) {
        // G Cost
        int xDistance = Math.abs(node.col - startNode.col);
        int yDistance = Math.abs(node.row - startNode.row);
        node.gCost = xDistance + yDistance;

        // H Cost
        xDistance = Math.abs(node.col - goalNode.col);
        yDistance = Math.abs(node.row - goalNode.row);
        node.hCost = xDistance + yDistance;

        // F Cost
        node.fCost = node.gCost + node.hCost;
    }

    public boolean search() {
        while (!goalReached && step < 500) {

            int col = currentNode.col;
            int row = currentNode.row;

            openList.remove(currentNode);

            if (row - 1 >= 0) openNode(node[col][row - 1]);
            if (row + 1 < maxRow) openNode(node[col][row + 1]);
            if (col - 1 >= 0) openNode(node[col - 1][row]);
            if (col + 1 < maxCol) openNode(node[col + 1][row]);

            int bestNodeIndex = 0;
            int bestNodefCost = 999;

            for (int i = 0; i < openList.size(); i++) {
                if (openList.get(i).fCost < bestNodefCost) {
                    bestNodeIndex = i;
                    bestNodefCost = openList.get(i).fCost;
                } else if (openList.get(i).fCost == bestNodefCost) {
                    if (openList.get(i).gCost < openList.get(bestNodeIndex).gCost) {
                        bestNodeIndex = i;
                    }
                }
            }

            if (openList.isEmpty()) {
                break;
            }

            currentNode = openList.get(bestNodeIndex);

            if (currentNode == goalNode) {
                goalReached = true;
                trackThePath();
            }
            step++;
        }
        return goalReached;
    }

    private void openNode(Node node) {
        if (!node.solid && node.parent == null && node != startNode && !openList.contains(node)) {
            node.parent = currentNode;
            openList.add(node);
        }
    }

    private void trackThePath() {
        Node current = goalNode;
        while (current != startNode && current != null) {
            pathList.add(0, current);
            current = current.parent;
        }
    }
}
