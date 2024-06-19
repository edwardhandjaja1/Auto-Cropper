package selector;

import static selector.SelectionModel.SelectionState.*;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.util.LinkedList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import scissors.ScissorsSelectionModel;
import selector.SelectionModel.SelectionState;

/**
 * A graphical application for selecting and extracting regions of images.
 */
public class SelectorApp implements PropertyChangeListener {

    /**
     * Our application window.  Disposed when application exits.
     */
    private final JFrame frame;

    /**
     * Component for displaying the current image and selection tool.
     */
    private final ImagePanel imgPanel;

    /**
     * The current state of the selection tool.  Must always match the model used by `imgPanel`.
     */
    private SelectionModel model;

    /* Components whose state must be changed during the selection process. */
    private JMenuItem saveItem;
    private JMenuItem undoItem;
    private JButton cancelButton;
    private JButton undoButton;
    private JButton resetButton;
    private JButton finishButton;
    private final JLabel statusLabel;

    private JComboBox selectionModelOptions;

    private JProgressBar processingProgress;


    /**
     * Construct a new application instance.  Initializes GUI components, so must be invoked on the
     * Swing Event Dispatch Thread.  Does not show the application window (call `start()` to do
     * that).
     */
    public SelectorApp() {
        // Initialize application window
        frame = new JFrame("Selector");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Add status bar
        statusLabel = new JLabel();

        frame.add(statusLabel, BorderLayout.SOUTH);

        // Add image component with scrollbars
        imgPanel = new ImagePanel();

        JScrollPane scrollImgPanel = new JScrollPane(imgPanel);
        scrollImgPanel.setPreferredSize(new Dimension(700, 600));
        frame.add(scrollImgPanel, BorderLayout.CENTER);

        processingProgress = new JProgressBar();
        frame.add(processingProgress, BorderLayout.PAGE_START);

        // Add menu bar
        frame.setJMenuBar(makeMenuBar());

        // Add control buttons
        frame.add(makeControlPanel(), BorderLayout.EAST);

        // Controller: Set initial selection tool and update components to reflect its state
        setSelectionModel(new PointToPointSelectionModel(true));
    }

