package gui;

import service.BookLoanService;
import service.BookService;
import service.BorrowerService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.Map;

/**
 * Panel for listing and managing book loans (Add, Search, Update, Delete).
 *
 * @author AI
 */
public class LoanPanel extends JPanel {
    private JTable allTable;
    private DefaultTableModel allModel;
    private JTable overdueTable;
    private DefaultTableModel overdueModel;
    private JTable reservationsTable;
    private DefaultTableModel reservationsModel;
    private JTable returnedTable;
    private DefaultTableModel returnedModel;
    private JFrame owner;
    private BookLoanService loanService;

    // cache for book titles keyed by book id to avoid repeated DB queries
    private Map<Integer,String> bookTitleCache = new HashMap<>();
    // cache for borrower full names keyed by borrower id
    private Map<Integer,String> borrowerNameCache = new HashMap<>();

    public LoanPanel(JFrame owner) {
        super(new BorderLayout());
        this.owner = owner;
        this.loanService = new BookLoanService();

        JPanel top = new JPanel(new BorderLayout());
        JLabel header = new JLabel("Book Loans");
        header.setFont(header.getFont().deriveFont(Font.BOLD, 16f));
        header.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        top.add(header, BorderLayout.WEST);

        JButton addBtn = new JButton("Add Loan");
        addBtn.addActionListener(e -> {
            LoanFormDialog d = new LoanFormDialog(owner, new Runnable() { @Override public void run() { loadLoans(); } });
            d.setVisible(true);
        });
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        right.add(addBtn);
        top.add(right, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);

        String[] cols = new String[] {"ID", "BookID", "Borrower", "BorrowedAt", "DueDate", "ReturnedAt"};
        allModel = new DefaultTableModel(cols, 0) { @Override public boolean isCellEditable(int r,int c){ return false; } };
        allTable = new JTable(allModel);

        overdueModel = new DefaultTableModel(cols, 0) { @Override public boolean isCellEditable(int r,int c){ return false; } };
        overdueTable = new JTable(overdueModel);

        reservationsModel = new DefaultTableModel(cols, 0) { @Override public boolean isCellEditable(int r,int c){ return false; } };
        reservationsTable = new JTable(reservationsModel);

        returnedModel = new DefaultTableModel(cols, 0) { @Override public boolean isCellEditable(int r,int c){ return false; } };
        returnedTable = new JTable(returnedModel);

        allTable.setFillsViewportHeight(true);
        allTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        overdueTable.setFillsViewportHeight(true);
        overdueTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        reservationsTable.setFillsViewportHeight(true);
        reservationsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        returnedTable.setFillsViewportHeight(true);
        returnedTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // Renderer that shows book title for the book id stored in the model
        javax.swing.table.TableCellRenderer titleRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String text = "";
                if(value != null) {
                    try {
                        int bid = Integer.parseInt(String.valueOf(value));
                        String t = bookTitleCache.get(bid);
                        text = (t != null) ? t : String.valueOf(bid);
                    } catch(Exception ex) {
                        text = String.valueOf(value);
                    }
                }
                setText(text);
                return this;
            }
        };

        // Renderer that shows borrower full name for the borrower id
        javax.swing.table.TableCellRenderer borrowerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String text = "";
                if(value != null) {
                    try {
                        int bid = Integer.parseInt(String.valueOf(value));
                        String b = borrowerNameCache.get(bid);
                        text = (b != null) ? b : String.valueOf(bid);
                    } catch(Exception ex) {
                        text = String.valueOf(value);
                    }
                }
                setText(text);
                return this;
            }
        };

        allTable.getColumnModel().getColumn(1).setCellRenderer(titleRenderer);
        allTable.getColumnModel().getColumn(2).setCellRenderer(borrowerRenderer);
        overdueTable.getColumnModel().getColumn(1).setCellRenderer(titleRenderer);
        overdueTable.getColumnModel().getColumn(2).setCellRenderer(borrowerRenderer);
        reservationsTable.getColumnModel().getColumn(1).setCellRenderer(titleRenderer);
        reservationsTable.getColumnModel().getColumn(2).setCellRenderer(borrowerRenderer);
        returnedTable.getColumnModel().getColumn(1).setCellRenderer(titleRenderer);
        returnedTable.getColumnModel().getColumn(2).setCellRenderer(borrowerRenderer);

        // share common mouse handlers
        MouseAdapter ma = new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                JTable src = (JTable)e.getSource();
                int row = src.rowAtPoint(e.getPoint()); if(row < 0) return;
                if(!src.isRowSelected(row)) src.setRowSelectionInterval(row,row);
                if(SwingUtilities.isRightMouseButton(e) || e.isPopupTrigger()) { showRowPopup(e, src, row); }
                else if(e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) { showUpdateDialogForRow(row, src); }
            }
        };

        allTable.addMouseListener(ma);
        overdueTable.addMouseListener(ma);
        reservationsTable.addMouseListener(ma);
        returnedTable.addMouseListener(ma);

        // Delete key binding
        allTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE,0), "deleteRows");
        allTable.getActionMap().put("deleteRows", new AbstractAction(){ @Override public void actionPerformed(ActionEvent e){ deleteSelectedRowsOn(allTable); } });

        overdueTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE,0), "deleteRows");
        overdueTable.getActionMap().put("deleteRows", new AbstractAction(){ @Override public void actionPerformed(ActionEvent e){ deleteSelectedRowsOn(overdueTable); } });

        reservationsTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE,0), "deleteRows");
        reservationsTable.getActionMap().put("deleteRows", new AbstractAction(){ @Override public void actionPerformed(ActionEvent e){ deleteSelectedRowsOn(reservationsTable); } });

        JTabbedPane loansTabs = new JTabbedPane();
        loansTabs.addTab("All Loans", new JScrollPane(allTable));
        loansTabs.addTab("Overdue", new JScrollPane(overdueTable));
        loansTabs.addTab("Reservations", new JScrollPane(reservationsTable));
        loansTabs.addTab("Returned", new JScrollPane(returnedTable));

        add(loansTabs, BorderLayout.CENTER);
    }

    public void loadLoans() {
        try {
            java.util.List<Map<String,Object>> rows = loanService.ReadBookLoan().Read();
            allModel.setRowCount(0);
            overdueModel.setRowCount(0);
            reservationsModel.setRowCount(0);
            returnedModel.setRowCount(0);
            java.time.LocalDate today = java.time.LocalDate.now();

            for(Map<String,Object> r : rows) {
                Object id = r.getOrDefault("id", r.getOrDefault("loan_id", ""));
                Object bookIdObj = r.getOrDefault("book_id", "");
                Object borrowerIdObj = r.getOrDefault("borrower_id", "");
                Object borrowed = r.getOrDefault("borrowed_at", "");
                Object due = r.getOrDefault("due_date", "");
                Object returned = r.getOrDefault("returned_at", "");

                int bookIdInt = 0;
                int borrowerIdInt = 0;
                try { bookIdInt = Integer.parseInt(String.valueOf(bookIdObj)); } catch(Exception ignore) {}
                try { borrowerIdInt = Integer.parseInt(String.valueOf(borrowerIdObj)); } catch(Exception ignore) {}

                if(bookIdInt > 0 && !bookTitleCache.containsKey(bookIdInt)) {
                    try { List<Map<String,Object>> brows = BookService.ReadBook().WhereBookID(bookIdInt).Read(); if(!brows.isEmpty()) bookTitleCache.put(bookIdInt, String.valueOf(brows.get(0).getOrDefault("title", ""))); } catch(Exception ignore) {}
                }

                if(borrowerIdInt > 0 && !borrowerNameCache.containsKey(borrowerIdInt)) {
                    try {
                        BorrowerService borrowerService = new BorrowerService();
                        List<Map<String,Object>> brows = borrowerService.ReadBorrower().WhereID(borrowerIdInt).Read();
                        if(!brows.isEmpty()) {
                            Map<String,Object> brow = brows.get(0);
                            String lastName = String.valueOf(brow.getOrDefault("last_name", ""));
                            String firstName = String.valueOf(brow.getOrDefault("first_name", ""));
                            String middleName = String.valueOf(brow.getOrDefault("middle_name", ""));
                            String fullName = lastName + ", " + firstName;
                            if(middleName != null && !middleName.isEmpty() && !"null".equalsIgnoreCase(middleName)) {
                                fullName += " " + middleName;
                            }
                            borrowerNameCache.put(borrowerIdInt, fullName);
                        }
                    } catch(Exception ignore) {}
                }

                Object bookModelValue = (bookIdInt != 0) ? bookIdInt : bookIdObj;
                Object borrowerModelValue = (borrowerIdInt != 0) ? borrowerIdInt : borrowerIdObj;
                allModel.addRow(new Object[] { id, bookModelValue, borrowerModelValue, borrowed, due, returned });

                boolean isOverdue = false;
                if(returned == null || String.valueOf(returned).trim().isEmpty()) {
                    String dueStr = String.valueOf(due);
                    try { java.time.LocalDate dueDate = java.time.LocalDate.parse(dueStr); if(dueDate.isBefore(today)) isOverdue = true; }
                    catch(Exception pe) { try { if(!dueStr.isEmpty() && dueStr.compareTo(today.toString()) < 0) isOverdue = true; } catch(Exception ignore) { } }
                }
                if(isOverdue) overdueModel.addRow(new Object[] { id, bookModelValue, borrowerModelValue, borrowed, due, returned });

                boolean isReservation = false;
                try {
                    String borrowedStr = String.valueOf(borrowed);
                    if(borrowedStr != null && !borrowedStr.trim().isEmpty()) {
                        java.time.LocalDate b = java.time.LocalDate.parse(borrowedStr);
                        if(b.isAfter(today)) isReservation = true;
                    }
                } catch(Exception ignore) { try { if(String.valueOf(borrowed).compareTo(today.toString()) > 0) isReservation = true; } catch(Exception i) {} }
                if(isReservation) reservationsModel.addRow(new Object[] { id, bookModelValue, borrowerModelValue, borrowed, due, returned });

                boolean isReturned = false;
                if(returned != null && !String.valueOf(returned).trim().isEmpty()) isReturned = true;
                if(isReturned) returnedModel.addRow(new Object[] { id, bookModelValue, borrowerModelValue, borrowed, due, returned });
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to load loans: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void performSearch(String criteria, String term) {
        if(term == null || term.isEmpty()) { loadLoans(); return; }
        try {
            java.util.List<Map<String,Object>> rows;
            if("BookID".equalsIgnoreCase(criteria)) {
                int id; try { id = Integer.parseInt(term); } catch(Exception ex) { JOptionPane.showMessageDialog(this, "BookID must be a number", "Validation", JOptionPane.WARNING_MESSAGE); return; }
                rows = loanService.ReadBookLoan().WhereBookID(id).Read();
            } else {
                java.util.List<Map<String,Object>> all = loanService.ReadBookLoan().Read();
                String lower = term.toLowerCase(); rows = new java.util.ArrayList<>();
                for(Map<String,Object> r : all) {
                    String s = String.valueOf(r.values());
                    if(s.toLowerCase().contains(lower)) rows.add(r);
                }
            }

            allModel.setRowCount(0);
            overdueModel.setRowCount(0);
            reservationsModel.setRowCount(0);
            java.time.LocalDate today = java.time.LocalDate.now();
            for(Map<String,Object> r : rows) {
                Object id = r.getOrDefault("id", r.getOrDefault("loan_id", ""));
                Object bookIdObj = r.getOrDefault("book_id", "");
                Object borrowerIdObj = r.getOrDefault("borrower_id", "");
                Object borrowed = r.getOrDefault("borrowed_at", "");
                Object due = r.getOrDefault("due_date", "");
                Object returned = r.getOrDefault("returned_at", "");

                int bookIdInt = 0;
                int borrowerIdInt = 0;
                try { bookIdInt = Integer.parseInt(String.valueOf(bookIdObj)); } catch(Exception ignore) {}
                try { borrowerIdInt = Integer.parseInt(String.valueOf(borrowerIdObj)); } catch(Exception ignore) {}

                if(bookIdInt > 0 && !bookTitleCache.containsKey(bookIdInt)) {
                    try { List<Map<String,Object>> brows = BookService.ReadBook().WhereBookID(bookIdInt).Read(); if(!brows.isEmpty()) bookTitleCache.put(bookIdInt, String.valueOf(brows.get(0).getOrDefault("title", ""))); } catch(Exception ignore) {}
                }

                if(borrowerIdInt > 0 && !borrowerNameCache.containsKey(borrowerIdInt)) {
                    try {
                        BorrowerService borrowerService = new BorrowerService();
                        List<Map<String,Object>> brows = borrowerService.ReadBorrower().WhereID(borrowerIdInt).Read();
                        if(!brows.isEmpty()) {
                            Map<String,Object> brow = brows.get(0);
                            String lastName = String.valueOf(brow.getOrDefault("last_name", ""));
                            String firstName = String.valueOf(brow.getOrDefault("first_name", ""));
                            String middleName = String.valueOf(brow.getOrDefault("middle_name", ""));
                            String fullName = lastName + ", " + firstName;
                            if(middleName != null && !middleName.isEmpty() && !"null".equalsIgnoreCase(middleName)) {
                                fullName += " " + middleName;
                            }
                            borrowerNameCache.put(borrowerIdInt, fullName);
                        }
                    } catch(Exception ignore) {}
                }

                Object bookModelValue = (bookIdInt != 0) ? bookIdInt : bookIdObj;
                Object borrowerModelValue = (borrowerIdInt != 0) ? borrowerIdInt : borrowerIdObj;
                allModel.addRow(new Object[] { id, bookModelValue, borrowerModelValue, borrowed, due, returned });

                boolean isOverdue = false;
                if(returned == null || String.valueOf(returned).trim().isEmpty()) {
                    String dueStr = String.valueOf(due);
                    try { java.time.LocalDate dueDate = java.time.LocalDate.parse(dueStr); if(dueDate.isBefore(today)) isOverdue = true; }
                    catch(Exception pe) { try { if(!dueStr.isEmpty() && dueStr.compareTo(today.toString()) < 0) isOverdue = true; } catch(Exception ignore) { } }
                }
                if(isOverdue) overdueModel.addRow(new Object[] { id, bookModelValue, borrowerModelValue, borrowed, due, returned });

                boolean isReservation = false;
                try {
                    String borrowedStr = String.valueOf(borrowed);
                    if(borrowedStr != null && !borrowedStr.trim().isEmpty()) {
                        java.time.LocalDate b = java.time.LocalDate.parse(borrowedStr);
                        if(b.isAfter(today)) isReservation = true;
                    }
                } catch(Exception ignore) { try { if(String.valueOf(borrowed).compareTo(today.toString()) > 0) isReservation = true; } catch(Exception i) {} }
                if(isReservation) reservationsModel.addRow(new Object[] { id, bookModelValue, borrowerModelValue, borrowed, due, returned });
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Search failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showRowPopup(MouseEvent e, JTable src, int row) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem update = new JMenuItem("Update");
        JMenuItem delete = new JMenuItem("Delete");
        update.addActionListener(ae -> showUpdateDialogForRow(row, src));
        delete.addActionListener(ae -> deleteSelectedRowsOn(src));
        menu.add(update); menu.add(delete);
        menu.show(e.getComponent(), e.getX(), e.getY());
    }

    private void showUpdateDialogForRow(int row, JTable src) {
        Object idObj = src.getValueAt(row, 0);
        Object bookIdObj = src.getValueAt(row, 1);
        Object borrowerIdObj = src.getValueAt(row, 2);
        Object borrowed = src.getValueAt(row, 3);
        Object due = src.getValueAt(row, 4);
        Object returned = src.getValueAt(row, 5);

        int id; int bookId; int borrowerId;
        try { id = Integer.parseInt(String.valueOf(idObj)); } catch(Exception ex) { JOptionPane.showMessageDialog(this, "Invalid loan id", "Error", JOptionPane.ERROR_MESSAGE); return; }
        try { bookId = Integer.parseInt(String.valueOf(bookIdObj)); } catch(Exception ex) { bookId = 0; }
        try { borrowerId = Integer.parseInt(String.valueOf(borrowerIdObj)); } catch(Exception ex) { borrowerId = 0; }

        LoanFormDialog d = new LoanFormDialog(owner, id, bookId, borrowerId, String.valueOf(due), String.valueOf(borrowed), String.valueOf(returned), new Runnable() { @Override public void run() { loadLoans(); } });
        d.setVisible(true);
    }

    public void deleteSelectedRowsOn(JTable src) {
        int[] sel = src.getSelectedRows(); if(sel == null || sel.length == 0) return;
        int conf = JOptionPane.showConfirmDialog(this, "Delete selected loan(s)?", "Confirm", JOptionPane.YES_NO_OPTION);
        if(conf != JOptionPane.YES_OPTION) return;

        DefaultTableModel model = (DefaultTableModel)src.getModel();
        boolean anyFailed = false; StringBuilder errors = new StringBuilder();
        for(int i = sel.length - 1; i >= 0; i--) {
            int row = sel[i]; Object idObj = model.getValueAt(row, 0); int id;
            try { id = Integer.parseInt(String.valueOf(idObj)); } catch(Exception ex) { anyFailed = true; errors.append("Invalid id at row ").append(row).append("\n"); continue; }
            try { boolean ok = loanService.DeleteBookLoan().WhereID(id).Delete(); if(!ok) { anyFailed = true; errors.append("Failed to delete id: ").append(id).append("\n"); } } catch(Exception ex) { anyFailed = true; errors.append("Error deleting id ").append(id).append(": ").append(ex.getMessage()).append("\n"); }
        }

        if(anyFailed) JOptionPane.showMessageDialog(this, "Some deletes failed:\n" + errors.toString(), "Partial Failure", JOptionPane.WARNING_MESSAGE); else JOptionPane.showMessageDialog(this, "Deleted.", "Deleted", JOptionPane.INFORMATION_MESSAGE);
        loadLoans();
    }
}