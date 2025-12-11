package lib.Borrower;

import config.DatabaseConnection;

/**
 * Builder class for deleting borrower records from the database.
 * 
 * @author dev-MichaelJohn
 */
public class DeleteBorrowerBuilder extends BorrowerBuilder<DeleteBorrowerBuilder> {
    private DatabaseConnection dbConnection;

    public DeleteBorrowerBuilder() {
        super();
        this.dbConnection = DatabaseConnection.GetInstance();
    }

    @Override
    protected DeleteBorrowerBuilder self() {
        return this;
    }

    /**
     * Sets the borrower ID condition for deleting a borrower record.
     * 
     * @param id The ID of the borrower to delete.
     * @return The current DeleteBorrowerBuilder instance.
     */
    public DeleteBorrowerBuilder WhereID(int id) {
        if(id <= 0) throw new IllegalArgumentException("Borrower ID must be positive");
        if(this.id != 0) throw new IllegalStateException("Borrower ID has already been set");

        this.id = id;
        return this.SetField("id", id);
    }

    /**
     * Sets the first name condition for deleting borrower records.
     * 
     * @param firstName The first name to filter by for deletion.
     * @return The current DeleteBorrowerBuilder instance.
     */
    public DeleteBorrowerBuilder WhereFirstName(String firstName) {
        if(firstName == null || firstName.trim().isEmpty()) throw new IllegalArgumentException("First name cannot be null or empty");
        if(this.firstName != null) throw new IllegalStateException("First name has already been set");

        this.firstName = firstName;
        return this.SetField("first_name", firstName);
    }

    /**
     * Sets the last name condition for deleting borrower records.
     * 
     * @param lastName The last name to filter by for deletion.
     * @return The current DeleteBorrowerBuilder instance.
     */
    public DeleteBorrowerBuilder WhereLastName(String lastName) {
        if(lastName == null || lastName.trim().isEmpty()) throw new IllegalArgumentException("Last name cannot be null or empty");
        if(this.lastName != null) throw new IllegalStateException("Last name has already been set");

        this.lastName = lastName;
        return this.SetField("last_name", lastName);
    }

    /**
     * Sets the contact number condition for deleting borrower records.
     * 
     * @param contactNum The contact number to filter by for deletion.
     * @return The current DeleteBorrowerBuilder instance.
     */
    public DeleteBorrowerBuilder WhereContactNum(String contactNum) {
        if(contactNum == null || contactNum.trim().isEmpty()) throw new IllegalArgumentException("Contact number cannot be null or empty");
        if(this.contactNum != null) throw new IllegalStateException("Contact number has already been set");

        this.contactNum = contactNum;
        return this.SetField("contact_num", contactNum);
    }

    /**
     * Executes the delete operation to remove borrower records from the database.
     * 
     * @return true if the deletion was successful, false otherwise.
     */
    public boolean Delete() {
        if(this.GetStatements().isEmpty()) throw new IllegalStateException("At least one field must be set for deletion");

        StringBuilder queryBuilder = new StringBuilder("DELETE FROM borrowers WHERE ");
        queryBuilder.append(String.join(" AND ", this.GetStatements()));

        String query = queryBuilder.toString();
        int rowsAffected;
        try {
            System.out.println("Calling Delete...");
            rowsAffected = this.dbConnection.ExecuteUpdate(query, this.GetValues().toArray());
        } catch (Exception e) {
            System.out.println("Calling failed!");
            throw e;
        }

        System.out.println("Calling success...");
        return rowsAffected > 0;
    }
}
