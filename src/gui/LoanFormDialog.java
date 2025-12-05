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
 * @author AI
 */
public class LoanFormDialog extends JDialog {
    private boolean isUpdateForm;

    // date formatting handled by JSpinner editors

    // Add mode
    public LoanFormDialog(JFrame owner, Runnable onSuccess) {
        super(owner, "Add Loan", true);
        init(null, onSuccess);
    }

    // Update mode (loanId is the loan record id)
    public LoanFormDialog(JFrame owner, int loanId, int bookId, String due, String borrowedAt, String returnedAt, Runnable onSuccess) {
        super(owner, "Update Loan", true);
        this.isUpdateForm = true;
        init(new LoanData(loanId, bookId, due, borrowedAt, returnedAt), onSuccess);
    }

    private void init(LoanData data, Runnable onSuccess) {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setResizable(false);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6,6,6,6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel bookLabel = new JLabel("Book:");
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0;
        form.add(bookLabel, gbc);

        // Search field + list to pick a book by title (shows larger title area)
        JPanel picker = new JPanel(new BorderLayout(6,6));
        JTextField searchField = new JTextField();
        searchField.setToolTipText("Search by title...");
        picker.add(searchField, BorderLayout.NORTH);

        DefaultListModel<BookItem> listModel = new DefaultListModel<>();
        JList<BookItem> resultsList = new JList<>(listModel);
        resultsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane listScroll = new JScrollPane(resultsList);
        listScroll.setPreferredSize(new Dimension(400, 120));
        picker.add(listScroll, BorderLayout.CENTER);

        // selected title area
        JTextArea selectedTitle = new JTextArea(2, 40);
        selectedTitle.setLineWrap(true); selectedTitle.setWrapStyleWord(true); selectedTitle.setEditable(false);
        picker.add(new JScrollPane(selectedTitle), BorderLayout.SOUTH);

        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        form.add(picker, gbc);

        // load initial list of books in background (no block)
        Runnable doLoadAll = () -> new javax.swing.SwingWorker<java.util.List<BookItem>, Void>() {
            Exception error = null;
            @Override protected java.util.List<BookItem> doInBackground() {
                try {
                    java.util.List<BookItem> out = new java.util.ArrayList<>();
                    java.util.List<java.util.Map<String, Object>> rows = BookService.ReadBook().Read();
                    for(java.util.Map<String, Object> r : rows) {
                        Object id = r.getOrDefault("id", r.getOrDefault("book_id", ""));
                        Object title = r.getOrDefault("title", "");
                        try { int iid = Integer.parseInt(String.valueOf(id)); out.add(new BookItem(iid, String.valueOf(title))); } catch(Exception ignore) { }
                    }
                    return out;
                } catch(Exception ex) { error = ex; return java.util.Collections.emptyList(); }
            }
            @Override protected void done() {
                try {
                    java.util.List<BookItem> items = get();
                    listModel.clear();
                    for(BookItem it : items) listModel.addElement(it);
                    if(data != null) {
                        // preselect
                        for(int i=0;i<listModel.getSize();i++) if(listModel.get(i).id == data.bookId) { resultsList.setSelectedIndex(i); selectedTitle.setText(listModel.get(i).title); break; }
                    }
                    if(error != null) throw error;
                } catch(Exception ex) {
                    JOptionPane.showMessageDialog(LoanFormDialog.this, "Failed to load books: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();

        doLoadAll.run();

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

        JLabel dueLabel = new JLabel("Due Date:");
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
        form.add(dueLabel, gbc);

        // use JSpinner date editor to avoid external libs; format yyyy-MM-dd
        SpinnerDateModel dueModel = new SpinnerDateModel(java.sql.Date.valueOf(java.time.LocalDate.now().plusWeeks(2)), null, null, java.util.Calendar.DAY_OF_MONTH);
        JSpinner dueSpinner = new JSpinner(dueModel);
        JSpinner.DateEditor dueEditor = new JSpinner.DateEditor(dueSpinner, "yyyy-MM-dd"); dueSpinner.setEditor(dueEditor);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0; form.add(dueSpinner, gbc);

        JLabel borrowedLabel = new JLabel("Borrowed At:");
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0; form.add(borrowedLabel, gbc);
        SpinnerDateModel borrowedModel = new SpinnerDateModel(java.sql.Date.valueOf(java.time.LocalDate.now()), null, null, java.util.Calendar.DAY_OF_MONTH);
        JSpinner borrowedSpinner = new JSpinner(borrowedModel);
        JSpinner.DateEditor borrowedEditor = new JSpinner.DateEditor(borrowedSpinner, "yyyy-MM-dd"); borrowedSpinner.setEditor(borrowedEditor);
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1.0; form.add(borrowedSpinner, gbc);

        JLabel returnedLabel = new JLabel("Returned At:");
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.0; 
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

            gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 1.0; form.add(returnedWrap, gbc);
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

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton save = new JButton("Save");
        JButton cancel = new JButton("Cancel");

        save.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                if(alreadyReturned[0]) { JOptionPane.showMessageDialog(LoanFormDialog.this, "This loan has already been returned and cannot be modified.", "Immutable", JOptionPane.INFORMATION_MESSAGE); return; }

                BookItem selected = resultsList.getSelectedValue();
                if(selected == null) { JOptionPane.showMessageDialog(LoanFormDialog.this, "Please select a book.", "Validation", JOptionPane.WARNING_MESSAGE); return; }

                int bookId = selected.id;
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

                try {
                    boolean ok;
                    BookLoanService svc = new BookLoanService();
                    if(data == null) {
                        // If borrowed date is in the future, it will be treated as a reservation by the UI
                        ok = svc.InsertBookLoan().SetBookID(bookId).SetDueDate(dueDate).SetBorrowedAt(borrowedDate).Insert();
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
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2; gbc.weightx = 1.0; form.add(buttons, gbc);

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
        int loanId; int bookId; String due, borrowedAt, returnedAt;
        LoanData(int loanId, int bookId, String due, String borrowedAt, String returnedAt) { this.loanId = loanId; this.bookId = bookId; this.due = due; this.borrowedAt = borrowedAt; this.returnedAt = returnedAt; }
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
}
