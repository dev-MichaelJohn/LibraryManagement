package gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Small search panel with criteria combo, text field and clear button.
 * It debounces input and notifies a SearchListener when search should run.
 * Now includes Genre as a search criteria.
 *
 * @author AI
 */
public class SearchPanel extends JPanel {
    /**
     * Interface that defines the callback when a search is triggered.
     * Called with the selected search criteria and the search term text.
     */
    public interface SearchListener { 
        void onSearch(String criteria, String term); 
    }

    private JComboBox<String> criteriaBox;  // Dropdown for selecting search field (All, Title, Author, etc)
    private JTextField searchField;         // Text input for the search query
    private JButton clearBtn;               // Button to clear the search

    /**
     * Constructor that builds the search UI and sets up event listeners.
     * 
     * @param listener The callback object that will be notified when search should execute
     */
    public SearchPanel(SearchListener listener) {
        super(new FlowLayout(FlowLayout.LEFT));

        // Create dropdown with search criteria options (All, Title, Author, ISBN, Year, Genre)
        criteriaBox = new JComboBox<>(new String[] {"All", "Title", "Author", "ISBN", "Year", "Genre"});
        searchField = new JTextField(20);
        clearBtn = new JButton("Clear");

        // Add UI components to the panel
        add(new JLabel("Search:"));
        add(criteriaBox);
        add(searchField);
        add(clearBtn);

        /**
         * DEBOUNCING MECHANISM:
         * A Timer is used to delay search execution. When the user types, we don't search immediately.
         * Instead, we wait 350ms after the user stops typing. This reduces unnecessary DB queries.
         * 
         * How it works:
         * 1. Timer is set to 350ms and doesn't repeat (setRepeats(false))
         * 2. When search field text changes, we restart the timer
         * 3. If user types again before 350ms, restart() restarts the countdown
         * 4. Only when 350ms pass without typing does the timer fire and search actually runs
         */
        final javax.swing.Timer searchTimer = new javax.swing.Timer(350, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Fire the search callback with current criteria and text
                listener.onSearch((String)criteriaBox.getSelectedItem(), searchField.getText().trim());
            }
        });
        searchTimer.setRepeats(false);

        /**
         * DocumentListener monitors changes to the search field text.
         * We implement all 3 methods (insert, remove, change) but they all do the same thing:
         * restart the debounce timer. This ensures search only fires after user stops typing.
         */
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void restart() {
                // If timer is already running, restart it (resets the 350ms countdown)
                // Otherwise start it fresh
                if (searchTimer.isRunning()) searchTimer.restart(); 
                else searchTimer.start();
            }

            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { restart(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { restart(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { restart(); }
        });

        /**
         * Clear button: stops any pending search and resets the search field to empty.
         * Also triggers a search with empty term, which typically loads all results again.
         */
        clearBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (searchTimer.isRunning()) searchTimer.stop();  // Cancel any pending search
                searchField.setText("");                           // Clear the text field
                listener.onSearch((String)criteriaBox.getSelectedItem(), "");  // Search with empty term
            }
        });
    }
}