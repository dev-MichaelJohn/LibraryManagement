# Library Management System - UI Architecture Guide

This document provides a comprehensive overview of the UI components in the Library Management System. It explains the architecture, design patterns, and provides code snippets to help understand how the Java Swing-based interface works.

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Class Descriptions](#class-descriptions)
3. [Key Design Patterns](#key-design-patterns)
4. [Component Interaction Diagram](#component-interaction-diagram)
5. [Common Swing Concepts](#common-swing-concepts)

---

## Architecture Overview

The UI is built using **Java Swing**, a framework for creating graphical user interfaces. The architecture follows a **component-based approach** where:

- **JFrame** = The main application window
- **JPanel** = Containers that hold other components
- **JTable** = Data display component (like Excel spreadsheet)
- **JDialog** = Modal windows (pops up on top, blocks interaction with main window)
- **Listeners** = Objects that respond to user actions (clicks, typing, etc.)

### Layout Structure

```
LibraryManager (JFrame)
├── JTabbedPane (tabs at the top)
│   ├── BookTablePanel (Books tab)
│   │   ├── Header (JLabel)
│   │   ├── Buttons (Add, Import, Export)
│   │   ├── SearchPanel (Search bar with debouncing)
│   │   └── JTable (displays book rows)
│   └── LoanPanel (Loans tab)
│       ├── Header
│       ├── Buttons (Add, Export)
│       └── Multiple JTables (all loans, overdue, etc.)
```

---

## Class Descriptions

### 1. **LibraryManager** (`LibraryManager.java`)

**Purpose**: The main application window and entry point.

**Key Responsibilities**:
- Creates the main window (JFrame)
- Sets up the tabbed interface
- Refreshes data when users switch between tabs

**Key Code Pattern - Tab Change Listener**:

```java
tabs.addChangeListener(new ChangeListener() {
    @Override 
    public void stateChanged(ChangeEvent e) {
        int idx = tabs.getSelectedIndex();
        Component c = tabs.getComponentAt(idx);
        // When user clicks a different tab, reload that tab's data
        if(c instanceof BookTablePanel) {
            ((BookTablePanel)c).loadBooks();  // Refresh from database
        } else if(c instanceof LoanPanel) {
            ((LoanPanel)c).loadLoans();
        }
    }
});
```

**Why This Pattern?**
- Data might have changed while user was on a different tab
- By reloading when the tab is selected, we ensure users always see current information
- This is called the "Refresh on Focus" pattern

---

### 2. **SearchPanel** (`SearchPanel.java`)

**Purpose**: A reusable component for searching with a debounced input.

**Key Responsibilities**:
- Provides a dropdown to select search field (Title, Author, ISBN, Year, Genre)
- Provides a text input for the search query
- Implements debouncing to avoid excessive database queries
- Notifies a listener when search should execute

**Key Code Pattern - Debouncing with Timer**:

```java
// Timer waits 350ms before firing the search callback
final javax.swing.Timer searchTimer = new javax.swing.Timer(350, new ActionListener() {
    @Override
    public void actionPerformed(ActionEvent e) {
        listener.onSearch((String)criteriaBox.getSelectedItem(), searchField.getText().trim());
    }
});
searchTimer.setRepeats(false);  // Only fire once per interval

// When user types, restart the timer (resets the countdown)
searchField.getDocument().addDocumentListener(new DocumentListener() {
    private void restart() {
        if (searchTimer.isRunning()) searchTimer.restart();  // Restart countdown
        else searchTimer.start();
    }
    
    @Override public void insertUpdate(DocumentEvent e) { restart(); }
    @Override public void removeUpdate(DocumentEvent e) { restart(); }
    @Override public void changedUpdate(DocumentEvent e) { restart(); }
});
```

**How Debouncing Works**:
1. User types "Java" → Timer starts (waiting 350ms)
2. Before 350ms, user types another letter "a" → Timer restarts (350ms from now)
3. User stops typing → 350ms passes → Timer fires → Search executes

**Why?** Typing is fast but database queries are slow. If we searched on every keystroke, we'd waste time querying for "J", then "Ja", then "Jav", etc. Debouncing waits until the user stops typing.

---

### 3. **BookTablePanel** (`BookTablePanel.java`)

**Purpose**: The main panel for viewing, searching, and managing books.

**Key Responsibilities**:
- Displays books in a JTable (like a spreadsheet)
- Provides Add/Edit/Delete operations
- Implements search with SearchPanel
- Handles CSV import/export
- Responds to mouse clicks (right-click menu, double-click to edit)
- Responds to Delete key

**UI Layout**:

```
┌─────────────────────────────────────────────────────────────┐
│ Books        [Add Book] [Import CSV] [Export CSV]          │
├─────────────────────────────────────────────────────────────┤
│ Search: [All ▼] [text field] [Clear]                       │
├─────────────────────────────────────────────────────────────┤
│ ID │ Title │ Author │ ISBN │ Year │ Genres │ Available      │
├─────────────────────────────────────────────────────────────┤
│  1 │ 1984  │ Orwell │ ...  │ 1949 │ Sci-Fi │ Yes           │
│  2 │ Dune  │ Herbert│ ...  │ 1965 │ Sci-Fi │ No            │
└─────────────────────────────────────────────────────────────┘
```

**Key Code Pattern - SwingWorker (Background Threading)**:

```java
public void loadBooks() {
    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));  // Show loading spinner
    
    new SwingWorker<List<Map<String, Object>>, Void>() {
        @Override 
        protected List<Map<String, Object>> doInBackground() {
            // This runs on a BACKGROUND thread - doesn't freeze UI
            return BookService.ReadBook().Read();  // Query database
        }
        
        @Override 
        protected void done() {
            // This runs on the UI thread - safe to update table
            try {
                List<Map<String, Object>> rows = get();  // Get results from background
                tableModel.setRowCount(0);  // Clear existing rows
                for(Map<String, Object> r : rows) {
                    Object id = r.getOrDefault("id", "");
                    Object title = r.getOrDefault("title", "");
                    // ... extract more fields
                    tableModel.addRow(new Object[] { id, title, ... });  // Add to table
                }
            } catch(Exception ex) {
                JOptionPane.showMessageDialog(BookTablePanel.this, 
                    "Failed to load books: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } finally {
                setCursor(Cursor.getDefaultCursor());  // Restore normal cursor
            }
        }
    }.execute();  // Start background thread
}
```

**Why SwingWorker?**
- Database queries can take time (1-2 seconds)
- If done on the UI thread, the UI freezes during the query
- SwingWorker runs the query on a background thread, keeping UI responsive
- It automatically switches back to the UI thread to display results

**Key Code Pattern - Mouse Interaction**:

```java
table.addMouseListener(new MouseAdapter() {
    @Override
    public void mouseClicked(MouseEvent e) {
        int row = table.rowAtPoint(e.getPoint());  // Which row was clicked?
        
        if (SwingUtilities.isRightMouseButton(e)) {
            // Right-click: show context menu
            showRowPopup(e, row);
        } else if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
            // Double-click: open edit dialog
            showUpdateDialogForRow(row);
        }
    }
});
```

**Key Code Pattern - Keyboard Shortcut (Delete Key)**:

```java
// Map Delete key to an action
table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
    .put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteRows");

// Define what the action does
table.getActionMap().put("deleteRows", new AbstractAction() {
    @Override public void actionPerformed(ActionEvent e) {
        deleteSelectedRows();  // Delete all selected rows when user presses Delete
    }
});
```

---

### 4. **BookFormDialog** (`BookFormDialog.java`)

**Purpose**: A modal dialog for adding or editing books.

**Key Characteristics**:
- Modal = Blocks interaction with main window until closed
- Reusable = Can be used for adding (no book data) or updating (with existing book data)
- Validates input before saving
- Manages genres (books can have multiple genres)

**Modal Dialog Pattern**:

```java
public class BookFormDialog extends JDialog {
    // Constructor for adding a new book
    public BookFormDialog(JFrame owner, Runnable onSuccess) {
        super(owner, "Add New Book", true);  // 'true' means modal
        init(null, onSuccess);
    }
    
    // Constructor for updating an existing book
    public BookFormDialog(JFrame owner, int id, String title, String author, 
                         String isbn, String year, Runnable onSuccess) {
        super(owner, "Update Book", true);
        init(new BookData(id, title, author, isbn, year), onSuccess);
    }
}
```

**Why Modal?**
- User should focus on completing the form before doing anything else
- Prevents confusion from having multiple windows interact
- User can't accidentally close the main window while editing

**Key Code Pattern - Form Validation**:

```java
submit.addActionListener(new ActionListener() {
    @Override
    public void actionPerformed(ActionEvent e) {
        String title = titleField.getText().trim();
        String author = authorField.getText().trim();
        String isbn = isbnField.getText().trim();
        String yearText = yearField.getText().trim();

        // Check required fields
        if(title.isEmpty() || author.isEmpty() || isbn.isEmpty() || yearText.isEmpty()) {
            JOptionPane.showMessageDialog(BookFormDialog.this, 
                "All fields are required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;  // Don't proceed
        }

        // Check year is a valid number
        int year;
        try {
            year = Integer.parseInt(yearText);
            if(year < 0) throw new NumberFormatException("negative");
        } catch(NumberFormatException ex) {
            JOptionPane.showMessageDialog(BookFormDialog.this, 
                "Year must be a non-negative integer.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // All validations passed, save to database
        try {
            if(data == null) {
                // Insert new book
                BookService.InsertBook()
                    .SetTitle(title)
                    .SetAuthor(author)
                    .SetIsbn(isbn)
                    .SetYearPublished(year)
                    .Insert();
            } else {
                // Update existing book
                BookService.UpdateBook()
                    .WhereBookID(data.id)
                    .SetTitle(title)
                    .SetAuthor(author)
                    .SetIsbn(isbn)
                    .SetYearPublished(year)
                    .Update();
            }
            
            onSuccess.run();  // Call callback (refreshes table in parent)
            dispose();        // Close the dialog
        } catch(Exception ex) {
            JOptionPane.showMessageDialog(BookFormDialog.this, 
                "Failed to save: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
});
```

**Key Code Pattern - Genre Management**:

```java
// Genre list model (holds genre strings)
DefaultListModel<String> genreListModel = new DefaultListModel<>();
JList<String> genreList = new JList<>(genreListModel);

// Add genre button
addGenreBtn.addActionListener(new ActionListener() {
    @Override
    public void actionPerformed(ActionEvent e) {
        // Prompt user for genre name
        String genre = JOptionPane.showInputDialog(BookFormDialog.this, 
            "Enter genre name:", "Add Genre", JOptionPane.PLAIN_MESSAGE);
        
        if(genre != null && !genre.trim().isEmpty()) {
            genre = genre.trim();
            // Prevent duplicates
            for(int i = 0; i < genreListModel.size(); i++) {
                if(genreListModel.get(i).equalsIgnoreCase(genre)) {
                    JOptionPane.showMessageDialog(BookFormDialog.this, 
                        "Genre already added.", "Duplicate", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }
            genreListModel.addElement(genre);  // Add to list
        }
    }
});

// Remove genre button
removeGenreBtn.addActionListener(new ActionListener() {
    @Override
    public void actionPerformed(ActionEvent e) {
        int selectedIndex = genreList.getSelectedIndex();
        if(selectedIndex >= 0) {
            genreListModel.remove(selectedIndex);  // Remove from list
        } else {
            JOptionPane.showMessageDialog(BookFormDialog.this, 
                "Please select a genre to remove.", "No Selection", JOptionPane.WARNING_MESSAGE);
        }
    }
});
```

---

### 5. **LoanPanel** (`LoanPanel.java`)

**Purpose**: The panel for viewing and managing book loans (borrowed books).

**Key Responsibilities**:
- Displays multiple tables for different loan states (all, overdue, returned)
- Manages book and borrower caching to avoid repeated queries
- Implements table cell renderers to show book titles instead of IDs
- Provides Add/Delete/Update operations for loans

**Key Code Pattern - Caching to Avoid Repeated Queries**:

```java
// Cache for book titles keyed by book ID
private Map<Integer, String> bookTitleCache = new HashMap<>();
// Cache for borrower full names keyed by borrower ID
private Map<Integer, String> borrowerNameCache = new HashMap<>();

// When displaying a table, use the cache to convert IDs to names
private void populateCaches() {
    try {
        // Load all books once and cache their titles
        List<Map<String, Object>> books = BookService.ReadBook().Read();
        for(Map<String, Object> b : books) {
            int id = (Integer) b.get("id");
            String title = (String) b.get("title");
            bookTitleCache.put(id, title);
        }
        
        // Load all borrowers once and cache their names
        List<Map<String, Object>> borrowers = BorrowerService.ReadBorrower().Read();
        for(Map<String, Object> bor : borrowers) {
            int id = (Integer) bor.get("id");
            String name = bor.get("first_name") + " " + bor.get("last_name");
            borrowerNameCache.put(id, name);
        }
    } catch(Exception ex) {
        System.err.println("Error populating caches: " + ex.getMessage());
    }
}
```

**Why Caching?**
- Without caching, displaying a table with 100 loans would require 100+ database queries (one for each book title)
- With caching, we load all books/borrowers once, then look them up in memory
- This is much faster and reduces database load

**Key Code Pattern - Table Cell Renderer**:

```java
// Custom renderer that converts book IDs to book titles
javax.swing.table.TableCellRenderer titleRenderer = new DefaultTableCellRenderer() {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, 
                                                   boolean isSelected, boolean hasFocus, 
                                                   int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        
        String text = "";
        if(value != null) {
            try {
                int bid = Integer.parseInt(String.valueOf(value));  // value is a book ID
                String t = bookTitleCache.get(bid);                 // Look up title in cache
                text = (t != null) ? t : String.valueOf(bid);       // Show title or ID if not found
            } catch(Exception ignore) { }
        }
        setText(text);  // Set the display text
        return this;
    }
};

// Apply renderer to the book ID column
allTable.getColumnModel().getColumn(1).setCellRenderer(titleRenderer);  // Column 1 = book ID
```

**Why Cell Renderer?**
- The table model stores book IDs (numbers)
- But users want to see book titles (human-readable)
- The renderer intercepts the display and converts IDs to titles before showing them
- This is like a "display formatter" - the data stays the same, but we show it differently

---

### 6. **LoanFormDialog** (`LoanFormDialog.java`)

**Purpose**: A modal dialog for adding or updating book loans.

**Key Responsibilities**:
- Allows user to select a book from a searchable list
- Allows user to select a borrower
- Validates loan dates
- Provides fields for due date, returned date (if applicable)

**Key Code Pattern - Dynamic List Population**:

```java
// Load books asynchronously in background
Runnable doLoadAll = () -> new SwingWorker<List<BookItem>, Void>() {
    @Override protected List<BookItem> doInBackground() {
        try {
            List<BookItem> out = new ArrayList<>();
            List<Map<String, Object>> rows = BookService.ReadBook().Read();
            for(Map<String, Object> r : rows) {
                int id = (Integer) r.getOrDefault("id", 0);
                String title = (String) r.getOrDefault("title", "");
                out.add(new BookItem(id, title));
            }
            return out;
        } catch(Exception ex) {
            return Collections.emptyList();
        }
    }
    
    @Override protected void done() {
        try {
            List<BookItem> items = get();
            listModel.clear();
            for(BookItem it : items) {
                listModel.addElement(it);  // Add to dropdown list
            }
        } catch(Exception ex) {
            JOptionPane.showMessageDialog(LoanFormDialog.this, 
                "Failed to load books: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}.execute();

doLoadAll.run();  // Start loading
```

---

## Key Design Patterns

### 1. **Model-View Pattern**
```
BookService (Model)
     ↓ (queries)
Database (Data)
     ↑ (results)
BookTablePanel (View) ← displays data
```

- Model (BookService): Responsible for data operations
- View (BookTablePanel): Responsible for displaying data
- They're loosely coupled - View calls Model when needed

### 2. **Observer Pattern (Listeners)**
```
User clicks button → ActionListener fires → onSuccess callback → Table reloads
```

- Components notify listeners when events occur
- Listeners respond by performing actions
- Decouples UI logic from component creation

### 3. **Composite Pattern (Nested Components)**
```
JPanel (main)
  ├── JPanel (header)
  ├── JPanel (search)
  └── JTable (content)
```

- Complex UIs built from simpler components
- Each component handles its own layout
- Easy to rearrange or replace components

### 4. **SwingWorker Pattern (Background Threading)**
```
SwingWorker
├── doInBackground() → runs on background thread → database query
└── done() → runs on UI thread → update display
```

- Long operations don't freeze the UI
- Automatic thread management
- Type-safe result passing

---

## Component Interaction Diagram

```
                    LibraryManager (JFrame)
                    /                 \
              NORTH: JTabbedPane        
              /                    \
         BookTablePanel          LoanPanel
         /    |    \              /  |  \
        /     |     \            /   |   \
    Header  Buttons  Search    Table  Cache  Buttons
      |        |        |        |      |       |
      v        v        v        v      v       v
   [Add]    [Import]  [Clear]  [Row]  [Map]  [Add]
   [Delete] [Export]           [Edit]        [Delete]
            \    |    /                \    |    /
             \   |   /                  \   |   /
              Database (via Service)     Database
```

---

## Common Swing Concepts

### Layout Managers

```java
// BorderLayout - 5 regions (North, South, East, West, Center)
JPanel panel = new JPanel(new BorderLayout());
panel.add(header, BorderLayout.NORTH);       // Top
panel.add(buttons, BorderLayout.EAST);       // Right
panel.add(table, BorderLayout.CENTER);       // Middle

// FlowLayout - components flow left to right
JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
buttons.add(button1);  // button1 on left
buttons.add(button2);  // button2 to the right of button1

// GridBagLayout - fine-grained control (used in dialogs)
GridBagConstraints gbc = new GridBagConstraints();
gbc.gridx = 0;
gbc.gridy = 0;
form.add(label, gbc);  // label at position (0,0)
```

### Event Handling

```java
// Anonymous inner class
button.addActionListener(new ActionListener() {
    @Override
    public void actionPerformed(ActionEvent e) {
        // Code runs when button is clicked
    }
});

// Lambda expression (Java 8+) - same thing, shorter syntax
button.addActionListener(e -> {
    // Code runs when button is clicked
});
```

### Modal vs. Modeless

```java
// Modal (true) - blocks interaction with parent window
JDialog dialog = new JDialog(parentFrame, "Title", true);

// Modeless (false) - allows interaction with parent
JDialog dialog = new JDialog(parentFrame, "Title", false);
```

### Table Concepts

```java
// TableModel = the data (rows and columns)
DefaultTableModel model = new DefaultTableModel(columnNames, 0);
model.addRow(new Object[] { value1, value2, value3 });

// JTable = the display
JTable table = new JTable(model);

// When you add a row to the model, the table automatically shows it
```

---

## Tips for Understanding UI Code

1. **Think visually**: Draw out the layout on paper
2. **Trace user actions**: Click → Listener → Callback → Action
3. **Follow the patterns**: Most code follows one of the patterns described above
4. **Focus on the constructor**: That's usually where the magic happens
5. **Read comments**: We've added detailed comments explaining tricky parts
6. **Test in IDE**: Run the code and click things to see what happens

---

## Further Reading

- **Oracle Swing Tutorial**: https://docs.oracle.com/javase/tutorial/uiswing/
- **MVC Pattern**: https://en.wikipedia.org/wiki/Model%E2%80%93view%E2%80%93controller
- **SwingWorker**: https://docs.oracle.com/javase/tutorial/uiswing/concurrency/
- **Design Patterns**: https://refactoring.guru/design-patterns

