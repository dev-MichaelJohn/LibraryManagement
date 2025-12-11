package gui;

import service.BookService;
import service.BookGenreService;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.io.*;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Encapsulates the JTable and related actions (load, search, delete, update).
 * Now includes genre display and search functionality.
 * 
 * This panel displays books in a table format and provides:
 * - Add/Edit/Delete operations
 * - Search functionality with debouncing
 * - CSV import/export capabilities
 * - Mouse and keyboard interactions
 * 
 * @author AI
 */
public class BookTablePanel extends JPanel {
    private JTable table;                   // The table that displays book data
    private DefaultTableModel tableModel;   // The data model behind the table (holds rows/columns)
    private JFrame owner;                   // Reference to parent frame (used for dialogs)

    /**
     * Constructor: Builds the entire Books panel with header, buttons, search, and table.
     * 
     * Layout structure (BorderLayout):
     *   NORTH: header, buttons (Add, Import, Export) and search bar
     *   CENTER: JTable with book data
     *   
     * @param owner The parent JFrame (used when opening dialogs like "Add Book" or "Edit Book")
     */
    public BookTablePanel(JFrame owner) {
        // Initialize as a JPanel with BorderLayout (organizes components in 5 regions: N, S, E, W, CENTER)
        super(new BorderLayout());
        this.owner = owner;
        
        // ========== TOP SECTION: Header and Control Buttons ==========
        JPanel top = new JPanel(new BorderLayout());
        
        // Header label: "Books" (left side)
        JLabel header = new JLabel("Books");
        header.setFont(header.getFont().deriveFont(Font.BOLD, 18f));  // Make it bold and larger
        header.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));   // Add padding around text
        top.add(header, BorderLayout.WEST);

        // RIGHT SIDE: Control buttons (Add, Import, Export)
        JPanel rightWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        /**
         * ADD BOOK button:
         * When clicked, opens a BookFormDialog (a modal dialog on top of the main window).
         * The dialog allows user to enter book details. When user clicks "Save" in the dialog,
         * the onSuccess callback fires, which calls loadBooks() to refresh the table.
         */
        JButton addButton = new JButton("Add Book");
        addButton.setBorder(BorderFactory.createEmptyBorder(6,10,6,10));
        addButton.addActionListener(e -> {
            // Create a new add dialog (no book ID means it's an add, not update)
            BookFormDialog d = new BookFormDialog(owner, new Runnable() { 
                @Override public void run() { 
                    loadBooks();  // Reload table after dialog closes successfully
                } 
            });
            d.setVisible(true);  // Show the dialog (blocks until user closes it)
        });
        rightWrap.add(addButton);

        /**
         * IMPORT CSV button:
         * Opens a file chooser dialog to let user select a CSV file to import.
         * The CSV file should contain book data (Title, Author, ISBN, Year, optional Genres).
         */
        JButton importBtn = new JButton("Import CSV");
        importBtn.setBorder(BorderFactory.createEmptyBorder(6,10,6,10));
        importBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();  // File chooser dialog
            fc.setFileFilter(new FileNameExtensionFilter("CSV files", "csv"));  // Only show .csv files
            int rv = fc.showOpenDialog(BookTablePanel.this);  // Show dialog and wait for user choice
            if(rv == JFileChooser.APPROVE_OPTION) {  // User clicked "Open"
                File f = fc.getSelectedFile();
                importBooksFromCSV(f);  // Process the CSV file
            }
        });
        rightWrap.add(importBtn);

        /**
         * EXPORT CSV button:
         * Exports all currently displayed table rows to a CSV file.
         * The user chooses where to save it via a file chooser dialog.
         */
        JButton exportBtn = new JButton("Export CSV");
        exportBtn.setBorder(BorderFactory.createEmptyBorder(6,10,6,10));
        exportBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("CSV files", "csv"));
            fc.setSelectedFile(new File("books_export.csv"));  // Default filename suggestion
            int rv = fc.showSaveDialog(BookTablePanel.this);   // Show save dialog
            if(rv == JFileChooser.APPROVE_OPTION) {
                File f = fc.getSelectedFile();
                exportTableToCSV(f);  // Write table data to CSV file
            }
        });
        rightWrap.add(exportBtn);
        
        top.add(rightWrap, BorderLayout.EAST);

        /**
         * SEARCH PANEL:
         * The SearchPanel handles the debounced search UI (dropdown for criteria, text field, clear button).
         * When user types, it waits 350ms then calls the onSearch callback with the search term and criteria.
         * We implement the SearchListener interface to receive those callbacks and call performSearch().
         */
        SearchPanel search = new SearchPanel(new SearchPanel.SearchListener() {
            @Override public void onSearch(String criteria, String term) { 
                performSearch(criteria, term);  // Execute search with the given criteria and term
            }
        });
        top.add(search, BorderLayout.SOUTH);
        
        add(top, BorderLayout.NORTH);  // Add the entire top panel (header + buttons + search) to NORTH

        // ========== TABLE SETUP: Define columns and create JTable ==========
        
        /**
         * Define table columns: ID, Title, Author, ISBN, Year, Genres, Available
         * These will appear as column headers in the table.
         */
        String[] columns = new String[] {"ID", "Title", "Author", "ISBN", "Year", "Genres", "Available"};
        
        /**
         * DefaultTableModel is the data structure behind the JTable.
         * Think of it as a 2D array: rows are books, columns are the fields above.
         * We override isCellEditable() to return false, meaning users can't directly edit cells in the table.
         * Instead, they must use the "Update" button or menu to open the edit dialog.
         */
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        /**
         * Create the JTable:
         * - Pass the data model so the table knows what columns and rows to display
         * - setFillsViewportHeight(true): if table is small, it fills the available space
         * - MULTIPLE_INTERVAL_SELECTION: users can select multiple rows by holding Ctrl
         */
        table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        /**
         * MOUSE LISTENER: Handles right-click (context menu) and double-click (edit) actions
         * 
         * mouseClicked fires every time user clicks in the table. The 'e' parameter tells us:
         * - e.getPoint(): where the click happened (x, y coordinates)
         * - e.getClickCount(): 1 for single click, 2 for double-click, etc.
         * - SwingUtilities.isRightMouseButton(): true if it was a right-click
         * - SwingUtilities.isLeftMouseButton(): true if it was a left-click
         */
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Find which row was clicked (rowAtPoint converts screen coords to table row number)
                int row = table.rowAtPoint(e.getPoint());
                if(row < 0) return;  // Invalid row (click was in empty area)
                
                // Make sure the clicked row is selected
                if(!table.isRowSelected(row)) table.setRowSelectionInterval(row, row);

                // RIGHT-CLICK: Show context menu
                if (SwingUtilities.isRightMouseButton(e) || e.isPopupTrigger()) {
                    if(!table.isRowSelected(row)) table.setRowSelectionInterval(row, row);
                    showRowPopup(e, row);  // Show the context menu at click location
                } 
                // DOUBLE-CLICK: Open edit dialog
                else if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    showUpdateDialogForRow(row);  // Open edit dialog for this row's book
                }
            }
        });

        /**
         * KEYBOARD SHORTCUT: Delete key
         * 
         * This is an advanced Swing feature:
         * 1. getInputMap(): Get the keyboard mapping for this component
         *    WHEN_ANCESTOR_OF_FOCUSED_COMPONENT means: respond to key presses whenever this table (or its children) has focus
         * 2. put(KeyStroke, actionName): Map the Delete key to the action name "deleteRows"
         * 3. getActionMap(): Maps action names to actual code (AbstractAction)
         * 
         * Result: When user presses Delete while table has focus, deleteSelectedRows() is called
         */
        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteRows");
        table.getActionMap().put("deleteRows", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { 
                deleteSelectedRows();  // Delete all selected rows
            }
        });

        /**
         * CENTER: Add the table to the center of the panel, wrapped in a JScrollPane
         * 
         * JScrollPane adds scrollbars (vertical and/or horizontal) so user can navigate large tables.
         * Without it, if the table is bigger than the panel, content would be clipped.
         */
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    /**
     * Fetch genres for a specific book ID.
     */
    private String getGenresForBook(int bookId) {
        try {
            List<Map<String, Object>> genres = BookGenreService.ReadBookGenre()
                .WhereBookID(bookId)
                .Read();
            
            if(genres.isEmpty()) return "";
            
            return genres.stream()
                .map(g -> (String) g.get("genre"))
                .filter(genre -> genre != null && !genre.trim().isEmpty())
                .collect(Collectors.joining(", "));
        } catch(Exception e) {
            System.err.println("Error loading genres for book " + bookId + ": " + e.getMessage());
            return "";
        }
    }

    /**
     * Load all books from the database and populate the table.
     * 
     * This method demonstrates the SWINGWORKER pattern - a special Swing pattern for running
     * long operations (like database queries) without freezing the UI.
     * 
     * Why SwingWorker?
     * If we directly call BookService.ReadBook().Read() here, the UI thread blocks while waiting
     * for the database to return data. This freezes the entire application - user can't click buttons,
     * resize windows, etc. SwingWorker solves this by:
     * 1. Running the database query on a background thread (doInBackground)
     * 2. Automatically switching back to the UI thread to display results (done)
     */
    public void loadBooks() {
        // Show a "wait" cursor (spinning circle icon) to indicate the app is working
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        
        /**
         * SwingWorker<ResultType, ProgressType>:
         * - ResultType = List<Map<String, Object>> (the data the background task returns)
         * - ProgressType = Void (we don't use progress updates in this case)
         */
        new javax.swing.SwingWorker<java.util.List<Map<String, Object>>, Void>() {
            Exception error = null;  // Store any error that occurs in the background thread
            
            /**
             * doInBackground: This runs on a BACKGROUND THREAD (not the UI thread).
             * Long operations like database queries should be here to keep the UI responsive.
             * 
             * IMPORTANT: Never update UI components directly from here! UI updates must happen
             * on the UI thread (which is done in the done() method instead).
             * 
             * @return The list of all books from the database
             */
            @Override 
            protected java.util.List<Map<String, Object>> doInBackground() {
                try {
                    // Query database for all books
                    return BookService.ReadBook().Read();
                } catch(Exception ex) { 
                    // Store error to throw later in done() method
                    error = ex; 
                    // Return empty list so done() doesn't crash
                    return java.util.Collections.emptyList(); 
                }
            }
            
            /**
             * done: This is called automatically after doInBackground finishes.
             * It runs on the UI THREAD, so we can safely update UI components here.
             * 
             * This is where we populate the table with the results from doInBackground.
             */
            @Override 
            protected void done() {
                try {
                    // Get the result from doInBackground (this will block if not done yet, but it should be)
                    java.util.List<Map<String, Object>> rows = get();
                    
                    // Clear all existing rows in the table model
                    tableModel.setRowCount(0);
                    
                    // Add each book as a new row in the table
                    for(Map<String, Object> r : rows) {
                        // Extract fields from the book record, with fallback names for different DB schemas
                        Object id = r.getOrDefault("id", r.getOrDefault("book_id", ""));
                        Object title = r.getOrDefault("title", "");
                        Object author = r.getOrDefault("author", "");
                        Object isbn = r.getOrDefault("isbn", "");
                        Object year = r.getOrDefault("year_published", r.getOrDefault("year", ""));
                        Object rawAvailable = r.getOrDefault("is_available", r.getOrDefault("is_avaible", null));
                        String available = formatAvailable(rawAvailable);  // Convert boolean/int to "Yes"/"No"
                        
                        // Fetch genres for this book (separate DB query, could be optimized)
                        String genres = "";
                        try {
                            int bookId = Integer.parseInt(String.valueOf(id));
                            genres = getGenresForBook(bookId);
                        } catch(Exception ex) {
                            System.err.println("Error getting genres: " + ex.getMessage());
                        }
                        
                        // Add row to table: [ID, Title, Author, ISBN, Year, Genres, Available]
                        tableModel.addRow(new Object[] { id, title, author, isbn, year, genres, available });
                    }
                    
                    // If an error occurred in the background, throw it now to show error dialog
                    if(error != null) throw error;
                } catch(Exception ex) {
                    // Show error dialog to user
                    JOptionPane.showMessageDialog(BookTablePanel.this, 
                        "Failed to load books: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    // Always restore normal cursor (not wait cursor) when done
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        }.execute();  // Start the background worker thread
    }

    public void performSearch(String criteria, String term) {
        if(term == null || term.isEmpty()) { 
            loadBooks(); 
            return; 
        }
        
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new javax.swing.SwingWorker<java.util.List<Map<String, Object>>, Void>() {
            Exception error = null;
            
            @Override 
            protected java.util.List<Map<String, Object>> doInBackground() {
                try {
                    // Handle genre search separately
                    if("Genre".equalsIgnoreCase(criteria)) {
                        return searchByGenre(term);
                    }
                    
                    // Handle other criteria
                    if("Title".equalsIgnoreCase(criteria)) {
                        return BookService.ReadBook().WhereTitle(term).Read();
                    }
                    if("Author".equalsIgnoreCase(criteria)) {
                        return BookService.ReadBook().WhereAuthor(term).Read();
                    }
                    if("ISBN".equalsIgnoreCase(criteria)) {
                        return BookService.ReadBook().WhereIsbn(term).Read();
                    }
                    if("Year".equalsIgnoreCase(criteria)) {
                        int y; 
                        try { 
                            y = Integer.parseInt(term); 
                        } catch(Exception ex) { 
                            throw new RuntimeException("Year must be a number."); 
                        }
                        return BookService.ReadBook().WhereYearPublished(y).Read();
                    }
                    
                    // "All" criteria - search across all fields including genres
                    return searchAll(term);
                    
                } catch(Exception ex) { 
                    error = ex; 
                    return java.util.Collections.emptyList(); 
                }
            }
            
            @Override 
            protected void done() {
                try {
                    java.util.List<Map<String, Object>> rows = get();
                    tableModel.setRowCount(0);
                    for(Map<String, Object> r : rows) {
                        Object id = r.getOrDefault("id", r.getOrDefault("book_id", ""));
                        Object title = r.getOrDefault("title", "");
                        Object author = r.getOrDefault("author", "");
                        Object isbn = r.getOrDefault("isbn", "");
                        Object year = r.getOrDefault("year_published", r.getOrDefault("year", ""));
                        Object rawAvailable = r.getOrDefault("is_available", r.getOrDefault("is_avaible", null));
                        String available = formatAvailable(rawAvailable);
                        
                        // Fetch genres for this book
                        String genres = "";
                        try {
                            int bookId = Integer.parseInt(String.valueOf(id));
                            genres = getGenresForBook(bookId);
                        } catch(Exception ex) {
                            System.err.println("Error getting genres: " + ex.getMessage());
                        }
                        
                        tableModel.addRow(new Object[] { id, title, author, isbn, year, genres, available });
                    }
                    if(error != null) throw error;
                } catch(Exception ex) {
                    JOptionPane.showMessageDialog(BookTablePanel.this, 
                        "Search failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                } finally { 
                    setCursor(Cursor.getDefaultCursor()); 
                }
            }
        }.execute();
    }

    /**
     * Normalize various DB representations of availability into a human string.
     */
    private String formatAvailable(Object raw) {
        if(raw == null) return "No";
        try {
            if(raw instanceof Boolean) return ((Boolean) raw) ? "Yes" : "No";
            if(raw instanceof Number) return (((Number) raw).intValue() != 0) ? "Yes" : "No";
            String s = String.valueOf(raw).trim();
            if(s.isEmpty()) return "No";
            if(s.equalsIgnoreCase("1") || s.equalsIgnoreCase("true") || s.equalsIgnoreCase("yes") ) return "Yes";
            return "No";
        } catch(Exception ex) {
            return "No";
        }
    }

    /**
     * Search books by genre.
     */
    private java.util.List<Map<String, Object>> searchByGenre(String genreTerm) throws Exception {
        System.out.println("Searching by genre: '" + genreTerm + "'");
        
        // First find all genres matching the term
        List<Map<String, Object>> matchingGenres = BookGenreService.ReadBookGenre()
            .WhereGenre(genreTerm)
            .Read();
        
        System.out.println("Found " + matchingGenres.size() + " matching genre records");
        
        if(matchingGenres.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        
        // Extract unique book IDs
        Set<Integer> bookIds = new HashSet<>();
        for(Map<String, Object> genre : matchingGenres) {
            Integer bookId = (Integer) genre.get("book_id");
            if(bookId != null) {
                bookIds.add(bookId);
            }
        }
        
        System.out.println("Found " + bookIds.size() + " unique books with matching genres");
        
        // Fetch full book details for these IDs
        java.util.List<Map<String, Object>> results = new ArrayList<>();
        for(Integer bookId : bookIds) {
            List<Map<String, Object>> books = BookService.ReadBook()
                .WhereBookID(bookId)
                .Read();
            results.addAll(books);
        }
        
        return results;
    }

    /**
     * Search across all fields including genres.
     */
    private java.util.List<Map<String, Object>> searchAll(String term) throws Exception {
        java.util.List<Map<String, Object>> all = BookService.ReadBook().Read();
        String lower = term.toLowerCase();
        java.util.List<Map<String, Object>> out = new java.util.ArrayList<>();
        
        for(Map<String, Object> r : all) {
            String title = String.valueOf(r.getOrDefault("title", "")).toLowerCase();
            String author = String.valueOf(r.getOrDefault("author", "")).toLowerCase();
            String isbn = String.valueOf(r.getOrDefault("isbn", "")).toLowerCase();
            String year = String.valueOf(r.getOrDefault("year_published", r.getOrDefault("year", ""))).toLowerCase();
            
            // Also check genres
            String genres = "";
            try {
                int bookId = Integer.parseInt(String.valueOf(r.getOrDefault("id", r.getOrDefault("book_id", "0"))));
                genres = getGenresForBook(bookId).toLowerCase();
            } catch(Exception ex) {
                // Ignore
            }
            
            if(title.contains(lower) || author.contains(lower) || 
               isbn.contains(lower) || year.contains(lower) || genres.contains(lower)) {
                out.add(r);
            }
        }
        return out;
    }

    private void showRowPopup(MouseEvent e, int row) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem updateItem = new JMenuItem("Update");
        JMenuItem deleteItem = new JMenuItem("Delete");

        updateItem.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent ev) { showUpdateDialogForRow(row); }
        });

        deleteItem.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent ev) { deleteSelectedRows(); }
        });

        menu.add(updateItem); 
        menu.add(deleteItem);
        menu.show(e.getComponent(), e.getX(), e.getY());
    }

    private void showUpdateDialogForRow(int row) {
        Object idObj = tableModel.getValueAt(row, 0);
        int id;
        try { 
            id = Integer.parseInt(String.valueOf(idObj)); 
        } catch(Exception ex) { 
            JOptionPane.showMessageDialog(this, "Invalid book id.", "Error", JOptionPane.ERROR_MESSAGE); 
            return; 
        }
        
        String curTitle = String.valueOf(tableModel.getValueAt(row, 1));
        String curAuthor = String.valueOf(tableModel.getValueAt(row, 2));
        String curIsbn = String.valueOf(tableModel.getValueAt(row, 3));
        String curYear = String.valueOf(tableModel.getValueAt(row, 4));

        BookFormDialog d = new BookFormDialog(owner, id, curTitle, curAuthor, curIsbn, curYear, new Runnable() {
            @Override public void run() { loadBooks(); }
        });
        d.setVisible(true);
    }

    public void deleteSelectedRows() {
        int[] sel = table.getSelectedRows();
        if(sel == null || sel.length == 0) return;
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Delete selected book(s)?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if(confirm != JOptionPane.YES_OPTION) return;

        boolean anyFailed = false; 
        StringBuilder errors = new StringBuilder();
        
        for(int i = sel.length - 1; i >= 0; i--) {
            int row = sel[i]; 
            Object idObj = tableModel.getValueAt(row, 0); 
            int id;
            try { 
                id = Integer.parseInt(String.valueOf(idObj)); 
            } catch(Exception ex) { 
                anyFailed = true; 
                errors.append("Invalid id at row ").append(row).append("\n"); 
                continue; 
            }
            
            try { 
                boolean ok = BookService.DeleteBook().WhereBookID(id).Delete(); 
                if(!ok) { 
                    anyFailed = true; 
                    errors.append("Failed to delete id: ").append(id).append("\n"); 
                } 
            } catch(Exception ex) { 
                anyFailed = true; 
                errors.append("Error deleting id ").append(id).append(": ").append(ex.getMessage()).append("\n"); 
            }
        }

        if(anyFailed) {
            JOptionPane.showMessageDialog(this, 
                "Some deletes failed:\n" + errors.toString(), "Partial Failure", JOptionPane.WARNING_MESSAGE); 
        } else {
            JOptionPane.showMessageDialog(this, 
                "Selected book(s) deleted.", "Deleted", JOptionPane.INFORMATION_MESSAGE);
        }
        
        loadBooks();
    }

    private void importBooksFromCSV(File file) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new javax.swing.SwingWorker<Void, Void>() {
            Exception error = null;
            int success = 0;
            int skipped = 0;
            int duplicatesSkipped = 0;
            java.util.List<String> failures = new ArrayList<>();

            private String trimQuotes(String s) {
                if(s == null) return "";
                s = s.trim();
                if(s.startsWith("\"") && s.endsWith("\"")) {
                    s = s.substring(1, s.length()-1);
                }
                return s;
            }

            @Override
            protected Void doInBackground() {
                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    String line;
                    boolean first = true;
                    while((line = br.readLine()) != null) {
                        if(line.trim().isEmpty()) continue;
                        String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                        for(int i=0;i<parts.length;i++) parts[i] = trimQuotes(parts[i]);

                        // detect header line
                        if(first) {
                            first = false;
                            String low = String.join("|", parts).toLowerCase();
                            if(low.contains("title") || low.contains("author") || low.contains("isbn")) {
                                // assume header, skip
                                continue;
                            }
                        }

                        // Expect at least 4 columns: title, author, isbn, year
                        if(parts.length < 4) { skipped++; failures.add("Too few columns: " + line); continue; }
                        String title = parts[0].trim();
                        String author = parts[1].trim();
                        String isbn = parts[2].trim();
                        String yearStr = parts[3].trim();
                        int year = 0;
                        try { year = Integer.parseInt(yearStr); } catch(Exception ex) { skipped++; failures.add("Invalid year for: " + title); continue; }

                        String genresCol = (parts.length >= 5) ? parts[4].trim() : "";
                        try {
                            int bookId = 0;
                            boolean matchedExisting = false;
                            boolean exactMatch = false;

                            // 1) If ISBN is present, try to find by ISBN first
                            if(isbn != null && !isbn.isEmpty()) {
                                try {
                                    java.util.List<Map<String,Object>> found = BookService.ReadBook().WhereIsbn(isbn).Read();
                                    if(found != null && !found.isEmpty()) {
                                        Object bidObj = found.get(0).getOrDefault("id", found.get(0).getOrDefault("book_id", "0"));
                                        try { bookId = Integer.parseInt(String.valueOf(bidObj)); } catch(Exception ignore) { bookId = 0; }
                                        if(bookId != 0) {
                                            // compare fields to determine if it's the same data
                                            String existingTitle = String.valueOf(found.get(0).getOrDefault("title", "")).trim();
                                            String existingAuthor = String.valueOf(found.get(0).getOrDefault("author", "")).trim();
                                            String existingYear = String.valueOf(found.get(0).getOrDefault("year_published", found.get(0).getOrDefault("year", ""))).trim();
                                            if(existingTitle.equalsIgnoreCase(title.trim()) && existingAuthor.equalsIgnoreCase(author.trim()) && existingYear.equals(String.valueOf(year))) {
                                                matchedExisting = true; // exact same data present
                                                exactMatch = true;
                                            } else {
                                                // found by ISBN but data differs; treat as existing to avoid duplicate inserts
                                                matchedExisting = true;
                                            }
                                        }
                                    }
                                } catch(Exception ex) {
                                    // ignore
                                }
                            }

                            // 2) If still not found, try match by title+author+year
                            if(bookId == 0) {
                                try {
                                    java.util.List<Map<String,Object>> byFields = BookService.ReadBook()
                                            .WhereTitle(title)
                                            .WhereAuthor(author)
                                            .WhereYearPublished(year)
                                            .Read();
                                    if(byFields != null && !byFields.isEmpty()) {
                                        Object bidObj = byFields.get(0).getOrDefault("id", byFields.get(0).getOrDefault("book_id", "0"));
                                        try { bookId = Integer.parseInt(String.valueOf(bidObj)); } catch(Exception ignore) { bookId = 0; }
                                        if(bookId != 0) { matchedExisting = true; exactMatch = true; }
                                    }
                                } catch(Exception ex) {
                                    // ignore
                                }
                            }

                            // 3) If still not found, insert the book
                            boolean inserted = false;
                            if(bookId == 0) {
                                try {
                                    inserted = BookService.InsertBook()
                                            .SetTitle(title)
                                            .SetAuthor(author)
                                            .SetIsbn(isbn)
                                            .SetYearPublished(year)
                                            .Insert();
                                } catch(Exception insEx) {
                                    inserted = false;
                                }

                                // try to locate the inserted book (prefer by ISBN if available)
                                if(isbn != null && !isbn.isEmpty()) {
                                    try {
                                        java.util.List<Map<String,Object>> found = BookService.ReadBook().WhereIsbn(isbn).Read();
                                        if(found != null && !found.isEmpty()) {
                                            Object bidObj = found.get(0).getOrDefault("id", found.get(0).getOrDefault("book_id", "0"));
                                            try { bookId = Integer.parseInt(String.valueOf(bidObj)); } catch(Exception ignore) { bookId = 0; }
                                        }
                                    } catch(Exception ignore) {}
                                }
                                if(bookId == 0) {
                                    try {
                                        java.util.List<Map<String,Object>> byFields = BookService.ReadBook()
                                                .WhereTitle(title)
                                                .WhereAuthor(author)
                                                .WhereYearPublished(year)
                                                .Read();
                                        if(byFields != null && !byFields.isEmpty()) {
                                            Object bidObj = byFields.get(0).getOrDefault("id", byFields.get(0).getOrDefault("book_id", "0"));
                                            try { bookId = Integer.parseInt(String.valueOf(bidObj)); } catch(Exception ignore) { bookId = 0; }
                                        }
                                    } catch(Exception ignore) {}
                                }

                                if(bookId == 0 && !inserted) {
                                    skipped++; failures.add("DB insert failed and could not locate book: " + title);
                                    continue;
                                }
                            }

                            // At this point bookId should be set (either found or inserted)
                            if(bookId == 0) {
                                skipped++; failures.add("Could not determine book id for: " + title);
                                continue;
                            }

                            // 4) Insert genres if provided, with dedup logic
                            if(genresCol != null && !genresCol.isEmpty()) {
                                Set<String> existingGenresNorm = new HashSet<>();
                                try {
                                    java.util.List<Map<String,Object>> eg = BookGenreService.ReadBookGenre()
                                            .WhereBookID(bookId)
                                            .Read();
                                    for(Map<String,Object> ge : eg) {
                                        Object gv = ge.getOrDefault("genre", "");
                                        String gn = String.valueOf(gv).trim().toLowerCase();
                                        if(!gn.isEmpty()) existingGenresNorm.add(gn);
                                    }
                                } catch(Exception ex) {
                                    // ignore failures fetching existing genres; we'll still attempt inserts
                                }

                                String[] genreParts = genresCol.split("[;,]");
                                Set<String> addedThisRow = new HashSet<>();
                                for(String g : genreParts) {
                                    String gm = g.trim();
                                    if(gm.isEmpty()) continue;
                                    String gmNorm = gm.toLowerCase();
                                    if(existingGenresNorm.contains(gmNorm) || addedThisRow.contains(gmNorm)) continue;
                                    try {
                                        boolean gok = BookGenreService.InsertBookGenre()
                                            .SetBookID(bookId)
                                            .SetGenre(gm)
                                            .Insert();
                                        if(!gok) {
                                            failures.add("Failed to insert genre '" + gm + "' for book: " + title);
                                        } else {
                                            existingGenresNorm.add(gmNorm);
                                            addedThisRow.add(gmNorm);
                                        }
                                    } catch(Exception ge) {
                                        failures.add("Error inserting genre '" + gm + "' for book: " + title + " -> " + ge.getMessage());
                                    }
                                }
                            }

                            // Count as duplicate if exactMatch (same data already present)
                            if(exactMatch) {
                                duplicatesSkipped++;
                            } else if(bookId != 0) {
                                success++;
                            } else {
                                skipped++;
                            }
                        } catch(Exception ex) {
                            skipped++; failures.add("Error inserting: " + title + " -> " + ex.getMessage());
                        }
                    }
                } catch(Exception ex) {
                    error = ex;
                }
                return null;
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                if(error != null) {
                    JOptionPane.showMessageDialog(BookTablePanel.this, "Import failed: " + error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                StringBuilder msg = new StringBuilder();
                msg.append("Imported: ").append(success).append("\n");
                msg.append("Skipped: ").append(skipped).append("\n");
                if(duplicatesSkipped > 0) msg.append("Duplicates skipped: ").append(duplicatesSkipped).append("\n");
                if(!failures.isEmpty()) {
                    msg.append("Failures:\n");
                    for(String f : failures) msg.append(" - ").append(f).append("\n");
                }
                JOptionPane.showMessageDialog(BookTablePanel.this, msg.toString(), "Import Summary", JOptionPane.INFORMATION_MESSAGE);
                loadBooks();
            }
        }.execute();
    }

    /**
     * Exports the currently displayed table data to a CSV file.
     * 
     * @param file The file to save the CSV data to.
     */
    private void exportTableToCSV(File file) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            // Write header
            StringBuilder header = new StringBuilder();
            for(int i = 0; i < tableModel.getColumnCount(); i++) {
                if(i > 0) header.append(",");
                header.append("\"").append(tableModel.getColumnName(i)).append("\"");
            }
            writer.println(header.toString());

            // Write data rows
            for(int row = 0; row < tableModel.getRowCount(); row++) {
                StringBuilder rowData = new StringBuilder();
                for(int col = 0; col < tableModel.getColumnCount(); col++) {
                    if(col > 0) rowData.append(",");
                    Object value = tableModel.getValueAt(row, col);
                    String strValue = value != null ? value.toString() : "";
                    // Escape quotes and wrap in quotes
                    strValue = strValue.replace("\"", "\"\"");
                    rowData.append("\"").append(strValue).append("\"");
                }
                writer.println(rowData.toString());
            }

            JOptionPane.showMessageDialog(this, "Books exported successfully to:\n" + file.getAbsolutePath(), "Export Complete", JOptionPane.INFORMATION_MESSAGE);
        } catch(IOException ex) {
            JOptionPane.showMessageDialog(this, "Failed to export: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}