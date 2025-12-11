package lib.Borrower;

import config.DatabaseConnection;

/**
 * Builder class for updating borrower records in the database.
 * 
 * @author dev-MichaelJohn
 */
public class UpdateBorrowerBuilder extends BorrowerBuilder<UpdateBorrowerBuilder> {
    private DatabaseConnection dbConnection;

    public UpdateBorrowerBuilder() {
        super();
        this.dbConnection = DatabaseConnection.GetInstance();
    }

    @Override
    protected UpdateBorrowerBuilder self() {
        return this;
    }

    /**
     * Sets the first name of the borrower to be updated.
     * 
     * @param firstName The first name of the borrower.
     * @return The current UpdateBorrowerBuilder instance.
     */
    public UpdateBorrowerBuilder SetFirstName(String firstName) {
        if(firstName == null || firstName.trim().isEmpty()) throw new IllegalArgumentException("First name cannot be null or empty");
        if(firstName.length() > 256) throw new IllegalArgumentException("First name cannot exceed 256 characters");
        if(this.firstName != null) throw new IllegalStateException("First name has already been set");

        this.firstName = firstName;
        return this.SetField("first_name", firstName);
    }

    /**
     * Sets the middle name of the borrower to be updated. Middle name can be empty.
     * 
     * @param middleName The middle name of the borrower (optional, can be empty).
     * @return The current UpdateBorrowerBuilder instance.
     */
    public UpdateBorrowerBuilder SetMiddleName(String middleName) {
        if(middleName == null) middleName = "";
        if(middleName.length() > 256) throw new IllegalArgumentException("Middle name cannot exceed 256 characters");
        if(this.middleName != null) throw new IllegalStateException("Middle name has already been set");

        this.middleName = middleName;
        return this.SetField("middle_name", middleName.isEmpty() ? "" : middleName);
    }

    /**
     * Sets the last name of the borrower to be updated.
     * 
     * @param lastName The last name of the borrower.
     * @return The current UpdateBorrowerBuilder instance.
     */
    public UpdateBorrowerBuilder SetLastName(String lastName) {
        if(lastName == null || lastName.trim().isEmpty()) throw new IllegalArgumentException("Last name cannot be null or empty");
        if(lastName.length() > 256) throw new IllegalArgumentException("Last name cannot exceed 256 characters");
        if(this.lastName != null) throw new IllegalStateException("Last name has already been set");

        this.lastName = lastName;
        return this.SetField("last_name", lastName);
    }

    /**
     * Sets the contact number of the borrower to be updated (Philippine standard: 11 digits).
     * 
     * @param contactNum The contact number of the borrower.
     * @return The current UpdateBorrowerBuilder instance.
     */
    public UpdateBorrowerBuilder SetContactNum(String contactNum) {
        if(contactNum == null || contactNum.trim().isEmpty()) throw new IllegalArgumentException("Contact number cannot be null or empty");
        if(contactNum.length() != 11) throw new IllegalArgumentException("Contact number must be exactly 11 digits (Philippine standard)");
        if(!contactNum.matches("\\d+")) throw new IllegalArgumentException("Contact number must contain only digits");
        if(this.contactNum != null) throw new IllegalStateException("Contact number has already been set");

        this.contactNum = contactNum;
        return this.SetField("contact_num", contactNum);
    }

    /**
     * Sets the borrower ID condition for selecting which borrower record to update.
     * 
     * @param id The ID of the borrower to update.
     * @return The current UpdateBorrowerBuilder instance.
     */
    public UpdateBorrowerBuilder WhereID(int id) {
        if(id <= 0) throw new IllegalArgumentException("Borrower ID must be positive");
        if(this.id != 0) throw new IllegalStateException("Borrower ID has already been set");

        this.id = id;
        return this;
    }

    /**
     * Executes the update operation to modify the borrower record in the database.
     * 
     * @return true if the update was successful, false otherwise.
     */
    public boolean Update() {
        if(this.id == 0) throw new IllegalStateException("Borrower ID must be set for update");
        if(this.GetStatements().isEmpty()) throw new IllegalStateException("At least one field must be set for update");

        StringBuilder queryBuilder = new StringBuilder("UPDATE borrowers SET ");
        queryBuilder.append(String.join(", ", this.GetStatements()));
        queryBuilder.append(" WHERE id = ?");

        String query = queryBuilder.toString();
        this.GetValues().add(this.id);
        int rowsAffected = this.dbConnection.ExecuteUpdate(query, this.GetValues().toArray());

        return rowsAffected > 0;
    }
}