    /**
     * Create and populate a menu bar with our application's menus and items and attach listeners.
     * Should only be called from constructor, as it initializes menu item fields.
     */
    private JMenuBar makeMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // Create and populate File menu
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);
        JMenuItem openItem = new JMenuItem("Open...", KeyEvent.VK_T);
        fileMenu.add(openItem);
        saveItem = new JMenuItem("Save...");
        fileMenu.add(saveItem);
        JMenuItem closeItem = new JMenuItem("Close");
        fileMenu.add(closeItem);
        JMenuItem exitItem = new JMenuItem("Exit");
        fileMenu.add(exitItem);

        // Create and populate Edit menu
        JMenu editMenu = new JMenu("Edit");
        menuBar.add(editMenu);
        undoItem = new JMenuItem("Undo");
        editMenu.add(undoItem);

        //Assign KeyStroke objects to certain keyboard clicks (KeyStrokes)
        KeyStroke ctrlO = KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        openItem.setAccelerator(ctrlO);

        KeyStroke ctrlS = KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        saveItem.setAccelerator(ctrlS);

        KeyStroke ctrlC = KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        closeItem.setAccelerator(ctrlC);

        KeyStroke ctrlE = KeyStroke.getKeyStroke(KeyEvent.VK_E, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        exitItem.setAccelerator(ctrlE);

        KeyStroke ctrlZ = KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        undoItem.setAccelerator(ctrlZ);

        // Controller: Attach menu item listeners
        openItem.addActionListener(e -> openImage());
        closeItem.addActionListener(e -> imgPanel.setImage(null));
        saveItem.addActionListener(e -> saveSelection());
        exitItem.addActionListener(e -> frame.dispose());
        undoItem.addActionListener(e -> model.undo());

        return menuBar;
    }

    /**
     * Return a panel containing buttons for controlling image selection.  Should only be called
     * from constructor, as it initializes button fields.
     */
    private JPanel makeControlPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 1));

        //Make all buttons required
        cancelButton = new JButton("Cancel");

        undoButton = new JButton("Undo");

        resetButton = new JButton("Reset");

        finishButton = new JButton("Finish");

        String[] models = new String[]{"Point-to-point", "Intelligent Scissors", "RGB Scissors"};
        selectionModelOptions = new JComboBox(models);


        //Add action listeners to each button; connects a button to a given function to run
        cancelButton.addActionListener(e -> model.cancelProcessing());
        undoButton.addActionListener(e -> model.undo());
        resetButton.addActionListener(e -> model.reset());
        finishButton.addActionListener(e -> model.finishSelection());
        selectionModelOptions.addActionListener(e -> {
            if (selectionModelOptions.getSelectedItem().equals("Point-to-point")){
                SelectionModel newModel = new PointToPointSelectionModel(model);
                setSelectionModel(newModel);
            }else if(selectionModelOptions.getSelectedItem().equals("Intelligent Scissors")){
                SelectionModel newModel = new ScissorsSelectionModel("CrossGradMono", model);
                setSelectionModel(newModel);
            }else if(selectionModelOptions.getSelectedItem().equals("RGB Scissors")){
                SelectionModel newModel = new ScissorsSelectionModel("RGBWeight", model);
                setSelectionModel(newModel);
            }
        });



        // Add buttons to the panel
        panel.add(cancelButton);
        panel.add(undoButton);
        panel.add(resetButton);
        panel.add(finishButton);
        panel.add(selectionModelOptions);

        return panel;
    }

    /**
     * Start the application by showing its window.
     */
    public void start() {
        // Compute ideal window size
        frame.pack();

        frame.setVisible(true);
    }

    /**
     * React to property changes in an observed model. Supported properties include: * "state":
     *      * Update components to reflect the new selection state. * "progress": Update the processing
     *      * progress bar.
     *      */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        SelectionState newState = model.state();

        String propertyName = evt.getPropertyName();

        if("progress".equals(propertyName)){
            processingProgress.setIndeterminate(false);
            int newProgress = (Integer) evt.getNewValue();
            processingProgress.setValue(newProgress);
        }

        if("state".equals(propertyName)) {
            reflectSelectionState(model.state());
            if(newState == PROCESSING){
                processingProgress.setIndeterminate(true);
            }else{
                processingProgress.setIndeterminate(false);
                processingProgress.setValue(0);
            }
        }
    }

    /**
     * Update components to reflect a selection state of `state`.  Disable buttons and menu items
     * whose actions are invalid in that state, and update the status bar.
     */
    private void reflectSelectionState(SelectionState state) {
        // Update status bar to show current state
        statusLabel.setText(state.toString());

        if (state == NO_SELECTION){
            cancelButton.setEnabled(false);
            undoButton.setEnabled(false);
            resetButton.setEnabled(false);
            finishButton.setEnabled(false);
            saveItem.setEnabled(false);
        }
        if (state == SELECTING){
            cancelButton.setEnabled(false);
            undoButton.setEnabled(true);
            resetButton.setEnabled(true);
            finishButton.setEnabled(true);
            saveItem.setEnabled(false);
        }
        if (state == SELECTED){
            cancelButton.setEnabled(false);
            undoButton.setEnabled(false);
            resetButton.setEnabled(true);
            finishButton.setEnabled(false);
            saveItem.setEnabled(true);
        }
        if (state == PROCESSING){
            cancelButton.setEnabled(true);
            undoButton.setEnabled(true);
            resetButton.setEnabled(true);
            finishButton.setEnabled(false);
            saveItem.setEnabled(false);
        }
    }

    /**
     * Return the model of the selection tool currently in use.
     */
    public SelectionModel getSelectionModel() {
        return model;
    }

    /**
     * Use `newModel` as the selection tool and update our view to reflect its state.  This
     * application will no longer respond to changes made to its previous selection model and will
     * instead respond to property changes from `newModel`.
     */
    public void setSelectionModel(SelectionModel newModel) {
        // Stop listening to old model
        if (model != null) {
            model.removePropertyChangeListener(this);
        }

        imgPanel.setSelectionModel(newModel);
        model = imgPanel.selection();
        model.addPropertyChangeListener("state", this);
        model.addPropertyChangeListener("progress", this);

        // Since the new model's initial state may be different from the old model's state, manually
        //  trigger an update to our state-dependent view.
        reflectSelectionState(model.state());
    }

    /**
     * Start displaying and selecting from `img` instead of any previous image.  Argument may be
     * null, in which case no image is displayed and the current selection is reset.
     */
    public void setImage(BufferedImage img) {
        imgPanel.setImage(img);
    }

    /**
     * Allow the user to choose a new image from an "open" dialog.  If they do, start displaying and
     * selecting from that image.  Show an error message dialog (and retain any previous image) if
     * the chosen image could not be opened.
     */
    private void openImage() {
        JFileChooser chooser = new JFileChooser();
        // Start browsing in current directory
        chooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        // Filter for file extensions supported by Java's ImageIO readers
        chooser.setFileFilter(new FileNameExtensionFilter("Image files",
                ImageIO.getReaderFileSuffixes()));

        boolean imageCanLoad = true;

        while(imageCanLoad){
            int returnVal = chooser.showOpenDialog(imgPanel);
            if (returnVal == JFileChooser.APPROVE_OPTION){
                File selectedFile = chooser.getSelectedFile();
                BufferedImage selectedImage = null;
                try {
                    selectedImage = ImageIO.read(selectedFile);
                    if (selectedImage == null){
                        JOptionPane.showMessageDialog(imgPanel,"Could not read the image at " + selectedFile.getPath(),"Unsupported image format",JOptionPane.ERROR_MESSAGE);
                    }else{
                        this.setImage(selectedImage);
                        break;
                    }
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(imgPanel,"Error loading image: " + e.getMessage(),"Image Load Error",JOptionPane.ERROR_MESSAGE);
                }
            }else{
                imageCanLoad = false;
            }
        }

    }

    /**
     * Save the selected region of the current image to a file selected from a "save" dialog.
     * Show an error message dialog if the image could not be saved.
     */
    private void saveSelection() {
        JFileChooser chooser = new JFileChooser();
        // Start browsing in current directory
        chooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        // We always save in PNG format, so only show existing PNG files
        chooser.setFileFilter(new FileNameExtensionFilter("PNG images", "png"));

        boolean imageCanSave = true;

        while(imageCanSave){
            int returnVal = chooser.showSaveDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION){
            File selectedFile = chooser.getSelectedFile();
            if (!selectedFile.getAbsolutePath().endsWith(".png")){
                //Renames chosen file to save in if it does not end with ".png"
                selectedFile.renameTo(new File(chooser.getSelectedFile().getName() + "png"));
            }
            try {
                //Save selected region of the current image to selected file
                model.saveSelection(new FileOutputStream(selectedFile));
                break;
            } catch (IOException ex) {
                //Acts appropriately if failed to save
                JOptionPane.showMessageDialog(imgPanel,"Error saving image: " + ex.getMessage(), String.valueOf(ex.getClass()),JOptionPane.ERROR_MESSAGE);
                imageCanSave = false;
            }
        }else{
            imageCanSave = false;
        }
    }
        }

    /**
     * Run an instance of SelectorApp.  No program arguments are expected.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Set Swing theme to look the same (and less old) on all operating systems.
            try {
                UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            } catch (Exception ignored) {
                /* If the Nimbus theme isn't available, just use the platform default. */
            }

            // Create and start the app
            SelectorApp app = new SelectorApp();
            app.start();
        });
    }
}
