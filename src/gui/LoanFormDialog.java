package gui;

import service.BookLoanService;
import service.BookService;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
// using JSpinner for dates; no SimpleDateFormat needed

/**
 * Dialog to Add or Update a book loan.
 * 
 * This modal dialog allows users to:
 * - Search for and select a book to loan
 * - Select a borrower
 * - Set loan dates (borrowed, due, returned)
 * 
 * It has two modes:
 * 1. ADD MODE: Creates a new loan (constructor with just onSuccess)
 * 2. UPDATE MODE: Modifies an existing loan (constructor with all loan details)
 * 
 * Key feature: Debounced search for books - as user types in the search field,
 * the book list updates after a 300ms delay (to avoid excessive database queries).
 *
 * @author AI
 */
public class LoanFormDialog extends JDialog {
    private boolean isUpdateForm;  // Flag to track whether we're adding or updating

    // ========== CONSTRUCTORS ==========
    
    /**
     * Constructor for ADD mode (creating a new loan).
     * 
     * @param owner The parent JFrame (used for dialogs and positioning)
     * @param onSuccess Callback to execute after successful save (typically refreshes the loan table)
     */
    public LoanFormDialog(JFrame owner, Runnable onSuccess) {
        super(owner, "Add Loan", true);  // "Add Loan" title, modal (true)
        init(null, onSuccess);           // null means no existing loan data (it's a new one)
    }

    /**
     * Constructor for UPDATE mode (modifying an existing loan).
     * 
     * @param owner The parent JFrame
     * @param loanId The ID of the loan record to update
     * @param bookId The currently loaned book ID
     * @param borrowerId The ID of the borrower
     * @param due The due date string
     * @param borrowedAt The borrow date string
     * @param returnedAt The return date string (null if not yet returned)
     * @param onSuccess Callback after successful save
     */
    public LoanFormDialog(JFrame owner, int loanId, int bookId, int borrowerId, String due, 
                         String borrowedAt, String returnedAt, Runnable onSuccess) {
        super(owner, "Update Loan", true);  // "Update Loan" title, modal
        this.isUpdateForm = true;           // Mark as update mode
        init(new LoanData(loanId, bookId, borrowerId, due, borrowedAt, returnedAt), onSuccess);
    }

    /**
     * Initialize the dialog UI: build the form with all fields and buttons.
     * 
     * @param data Loan data if updating, or null if adding new
     * @param onSuccess Callback to run after successful save
     */
    private void init(LoanData data, Runnable onSuccess) {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);  // Close button disposes this dialog
        setResizable(false);                                  // User can't resize window

