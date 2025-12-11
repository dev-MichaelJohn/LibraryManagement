package gui;

import service.BookService;
import service.BookGenreService;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reusable add/update book dialog with genre management.
 * On success it runs the provided callback.
 *
 * @author AI
 */
public class BookFormDialog extends JDialog {
    private DefaultListModel<String> genreListModel;
    private JList<String> genreList;
    private List<Integer> genreRecordIds; // Track genre record IDs for updates
    
    public BookFormDialog(JFrame owner, Runnable onSuccess) {
        super(owner, "Add New Book", true);
        init(null, onSuccess);
    }

    public BookFormDialog(JFrame owner, int id, String title, String author, String isbn, String year, Runnable onSuccess) {
        super(owner, "Update Book", true);
        init(new BookData(id, title, author, isbn, year), onSuccess);
    }

    private void init(BookData data, Runnable onSuccess) {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setResizable(false);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6,6,6,6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title field
        JLabel titleLabel = new JLabel("Title:");
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0;
        form.add(titleLabel, gbc);

        JTextField titleField = new JTextField(data == null ? "" : data.title, 30);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        form.add(titleField, gbc);

        // Author field
        JLabel authorLabel = new JLabel("Author:");
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
        form.add(authorLabel, gbc);

        JTextField authorField = new JTextField(data == null ? "" : data.author, 30);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        form.add(authorField, gbc);

        // ISBN field
        JLabel isbnLabel = new JLabel("ISBN:");
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0;
        form.add(isbnLabel, gbc);

        JTextField isbnField = new JTextField(data == null ? "" : data.isbn, 20);
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1.0;
        form.add(isbnField, gbc);

        // Year field
        JLabel yearLabel = new JLabel("Year Published:");
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.0;
        form.add(yearLabel, gbc);

        JTextField yearField = new JTextField(data == null ? "" : data.year, 6);
        gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 1.0;
        form.add(yearField, gbc);

        // ========== GENRE SECTION ==========
        JLabel genreLabel = new JLabel("Genres:");
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.0; gbc.anchor = GridBagConstraints.NORTHWEST;
        form.add(genreLabel, gbc);

        // Genre panel with list and controls
        JPanel genrePanel = new JPanel(new BorderLayout(5, 5));
        
        // Genre list
        genreListModel = new DefaultListModel<>();
        genreRecordIds = new ArrayList<>();
        genreList = new JList<>(genreListModel);
        genreList.setVisibleRowCount(4);
        JScrollPane genreScrollPane = new JScrollPane(genreList);
        genrePanel.add(genreScrollPane, BorderLayout.CENTER);

        // Genre control buttons
        JPanel genreButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JButton addGenreBtn = new JButton("Add Genre");
        JButton removeGenreBtn = new JButton("Remove Selected");
        genreButtons.add(addGenreBtn);
        genreButtons.add(removeGenreBtn);
        genrePanel.add(genreButtons, BorderLayout.SOUTH);

        gbc.gridx = 1; gbc.gridy = 4; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.BOTH;
        form.add(genrePanel, gbc);

        // Load existing genres if updating
        if(data != null) {
            loadExistingGenres(data.id);
        }

        // Add genre button action
        addGenreBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String genre = JOptionPane.showInputDialog(BookFormDialog.this, 
                    "Enter genre name:", "Add Genre", JOptionPane.PLAIN_MESSAGE);
                
                if(genre != null && !genre.trim().isEmpty()) {
                    genre = genre.trim();
                    // Check for duplicates
                    for(int i = 0; i < genreListModel.size(); i++) {
                        if(genreListModel.get(i).equalsIgnoreCase(genre)) {
                            JOptionPane.showMessageDialog(BookFormDialog.this, 
                                "Genre already added.", "Duplicate", JOptionPane.WARNING_MESSAGE);
                            return;
                        }
                    }
                    genreListModel.addElement(genre);
                    genreRecordIds.add(-1); // -1 indicates new genre (not yet in DB)
                }
            }
        });

        // Remove genre button action
        removeGenreBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedIndex = genreList.getSelectedIndex();
                if(selectedIndex >= 0) {
                    genreListModel.remove(selectedIndex);
                    genreRecordIds.remove(selectedIndex);
                } else {
                    JOptionPane.showMessageDialog(BookFormDialog.this, 
                        "Please select a genre to remove.", "No Selection", JOptionPane.WARNING_MESSAGE);
                }
            }
        });

        // ========== BUTTONS SECTION ==========
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton submit = new JButton("Save");
        JButton cancel = new JButton("Cancel");

        submit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String title = titleField.getText().trim();
                String author = authorField.getText().trim();
                String isbn = isbnField.getText().trim();
                String yearText = yearField.getText().trim();

                if(title.isEmpty() || author.isEmpty() || isbn.isEmpty() || yearText.isEmpty()) {
                    JOptionPane.showMessageDialog(BookFormDialog.this, 
                        "All fields are required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                int year;
                try {
                    year = Integer.parseInt(yearText);
                    if(year < 0) throw new NumberFormatException("negative");
                } catch(NumberFormatException ex) {
                    JOptionPane.showMessageDialog(BookFormDialog.this, 
                        "Year must be a non-negative integer.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                try {
                    boolean ok;
                    int bookId;
                    
                    if(data == null) {
                        // Insert new book
                        ok = BookService.InsertBook()
                                .SetTitle(title)
                                .SetAuthor(author)
                                .SetIsbn(isbn)
                                .SetYearPublished(year)
                                .Insert();
                        
                        if(ok) {
                            // Get the newly inserted book ID
                            List<Map<String, Object>> books = BookService.ReadBook()
                                .WhereIsbn(isbn)
                                .Read();
                            if(!books.isEmpty()) {
                                bookId = (Integer) books.get(0).get("id");
                                // Insert genres
                                saveGenres(bookId);
                            }
                        }
                    } else {
                        // Update existing book
                        ok = BookService.UpdateBook()
                                .SetTitle(title)
                                .SetAuthor(author)
                                .SetIsbn(isbn)
                                .SetYearPublished(year)
                                .WhereBookID(data.id)
                                .Update();
                        
                        if(ok) {
                            // Update genres
                            updateGenres(data.id);
                        }
                    }

                    if(ok) {
                        JOptionPane.showMessageDialog(BookFormDialog.this, 
                            "Saved successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                        dispose();
                        if(onSuccess != null) onSuccess.run();
                    } else {
                        JOptionPane.showMessageDialog(BookFormDialog.this, 
                            "Failed to save.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch(Exception ex) {
                    JOptionPane.showMessageDialog(BookFormDialog.this, 
                        "Error: " + ex.getMessage(), "Exception", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            }
        });

        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        buttons.add(cancel);
        buttons.add(submit);

        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2; gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        form.add(buttons, gbc);

        getContentPane().add(form);
        pack();
        setLocationRelativeTo(getOwner());
    }

    /**
     * Load existing genres for a book when the dialog is opened in UPDATE mode.
     * This populates the genre list UI with genres already associated with the book.
     * 
     * We also track the genre record IDs (from the database) so we know which ones
     * are existing vs. new when we update later.
     * 
     * @param bookId The ID of the book to load genres for
     */
    private void loadExistingGenres(int bookId) {
        try {
            // Query the database for all genres linked to this book
            List<Map<String, Object>> genres = BookGenreService.ReadBookGenre()
                .WhereBookID(bookId)
                .Read();
            
            // Add each genre to the UI list and remember its database ID
            for(Map<String, Object> genreRecord : genres) {
                String genre = (String) genreRecord.get("genre");      // The genre name
                Integer id = (Integer) genreRecord.get("id");          // The database record ID
                genreListModel.addElement(genre);                      // Show in list
                genreRecordIds.add(id);                                // Track database ID
            }
        } catch(Exception e) {
            System.err.println("Error loading genres: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Save genres for a newly created book (INSERT mode).
     * 
     * When a new book is added, the genres don't exist yet in the database.
     * This method inserts each genre as a new record linked to the book.
     * 
     * @param bookId The ID of the newly inserted book
     */
    private void saveGenres(int bookId) {
        // Iterate through all genres in the list
        for(int i = 0; i < genreListModel.size(); i++) {
            String genre = genreListModel.get(i);
            
            // Skip empty genres (validation)
            if(genre == null || genre.trim().isEmpty()) {
                System.err.println("Skipping empty genre");
                continue;
            }
            
            try {
                // Insert a new BookGenre record linking this book to this genre
                BookGenreService.InsertBookGenre()
                    .SetBookID(bookId)
                    .SetGenre(genre.trim())
                    .Insert();
            } catch(Exception e) {
                System.err.println("Error inserting genre '" + genre + "': " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Update genres for an existing book (UPDATE mode).
     * 
     * This method handles the complex logic of:
     * 1. Deleting genres that were removed by the user
     * 2. Keeping genres that weren't changed
     * 3. Adding new genres that the user added
     * 
     * How does it know what's new vs. existing?
     * - genreRecordIds stores the database ID for each genre
     * - New genres have ID = -1 (special marker we set in the "Add Genre" code)
     * - Existing genres have a real ID (> 0)
     * 
     * @param bookId The ID of the book to update genres for
     */
    private void updateGenres(int bookId) {
        try {
            // Get all genres currently in the database for this book
            List<Map<String, Object>> dbGenres = BookGenreService.ReadBookGenre()
                .WhereBookID(bookId)
                .Read();
            
            /**
             * STEP 1: Find and delete genres that were removed
             * 
             * Scenario: Book had genres "Sci-Fi" and "Fiction".
             * User removes "Fiction" and saves.
             * We need to delete the "Fiction" record from the database.
             */
            for(Map<String, Object> dbGenre : dbGenres) {
                Integer dbId = (Integer) dbGenre.get("id");  // Database record ID
                
                // Is this ID still in our current list?
                if(!genreRecordIds.contains(dbId)) {
                    // No - user removed it, so delete it from database
                    BookGenreService.DeleteBookGenre()
                        .WhereID(dbId)
                        .Delete();
                }
            }
            
            /**
             * STEP 2: Add new genres that the user added
             * 
             * Scenario: User adds "Adventure" and "Mystery" to the list.
             * These have ID = -1 (our marker for "not yet in database").
             * We need to insert them as new records.
             */
            for(int i = 0; i < genreListModel.size(); i++) {
                // Check if this is a new genre (marked with ID = -1)
                if(genreRecordIds.get(i) == -1) {
                    String genre = genreListModel.get(i);
                    
                    // Skip empty genres (validation)
                    if(genre == null || genre.trim().isEmpty()) {
                        System.err.println("Skipping empty genre");
                        continue;
                    }
                    
                    // Insert the new genre into the database
                    BookGenreService.InsertBookGenre()
                        .SetBookID(bookId)
                        .SetGenre(genre.trim())
                        .Insert();
                }
            }
        } catch(Exception e) {
            System.err.println("Error updating genres: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static class BookData {
        int id;
        String title, author, isbn, year;
        BookData(int id, String t, String a, String i, String y) { 
            this.id = id; 
            title = t; 
            author = a; 
            isbn = i; 
            year = y; 
        }
    }
}