import java.util.*;

public final class SearchSolver
{
    public SearchResult breadthFirst(State start, State target)
    {
        return search(start, target, false);
    }

    public SearchResult depthFirst(State start, State target)
    {
        return search(start, target, true);
    }

    private SearchResult search(State start, State target, boolean depthFirst)
    {
        ArrayDeque<SearchNode> open = new ArrayDeque<SearchNode>();
        Set<String> discovered = new HashSet<String>();

        SearchNode root = new SearchNode(0, start, null);
        open.add(root);
        discovered.add(start.key());

        int iterations = 0;
        int maxOpen = 1;
        int maxOpenAndClosed = 1;

        while (!open.isEmpty())
        {
            SearchNode current = open.removeFirst();
            iterations++;

            if (current.state.isGoal(target))
            {
                List<Integer> moves = pathTo(current);
                return new SearchResult(current, moves, iterations,
                    maxOpen, open.size(), maxOpenAndClosed, discovered.size());
            }

            int[] centers = State.getCenters();
            for (int i = 0; i < centers.length; i++)
            {
                int center = centers[i];
                State next = current.state.rotate(center);
                String nextKey = next.key();

                if (!discovered.contains(nextKey))
                {
                    discovered.add(nextKey);
                    SearchNode child = new SearchNode(center, next, current);

                    if (depthFirst)
                    {
                        open.addFirst(child);
                    }
                    else
                    {
                        open.addLast(child);
                    }
                }
            }

            if (open.size() > maxOpen)
            {
                maxOpen = open.size();
            }

            if (discovered.size() > maxOpenAndClosed)
            {
                maxOpenAndClosed = discovered.size();
            }
        }

        List<Integer> emptyPath = new ArrayList<Integer>();
        return new SearchResult(null, emptyPath, iterations,
            maxOpen, 0, maxOpenAndClosed, discovered.size());
    }

    private List<Integer> pathTo(SearchNode goal)
    {
        List<Integer> path = new ArrayList<Integer>();
        SearchNode current = goal;

        while (current.parent != null)
        {
            path.add(current.moveCenter);
            current = current.parent;
        }

        Collections.reverse(path);
        return path;
    }

    public static final class SearchResult
    {
        public final SearchNode goalNode;
        public final List<Integer> moves;
        public final int iterations;
        public final int maxOpen;
        public final int openAtFinish;
        public final int maxOpenAndClosed;
        public final int totalDiscovered;

        private SearchResult(SearchNode goalNode, List<Integer> moves, int iterations,
            int maxOpen, int openAtFinish, int maxOpenAndClosed, int totalDiscovered)
        {
            this.goalNode = goalNode;
            this.moves = new ArrayList<Integer>(moves);
            this.iterations = iterations;
            this.maxOpen = maxOpen;
            this.openAtFinish = openAtFinish;
            this.maxOpenAndClosed = maxOpenAndClosed;
            this.totalDiscovered = totalDiscovered;
        }

        public boolean found()
        {
            return goalNode != null;
        }

        public String statsText()
        {
            String text = "Итераций: " + iterations;
            text = text + " | max O: " + maxOpen;
            text = text + " | O при завершении: " + openAtFinish;
            text = text + " | max (O + C): " + maxOpenAndClosed;
            return text;
        }
    }
}
