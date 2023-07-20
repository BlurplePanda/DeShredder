// This program is copyright VUW.
// You are granted permission to use it to construct your answer to a COMP103 assignment.
// You may not distribute it in any other way without permission.

/* Code for COMP103 - 2023T2, Assignment 1
 * Name: Amy Booth
 * Username: boothamy
 * ID: 300653766
 */

import ecs100.*;

import java.awt.Color;
import java.util.*;
import java.io.*;
import java.nio.file.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;

/**
 * DeShredder allows a user to sort fragments of a shredded document ("shreds") into strips, and
 * then sort the strips into the original document.
 * The program shows
 * - a list of all the shreds along the top of the window,
 * - the working strip (which the user is constructing) just below it.
 * - the list of completed strips below the working strip.
 * The "rotate" button moves the first shred on the list to the end of the list to let the
 * user see the shreds that have disappeared over the edge of the window.
 * The "shuffle" button reorders the shreds in the list randomly.
 * The user can use the mouse to drag shreds between the list at the top and the working strip,
 * and move shreds around in the working strip to get them in order.
 * When the user has the working strip complete, they can move
 * the working strip down into the list of completed strips, and reorder the completed strips.
 */
public class DeShredder {

    // Fields to store the lists of Shreds and strips.  These should never be null.
    private List<Shred> allShreds = new ArrayList<Shred>();    //  List of all shreds
    private List<Shred> workingStrip = new ArrayList<Shred>(); // Current strip of shreds
    private List<List<Shred>> completedStrips = new ArrayList<List<Shred>>();

    // Constants for the display and the mouse
    public static final double LEFT = 20;       // left side of the display
    public static final double TOP_ALL = 20;    // top of list of all shreds
    public static final double GAP = 5;         // gap between strips
    public static final double SIZE = Shred.SIZE; // size of the shreds

    public static final double TOP_WORKING = TOP_ALL + SIZE + GAP;
    public static final double TOP_STRIPS = TOP_WORKING + (SIZE + GAP);

    //Fields for recording where the mouse was pressed  (which list/strip and position in list)
    // note, the position may be past the end of the list!
    private List<Shred> fromStrip;   // The strip (List of Shreds) that the user pressed on
    private int fromPosition = -1;   // index of shred in the strip

    private Path directory;

    private JButton toggleSug;

    private boolean suggestions = false;

    /**
     * Initialises the UI window, and sets up the buttons.
     */
    public void setupGUI() {
        UI.addButton("Load library", this::loadLibrary);
        UI.addButton("Rotate", this::rotateList);
        UI.addButton("Shuffle", this::shuffleList);
        UI.addButton("Complete Strip", this::completeStrip);
        toggleSug = UI.addButton("Suggestions: off", this::toggleSuggestions);
        UI.addButton("Add white padding square", this::addPadding);
        UI.addButton("Save as PNG", this::save);
        UI.addButton("Quit", UI::quit);

        UI.setMouseListener(this::doMouse);
        UI.setWindowSize(1000, 800);
        UI.setDivider(0);
    }

    /**
     * Asks user for a library of shreds, loads it, and redisplays.
     * Uses UIFileChooser to let user select library
     * and finds out how many images are in the library
     * Calls load(...) to construct the List of all the Shreds
     */
    public void loadLibrary() {
        try {
            Path filePath = Path.of(UIFileChooser.open("Choose first shred in directory"));
            directory = filePath.getParent(); //subPath(0, filePath.getNameCount()-1);
            int count = 1;
            while (Files.exists(directory.resolve(count + ".png"))) {
                count++;
            }
            //loop stops when count.png doesn't exist
            count = count - 1;
            load(directory, count);   // YOU HAVE TO COMPLETE THE load METHOD
            display();
            UI.printMessage(""); // clears any "hey choose a file pls" messages
        } catch (NullPointerException e) {
            UI.printMessage("Please choose a file in the directory you want!");
        }
    }

    /**
     * Empties out all the current lists (the list of all shreds,
     * the working strip, and the completed strips).
     * Loads the library of shreds into the allShreds list.
     * Parameters are the directory containing the shred images and the number of shreds.
     * Each new Shred needs the directory and the number/id of the shred.
     */
    public void load(Path dir, int count) {
        allShreds.clear();
        workingStrip.clear();
        completedStrips.clear();
        for (int shredId = 1; shredId <= count; shredId++) {
            Shred shred = new Shred(dir, shredId);
            allShreds.add(shred);
        }

    }

    /**
     * Rotate the list of all shreds by one step to the left
     * and redisplay;
     * Should not have an error if the list is empty
     * (Called by the "Rotate" button)
     */
    public void rotateList() {
        if (!allShreds.isEmpty()) {
            Shred firstLast = allShreds.get(0);
            allShreds.remove(firstLast);
            allShreds.add(firstLast);
            display();
        }
    }

