import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class PuzzleGenerator
{
    private final Random random = new Random();

    public Puzzle create(int level)
    {
        if (level < 1 || level > 5)
        {
            throw new IllegalArgumentException("Уровень должен быть от 1 до 5.");
        }

        int[] targetCells = new int[19];
        List<Integer> places = new ArrayList<Integer>();

        for (int place = 1; place <= 19; place++)
        {
            places.add(place);
        }

        Collections.shuffle(places, random);

        for (int color = 1; color <= level; color++)
        {
            int place = places.get(color - 1);
            targetCells[place - 1] = color;
        }

        State target = new State(targetCells);

        for (int attempt = 0; attempt < 1000; attempt++)
        {
            State start = target;
            int steps = 18 + level * 5 + random.nextInt(8);

            for (int i = 0; i < steps; i++)
            {
                int[] centers = State.getCenters();
                int randomIndex = random.nextInt(centers.length);
                int center = centers[randomIndex];
                start = start.rotate(center);
            }

            boolean everySpecialMoved = true;
            for (int color = 1; color <= level; color++)
            {
                int startPlace = find(start, color);
                int targetPlace = find(target, color);

                if (startPlace == targetPlace)
                {
                    everySpecialMoved = false;
                    break;
                }
            }

            if (everySpecialMoved)
            {
                return new Puzzle(start, target);
            }
        }

        throw new IllegalStateException("Не удалось создать начальную расстановку.");
    }

    private int find(State state, int color)
    {
        for (int place = 1; place <= 19; place++)
        {
            if (state.getColorAt(place) == color)
            {
                return place;
            }
        }
        return -1;
    }

    public static final class Puzzle
    {
        public final State start;
        public final State target;

        private Puzzle(State start, State target)
        {
            this.start = start;
            this.target = target;
        }
    }
}
