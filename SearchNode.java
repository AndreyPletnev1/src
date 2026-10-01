public final class SearchNode
{
    State state;
    SearchNode parent;
    int moveCenter;

    public SearchNode(int moveCenter, State state, SearchNode parent)
    {
        this.moveCenter = moveCenter;
        this.state = state;
        this.parent = parent;
    }
}