    /**
     * Shuffle the list of all shreds into a random order
     * and redisplay;
     */
    public void shuffleList() {
        for (Shred shred : allShreds) {
            Random random = new Random();
            int randIndex = random.nextInt(allShreds.size());
            Shred replaced = allShreds.set(randIndex, shred);
            allShreds.set(allShreds.indexOf(shred), replaced);
        }
        display();
    }

    /**
     * Move the current working strip to the end of the list of completed strips.
     * (Called by the "Complete Strip" button)
     */
    public void completeStrip() {
        if (!workingStrip.isEmpty()) {
            completedStrips.add(workingStrip);
            workingStrip = new ArrayList<>();
            display();
        }
    }

    /**
     * Simple Mouse actions to move shreds and strips
     * User can
     * - move a Shred from allShreds to a position in the working strip
     * - move a Shred from the working strip back into allShreds
     * - move a Shred around within the working strip.
     * - move a completed Strip around within the list of completed strips
     * - move a completed Strip back to become the working strip
     * (but only if the working strip is currently empty)
     * Moving a shred to a position past the end of a List should put it at the end.
     * You should create additional methods to do the different actions - do not attempt
     * to put all the code inside the doMouse method - you will lose style points for this.
     * Attempting an invalid action should have no effect.
     * Note: doMouse uses getStrip and getColumn, which are written for you (at the end).
     * You should not change them.
     */
    public void doMouse(String action, double x, double y) {
        if (action.equals("pressed")) {
            fromStrip = getStrip(y);      // the List of shreds to move from (possibly null)
            fromPosition = getColumn(x);  // the index of the shred to move (may be off the end)
        }
        if (action.equals("released")) {
            List<Shred> toStrip = getStrip(y); // the List of shreds to move to (possibly null)
            int toPosition = getColumn(x);     // the index to move the shred to (may be off the end)
            // perform the correct action, depending on the from/to strips/positions
            if (!completedStrips.contains(fromStrip) && !completedStrips.contains(toStrip)) {
                moveShred(toStrip, toPosition);
            } else if (completedStrips.contains(fromStrip) && toStrip != allShreds) {
                moveStrip(toStrip);
            }
            display();
        }
        if (suggestions) {
            suggestShreds();
        }
    }

    // Additional methods to perform the different actions, called by doMouse

    /**
     * Moves a shred from one position to another
     * Does not move if no shred is selected, ie: either from or to positions/strips are null,
     * or if the strip it is being moved from is actually empty, or if it is being dragged from
     * beyond the bounds of the strip.
     *
     * @param toStrip the strip to move the shred to (can be same as current)
     * @param toPos   the position in toStrip to move to
     */
    public void moveShred(List<Shred> toStrip, int toPos) {
        if (fromStrip != null && toStrip != null && !fromStrip.isEmpty() && fromPosition < fromStrip.size()) {
            // find the right shred (if it exists) and remove it from the old strip
            Shred moving = fromStrip.get(fromPosition);
            fromStrip.remove(moving);
            if (moving.toString().equals("ID:0") && toStrip == allShreds){ // if a blank shred is moved to allShreds
                return; // skip the bit that adds it to the new strip (ie remove it from everything basically)
            }
            if (toPos >= toStrip.size()) { // if it's been moved to off the end of a strip,
                toStrip.add(moving); // put it at the end of the strip.
            } else {
                toStrip.add(toPos, moving); // "move"/add shred to new location
            }
        }
    }

    /**
     * Swaps the position of one strip with another
     * Can also move completed strips back to an empty working strip
     *
     * @param toStrip the strip to swap the moving strip with
     */
    public void moveStrip(List<Shred> toStrip) {
        if (toStrip == workingStrip && workingStrip.isEmpty()) {
            workingStrip.addAll(fromStrip);
            completedStrips.remove(fromStrip);
        } else if (completedStrips.contains(toStrip)) {
            int fromIndex = completedStrips.indexOf(fromStrip);
            int toIndex = completedStrips.indexOf(toStrip);
            completedStrips.set(toIndex, fromStrip);
            completedStrips.set(fromIndex, toStrip);
        }
    }

