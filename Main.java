import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

public final class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                GameWindow window = new GameWindow();
                window.setVisible(true);
            }
        });
    }

    private static final class GameWindow extends JFrame {
        private final PuzzleGenerator generator = new PuzzleGenerator();
        private final SearchSolver solver = new SearchSolver();
        private final BoardPanel currentBoard = new BoardPanel(true);
        private final BoardPanel targetBoard = new BoardPanel(false);
        private final JLabel levelLabel = new JLabel("", SwingConstants.CENTER);
        private final JLabel status = new JLabel("", SwingConstants.CENTER);
        private final JLabel stats = new JLabel("", SwingConstants.CENTER);
        private final JButton bfsButton = new JButton("Поиск в ширину (BFS)");
        private final JButton dfsButton = new JButton("Поиск в глубину (DFS)");
        private final JButton animateButton = new JButton("Запустить анимацию решения");
        private final JButton newButton = new JButton("Новая случайная задача");
        private final JButton nextButton = new JButton("Следующий уровень");
        private State current;
        private State target;
        private SearchSolver.SearchResult result;
        private List<Integer> animationMoves = new ArrayList<Integer>();
        private int animationIndex;
        private int animationFrame;
        private int manualAnimationFrame;
        private int level = 1;
        private boolean busy;

        GameWindow() {
            super("Rotation — поиск решения");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setMinimumSize(new Dimension(1060, 690));
            setLocationByPlatform(true);

            JPanel boards = new JPanel(new java.awt.GridLayout(1, 2, 12, 0));
            boards.setBorder(BorderFactory.createEmptyBorder(10, 14, 8, 14));
            boards.add(boardColumn("Текущее поле", currentBoard));
            boards.add(boardColumn("Целевой результат", targetBoard));

            JPanel controls = new JPanel(new java.awt.GridLayout(0, 1, 6, 6));
            controls.setBorder(BorderFactory.createEmptyBorder(8, 22, 14, 22));
            controls.add(levelLabel);
            controls.add(status);
            controls.add(stats);
            JPanel searchButtons = new JPanel(new java.awt.FlowLayout());
            searchButtons.add(bfsButton);
            searchButtons.add(dfsButton);
            controls.add(searchButtons);
            JPanel actionButtons = new JPanel(new java.awt.FlowLayout());
            actionButtons.add(animateButton);
            actionButtons.add(newButton);
            actionButtons.add(nextButton);
            controls.add(actionButtons);

            add(boards, BorderLayout.CENTER);
            add(controls, BorderLayout.SOUTH);
            currentBoard.setRotationAction(new RotationAction() {
                @Override
                public void rotate(int center) {
                    rotateByClick(center);
                }
            });

            bfsButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    findSolution(false);
                }
            });
            dfsButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    findSolution(true);
                }
            });
            animateButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    animateSolution();
                }
            });
            newButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    newPuzzle();
                }
            });
            nextButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    nextLevel();
                }
            });
            newPuzzle();
        }

        private JPanel boardColumn(String title, BoardPanel board) {
            JPanel column = new JPanel(new BorderLayout());
            JLabel heading = new JLabel(title, SwingConstants.CENTER);
            heading.setFont(heading.getFont().deriveFont(Font.BOLD, 15f));
            heading.setBorder(BorderFactory.createEmptyBorder(4, 3, 6, 3));
            column.add(heading, BorderLayout.NORTH);
            column.add(new JScrollPane(board), BorderLayout.CENTER);
            return column;
        }

        private void newPuzzle() {
            PuzzleGenerator.Puzzle puzzle = generator.create(level);
            current = puzzle.start;
            target = puzzle.target;
            resetSolution();
            refreshBoards();
            levelLabel.setText("Уровень " + level + " из 5 — особых шариков: " + level);
            status.setText("Поверни поле вручную или попроси алгоритм найти путь.");
            updateButtons();
        }

        private void nextLevel() {
            if (level < 5) {
                level++;
                newPuzzle();
            } else {
                JOptionPane.showMessageDialog(this, "Ты прошел все пять уровней!");
                level = 1;
                newPuzzle();
            }
        }

        private void rotateByClick(int center) {
            if (busy || current.isGoal(target)) {
                return;
            }
            resetSolution();
            busy = true;
            updateButtons();
            status.setText("Вращение вокруг позиции " + center + "…");
            State beforeRotation = current;
            final int frames = 20;
            manualAnimationFrame = 0;
            final Timer timer = new Timer(16, null);
            timer.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    if (manualAnimationFrame <= frames) {
                        currentBoard.showRotation(beforeRotation, center,
                            manualAnimationFrame / (double) frames);
                        manualAnimationFrame++;
                        return;
                    }
                    timer.stop();
                    current = beforeRotation.rotate(center);
                    currentBoard.clearRotation(current);
                    busy = false;
                    if (current.isGoal(target)) {
                        won();
                    } else {
                        status.setText("Поворот вокруг позиции " + center + ".");
                        updateButtons();
                    }
                }
            });
            timer.start();
        }

        private void findSolution(boolean depthFirst) {
            if (busy) {
                return;
            }
            busy = true;
            resetSolution();
            if (depthFirst) {
                status.setText("Идет поиск в глубину…");
            } else {
                status.setText("Идет поиск в ширину…");
            }
            updateButtons();
            State startSnapshot = current;
            State targetSnapshot = target;
            new SwingWorker<SearchSolver.SearchResult, Void>() {
                @Override
                protected SearchSolver.SearchResult doInBackground() {
                    if (depthFirst) {
                        return solver.depthFirst(startSnapshot, targetSnapshot);
                    } else {
                        return solver.breadthFirst(startSnapshot, targetSnapshot);
                    }
                }

                @Override
                protected void done() {
                    busy = false;
                    try {
                        result = get();
                        if (result.found()) {
                            animationMoves = result.moves;
                            animateButton.setEnabled(true);
                            String searchName;
                            if (depthFirst) {
                                searchName = "DFS нашёл путь";
                            } else {
                                searchName = "BFS нашёл путь";
                            }
                            status.setText(searchName + ": " + animationMoves.size()
                                + " поворотов: " + animationMoves);
                        } else {
                            status.setText("Решение не найдено.");
                        }
                        stats.setText(result.statsText());
                    } catch (Exception ex) {
                        status.setText("Ошибка поиска: " + ex.getMessage());
                    }
                    updateButtons();
                }
            }.execute();
        }

        private void animateSolution() {
            if (busy || animationMoves.isEmpty()) {
                return;
            }
            busy = true;
            animationIndex = 0;
            animationFrame = 0;
            updateButtons();
            status.setText("Анимация решения: ход 0 из " + animationMoves.size());
            final int framesPerTurn = 20;
            final Timer timer = new Timer(16, null);
            timer.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    if (animationIndex >= animationMoves.size()) {
                        timer.stop();
                        busy = false;
                        currentBoard.clearRotation(current);
                        refreshBoards();
                        won();
                        return;
                    }
                    int center = animationMoves.get(animationIndex);
                    if (animationFrame <= framesPerTurn) {
                        currentBoard.showRotation(current, center, animationFrame / (double) framesPerTurn);
                        animationFrame++;
                    } else {
                        current = current.rotate(center);
                        animationIndex++;
                        animationFrame = 0;
                        currentBoard.clearRotation(current);
                        status.setText("Анимация решения: ход " + animationIndex
                            + " из " + animationMoves.size());
                    }
                }
            });
            timer.start();
        }

        private void won()
        {
            status.setText("Уровень пройден!");
            nextButton.setEnabled(true);
            updateButtons();
        }

        private void resetSolution()
        {
            result = null;
            animationMoves = new ArrayList<Integer>();
            stats.setText(" ");
            animateButton.setEnabled(false);
            nextButton.setEnabled(false);
        }

        private void refreshBoards()
        {
            currentBoard.setState(current);
            targetBoard.setState(target);
        }

        private void updateButtons()
        {
            boolean levelIsComplete = current.isGoal(target);
            if (busy || levelIsComplete) {
                bfsButton.setEnabled(false);
                dfsButton.setEnabled(false);
            } else {
                bfsButton.setEnabled(true);
                dfsButton.setEnabled(true);
            }
            newButton.setEnabled(!busy);
            nextButton.setEnabled(!busy && levelIsComplete);
            if (!busy && result != null && result.found()) {
                animateButton.setEnabled(true);
            } else {
                animateButton.setEnabled(false);
            }
        }
    }

    private interface RotationAction
    {
        void rotate(int center);
    }

    private static final class BoardPanel extends JPanel
    {
        private static final Color[] colors = {
            new Color(48, 135, 216), new Color(230, 60, 75), new Color(61, 178, 91),
            new Color(248, 164, 43), new Color(160, 91, 205), new Color(242, 211, 55)
        };
        private State state;
        private final boolean interactive;
        private RotationAction rotationAction;
        private State rotatingState;
        private int rotatingCenter;
        private double rotationProgress;
        private final int[] px = new int[19];
        private final int[] py = new int[19];
        private final int radius = 27;

        BoardPanel(boolean interactive) {
            this.interactive = interactive;
            setBackground(new Color(238, 242, 249));
            setPreferredSize(new Dimension(480, 465));
            setMinimumSize(new Dimension(380, 420));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent event) {
                    if (!BoardPanel.this.interactive || state == null) {
                        return;
                    }
                    for (int place = 1; place <= 19; place++) {
                        double distanceX = event.getX() - px[place - 1];
                        double distanceY = event.getY() - py[place - 1];
                        double distance = Math.sqrt(distanceX * distanceX + distanceY * distanceY);
                        if (distance <= radius) {
                            int[] centers = State.getCenters();
                            for (int i = 0; i < centers.length; i++) {
                                if (centers[i] == place) {
                                    rotationAction.rotate(place);
                                    return;
                                }
                            }
                            return;
                        }
                    }
                }
            });
        }

        void setRotationAction(RotationAction action) {
            rotationAction = action;
        }

        void setState(State state) {
            this.state = state;
            repaint();
        }

        void showRotation(State from, int center, double progress) {
            rotatingState = from;
            rotatingCenter = center;
            rotationProgress = progress;
            repaint();
        }

        void clearRotation(State settledState) {
            state = settledState;
            rotatingState = null;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics)
        {
            super.paintComponent(graphics);
            if (state == null)
            {
                return;
            }
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int[] rowLengths = {3, 4, 5, 4, 3};
            int row = 0;
            int place = 1;
            int dy = Math.min(66, (getHeight() - 68) / 5);
            int dx = Math.min(74, (getWidth() - 100) / 4);
            int centerX = getWidth() / 2;
            int startY = Math.max(radius + 8, (getHeight() - dy * 4) / 2);
            for (int rowIndex = 0; rowIndex < rowLengths.length; rowIndex++) {
                int length = rowLengths[rowIndex];
                for (int col = 0; col < length; col++) {
                    int x = (int) (centerX + (col - (length - 1) / 2.0) * dx);
                    int y = startY + row * dy;
                    px[place - 1] = x;
                    py[place - 1] = y;
                    drawPositionLabel(g, place, x, y);
                    place++;
                }
                row++;
            }
            for (place = 1; place <= 19; place++) {
                if (rotatingState != null && isInRotationRing(place, rotatingCenter))
                {
                    continue;
                }
                drawBall(g, state.getColorAt(place), place, px[place - 1], py[place - 1]);
            }
            if (rotatingState != null)
            {
                drawRotatingBalls(g);
            }
            g.dispose();
        }

        private boolean isInRotationRing(int place, int center) {
            int[] ring = State.getRotationRing(center);
            for (int i = 0; i < ring.length; i++) {
                if (ring[i] == place) {
                    return true;
                }
            }
            return false;
        }

        private void drawRotatingBalls(Graphics2D g) {
            int[] ring = State.getRotationRing(rotatingCenter);
            int pivotX = px[rotatingCenter - 1];
            int pivotY = py[rotatingCenter - 1];
            double angle = -Math.PI / 3.0 * rotationProgress;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            for (int i = 0; i < ring.length; i++) {
                int source = ring[i];
                double dx = px[source - 1] - pivotX;
                double dy = py[source - 1] - pivotY;
                int x = (int) Math.round(pivotX + dx * cos - dy * sin);
                int y = (int) Math.round(pivotY + dx * sin + dy * cos);
                drawBall(g, rotatingState.getColorAt(source), source, x, y);
            }
        }

        private void drawPositionLabel(Graphics2D g, int place, int x, int y) {
            g.setFont(getFont().deriveFont(9f));
            g.setColor(new Color(70, 76, 88));
            String number = Integer.toString(place);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(number, x - fm.stringWidth(number) / 2, y + radius + 12);
        }

        private void drawBall(Graphics2D g, int colorId, int place, int x, int y) {
            Color color = colors[colorId];
            boolean center = false;
            int[] centers = State.getCenters();
            for (int i = 0; i < centers.length; i++) {
                if (centers[i] == place) {
                    center = true;
                }
            }
            g.setColor(new Color(0, 0, 0, 45));
            g.fillOval(x - radius + 3, y - radius + 4, radius * 2, radius * 2);
            g.setColor(color.darker());
            g.fillOval(x - radius, y - radius, radius * 2, radius * 2);
            g.setColor(color);
            g.fillOval(x - radius + 3, y - radius + 3, radius * 2 - 6, radius * 2 - 6);
            g.setColor(Color.WHITE);
            g.fillOval(x - 8, y - 9, 16, 17);
            g.setColor(new Color(30, 36, 48));
            float borderWidth = 1.3f;
            if (center && interactive)
            {
                borderWidth = 3f;
            }
            g.setStroke(new BasicStroke(borderWidth));
            g.drawOval(x - radius + 3, y - radius + 3, radius * 2 - 6, radius * 2 - 6);
            g.setFont(getFont().deriveFont(Font.BOLD, 11f));
            if (colorId > 0) {
                String mark = Integer.toString(colorId);
                FontMetrics fm = g.getFontMetrics();
                g.drawString(mark, x - fm.stringWidth(mark) / 2, y + fm.getAscent() / 3);
            }
            if (center && interactive && rotatingState == null) {
                g.setColor(new Color(25, 110, 65));
                g.drawOval(x - radius - 2, y - radius - 2, radius * 2 + 4, radius * 2 + 4);
            }
        }
    }
}
