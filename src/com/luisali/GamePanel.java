package com.luisali;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class GamePanel extends JPanel implements ActionListener {

    // defining some constants and an initially empty grid, this is for the logic aspect
    private final static int yDimension = 30;
    private final static int xDimension = 16;
    private final static int mines = 99;
    private static int remainingMines;
    private final int[][] grid = new int[xDimension][yDimension];
    private final JButton[][] buttonGrid = new JButton[xDimension][yDimension];
    private ArrayList<int[]> squaresToReveal = new ArrayList<>();
    private static final Random random = new Random();

    // defining some constants for the GUI aspect
    private final static int SCREEN_WIDTH = 930;
    private final static int SCREEN_LENGTH = 534;
    private final static int UNIT = 30;

    public GamePanel() {

        // setting up the fonts used in the dialogue panels used when a game is won/lost
        UIManager.put("OptionPane.messageFont", new Font("Times New Roman", Font.PLAIN, 14));
        UIManager.put("OptionPane.buttonFont", new Font("Times New Roman", Font.PLAIN, 12));

        // number of unmarked mines left on the board
        remainingMines = mines;

        // step 1: generate the mines in the grid
        generateMines();

        // step 2: calculate the numbers in the grid corresponding to the number of mines in its vicinity
        classifySpots();

        // step 3: now we need to involve swing to make the graphics
        setUpBoard();

    }

    private void generateMines() {

        // -1 = mine, 0 = no mines, 1-8 = mines in neighboring cells

        // this function select random coordinates to put the mines in, ensuring to not put mines in repeating spots

        ArrayList<int[]> coordinates = new ArrayList<>();
        while (coordinates.size() < mines) {
            int[] newCoords = randomXY();
            if (!includes(coordinates,newCoords)) {
                coordinates.add(newCoords);
            }
        }

        for (int[] coord : coordinates) {
            grid[coord[0]][coord[1]] = -1;

        }

    }

    private void classifySpots() {

        int total;

        for (int i = 0; i < xDimension; i++) {
            for (int m = 0; m < yDimension; m++) {

                // if the cell is a mine, then it does not need to be assigned a new number
                if (grid[i][m]!=-1) {
                    total = 0;
                    // if the cell is a number, then check through its neighbors and count how many mines there are
                    for (int[] coords : getAllNeighbors(i,m)) {
                        if (grid[coords[0]][coords[1]] == -1) {
                            total++;
                        }
                    }
                    grid[i][m] = total;
                }
            }
        }

        printGrid();

    }

    private void setUpBoard() {

        this.setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_LENGTH));
        this.setFocusable(true);
        this.setBackground(Color.lightGray);

        // top panel has the remaining mines = xx text, bottom panel has all the buttons
        JPanel topPanel = new JPanel();
        JPanel bottomPanel = new JPanel();

        JLabel remainingMine = new JLabel("Remaining mines: " + remainingMines);
        topPanel.setBackground(new Color(230, 230, 230));
        topPanel.add(remainingMine);
        remainingMine.setFont(new Font("Times New Roman", Font.ITALIC, 20));
        topPanel.setPreferredSize(new Dimension(SCREEN_WIDTH, 30));

        // this should create a grid along with the grey background of the buttons in the bottom panel and the gaps
        bottomPanel.setBackground(Color.BLACK);

        GridLayout gridLayout = new GridLayout(xDimension, yDimension);
        gridLayout.setHgap(1);
        gridLayout.setVgap(1);
        bottomPanel.setLayout(gridLayout);

        // for every spot in the grid, create a button

        for (int i = 0; i < xDimension; i++) {
            for (int y = 0; y < yDimension; y++) {

                buttonGrid[i][y] = new JButton();
                buttonGrid[i][y].setPreferredSize(new Dimension(UNIT, UNIT));
                buttonGrid[i][y].addActionListener(this);
                buttonGrid[i][y].setBackground(Color.GRAY);
                buttonGrid[i][y].setBorder(null);
                buttonGrid[i][y].setFont(new Font("Times New Roman", Font.BOLD, 20));
                buttonGrid[i][y].addMouseListener(new MouseAdapter() {

                    // this mouse adapter takes care of the functionality of right clicking to mark spots
                    @Override
                    public void mouseClicked(MouseEvent e) {

                        // clicking it twice unmarks the spot, which then enables the left click function on that button

                        if (e.getButton() == MouseEvent.BUTTON3) {
                            JButton button = (JButton) e.getSource();
                            // if the button is blank and the background is grey (which means that the square has not been "solved" yet), mark it as needed
                            if (button.getText().isBlank() && button.getBackground() == Color.GRAY) {
                                button.setText("X");
                                button.setBackground(Color.LIGHT_GRAY);
                                // update the text above the game to reflect mines left
                                remainingMines--;
                                remainingMine.setText("Remaining mines: " + remainingMines);
                            } else if (button.getText().equals("X")) {
                                // if user right clicks on a spot that has already been marked, unmark it and add 1 to remaining mines.
                                button.setText("");
                                button.setBackground(Color.GRAY);
                                remainingMines++;
                                remainingMine.setText("Remaining mines: " + remainingMines);
                            }
                        }
                    }
                });
                // adding the buttons to the bottom Panel
                bottomPanel.add(buttonGrid[i][y]);
            }
        }

        // adding both panels to the GamePanel.
        this.add(topPanel, BorderLayout.NORTH);
        this.add(bottomPanel, BorderLayout.CENTER);

    }

    // function that returns a random set of x y coordinates, put into an int[] array
    public int[] randomXY() {
        int x = random.nextInt(xDimension);
        int y = random.nextInt(yDimension);
        return new int[] {x,y};
    }

    // function that places a singular mine in a spot not taken
    private void placeMine() {
        int[] xy;
        boolean placed = false;
        while (!placed) {
            xy = randomXY();
            if (grid[xy[0]][xy[1]] != -1) {
                grid[xy[0]][xy[0]] = -1;
                placed = true;
            }
        }
    }

    // this function handles the process after a user left-clicks a button

    @Override
    public void actionPerformed(ActionEvent e) {

        for (int i = 0; i < xDimension; i++) {
            for (int m = 0; m < yDimension; m++) {

                if (e.getSource() == buttonGrid[i][m]) {
                    // we know that the button clicked is in the [i][m] position of the grid

                    // first, check if the spot is marked with an X, if it is, then do nothing upon left click
                    JButton button = (JButton) e.getSource();

                    if (!button.getText().equals("X")) {

                        if (grid[i][m] == -1) {
                            // it's a mine, game over immediately
                            gameOver(i,m);

                        } else if (grid[i][m] == 0) {

                            // reveal this square as blank, all connecting 0s, and all numbers immediately next to the 0

                            // reveal the clicked square
                            int[] coord = new int[]{i, m};
                            revealNumber(i,m);
                            revealAllNeighbors(i,m);

                            // reveal all connected 0 squares via recursion
                            revealSquares(coord);
                            for (int[] ints : squaresToReveal) {
                                revealNumber(ints[0], ints[1]);
                            }

                            // now reveal the number squares next to the 0 squares
                            for (int[] ints : squaresToReveal) {
                                for (int[] neighbor : getAllNeighbors(ints[0], ints[1])) {
                                    // could use a if statement to avoid revealing the same square, but it's most likely faster to reveal all regardless of repetition
                                    revealNumber(neighbor[0], neighbor[1]);
                                }
                            }

                        } else {

                            // the clicked square is a number and it is yet to be revealed, so reveal it
                            if (button.getText().isBlank()) {
                                revealNumber(i, m);
                            } else {

                                // the clicked square is a number that has already been revealed

                                // logic: if total marked mines != total mines in the area, then do nothing
                                // else, check if mines were marked correctly, if yes, reveal all squares number neighbors of the cell, if not, game ends.

                                // 1. check how many mines are marked within the neighbors of this number cell
                                int totalMarkedMines = 0;
                                for (int[] neighbor : getAllNeighbors(i,m)) {
                                    if (buttonGrid[neighbor[0]][neighbor[1]].getText().equals("X")) {
                                        totalMarkedMines++;
                                    }
                                }
                                
                                // 2. if totalMarkedMines == number on the current cell, check if the mines were marked correctly
                                if (totalMarkedMines==grid[i][m]) {
                                    for (int[] neighbor : getAllNeighbors(i,m)) {

                                        // to reduce the amount of text:
                                        int a = neighbor[0];
                                        int b = neighbor[1];

                                        // if it is supposed to be marked as a bomb, but isn't marked
                                        if (grid[a][b] == -1 && !buttonGrid[a][b].getText().equals("X")) {
                                            gameOver(a, b);
                                        // else if it is marked as a bomb, but isn't a bomb
                                        } else if (buttonGrid[a][b].getText().equals("X") && grid[a][b] != -1) {
                                            gameOver(a, b);
                                        }
                                    }
                                    
                                    // if the code gets here without gameOver() being called, then the mines were marked successfully, and all the numbers in the neighboring cells should be revealed.
                                    
                                    for (int[] neighbor : getAllNeighbors(i,m)) {
                                        int x = neighbor[0];
                                        int y = neighbor[1];
                                        if (grid[x][y] != -1) {
                                            revealNumber(x, y);
                                            if (grid[x][y] == 0) {
                                                revealAllNeighbors(x, y);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // after a button has been pressed, check if the player has won

        boolean won = true;
        for (int i = 0; i < xDimension; i++) {
            for (int m = 0; m < yDimension; m++) {
                if (buttonGrid[i][m].getBackground()!=Color.LIGHT_GRAY) {
                    // the user wins when all the squares have changed colors
                    won = false;
                }
            }
        }
        if (won) {
            gameWon();
        }

    }

    // as said, takes coordinates i and m and updates the GUI to reveal the square indicated

    private void revealNumber(int i, int m) {
        // if there are 0 mines, we will display it as an empty light grey square, as is in the original game
        JButton button = buttonGrid[i][m];
        if (grid[i][m] == -1) {
            buttonGrid[i][m].setForeground(new Color(102, 25,13));
            // display mines as "X" for aesthetic reasons
            buttonGrid[i][m].setText("X");
        } else if (!(grid[i][m] == 0)) {
            button.setText(grid[i][m] + "");
        }
        button.setBackground(Color.LIGHT_GRAY);
    }

    private void revealSquares(int[] coords) {

        // recursively returns all the neighbors that are 0

        ArrayList<int[]> zeroNeighbors = new ArrayList<>();

        for (int[] n : getAllNeighbors(coords[0], coords[1])) {
            if (grid[n[0]][n[1]] == 0) {
                // the neighbor has number 0
                zeroNeighbors.add(n);
            }
        }

        for (int[] c : zeroNeighbors) {
            if (!includes(squaresToReveal, c)) {
                squaresToReveal.add(c);
                revealSquares(c);
            }
        }

    }

    // returns an arraylist of all neighbors regardless of the number in it
    private ArrayList<int[]> getAllNeighbors(int x, int y) {

        ArrayList<int[]> neighbors = new ArrayList<>();

        if (y+1<yDimension) {
            // add this neighbor
            neighbors.add(new int[]{x,y+1});
        }
        if (y-1>=0) {
            neighbors.add(new int[]{x,y-1});
        }
        if (x+1<xDimension) {
            neighbors.add(new int[] {x+1, y});
        }
        if (x-1>=0) {
            neighbors.add(new int[] {x-1, y});
        }
        if (x-1>=0 && y-1>=0) {
            neighbors.add(new int[] {x-1, y-1});
        }
        if (x-1>=0 && y+1<yDimension) {
            neighbors.add(new int[] {x-1, y+1});
        }
        if (y-1>=0 && x+1<xDimension) {
            neighbors.add(new int[] {x+1, y-1});
        }
        if (x+1<xDimension && y+1<yDimension) {
            neighbors.add(new int[] {x+1, y+1});
        }

        return neighbors;
    }

    // used to replace the function .contains, which does not work in this case as it returns false even for two arrays that has the same length & element has the same length & element

    public boolean includes(ArrayList<int[]> parent, int[] child) {
        for (int[] p : parent) {
            if (Arrays.equals(p, child)) {
                return true;
            }
        }
        return false;
    }

    private void gameWon() {

        Object[] options = {"Yes!", "No thanks."};

        int n = JOptionPane.showOptionDialog(this, "You win! Play again?", "Game won", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        // n=0: yes, n=1: no, n=2: closed

        if (n==0) {
            this.setVisible(false);
            Container container = this.getParent();
            this.getParent().removeAll();
            container.add(new GamePanel());
        } else if (n==1) {
            System.exit(0);
        }
    }

    private void revealAllNeighbors(int x, int y) {
        for (int[] n : getAllNeighbors(x,y)) {
            revealNumber(n[0],n[1]);
        }
    }

    // user clicked a bomb
    private void gameOver(int x, int y) {

        // 1. reveal all and paint the bomb the user lost on as light red

        revealAll();
        buttonGrid[x][y].setBackground(new Color(191,93,78));


        // 3. open dialogue to ask the user if they want to play again

        Object[] options = {"Yes!", "No thanks."};

        int n = JOptionPane.showOptionDialog(this, "You lost, play again?", "Game over", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        // n=0: yes, n=1: no, n=2: closed

        if (n==0) {
            this.setVisible(false);
            Container container = this.getParent();
            this.getParent().removeAll();
            container.add(new GamePanel());
        } else if (n==1) {
            System.exit(0);
        }

    }

    // function that reveals all the squares in the grid

    public void revealAll() {
        for (int n = 0; n<xDimension; n++) {
            for (int m = 0; m<yDimension; m++) {
                revealNumber(n, m);
            }
        }
    }

    // test function that prints out the grid in console
    public void printGrid() {

        int total = 0;
        for (int i = 0; i < xDimension; i++) {
            for (int y = 0; y < yDimension; y++) {
                System.out.print(grid[i][y] + " ");
                if (grid[i][y] == -1) {
                    total++;
                }
            }
        }
        System.out.println();
        System.out.println(total);
    }

}
