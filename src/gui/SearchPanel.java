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
    public interface SearchListener { 
        void onSearch(String criteria, String term); 
    }

    private JComboBox<String> criteriaBox;
    private JTextField searchField;
    private JButton clearBtn;

    public SearchPanel(SearchListener listener) {
        super(new FlowLayout(FlowLayout.LEFT));

        // Added "Genre" to the search criteria
        criteriaBox = new JComboBox<>(new String[] {"All", "Title", "Author", "ISBN", "Year", "Genre"});
        searchField = new JTextField(20);
        clearBtn = new JButton("Clear");

        add(new JLabel("Search:"));
        add(criteriaBox);
        add(searchField);
        add(clearBtn);

        final javax.swing.Timer searchTimer = new javax.swing.Timer(350, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                listener.onSearch((String)criteriaBox.getSelectedItem(), searchField.getText().trim());
            }
        });
        searchTimer.setRepeats(false);

        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void restart() {
                if (searchTimer.isRunning()) searchTimer.restart(); 
                else searchTimer.start();
            }

            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { restart(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { restart(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { restart(); }
        });

        clearBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (searchTimer.isRunning()) searchTimer.stop();
                searchField.setText("");
                listener.onSearch((String)criteriaBox.getSelectedItem(), "");
            }
        });
    }
}