package gui;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

/**
 * LibraryManager now composes smaller components: a header, a search panel,
 * and the book table panel. This keeps the top-level window simple and
 * moves detailed UI logic into separate classes.
 * 
 * This class represents the main application window that users see when they launch the program.
 * It uses a tabbed interface to organize features (Books tab, Loans tab) and refreshes data
 * whenever the user switches between tabs to ensure they always see current information.
 *
 * @author AI
 */
public class LibraryManager extends JFrame {
    private BookTablePanel tablePanel;  // Tab for viewing and managing books
    private LoanPanel loanPanel;        // Tab for viewing and managing book loans

    /**
     * Constructor: Sets up the main window with a title, size, and initial UI.
     * 
     * NOTE: JFrame extends Window, so this represents the actual OS window that contains
     * all other components. Think of it as the container that holds everything.
     */
    public LibraryManager() {
        // Configure the window title (shown in title bar)
        setTitle("Library Manager");
        
        // Set window size: 900 pixels wide, 600 pixels tall
        setSize(900, 600);
        
        // Define what happens when user clicks the X button to close: EXIT_ON_CLOSE means exit the program
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Center the window on the screen (setLocationRelativeTo(null) means use screen center)
        setLocationRelativeTo(null);
        
        // Make the window visible to the user
        setVisible(true);

        // Initialize all the UI components (tabs, panels, buttons, etc)
        initUI();
        
        // Load initial data from database for both tabs
        tablePanel.loadBooks();
        loanPanel.loadLoans();
    }

    /**
     * Initializes the UI structure: creates the tabbed pane and adds the two main panels.
     * 
     * This method demonstrates COMPOSITION - instead of putting all code here, we delegate
     * to specialized classes: BookTablePanel handles books, LoanPanel handles loans.
     * This keeps code organized and maintainable.
     */
    private void initUI() {
        // Main panel uses BorderLayout with 10px padding between components
        JPanel main = new JPanel(new BorderLayout(10, 10));

        /**
         * TABBED INTERFACE:
         * JTabbedPane creates the tabs you see at the top. Users can click on tabs to switch views.
         * BorderLayout organizes the components: tabs will appear at the top, main content below.
         */
        JTabbedPane tabs = new JTabbedPane();

        /**
         * Create the Books tab: This is a BookTablePanel instance which handles all book-related UI
         * (table display, search, add/edit/delete buttons, import/export).
         * We pass 'this' (the LibraryManager frame) so child components can open dialogs on top of the main window.
         */
        tablePanel = new BookTablePanel(this);
        tabs.addTab("Books", tablePanel);

        /**
         * Create the Loans tab: This is a LoanPanel instance which handles all loan-related UI
         * (displays multiple tables for all loans, overdue loans, etc).
         */
        loanPanel = new LoanPanel(this);
        tabs.addTab("Loans", loanPanel);

        /**
         * TAB CHANGE LISTENER - This is the "magic" that keeps data fresh:
         * Whenever the user clicks on a different tab, this listener fires.
         * It checks which tab is now selected and reloads that tab's data from the database.
         * 
         * Why do this? Because data might have changed while the user was on a different tab.
         * For example: user adds a book while on Books tab, switches to Loans tab, then back to Books -
         * we want to show the newly added book, so we reload.
         */
        tabs.addChangeListener(new ChangeListener() {
            @Override 
            public void stateChanged(ChangeEvent e) {
                int idx = tabs.getSelectedIndex();  // Get the index of the now-selected tab
                if(idx < 0) return;  // Invalid index, exit
                
                // Get the component (panel) for this tab
                java.awt.Component c = tabs.getComponentAt(idx);
                
                // Check which panel is selected and reload its data
                if(c instanceof BookTablePanel) {
                    try { 
                        ((BookTablePanel)c).loadBooks();  // Reload all books from database
                    } catch(Exception ex) { 
                        ex.printStackTrace(); 
                    }
                } else if(c instanceof LoanPanel) {
                    try { 
                        ((LoanPanel)c).loadLoans();  // Reload all loans from database
                    } catch(Exception ex) { 
                        ex.printStackTrace(); 
                    }
                }
            }
        });

        // Add the tabbed pane to the center of the main panel
        main.add(tabs, BorderLayout.CENTER);

        // Add the main panel to the frame's content pane (the actual viewable area of the frame)
        getContentPane().add(main);
    }
}