    /**
     * Saves completed strips as a PNG image file.
     * Strips must be the same length in order to save it
     * Compiles each pixel from the shreds' 2d color arrays into one larger 2d array
     * Calls saveImage() to save it as a file.
     */
    public void save() {
        if (completedStrips.isEmpty()) {
            return; // don't try and save it if nothing is there to save
        }
        int size = (int) (Shred.SIZE);
        int shredsInRow = completedStrips.get(0).size();
        int fullNumRows = completedStrips.size() * size;
        int fullNumCols = shredsInRow * size;
        Color[][] fullImg = new Color[fullNumRows][fullNumCols];

        for (int stripNum = 0; stripNum < completedStrips.size(); stripNum++) {
            List<Shred> strip = completedStrips.get(stripNum);

            // if the strip is a different length, stop
            if (strip.size() != shredsInRow) {
                UI.println("Please ensure all strips are the same length!");
                return;
            }

            // go row by row in each strip
            for (int row = 0; row < size; row++) {
                Color[] imgRow = new Color[fullNumCols];

                // go through all the shreds in the strip
                for (int shred = 0; shred < strip.size(); shred++) {
                    String shredName = strip.get(shred).toString().substring(3) + ".png";
                    String file = directory.resolve(shredName).toString();
                    Color[][]shredImg;
                    if (!shredName.equals("0.png")) {
                        shredImg = loadImage(file);
                    }
                    else {
                        shredImg = new Color[(int) Shred.SIZE][(int) Shred.SIZE];
                        for (int i = 0; i < Shred.SIZE; i++) {
                            Color[] whiteRow = new Color[(int) Shred.SIZE];
                            for (int j = 0; j < Shred.SIZE; j++) {
                                whiteRow[j] = Color.white;
                            }
                            shredImg[i] = whiteRow;
                        }
                    }
                    // add every pixel from the current row (from all the shreds in the strip) to one array
                    for (int col = 0; col < size; col++) {
                        /* make sure the Color/pixel is added in the right place
                           (may not be the first shred in the strip)*/
                        imgRow[shred * size + col] = shredImg[row][col];
                    }
                }
                /* add the row array to the full image 2d array.
                   make sure it's in the right place as it might not be the first strip anymore */
                fullImg[stripNum * size + row] = imgRow;
            }
        }
        saveImage(fullImg, "test.png");
        UI.println("Saved");
    }

    public void addPadding(){
        Shred padding = new BlankShred();
        workingStrip.add(padding);
        display();
    }

    public void toggleSuggestions() {
        if (suggestions) {
            suggestions = false;
            toggleSug.setText("Suggestions: off");
        }
        else {
            suggestions = true;
            toggleSug.setText("Suggestions: on");
            suggestShreds();
        }
    }

    public void suggestShreds(){
            List<Shred> suggested = new ArrayList<>();
            // use copies of lists so user can still move shreds at same time
            List<Shred> workingStripCopy = new ArrayList<>(workingStrip);
            List<Shred> allShredsCopy = new ArrayList<>(allShreds);
            if (!workingStripCopy.isEmpty()) {
                // get rightmost column of rightmost shred in working strip
                Shred current = workingStripCopy.get(workingStripCopy.size() - 1);
                String currentName = current.toString().substring(3) + ".png";
                String currentFile = directory.resolve(currentName).toString();
                Color[][] currentImg = loadImage(currentFile);
                if (currentImg != null) {
                    Color[] currentLastCol = new Color[currentImg.length];
                    for (int row = 0; row < currentImg.length; row++) {
                        currentLastCol[row] = currentImg[row][currentImg[row].length - 1];
                    }

                    // check how well each shred in allShreds matches
                    for (Shred shred : allShredsCopy) {
                        int matching = 0;

                        // get rightmost column of the shred
                        String imgName = shred.toString().substring(3) + ".png";
                        Color[][] checkImg = loadImage(directory.resolve(imgName).toString());
                        Color[] checkFirstCol = new Color[checkImg.length];
                        for (int row = 0; row < checkImg.length; row++) {
                            checkFirstCol[row] = checkImg[row][0];
                        }

                        // check it against the rightmost column of the last working strip shred
                        for (int px = 0; px < checkFirstCol.length; px++) {
                            // doesn't count white pixels as matches
                            if (checkFirstCol[px].equals(currentLastCol[px]) && !checkFirstCol[px].equals(Color.white)) {
                                matching++;
                            }
                        }

                        // if 5 or more pixels match, it should be suggested
                        if (matching >= 5) {
                            suggested.add(shred);
                        }
                    }

                    // highlight the suggested shreds with a green border
                    for (Shred shred : suggested) {
                        int index = allShredsCopy.indexOf(shred);
                        double left = LEFT + Shred.SIZE * index;
                        UI.setColor(Color.green);
                        UI.setLineWidth(3);
                        UI.drawRect(left, TOP_ALL, Shred.SIZE, Shred.SIZE);
                        UI.setLineWidth(1);
                        UI.setColor(Color.black);
                    }
                }
            }
    }

    //=============================================================================
    // Completed for you. Do not change.
    // loadImage and saveImage may be useful for the challenge.

