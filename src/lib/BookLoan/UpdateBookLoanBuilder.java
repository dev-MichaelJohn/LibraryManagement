package lib.BookLoan;

import java.util.Date;
import config.DatabaseConnection;

/**
 * Builder class for updating book loan records in the database.
 *
 * @author dev-MichaelJohn
 */
public class UpdateBookLoanBuilder extends BookLoanBuilder<UpdateBookLoanBuilder> {
    private DatabaseConnection databaseConnection;

    public UpdateBookLoanBuilder() {
        self();
        this.databaseConnection = DatabaseConnection.GetInstance();
    }

    @Override
    protected UpdateBookLoanBuilder self() { return this; }

    /**
     * Sets the due date value for the book loan record being updated.
     * 
     * @param dueDate The new due date for the loan.
     * @return The current UpdateBookLoanBuilder instance.
     */
    /**
     * Sets the due date value for the book loan record being updated.
     * 
     * @param dueDate The new due date for the loan.
     * @return The current UpdateBookLoanBuilder instance.
     */
    public UpdateBookLoanBuilder SetDueDate(Date dueDate) {
        if(dueDate == null) throw new IllegalStateException("Due date cannot be null or empty");
        if(this.dueDate != null) throw new IllegalStateException("Due date has been already set");

        this.dueDate = dueDate;
        return this.SetField("due_date", dueDate);
    }

    /**
     * Sets the borrower ID value for the book loan record being updated.
     * 
     * @param borrowerID The new borrower ID for the loan.
     * @return The current UpdateBookLoanBuilder instance.
     */
    public UpdateBookLoanBuilder SetBorrowerID(int borrowerID) {
        if(borrowerID <= 0) throw new IllegalStateException("Borrower ID cannot be less than or equal to 0");
        if(this.borrowerID != 0) throw new IllegalStateException("Borrower ID has already been set");

        this.borrowerID = borrowerID;
        return this.SetField("borrower_id", borrowerID);
    }

    /**
     * Sets the borrowed date value for the book loan record being updated.
     * 
     * @param borrowDate The new borrowed date for the loan.
     * @return The current UpdateBookLoanBuilder instance.
     */
    public UpdateBookLoanBuilder SetBorrowedAt(Date borrowDate) {
        if(borrowDate == null) throw new IllegalStateException("Due date cannot be null or empty");
        if(this.borrowedAt != null) throw new IllegalStateException("Due date has been already set");

        this.borrowedAt = borrowDate;
        return this.SetField("borrowed_at", borrowDate);
    }

    /**
     * Sets the returned date value for the book loan record being updated.
     * 
     * @param returnDate The returned date to set on the loan.
     * @return The current UpdateBookLoanBuilder instance.
     */
    public UpdateBookLoanBuilder SetReturnedAt(Date returnDate) {
        if(returnDate == null) throw new IllegalStateException("Due date cannot be null or empty");
        if(this.returnedAt != null) throw new IllegalStateException("Due date has been already set");

        this.returnedAt = returnDate;
        return this.SetField("returned_at", returnDate);
    }

    /**
     * Sets the ID condition for selecting which book loan record to update.
     * 
     * @param id The ID of the book loan record to update.
     * @return The current UpdateBookLoanBuilder instance.
     */
    public UpdateBookLoanBuilder WhereID(int id) {
        if(id <= 0) throw new IllegalStateException("ID cannot be less than or equal to 0");
        if(this.id != 0) throw new IllegalStateException("ID has already been set");

        this.id = id;
        return this.SetField("id", id);
    }

    /**
     * Executes the update operation to modify the book loan record in the database.
     * 
     * @return true if the update was successful, false otherwise.
     */
    public boolean Update() {
        if(this.id == 0) throw new IllegalStateException("Loan :qID must be set for update");
        if(this.GetStatements().isEmpty()) throw new IllegalStateException("At least one field must be set for update");

        StringBuilder queryBuilder = new StringBuilder("UPDATE book_loans SET ");
        queryBuilder.append(String.join(", ", this.GetStatements()));
        queryBuilder.append(" WHERE id = ?");

        String query = queryBuilder.toString();
        System.out.println(query);
        this.GetValues().add(this.id);
        int rowsAffected = this.databaseConnection.ExecuteUpdate(query, this.GetValues().toArray());
        
        return rowsAffected > 0;
    }
}