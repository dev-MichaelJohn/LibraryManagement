package lib.BookLoan;

import java.util.Date;
import config.DatabaseConnection;

public class DeleteBookLoanBuilder extends BookLoanBuilder<DeleteBookLoanBuilder> {
    protected DatabaseConnection databaseConnection;

    public DeleteBookLoanBuilder() {
        super();
        this.databaseConnection = DatabaseConnection.GetInstance();
    }

    @Override
    protected DeleteBookLoanBuilder self() { return this; }

    /**
     * Sets the due date condition for deleting book loan records.
     * 
     * @param dueDate The due date to filter by for deletion.
     * @return The current DeleteBookLoanBuilder instance.
     */
    public DeleteBookLoanBuilder WhereDueDate(Date dueDate) {
        if(dueDate == null) throw new IllegalStateException("Due date cannot be null or empty");
        if(this.dueDate != null) throw new IllegalStateException("Due date has been already set");

        this.dueDate = dueDate;
        return this.SetField("due_date", dueDate);
    }

    /**
     * Sets the borrowed date condition for deleting book loan records.
     * 
     * @param borrowDate The borrowed date to filter by for deletion.
     * @return The current DeleteBookLoanBuilder instance.
     */
    public DeleteBookLoanBuilder WhereBorrowedAt(Date borrowDate) {
        if(borrowDate == null) throw new IllegalStateException("Borrowed date cannot be null or empty");
        if(this.borrowedAt != null) throw new IllegalStateException("Borrowed date has been already set");

        this.borrowedAt = borrowDate;
        return this.SetField("borrowed_at", borrowDate);
    }

    /**
     * Sets the returned date condition for deleting book loan records.
     * 
     * @param returnDate The returned date to filter by for deletion.
     * @return The current DeleteBookLoanBuilder instance.
     */
    public DeleteBookLoanBuilder WhereReturnedAt(Date returnDate) {
        if(returnDate == null) throw new IllegalStateException("Returned date cannot be null or empty");
        if(this.returnedAt != null) throw new IllegalStateException("Returned date has been already set");

        this.returnedAt = returnDate;
        return this.SetField("returned_at", returnDate);
    }

    /**
     * Sets the book ID condition for deleting book loan records.
     * 
     * @param bookID The book ID to filter by for deletion.
     * @return The current DeleteBookLoanBuilder instance.
     */
    public DeleteBookLoanBuilder WhereBookID(int bookID) {
        if(bookID <= 0) throw new IllegalStateException("Book ID cannot be less than or equal to 0");
        if(this.bookID != 0) throw new IllegalStateException("Book ID has already been set");

        this.bookID = bookID;
        return this.SetField("book_id", bookID);
    }

    /**
     * Sets the borrower ID condition for deleting book loan records.
     * 
     * @param borrowerID The borrower ID to filter by for deletion.
     * @return The current DeleteBookLoanBuilder instance.
     */
    public DeleteBookLoanBuilder WhereBorrowerID(int borrowerID) {
        if(borrowerID <= 0) throw new IllegalStateException("Borrower ID cannot be less than or equal to 0");
        if(this.borrowerID != 0) throw new IllegalStateException("Borrower ID has already been set");

        this.borrowerID = borrowerID;
        return this.SetField("borrower_id", borrowerID);
    }
    
    /**
     * Sets the ID condition for deleting a specific book loan record.
     * 
     * @param id The loan ID to delete.
     * @return The current DeleteBookLoanBuilder instance.
     */
    public DeleteBookLoanBuilder WhereID(int id) {
        if(id <= 0) throw new IllegalStateException("ID cannot be less than or equal to 0");
        if(this.id != 0) throw new IllegalStateException("ID has already been set");

        this.id = id;
        return this.SetField("id", id);
    }

    /**
     * Executes the delete operation to remove book loan records from the database.
     * 
     * @return true if the deletion was successful, false otherwise.
     */
    public boolean Delete() {
        if(this.GetStatements().isEmpty()) throw new IllegalStateException("At least one field must be set for deletion");

        StringBuilder queryBuilder = new StringBuilder("DELETE FROM book_loans WHERE ");
        queryBuilder.append(String.join(" AND ", this.GetStatements()));

        String query = queryBuilder.toString();
        int rowsAffected;
        try {
            System.out.println("Calling Delete...");
            rowsAffected = this.databaseConnection.ExecuteUpdate(query, this.GetValues().toArray());
        } catch (Exception e) {
            System.out.println("Calling failed!");
            throw e;
        }

        System.out.println("Calling sucess...");
        return rowsAffected > 0;
    }
}
