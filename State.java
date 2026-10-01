public final class State {
    private static final int[][] rotations = {
        {5, 2, 1, 4, 9, 10, 6},
        {6, 3, 2, 5, 10, 11, 7},
        {9, 5, 4, 8, 13, 14, 10},
        {10, 6, 5, 9, 14, 15, 11},
        {11, 7, 6, 10, 15, 16, 12},
        {14, 10, 9, 13, 17, 18, 15},
        {15, 11, 10, 14, 18, 19, 16}
    };

    private final int[] cells;

    public State(int[] initialCells)
    {
        if (initialCells == null || initialCells.length != 19) {
            throw new IllegalArgumentException("Поле должно содержать ровно 19 ячеек.");
        }
        this.cells = initialCells.clone();
    }

    public int getColorAt(int place)
    {
        if (place < 1 || place > 19)
        {
            throw new IllegalArgumentException("Позиция должна быть от 1 до 19.");
        }
        return cells[place - 1];
    }

    public int[] toArray()
    {
        return cells.clone();
    }

    public static int[] getCenters()
    {
        return new int[] {5, 6, 9, 10, 11, 14, 15};
    }

    /** The six positions around a pivot, ordered in the direction of rotation. */
    public static int[] getRotationRing(int center)
    {
        for (int i = 0; i < rotations.length; i++)
        {
            if (rotations[i][0] == center)
            {
                int[] ring = new int[6];
                for (int j = 0; j < ring.length; j++)
                {
                    ring[j] = rotations[i][j + 1];
                }
                return ring;
            }
        }
        throw new IllegalArgumentException("У этой позиции нет кольца вращения: " + center);
    }

    public State rotate(int center)
    {
        for (int i = 0; i < rotations.length; i++)
        {
            if (rotations[i][0] == center)
            {
                int[] result = cells.clone();
                for (int j = 1; j < rotations[i].length; j++)
                {
                    int from = rotations[i][j];
                    int to = rotations[i][j % 6 + 1];
                    result[to - 1] = cells[from - 1];
                }
                return new State(result);
            }
        }
        throw new IllegalArgumentException("Нельзя вращать граничную позицию " + center + ".");
    }

    public boolean isGoal(State target)
    {
        for (int i = 0; i < cells.length; i++)
        {
            if (cells[i] != target.cells[i])
            {
                return false;
            }
        }
        return true;
    }

    /** Compact, unambiguous key used by search to avoid visiting a state twice. */
    public String key()
    {
        String key = "";
        for (int i = 0; i < cells.length; i++)
        {
            key = key + cells[i] + ",";
        }
        return key;
    }
}