        // Main form panel using GridBagLayout (gives fine-grained control over position)
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6,6,6,6);   // 6px padding between components
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // ========== BOOK SELECTION SECTION ==========
        
        JLabel bookLabel = new JLabel("Book:");
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; gbc.weightx = 1.0; 
        gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL;
        form.add(bookLabel, gbc);

        /**
         * BOOK SEARCH FIELD:
         * User types a book title here. As they type, the list below updates
         * (with 300ms debounce delay to avoid excessive database queries).
         */
        JLabel bookSearchLabel = new JLabel("Search by Title:");
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1; gbc.weightx = 0.0; 
        gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE;
        form.add(bookSearchLabel, gbc);

        JTextField searchField = new JTextField(30);
        searchField.setToolTipText("Search by title...");
        gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 1; gbc.weightx = 1.0; 
        gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL;
        form.add(searchField, gbc);

        /**
         * BOOK LIST:
         * Displays books matching the search (or all books if search is empty).
         * User clicks to select which book to loan.
         * 
         * DefaultListModel is like a simple array/list for the JList to display.
         * JList is the display component that shows the model's contents.
         */
        DefaultListModel<BookItem> listModel = new DefaultListModel<>();
        JList<BookItem> resultsList = new JList<>(listModel);
        resultsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);  // Can only select one book
        JScrollPane listScroll = new JScrollPane(resultsList);
        listScroll.setPreferredSize(new Dimension(400, 80));  // Size of list display
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.weightx = 1.0; 
        gbc.weighty = 0.1; gbc.anchor = GridBagConstraints.CENTER; gbc.fill = GridBagConstraints.BOTH;
        form.add(listScroll, gbc);

        /**
         * SELECTED BOOK DISPLAY:
         * Shows the title of the currently selected book (read-only).
         * This gives user visual feedback of which book they selected.
         */
        JTextArea selectedTitle = new JTextArea(1, 40);
        selectedTitle.setLineWrap(true);         // Wrap long titles to multiple lines
        selectedTitle.setWrapStyleWord(true);    // Wrap at word boundaries, not mid-word
        selectedTitle.setEditable(false);        // User can't type here
        selectedTitle.setText("(no book selected)");  // Default text
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.weightx = 1.0; 
        gbc.weighty = 0.0; gbc.anchor = GridBagConstraints.CENTER; gbc.fill = GridBagConstraints.BOTH;
        form.add(new JScrollPane(selectedTitle), gbc);

        /**
         * LOAD BOOKS IN BACKGROUND:
         * When the dialog opens, load all books from database in background thread.
         * This doesn't freeze the UI while we wait for database results.
         * 
         * SwingWorker pattern:
         * - doInBackground: runs on background thread, queries database
         * - done: runs on UI thread, populates the list with results
         */
        Runnable doLoadAll = () -> new javax.swing.SwingWorker<java.util.List<BookItem>, Void>() {
            Exception error = null;
            
            @Override 
            protected java.util.List<BookItem> doInBackground() {
                try {
                    java.util.List<BookItem> out = new java.util.ArrayList<>();
                    // Query database for all books
                    java.util.List<java.util.Map<String, Object>> rows = BookService.ReadBook().Read();
                    
                    // Convert each database record to a BookItem (id + title)
                    for(java.util.Map<String, Object> r : rows) {
                        Object id = r.getOrDefault("id", r.getOrDefault("book_id", ""));
                        Object title = r.getOrDefault("title", "");
                        try { 
                            int iid = Integer.parseInt(String.valueOf(id)); 
                            out.add(new BookItem(iid, String.valueOf(title))); 
                        } catch(Exception ignore) { }
                    }
                    return out;
                } catch(Exception ex) { 
                    error = ex; 
                    return java.util.Collections.emptyList(); 
                }
            }
            
            @Override 
            protected void done() {
                try {
                    // Get results from background thread
                    java.util.List<BookItem> items = get();
                    listModel.clear();  // Clear any old items
                    
                    // Add all books to the list
                    for(BookItem it : items) {
                        listModel.addElement(it);
                    }
                    
                    // If updating an existing loan, pre-select the current book
                    if(data != null) {
                        for(int i=0; i<listModel.getSize(); i++) {
                            if(listModel.get(i).id == data.bookId) {
                                resultsList.setSelectedIndex(i);           // Highlight it
                                selectedTitle.setText(listModel.get(i).title);  // Show title
                                break;
                            }
                        }
                    }
                    
                    if(error != null) throw error;
                } catch(Exception ex) {
                    JOptionPane.showMessageDialog(LoanFormDialog.this, 
                        "Failed to load books: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();

        doLoadAll.run();  // Start loading books

        // debounce search
        final javax.swing.Timer searchTimer = new javax.swing.Timer(300, ev -> {
            String q = searchField.getText().trim();
            new javax.swing.SwingWorker<java.util.List<BookItem>, Void>() {
                Exception error = null;
                @Override protected java.util.List<BookItem> doInBackground() {
                    try {
                        java.util.List<BookItem> out = new java.util.ArrayList<>();
                        if(q.isEmpty()) {
                            java.util.List<java.util.Map<String, Object>> rows = BookService.ReadBook().Read();
                            for(java.util.Map<String, Object> r : rows) {
                                Object id = r.getOrDefault("id", r.getOrDefault("book_id", ""));
                                Object title = r.getOrDefault("title", "");
                                try { int iid = Integer.parseInt(String.valueOf(id)); out.add(new BookItem(iid, String.valueOf(title))); } catch(Exception ignore) { }
                            }
                        } else {
                            java.util.List<java.util.Map<String, Object>> rows = BookService.ReadBook().WhereTitle(q).Read();
                            for(java.util.Map<String, Object> r : rows) {
                                Object id = r.getOrDefault("id", r.getOrDefault("book_id", ""));
                                Object title = r.getOrDefault("title", "");
                                try { int iid = Integer.parseInt(String.valueOf(id)); out.add(new BookItem(iid, String.valueOf(title))); } catch(Exception ignore) { }
                            }
                        }
                        return out;
                    } catch(Exception ex) { error = ex; return java.util.Collections.emptyList(); }
                }
                @Override protected void done() {
                    try {
                        java.util.List<BookItem> items = get(); listModel.clear(); for(BookItem it : items) listModel.addElement(it);
                        if(error != null) throw error;
                    } catch(Exception ex) { /* ignore search errors for now */ }
                }
            }.execute();
        });
        searchTimer.setRepeats(false);
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void restart() { if(searchTimer.isRunning()) searchTimer.restart(); else searchTimer.start(); }
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { restart(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { restart(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { restart(); }
        });

        resultsList.addListSelectionListener(ev -> {
            if(!ev.getValueIsAdjusting()) { BookItem it = resultsList.getSelectedValue(); if(it != null) selectedTitle.setText(it.title); }
        });

        // ============ BORROWER SECTION ============
        JLabel borrowerLabel = new JLabel("Borrower:");
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2; gbc.weightx = 1.0; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL;
        form.add(borrowerLabel, gbc);

        // Borrower search field with label
        JLabel borrowerSearchLabel = new JLabel("Search by Name:");
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 1; gbc.weightx = 0.0; gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE;
        form.add(borrowerSearchLabel, gbc);

        JTextField borrowerSearchField = new JTextField(30);
        borrowerSearchField.setToolTipText("Search by name...");
        gbc.gridx = 1; gbc.gridy = 5; gbc.gridwidth = 1; gbc.weightx = 1.0; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL;
        form.add(borrowerSearchField, gbc);

        DefaultListModel<BorrowerItem> borrowerListModel = new DefaultListModel<>();
        JList<BorrowerItem> borrowerResultsList = new JList<>(borrowerListModel);
        borrowerResultsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane borrowerScroll = new JScrollPane(borrowerResultsList);
        borrowerScroll.setPreferredSize(new Dimension(400, 80));
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2; gbc.weightx = 1.0; gbc.weighty = 0.1; gbc.anchor = GridBagConstraints.CENTER; gbc.fill = GridBagConstraints.BOTH;
        form.add(borrowerScroll, gbc);

        JTextArea selectedBorrower = new JTextArea(1, 40);
        selectedBorrower.setLineWrap(true); selectedBorrower.setWrapStyleWord(true); selectedBorrower.setEditable(false); selectedBorrower.setText("(no borrower selected)");
        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 2; gbc.weightx = 1.0; gbc.weighty = 0.0; gbc.anchor = GridBagConstraints.CENTER; gbc.fill = GridBagConstraints.BOTH;
        form.add(new JScrollPane(selectedBorrower), gbc);

        // Button panel for create borrower (separate row)
        JButton createBorrowerBtn = new JButton("+ Create New Borrower");
        gbc.gridx = 0; gbc.gridy = 8; gbc.gridwidth = 2; gbc.weightx = 1.0; gbc.weighty = 0.0; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.EAST;
        JPanel borrowerBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        borrowerBtnPanel.add(createBorrowerBtn);
        form.add(borrowerBtnPanel, gbc);

        // Load borrowers in background
        Runnable doLoadBorrowers = () -> new javax.swing.SwingWorker<java.util.List<BorrowerItem>, Void>() {
            Exception error = null;
            @Override protected java.util.List<BorrowerItem> doInBackground() {
                try {
                    java.util.List<BorrowerItem> out = new java.util.ArrayList<>();
                    java.util.List<java.util.Map<String, Object>> rows = service.BorrowerService.ReadBorrower().Read();
                    for(java.util.Map<String, Object> r : rows) {
                        Object id = r.getOrDefault("id", "");
                        Object fn = r.getOrDefault("first_name", "");
                        Object ln = r.getOrDefault("last_name", "");
                        try { int iid = Integer.parseInt(String.valueOf(id)); out.add(new BorrowerItem(iid, String.valueOf(fn), String.valueOf(ln))); } catch(Exception ignore) { }
                    }
                    return out;
                } catch(Exception ex) { error = ex; return java.util.Collections.emptyList(); }
            }
            @Override protected void done() {
                try {
                    java.util.List<BorrowerItem> items = get();
                    borrowerListModel.clear();
                    for(BorrowerItem it : items) borrowerListModel.addElement(it);
                    if(data != null && data.borrowerId > 0) {
                        // preselect borrower if editing
                        for(int i=0;i<borrowerListModel.getSize();i++) if(borrowerListModel.get(i).id == data.borrowerId) { borrowerResultsList.setSelectedIndex(i); selectedBorrower.setText(borrowerListModel.get(i).firstName + " " + borrowerListModel.get(i).lastName); break; }
                    }
                    if(error != null) throw error;
                } catch(Exception ex) {
                    JOptionPane.showMessageDialog(LoanFormDialog.this, "Failed to load borrowers: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();

        doLoadBorrowers.run();

        // Debounce borrower search
        final javax.swing.Timer borrowerSearchTimer = new javax.swing.Timer(300, ev -> {
            String q = borrowerSearchField.getText().trim();
            new javax.swing.SwingWorker<java.util.List<BorrowerItem>, Void>() {
                Exception error = null;
                @Override protected java.util.List<BorrowerItem> doInBackground() {
                    try {
                        java.util.List<BorrowerItem> out = new java.util.ArrayList<>();
                        if(q.isEmpty()) {
                            java.util.List<java.util.Map<String, Object>> rows = service.BorrowerService.ReadBorrower().Read();
                            for(java.util.Map<String, Object> r : rows) {
                                Object id = r.getOrDefault("id", "");
                                Object fn = r.getOrDefault("first_name", "");
                                Object ln = r.getOrDefault("last_name", "");
                                try { int iid = Integer.parseInt(String.valueOf(id)); out.add(new BorrowerItem(iid, String.valueOf(fn), String.valueOf(ln))); } catch(Exception ignore) { }
                            }
                        } else {
                            java.util.List<java.util.Map<String, Object>> rows = service.BorrowerService.ReadBorrower().WhereLastName(q).Read();
                            for(java.util.Map<String, Object> r : rows) {
                                Object id = r.getOrDefault("id", "");
                                Object fn = r.getOrDefault("first_name", "");
                                Object ln = r.getOrDefault("last_name", "");
                                try { int iid = Integer.parseInt(String.valueOf(id)); out.add(new BorrowerItem(iid, String.valueOf(fn), String.valueOf(ln))); } catch(Exception ignore) { }
                            }
                        }
                        return out;
                    } catch(Exception ex) { error = ex; return java.util.Collections.emptyList(); }
                }
                @Override protected void done() {
                    try {
                        java.util.List<BorrowerItem> items = get(); borrowerListModel.clear(); for(BorrowerItem it : items) borrowerListModel.addElement(it);
                        if(error != null) throw error;
                    } catch(Exception ex) { /* ignore search errors for now */ }
                }
            }.execute();
        });
        borrowerSearchTimer.setRepeats(false);
        borrowerSearchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void restart() { if(borrowerSearchTimer.isRunning()) borrowerSearchTimer.restart(); else borrowerSearchTimer.start(); }
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { restart(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { restart(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { restart(); }
        });

        borrowerResultsList.addListSelectionListener(ev -> {
            if(!ev.getValueIsAdjusting()) { BorrowerItem it = borrowerResultsList.getSelectedValue(); if(it != null) selectedBorrower.setText(it.firstName + " " + it.lastName); }
        });

        // Create new borrower button action
        createBorrowerBtn.addActionListener(ae -> {
            // Show inline form for creating new borrower
            JDialog createDialog = new JDialog(this, "Create New Borrower", true);
            createDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            
            JPanel formPanel = new JPanel(new GridBagLayout());
            GridBagConstraints fbgc = new GridBagConstraints();
            fbgc.insets = new Insets(6,6,6,6);
            fbgc.fill = GridBagConstraints.HORIZONTAL;
            fbgc.anchor = GridBagConstraints.WEST;
            
            // First Name
            fbgc.gridx = 0; fbgc.gridy = 0; fbgc.weightx = 0.0;
            formPanel.add(new JLabel("First Name:"), fbgc);
            JTextField firstNameField = new JTextField(20);
            fbgc.gridx = 1; fbgc.weightx = 1.0;
            formPanel.add(firstNameField, fbgc);
            
            // Middle Name
            fbgc.gridx = 0; fbgc.gridy = 1; fbgc.weightx = 0.0;
            formPanel.add(new JLabel("Middle Name:"), fbgc);
            JTextField middleNameField = new JTextField(20);
            fbgc.gridx = 1; fbgc.weightx = 1.0;
            formPanel.add(middleNameField, fbgc);
            
            // Last Name
            fbgc.gridx = 0; fbgc.gridy = 2; fbgc.weightx = 0.0;
            formPanel.add(new JLabel("Last Name:"), fbgc);
            JTextField lastNameField = new JTextField(20);
            fbgc.gridx = 1; fbgc.weightx = 1.0;
            formPanel.add(lastNameField, fbgc);
            
            // Contact Number
            fbgc.gridx = 0; fbgc.gridy = 3; fbgc.weightx = 0.0;
            formPanel.add(new JLabel("Contact Number:"), fbgc);
            JTextField contactNumField = new JTextField(20);
            fbgc.gridx = 1; fbgc.weightx = 1.0;
            formPanel.add(contactNumField, fbgc);
            
            // Buttons
            fbgc.gridx = 0; fbgc.gridy = 4; fbgc.gridwidth = 2; fbgc.weightx = 1.0; fbgc.fill = GridBagConstraints.NONE; fbgc.anchor = GridBagConstraints.CENTER;
            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
            JButton saveBorBtn = new JButton("Save");
            JButton cancelBorBtn = new JButton("Cancel");
            btnPanel.add(saveBorBtn); btnPanel.add(cancelBorBtn);
            formPanel.add(btnPanel, fbgc);
            
            saveBorBtn.addActionListener(savEv -> {
                String fn = firstNameField.getText().trim();
                String mn = middleNameField.getText().trim();
                String ln = lastNameField.getText().trim();
                String cn = contactNumField.getText().trim();
                
                if(fn.isEmpty() || ln.isEmpty() || cn.isEmpty()) {
                    JOptionPane.showMessageDialog(createDialog, "First Name, Last Name, and Contact Number are required.", "Validation", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                
                try {
                    service.BorrowerService borrowerSvc = new service.BorrowerService();
                    boolean ok = borrowerSvc.InsertBorrower()
                        .SetFirstName(fn)
                        .SetMiddleName(mn.isEmpty() ? "" : mn)
                        .SetLastName(ln)
                        .SetContactNum(cn)
                        .Insert();
                    
                    if(ok) {
                        JOptionPane.showMessageDialog(createDialog, "Borrower created successfully. Refreshing list...", "Success", JOptionPane.INFORMATION_MESSAGE);
                        createDialog.dispose();
                        doLoadBorrowers.run(); // refresh borrower list
                    } else {
                        JOptionPane.showMessageDialog(createDialog, "Failed to create borrower.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch(Exception ex) {
                    JOptionPane.showMessageDialog(createDialog, "Error creating borrower: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
            
            cancelBorBtn.addActionListener(cancelEv -> createDialog.dispose());
            
            createDialog.setContentPane(formPanel);
            createDialog.pack();
            createDialog.setLocationRelativeTo(LoanFormDialog.this);
            createDialog.setVisible(true);
        });

        // ============ DATES SECTION ============
        JLabel dueLabel = new JLabel("Due Date:");
        gbc.gridx = 0; gbc.gridy = 9; gbc.gridwidth = 1; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE;
        form.add(dueLabel, gbc);

        // use JSpinner date editor to avoid external libs; format yyyy-MM-dd
        SpinnerDateModel dueModel = new SpinnerDateModel(java.sql.Date.valueOf(java.time.LocalDate.now().plusWeeks(2)), null, null, java.util.Calendar.DAY_OF_MONTH);
        JSpinner dueSpinner = new JSpinner(dueModel);
        JSpinner.DateEditor dueEditor = new JSpinner.DateEditor(dueSpinner, "yyyy-MM-dd"); dueSpinner.setEditor(dueEditor);
        gbc.gridx = 1; gbc.gridy = 9; gbc.gridwidth = 1; gbc.weightx = 1.0; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL; form.add(dueSpinner, gbc);

        JLabel borrowedLabel = new JLabel("Borrowed At:");
        gbc.gridx = 0; gbc.gridy = 10; gbc.gridwidth = 1; gbc.weightx = 0.0; gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE; form.add(borrowedLabel, gbc);
        SpinnerDateModel borrowedModel = new SpinnerDateModel(java.sql.Date.valueOf(java.time.LocalDate.now()), null, null, java.util.Calendar.DAY_OF_MONTH);
        JSpinner borrowedSpinner = new JSpinner(borrowedModel);
        JSpinner.DateEditor borrowedEditor = new JSpinner.DateEditor(borrowedSpinner, "yyyy-MM-dd"); borrowedSpinner.setEditor(borrowedEditor);
        gbc.gridx = 1; gbc.gridy = 10; gbc.gridwidth = 1; gbc.weightx = 1.0; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL; form.add(borrowedSpinner, gbc);
        
        // Flag to enable auto-adjust after prefill is complete
        final boolean[] enableAutoAdjust = new boolean[] { false };

        JLabel returnedLabel = new JLabel("Returned At:");
        gbc.gridx = 0; gbc.gridy = 11; gbc.gridwidth = 1; gbc.weightx = 0.0; gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE;
        if(this.isUpdateForm) form.add(returnedLabel, gbc);

        // Spinner must have a non-null initial value; we keep the spinner disabled by default
        SpinnerDateModel returnedModel = new SpinnerDateModel(java.sql.Date.valueOf(java.time.LocalDate.now()), null, null, java.util.Calendar.DAY_OF_MONTH);
        JSpinner returnedSpinner = new JSpinner(returnedModel);
        JSpinner.DateEditor returnedEditor = new JSpinner.DateEditor(returnedSpinner, "yyyy-MM-dd"); returnedSpinner.setEditor(returnedEditor);
        returnedSpinner.setEnabled(false);

        JCheckBox returnedCheck = new JCheckBox("Returned");
        returnedCheck.addActionListener(ev -> returnedSpinner.setEnabled(returnedCheck.isSelected()));

        JPanel returnedWrap = new JPanel(new BorderLayout(6,6));
        if(this.isUpdateForm) {
            returnedWrap.add(returnedSpinner, BorderLayout.CENTER);
            returnedWrap.add(returnedCheck, BorderLayout.EAST);

            gbc.gridx = 1; gbc.gridy = 11; gbc.gridwidth = 1; gbc.weightx = 1.0; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL; form.add(returnedWrap, gbc);
        }

        // prefill spinners if editing existing loan
        final boolean[] alreadyReturned = new boolean[] { false };
        if(data != null) {
            try {
                java.sql.Date d = parseSqlDate(data.due);
                if(d != null) dueSpinner.setValue(d);
            } catch(Exception ignore) {}
            try {
                java.sql.Date b = parseSqlDate(data.borrowedAt);
                if(b != null) borrowedSpinner.setValue(b);
            } catch(Exception ignore) {}
            try {
                java.sql.Date r = parseSqlDate(data.returnedAt);
                if(r != null) {
                    returnedSpinner.setValue(r);
                    // mark as already returned and prevent modification
                    returnedCheck.setSelected(true);
                    returnedCheck.setEnabled(false);
                    returnedSpinner.setEnabled(false);
                    alreadyReturned[0] = true;
                }
            } catch(Exception ignore) {}
        }

        // Now enable auto-adjust after prefill is complete
        enableAutoAdjust[0] = true;
        
        // Listener to auto-adjust due date if borrowed date is changed to be after due date
        borrowedModel.addChangeListener(ev -> {
            if(!enableAutoAdjust[0]) return; // skip during prefill
            java.util.Date borrowedVal = (java.util.Date) borrowedModel.getValue();
            java.util.Date dueVal = (java.util.Date) dueModel.getValue();
            if(borrowedVal != null && dueVal != null && borrowedVal.after(dueVal)) {
                // Auto-adjust due date to be 14 days after borrowed date
                java.time.LocalDate bLocal = new java.sql.Date(borrowedVal.getTime()).toLocalDate();
                java.sql.Date newDue = java.sql.Date.valueOf(bLocal.plusDays(14));
                dueModel.setValue(newDue);
            }
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton save = new JButton("Save");
        JButton cancel = new JButton("Cancel");

        save.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                if(alreadyReturned[0]) { JOptionPane.showMessageDialog(LoanFormDialog.this, "This loan has already been returned and cannot be modified.", "Immutable", JOptionPane.INFORMATION_MESSAGE); return; }

                BookItem selected = resultsList.getSelectedValue();
                if(selected == null) { JOptionPane.showMessageDialog(LoanFormDialog.this, "Please select a book.", "Validation", JOptionPane.WARNING_MESSAGE); return; }

                BorrowerItem borrowerSelected = borrowerResultsList.getSelectedValue();
                if(borrowerSelected == null) { JOptionPane.showMessageDialog(LoanFormDialog.this, "Please select a borrower.", "Validation", JOptionPane.WARNING_MESSAGE); return; }

                int bookId = selected.id;
                int borrowerId = borrowerSelected.id;
                java.util.Date dueDate = (java.util.Date) dueSpinner.getValue();
                java.util.Date borrowedDate = (java.util.Date) borrowedSpinner.getValue();
                java.util.Date returnedDate = null;
                // Only take returned date if the user explicitly enabled/checked it and it's editable.
                boolean returnedEditable = returnedCheck.isEnabled() && returnedCheck.isSelected();
                if(returnedEditable) {
                    try { returnedDate = (java.util.Date) returnedSpinner.getValue(); } catch(Exception ignore) { returnedDate = null; }
                } else {
                    // if this was an existing returned value (existingReturned), don't attempt to change it on update
                    returnedDate = null;
                }

                // validation: due date must be present
                if(dueDate == null) { JOptionPane.showMessageDialog(LoanFormDialog.this, "Please provide a due date.", "Validation", JOptionPane.WARNING_MESSAGE); return; }
                
                // validation: due date must not be less than borrowed date
                if(borrowedDate != null && dueDate.before(borrowedDate)) {
                    JOptionPane.showMessageDialog(LoanFormDialog.this, "Due date cannot be before the borrowed date.", "Validation", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                try {
                    boolean ok;
                    BookLoanService svc = new BookLoanService();
                    if(data == null) {
                        // If borrowed date is in the future, it will be treated as a reservation by the UI
                        ok = svc.InsertBookLoan().SetBookID(bookId).SetBorrowerID(borrowerId).SetBorrowedAt(borrowedDate).SetDueDate(dueDate).Insert();
                    } else {
                        var builder = svc.UpdateBookLoan();
                        if(dueDate != null) builder.SetDueDate(dueDate);
                        if(borrowedDate != null) builder.SetBorrowedAt(borrowedDate);
                        if(returnedDate != null) builder.SetReturnedAt(returnedDate);
                        builder.WhereID(data.loanId);
                        ok = builder.Update();
                    }

                    if(ok) {
                        // update book availability:
                        try {
                            if(data == null) {
                                // Insert: if borrowed date is today or earlier, mark book unavailable
                                if(borrowedDate != null) {
                                    java.time.LocalDate b = new java.sql.Date(borrowedDate.getTime()).toLocalDate();
                                    if(!b.isAfter(java.time.LocalDate.now())) {
                                        try { BookService.UpdateBook().SetIsAvailable(false).WhereBookID(bookId).Update(); } catch(Exception ignore) {}
                                    }
                                }
                            } else {
                                // Update: if we just set a returned date, mark book available
                                boolean didReturnNow = (returnedCheck.isEnabled() && returnedCheck.isSelected() && returnedDate != null);
                                if(didReturnNow) {
                                    try { BookService.UpdateBook().SetIsAvailable(true).WhereBookID(bookId).Update(); } catch(Exception ignore) {}
                                }
                            }
                        } catch(Exception ignore) {}

                        JOptionPane.showMessageDialog(LoanFormDialog.this, "Saved.", "Success", JOptionPane.INFORMATION_MESSAGE);
                        dispose(); if(onSuccess != null) onSuccess.run();
                    } else {
                        JOptionPane.showMessageDialog(LoanFormDialog.this, "Failed to save.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch(Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(LoanFormDialog.this, "Error: " + ex.getMessage(), "Exception", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        cancel.addActionListener(ae -> dispose());

        buttons.add(cancel); buttons.add(save);
        gbc.gridx = 0; gbc.gridy = 12; gbc.gridwidth = 2; gbc.weightx = 1.0; gbc.weighty = 0.0; gbc.anchor = GridBagConstraints.CENTER; gbc.fill = GridBagConstraints.NONE; form.add(buttons, gbc);

        // If this loan has already been returned, make the whole form readonly and disable saving
        if(alreadyReturned[0]) {
            save.setEnabled(false);
            resultsList.setEnabled(false);
            searchField.setEnabled(false);
            dueSpinner.setEnabled(false);
            borrowedSpinner.setEnabled(false);
        }

        getContentPane().add(form);
        pack(); setLocationRelativeTo(getOwner());
    }

    private static class LoanData {
        int loanId; int bookId; int borrowerId; String due, borrowedAt, returnedAt;
        LoanData(int loanId, int bookId, int borrowerId, String due, String borrowedAt, String returnedAt) { this.loanId = loanId; this.bookId = bookId; this.borrowerId = borrowerId; this.due = due; this.borrowedAt = borrowedAt; this.returnedAt = returnedAt; }
    }

    // Try parsing ISO date, ISO datetime, or a date-prefix (first 10 chars). Returns null if unparseable.
    private static java.sql.Date parseSqlDate(String s) {
        if(s == null) return null; s = s.trim(); if(s.isEmpty()) return null;
        try {
            java.time.LocalDate ld = java.time.LocalDate.parse(s);
            return java.sql.Date.valueOf(ld);
        } catch(Exception ignore) {}
        try {
            java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(s);
            return java.sql.Date.valueOf(ldt.toLocalDate());
        } catch(Exception ignore) {}
        try {
            if(s.length() >= 10) {
                String p = s.substring(0,10);
                java.time.LocalDate ld = java.time.LocalDate.parse(p);
                return java.sql.Date.valueOf(ld);
            }
        } catch(Exception ignore) {}
        return null;
    }

    private static class BookItem {
        final int id;
        final String title;
        BookItem(int id, String title) { this.id = id; this.title = title == null ? "" : title; }
        @Override public String toString() { return id + " - " + title; }
    }

    private static class BorrowerItem {
        final int id;
        final String firstName;
        final String lastName;
        BorrowerItem(int id, String firstName, String lastName) { this.id = id; this.firstName = firstName == null ? "" : firstName; this.lastName = lastName == null ? "" : lastName; }
        @Override public String toString() { return id + " - " + firstName + " " + lastName; }
    }
}
