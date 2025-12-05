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
 * @author AI
 */
public class LibraryManager extends JFrame {
    private BookTablePanel tablePanel;
    private LoanPanel loanPanel;

    public LibraryManager() {
        setTitle("Library Manager");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);

        initUI();
        // load initial data
        tablePanel.loadBooks();
        loanPanel.loadLoans();
    }

    private void initUI() {
        JPanel main = new JPanel(new BorderLayout(10, 10));

        // Create the tabbed view. Header/controls are integrated into each tab now.
        JTabbedPane tabs = new JTabbedPane();

        // Create panels/tabs
        tablePanel = new BookTablePanel(this);
        tabs.addTab("Books", tablePanel);

        loanPanel = new LoanPanel(this);
        tabs.addTab("Loans", loanPanel);

        // Reload data whenever a tab is selected so the view stays fresh.
        tabs.addChangeListener(new ChangeListener() {
            @Override public void stateChanged(ChangeEvent e) {
                int idx = tabs.getSelectedIndex();
                if(idx < 0) return;
                java.awt.Component c = tabs.getComponentAt(idx);
                if(c instanceof BookTablePanel) {
                    try { ((BookTablePanel)c).loadBooks(); } catch(Exception ex) { ex.printStackTrace(); }
                } else if(c instanceof LoanPanel) {
                    try { ((LoanPanel)c).loadLoans(); } catch(Exception ex) { ex.printStackTrace(); }
                }
            }
        });

        main.add(tabs, BorderLayout.CENTER);

        getContentPane().add(main);
    }
}