# UI Classes Quick Reference

A cheat sheet for the Java Swing UI classes in the Library Management System.

## File Locations

```
src/gui/
├── LibraryManager.java          (Main window frame)
├── BookTablePanel.java          (Books tab)
├── BookFormDialog.java          (Add/Edit book dialog)
├── SearchPanel.java             (Reusable search component)
├── LoanPanel.java               (Loans tab)
└── LoanFormDialog.java          (Add/Edit loan dialog)
```

---

## Class Summary Table

| Class | Type | Purpose | Key Methods |
|-------|------|---------|------------|
| **LibraryManager** | JFrame | Main app window | `LibraryManager()` |
| **BookTablePanel** | JPanel | Books display & management | `loadBooks()`, `performSearch()`, `deleteSelectedRows()` |
| **BookFormDialog** | JDialog | Add/edit book form | `saveGenres()`, `updateGenres()` |
| **SearchPanel** | JPanel | Search UI component | Constructor takes `SearchListener` callback |
| **LoanPanel** | JPanel | Loans display (4 tables) | `loadLoans()` |
| **LoanFormDialog** | JDialog | Add/edit loan form | Constructor has add and update modes |

---

## Key Concepts at a Glance

### 1. **Threading (SwingWorker)**
```java
// Pattern: Don't freeze UI during long operations
new SwingWorker<Result, Progress>() {
    @Override protected Result doInBackground() {
        // Long operation here (database query, file I/O, etc.)
        return result;
    }
    @Override protected void done() {
        // Update UI here (safe, runs on UI thread)
    }
}.execute();
```

### 2. **Debouncing (SearchPanel)**
```java
// Pattern: Wait 350ms after user stops typing before searching
Timer timer = new Timer(350, e -> performSearch());
timer.setRepeats(false);
documentListener.insertUpdate() -> timer.restart();
```

### 3. **Modal Dialogs (BookFormDialog, LoanFormDialog)**
```java
// Pattern: Block interaction with main window until dialog closes
JDialog dialog = new JDialog(owner, "Title", true);  // true = modal
dialog.setVisible(true);  // Blocks until user closes
```

### 4. **Cell Renderers (LoanPanel)**
```java
// Pattern: Display formatted data in table cells
table.getColumnModel().getColumn(1).setCellRenderer(
    new DefaultTableCellRenderer() {
        public Component getTableCellRendererComponent(...) {
            // Convert raw data to display text
            setText(displayText);
            return this;
        }
    }
);
```

### 5. **Event Listeners (Everywhere)**
```java
// Pattern: Respond to user actions
button.addActionListener(e -> {
    // Code runs when button clicked
});

table.addMouseListener(new MouseAdapter() {
    @Override public void mouseClicked(MouseEvent e) {
        if(e.getClickCount() == 2) {
            // Double-click handler
        }
    }
});
```

### 6. **Keyboard Shortcuts (BookTablePanel)**
```java
// Pattern: Map keys to actions
table.getInputMap().put(
    KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), 
    "deleteAction"
);
table.getActionMap().put("deleteAction", new AbstractAction() {
    public void actionPerformed(ActionEvent e) {
        deleteSelectedRows();
    }
});
```

### 7. **Layout Managers**
```java
// BorderLayout: 5 regions (NORTH, SOUTH, EAST, WEST, CENTER)
panel.setLayout(new BorderLayout());
panel.add(header, BorderLayout.NORTH);
panel.add(table, BorderLayout.CENTER);

// FlowLayout: left-to-right flow
panel.setLayout(new FlowLayout(FlowLayout.RIGHT));

// GridBagLayout: grid with fine control
GridBagConstraints gbc = new GridBagConstraints();
gbc.gridx = 0; gbc.gridy = 0;
panel.add(component, gbc);
```

---

## Component Hierarchy

```
LibraryManager (JFrame)
│
└── JPanel (main)
    │
    └── JTabbedPane
        ├── BookTablePanel
        │   ├── JPanel (header)
        │   │   ├── JLabel "Books"
        │   │   ├── JButton "Add Book"
        │   │   ├── JButton "Import CSV"
        │   │   └── JButton "Export CSV"
        │   ├── SearchPanel
        │   │   ├── JComboBox (criteria)
        │   │   ├── JTextField (search)
        │   │   └── JButton "Clear"
        │   └── JTable (book data)
        │
        └── LoanPanel
            ├── JPanel (header)
            │   ├── JLabel "Book Loans"
            │   ├── JButton "Add Loan"
            │   └── JButton "Export CSV"
            └── JTabbedPane (loan views)
                ├── JTable (all loans)
                ├── JTable (overdue)
                ├── JTable (reservations)
                └── JTable (returned)
```

---

## Dialog Flows

### Add Book Flow
```
User clicks "Add Book"
  → BookFormDialog opens (modal)
  → User enters data
  → User clicks "Save"
  → Dialog calls onSuccess callback
  → BookTablePanel.loadBooks() executes
  → Dialog closes
  → Table refreshes with new book
```

