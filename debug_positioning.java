// Debug script to trace ghost positioning
class DebugPositioning {
    public static void main(String[] args) {
        String[] maze = {
            "###########",
            "#.........#", 
            "#.........#",
            "#...B.....#", 
            "#.........#",
            "#....S....#",
            "###########"
        };
        
        System.out.println("Maze layout:");
        for (int row = 0; row < maze.length; row++) {
            System.out.println("Row " + row + ": " + maze[row]);
            for (int col = 0; col < maze[row].length(); col++) {
                char c = maze[row].charAt(col);
                if (c == 'B') {
                    System.out.println("Found Blinky 'B' at position (" + row + ", " + col + ")");
                    System.out.println("Character at that position: '" + c + "'");
                    // Check surrounding cells
                    if (row > 0) System.out.println("Above: '" + maze[row-1].charAt(col) + "'");
                    if (row < maze.length-1) System.out.println("Below: '" + maze[row+1].charAt(col) + "'");
                    if (col > 0) System.out.println("Left: '" + maze[row].charAt(col-1) + "'");
                    if (col < maze[row].length()-1) System.out.println("Right: '" + maze[row].charAt(col+1) + "'");
                }
            }
        }
    }
}