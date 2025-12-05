package lib.BookLoan;

import config.DatabaseConnection;

/**
 * Builder class for inserting new book loan records into the database.
 *
 * @author dev-MichaelJohn
 */
public class InsertBookLoanBuilder extends BookLoanBuilder<InsertBookLoanBuilder> {
    private DatabaseConnection dbConnection;

    public InsertBookLoanBuilder() {
        super();
        this.dbConnection = DatabaseConnection.GetInstance();
    }

    @Override
    protected InsertBookLoanBuilder self() {
        return this;
    }

    /**
     * Sets the book ID for the new book loan record.
     * 
     * @param bookID The book ID of the book loan record.
     * @return The current InsertBookLoanBuilder instance.
     */
    public InsertBookLoanBuilder SetBookID(int bookID) {
        if(bookID <= 0) throw new IllegalArgumentException("Book ID must be positive");
        if(this.bookID != 0) throw new IllegalStateException("Book ID has already been set");

        this.bookID = bookID;
        return this.SetField("book_id", bookID);
    }

    /**
     * Sets the borrowed date/time for the new book loan record.
     * 
     * @param borrowedAt The date/time when the book was borrowed.
     * @return The current InsertBookLoanBuilder instance.
     */
    public InsertBookLoanBuilder SetBorrowedAt(java.util.Date borrowedAt) {
        if(borrowedAt == null) throw new IllegalArgumentException("borrowedAt must be provided");
        if(this.borrowedAt != null) throw new IllegalStateException("borrowedAt already set");
        this.borrowedAt = borrowedAt;
        return this.SetField("borrowed_at", borrowedAt);
    }

    /**
     * Sets the due date for the new book loan record.
     * 
     * @param dueDate The due date of the loan.
     * @return The current InsertBookLoanBuilder instance.
     */
    public InsertBookLoanBuilder SetDueDate(java.util.Date dueDate) {
        if(dueDate == null) throw new IllegalArgumentException("dueDate must be provided");
        if(this.dueDate != null) throw new IllegalStateException("dueDate already set");
        this.dueDate = dueDate;
        return this.SetField("due_date", dueDate);
    }

    /**
     * Executes the insert operation to add the new book loan record to the database.
     * 
     * @return true if the insert was successful, false otherwise.
     */
    public boolean Insert() {
        if(this.bookID == 0) throw new IllegalStateException("Book ID must be set before inserting");
        if(this.borrowedAt == null) throw new IllegalStateException("borrowedAt must be set before inserting");

        String baseStatement = "INSERT INTO book_loans (book_id, borrowed_at, due_date) VALUES (?, ?, ?)";
        int rowsAffected;
        try {
            rowsAffected = dbConnection.ExecuteUpdate(baseStatement, this.GetValues().toArray());
        } catch(Exception e) {
            throw e;
        }

        return rowsAffected > 0;
    }
}