### Search Book Flow
```
User types in SearchPanel
  → DocumentListener fires
  → Timer starts (350ms debounce)
  → If user types more, timer restarts
  → 350ms passes without typing
  → Timer fires callback
  → performSearch() executes
  → Table updates with search results
```

### Loan Selection Flow
```
LoanFormDialog opens
  → Books load in background (SwingWorker)
  → User types in search field
  → 300ms debounce timer fires
  → Book list updates
  → User clicks a book
  → selectedTitle shows chosen book
  → User clicks "Save"
  → Dialog saves loan
  → onSuccess callback refreshes table
```

---

## Common UI Operations

### Load Data from Database
```java
public void loadBooks() {
    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    new SwingWorker<List<Map<String, Object>>, Void>() {
        @Override 
        protected List<Map<String, Object>> doInBackground() {
            return BookService.ReadBook().Read();  // Query
        }
        @Override 
        protected void done() {
            tableModel.setRowCount(0);  // Clear
            for(Map<String, Object> r : get()) {  // Populate
                tableModel.addRow(new Object[] { /* data */ });
            }
            setCursor(Cursor.getDefaultCursor());
        }
    }.execute();
}
```

### Handle Table Right-Click
```java
table.addMouseListener(new MouseAdapter() {
    @Override public void mouseClicked(MouseEvent e) {
        if(SwingUtilities.isRightMouseButton(e)) {
            int row = table.rowAtPoint(e.getPoint());
            showRowPopup(e, row);  // Show context menu
        }
    }
});
```

### Validate Form Input
```java
String title = titleField.getText().trim();
if(title.isEmpty()) {
    JOptionPane.showMessageDialog(this, 
        "Title is required.", "Validation Error", 
        JOptionPane.WARNING_MESSAGE);
    return;  // Don't proceed
}
// Validation passed
saveToDatabase();
```

### Show Error Dialog
```java
JOptionPane.showMessageDialog(this, 
    "An error occurred: " + error.getMessage(), 
    "Error", 
    JOptionPane.ERROR_MESSAGE);
```

### Show Confirmation Dialog
```java
int result = JOptionPane.showConfirmDialog(this, 
    "Are you sure you want to delete?", 
    "Confirm Delete", 
    JOptionPane.YES_NO_OPTION);
if(result == JOptionPane.YES_OPTION) {
    // User clicked Yes
    performDelete();
}
```

---

## Debugging Tips

### Print Component Hierarchy
```java
// In any component:
System.out.println(this.getParent());           // Parent component
System.out.println(getClass().getSimpleName()); // Class name
```

### Check if Method is on UI Thread
```java
if(SwingUtilities.isEventDispatchThread()) {
    System.out.println("On UI thread");
} else {
    System.out.println("On background thread");
}
```

### Debug Table Data
```java
for(int i = 0; i < tableModel.getRowCount(); i++) {
    for(int j = 0; j < tableModel.getColumnCount(); j++) {
        System.out.println(tableModel.getValueAt(i, j));
    }
}
```

### Check Event Listener Count
```java
javax.swing.event.EventListenerList list = button.getListeners(ActionListener.class);
System.out.println("Listeners: " + list.length);
```

---

## Common Mistakes

❌ **Don't do this:**
```java
// WRONG: Database query on UI thread (freezes UI)
List<Book> books = BookService.ReadBook().Read();  // Blocks!
tableModel.setRowCount(0);
for(Book b : books) tableModel.addRow(b);
```

✅ **Do this instead:**
```java
// RIGHT: Database query on background thread
new SwingWorker<...>() {
    @Override protected ... doInBackground() {
        return BookService.ReadBook().Read();  // Background
    }
    @Override protected void done() {
        tableModel.setRowCount(0);  // UI thread
        for(Book b : get()) tableModel.addRow(b);
    }
}.execute();
```

---

❌ **Don't do this:**
```java
// WRONG: Can cause duplicate listeners
for(int i = 0; i < 100; i++) {
    button.addActionListener(e -> doSomething());  // Adds 100 listeners!
}
```

✅ **Do this instead:**
```java
// RIGHT: Remove old listeners before adding new ones
button.removeActionListener(oldListener);
button.addActionListener(newListener);
```

---

## Performance Tips

1. **Use SwingWorker for database queries** - keeps UI responsive
2. **Cache frequently accessed data** - LoanPanel caches book titles
3. **Debounce search input** - prevents excessive queries while user is typing
4. **Use cell renderers** - convert IDs to display text without extra queries
5. **Close resources properly** - file streams, database connections, etc.

---

## Useful Swing Documentation Links

- [Oracle Swing Tutorial](https://docs.oracle.com/javase/tutorial/uiswing/)
- [JTable Guide](https://docs.oracle.com/javase/tutorial/uiswing/components/table.html)
- [SwingWorker](https://docs.oracle.com/javase/tutorial/uiswing/concurrency/worker.html)
- [Layout Managers](https://docs.oracle.com/javase/tutorial/uiswing/layout/index.html)
- [Event Handling](https://docs.oracle.com/javase/tutorial/uiswing/events/)

