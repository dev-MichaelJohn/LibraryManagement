package lib.Borrower;

import config.DatabaseConnection;

/**
 * Builder class for inserting new borrower records into the database.
 * 
 * @author dev-MichaelJohn
 */
public class InsertBorrowerBuilder extends BorrowerBuilder<InsertBorrowerBuilder> {
    private DatabaseConnection dbConnection;

    public InsertBorrowerBuilder() {
        super();
        this.dbConnection = DatabaseConnection.GetInstance();
    }

    @Override
    protected InsertBorrowerBuilder self() {
        return this;
    }

    /**
     * Sets the first name of the borrower.
     * 
     * @param firstName The first name of the borrower.
     * @return The current InsertBorrowerBuilder instance.
     */
    public InsertBorrowerBuilder SetFirstName(String firstName) {
        if(firstName == null || firstName.trim().isEmpty()) throw new IllegalArgumentException("First name cannot be null or empty");
        if(firstName.length() > 256) throw new IllegalArgumentException("First name cannot exceed 256 characters");
        if(this.firstName != null) throw new IllegalStateException("First name has already been set");

        this.firstName = firstName;
        return this.SetField("first_name", firstName);
    }

    /**
     * Sets the middle name of the borrower. Middle name can be empty.
     * 
     * @param middleName The middle name of the borrower (optional, can be empty).
     * @return The current InsertBorrowerBuilder instance.
     */
    public InsertBorrowerBuilder SetMiddleName(String middleName) {
        if(middleName == null) middleName = "";
        if(middleName.length() > 256) throw new IllegalArgumentException("Middle name cannot exceed 256 characters");
        if(this.middleName != null) throw new IllegalStateException("Middle name has already been set");

        this.middleName = middleName;
        return this.SetField("middle_name", middleName.isEmpty() ? "" : middleName);
    }

    /**
     * Sets the last name of the borrower.
     * 
     * @param lastName The last name of the borrower.
     * @return The current InsertBorrowerBuilder instance.
     */
    public InsertBorrowerBuilder SetLastName(String lastName) {
        if(lastName == null || lastName.trim().isEmpty()) throw new IllegalArgumentException("Last name cannot be null or empty");
        if(lastName.length() > 256) throw new IllegalArgumentException("Last name cannot exceed 256 characters");
        if(this.lastName != null) throw new IllegalStateException("Last name has already been set");

        this.lastName = lastName;
        return this.SetField("last_name", lastName);
    }

    /**
     * Sets the contact number of the borrower (Philippine standard: 11 digits).
     * 
     * @param contactNum The contact number of the borrower.
     * @return The current InsertBorrowerBuilder instance.
     */
    public InsertBorrowerBuilder SetContactNum(String contactNum) {
        if(contactNum == null || contactNum.trim().isEmpty()) throw new IllegalArgumentException("Contact number cannot be null or empty");
        if(contactNum.length() != 11) throw new IllegalArgumentException("Contact number must be exactly 11 digits (Philippine standard)");
        if(!contactNum.matches("\\d+")) throw new IllegalArgumentException("Contact number must contain only digits");
        if(this.contactNum != null) throw new IllegalStateException("Contact number has already been set");

        this.contactNum = contactNum;
        return this.SetField("contact_num", contactNum);
    }

    /**
     * Executes the insert operation to add the new borrower record to the database.
     * 
     * @return true if the insert was successful, false otherwise.
     */
    public boolean Insert() {
        if(this.firstName == null) throw new IllegalStateException("First name must be set before inserting");
        if(this.middleName == null) throw new IllegalStateException("Middle name must be set before inserting");
        if(this.lastName == null) throw new IllegalStateException("Last name must be set before inserting");
        if(this.contactNum == null) throw new IllegalStateException("Contact number must be set before inserting");

        String baseStatement = "INSERT INTO borrowers (first_name, middle_name, last_name, contact_num) VALUES (?, ?, ?, ?)";
        int rowsAffected;
        try {
            rowsAffected = dbConnection.ExecuteUpdate(baseStatement, this.GetValues().toArray());
        } catch(Exception e) {
            throw e;
        }

        return rowsAffected > 0;
    }
}
