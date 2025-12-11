package lib.BookLoan;

import java.util.Date;
import java.util.Map;
import java.util.List;

import config.DatabaseConnection;

/**
 * Builder class for reading book loan records from the database.
 *
 * Author: dev-MichaelJohn
 */
public class ReadBookLoanBuilder extends BookLoanBuilder<ReadBookLoanBuilder> {
    private DatabaseConnection databaseConnection;

    public ReadBookLoanBuilder() {
        super();
        this.databaseConnection = DatabaseConnection.GetInstance();
    }

    @Override
    protected ReadBookLoanBuilder self() { return this; }

    /**
     * Sets the book ID condition for filtering book loan records.
     * 
     * @param bookID The book ID to filter by.
     * @return The current ReadBookLoanBuilder instance.
     */
    public ReadBookLoanBuilder WhereBookID(int bookID) {
        if(bookID <= 0) throw new IllegalStateException("Book ID cannot be less than or equal to 0");
        if(this.bookID != 0) throw new IllegalStateException("Book ID has already been set");

        this.bookID = bookID;
        return this.SetField("book_id", bookID);
    }

    /**
     * Sets the borrower ID condition for filtering book loan records.
     * 
     * @param borrowerID The borrower ID to filter by.
     * @return The current ReadBookLoanBuilder instance.
     */
    public ReadBookLoanBuilder WhereBorrowerID(int borrowerID) {
        if(borrowerID <= 0) throw new IllegalStateException("Borrower ID cannot be less than or equal to 0");
        if(this.borrowerID != 0) throw new IllegalStateException("Borrower ID has already been set");

        this.borrowerID = borrowerID;
        return this.SetField("borrower_id", borrowerID);
    }

    /**
     * Sets the due date condition for filtering book loan records.
     * 
     * @param dueDate The due date to filter by.
     * @return The current ReadBookLoanBuilder instance.
     */
    public ReadBookLoanBuilder WhereDueDate(Date dueDate) {
        if(dueDate == null) throw new IllegalStateException("Due date cannot be null or empty");
        if(this.dueDate != null) throw new IllegalStateException("Due date has been already set");

        this.dueDate = dueDate;
        return this.SetField("due_date", dueDate);
    }

    /**
     * Sets the borrowed date condition for filtering book loan records.
     * 
     * @param borrowDate The borrowed date to filter by.
     * @return The current ReadBookLoanBuilder instance.
     */
    public ReadBookLoanBuilder WhereBorrowedAt(Date borrowDate) {
        if(borrowDate == null) throw new IllegalStateException("Due date cannot be null or empty");
        if(this.borrowedAt != null) throw new IllegalStateException("Due date has been already set");

        this.borrowedAt = borrowDate;
        return this.SetField("borrowed_at", borrowDate);
    }

    /**
     * Sets the returned date condition for filtering book loan records.
     * 
     * @param returnDate The returned date to filter by.
     * @return The current ReadBookLoanBuilder instance.
     */
    public ReadBookLoanBuilder WhereReturnedAt(Date returnDate) {
        if(returnDate == null) throw new IllegalStateException("Due date cannot be null or empty");
        if(this.returnedAt != null) throw new IllegalStateException("Due date has been already set");

        this.returnedAt = returnDate;
        return this.SetField("returned_at", returnDate);
    }

    /**
     * Executes the read operation to fetch book loan records from the database.
     * 
     * @return A list of maps representing the fetched book loan records.
     */
    public List<Map<String, Object>> Read() {
        StringBuilder queyBuilder = new StringBuilder("SELECT * FROM book_loans");
        if(!this.GetStatements().isEmpty()) {
            queyBuilder.append(" WHERE ");
            queyBuilder.append(String.join(" AND ", this.GetStatements()));
        }

        String query = queyBuilder.toString();
        List<Map<String, Object>> results;
        try {
            System.out.println("Calling Read...");
            results = this.databaseConnection.ExecuteQuery(query, this.GetValues().toArray());
        } catch(Exception e) {
            System.out.println("Calling Failed...");
            throw e;
        }

        System.out.println("Calling success... " + results.toString());
        return results;
    }
}