    /**
     * Displays the remaining Shreds, the working strip, and all completed strips
     */
    public void display() {
        UI.clearGraphics();

        // list of all the remaining shreds that haven't been added to a strip
        double x = LEFT;
        for (Shred shred : allShreds) {
            shred.drawWithBorder(x, TOP_ALL);
            x += SIZE;
        }

        //working strip (the one the user is currently working on)
        x = LEFT;
        for (Shred shred : workingStrip) {
            shred.draw(x, TOP_WORKING);
            x += SIZE;
        }
        UI.setColor(Color.red);
        UI.drawRect(LEFT - 1, TOP_WORKING - 1, SIZE * workingStrip.size() + 2, SIZE + 2);
        UI.setColor(Color.black);

        //completed strips
        double y = TOP_STRIPS;
        for (List<Shred> strip : completedStrips) {
            x = LEFT;
            for (Shred shred : strip) {
                shred.draw(x, y);
                x += SIZE;
            }
            UI.drawRect(LEFT - 1, y - 1, SIZE * strip.size() + 2, SIZE + 2);
            y += SIZE + GAP;
        }
    }

    /**
     * Returns which column the mouse position is on.
     * This will be the index in the list of the shred that the mouse is on,
     * (or the index of the shred that the mouse would be on if the list were long enough)
     */
    public int getColumn(double x) {
        return (int) ((x - LEFT) / (SIZE));
    }

    /**
     * Returns the strip that the mouse position is on.
     * This may be the list of all remaining shreds, the working strip, or
     * one of the completed strips.
     * If it is not on any strip, then it returns null.
     */
    public List<Shred> getStrip(double y) {
        int row = (int) ((y - TOP_ALL) / (SIZE + GAP));
        if (row <= 0) {
            return allShreds;
        } else if (row == 1) {
            return workingStrip;
        } else if (row - 2 < completedStrips.size()) {
            return completedStrips.get(row - 2);
        } else {
            return null;
        }
    }


    /**
     * Load an image from a file and return as a two-dimensional array of Color.
     * From COMP 102 assignment 8&9.
     * Maybe useful for the challenge. Not required for the core or completion.
     */
    public Color[][] loadImage(String imageFileName) {
        if (imageFileName == null || !Files.exists(Path.of(imageFileName))) {
            return null;
        }
        try {
            BufferedImage img = ImageIO.read(Files.newInputStream(Path.of(imageFileName)));
            int rows = img.getHeight();
            int cols = img.getWidth();
            Color[][] ans = new Color[rows][cols];
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {
                    Color c = new Color(img.getRGB(col, row));
                    ans[row][col] = c;
                }
            }
            return ans;
        } catch (IOException e) {
            UI.println("Reading Image from " + imageFileName + " failed: " + e);
        }
        return null;
    }

    /**
     * Save a 2D array of Color as an image file
     * From COMP 102 assignment 8&9.
     * Maybe useful for the challenge. Not required for the core or completion.
     */
    public void saveImage(Color[][] imageArray, String imageFileName) {
        int rows = imageArray.length;
        int cols = imageArray[0].length;
        BufferedImage img = new BufferedImage(cols, rows, BufferedImage.TYPE_INT_RGB);
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                Color c = imageArray[row][col];
                img.setRGB(col, row, c.getRGB());
            }
        }
        try {
            if (imageFileName == null) {
                return;
            }
            ImageIO.write(img, "png", Files.newOutputStream(Path.of(imageFileName)));
        } catch (IOException e) {
            UI.println("Image reading failed: " + e);
        }

    }

    /**
     * Creates an object and set up the user interface
     */
    public static void main(String[] args) {
        DeShredder ds = new DeShredder();
        ds.setupGUI();

    }

}

/**
 * BlankShred is a Shred object that draws a white rectangle instead of an image
 * Both Shred draw methods are overridden to achieve this
 * Passes dummy/unused path and id since path won't be used
 * Behaves as a Shred (since it is one due to the inheritance)
 */
class BlankShred extends Shred {

    /**
     * Construct a new Shred object.
     * Parameters of Shred are the name of the directory and the id of the image
     */
    BlankShred() {
        super(Path.of(""), 0); // id 0 bc based on other code there'd never normally be a 0.png
    }

    @Override
    public void draw(double left, double top) {
        UI.setColor(Color.white);
        UI.fillRect(left, top, SIZE, SIZE);
        UI.setColor(Color.black);
    }

    @Override
    public void drawWithBorder(double left, double top) {
        UI.setColor(Color.white);
        UI.fillRect(left, top, SIZE, SIZE);
        UI.setColor(Color.black);
        UI.drawRect(left, top, SIZE, SIZE);
    }
}