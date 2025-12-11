package lib.Borrower;

import java.util.List;
import java.util.Map;

import config.DatabaseConnection;

/**
 * Builder class for reading borrower records from the database.
 * 
 * @author dev-MichaelJohn
 */
public class ReadBorrowerBuilder extends BorrowerBuilder<ReadBorrowerBuilder> {
    private DatabaseConnection dbConnection;

    public ReadBorrowerBuilder() {
        super();
        this.dbConnection = DatabaseConnection.GetInstance();
    }

    @Override
    protected ReadBorrowerBuilder self() {
        return this;
    }

    /**
     * Sets the borrower ID condition for fetching borrower records.
     * 
     * @param id The ID of the borrower to filter by.
     * @return The current ReadBorrowerBuilder instance.
     */
    public ReadBorrowerBuilder WhereID(int id) {
        if(id <= 0) throw new IllegalArgumentException("Borrower ID must be greater than 0");
        if(this.id != 0) throw new IllegalStateException("Borrower ID has already been set");

        this.id = id;
        return this.SetField("id", id);
    }

    /**
     * Sets the first name condition for fetching borrower records.
     * 
     * @param firstName The first name to filter by.
     * @return The current ReadBorrowerBuilder instance.
     */
    public ReadBorrowerBuilder WhereFirstName(String firstName) {
        if(firstName == null || firstName.trim().isEmpty()) throw new IllegalArgumentException("First name cannot be null or empty");
        if(this.firstName != null) throw new IllegalStateException("First name has already been set");

        this.firstName = firstName;
        return this.SetField("first_name LIKE ?", firstName + "%");
    }

    /**
     * Sets the last name condition for fetching borrower records.
     * 
     * @param lastName The last name to filter by.
     * @return The current ReadBorrowerBuilder instance.
     */
    public ReadBorrowerBuilder WhereLastName(String lastName) {
        if(lastName == null || lastName.trim().isEmpty()) throw new IllegalArgumentException("Last name cannot be null or empty");
        if(this.lastName != null) throw new IllegalStateException("Last name has already been set");

        this.lastName = lastName;
        return this.SetField("last_name LIKE ?", lastName + "%");
    }

    /**
     * Sets the contact number condition for fetching borrower records.
     * 
     * @param contactNum The contact number to filter by.
     * @return The current ReadBorrowerBuilder instance.
     */
    public ReadBorrowerBuilder WhereContactNum(String contactNum) {
        if(contactNum == null || contactNum.trim().isEmpty()) throw new IllegalArgumentException("Contact number cannot be null or empty");
        if(this.contactNum != null) throw new IllegalStateException("Contact number has already been set");

        this.contactNum = contactNum;
        return this.SetField("contact_num", contactNum);
    }

    /**
     * Executes the read operation to fetch borrower records from the database.
     * 
     * @return A list of maps representing the fetched borrower records.
     */
    public List<Map<String, Object>> Read() {
        StringBuilder queryBuilder = new StringBuilder("SELECT * FROM borrowers");
        if(!this.GetStatements().isEmpty()) {
            queryBuilder.append(" WHERE ");
            queryBuilder.append(String.join(" AND ", this.GetStatements()));
        }

        String query = queryBuilder.toString();
        List<Map<String, Object>> results;
        try {
            System.out.println("Calling Read...");
            results = this.dbConnection.ExecuteQuery(query, this.GetValues().toArray());
        } catch(Exception e) {
            System.out.println("Calling Failed...");
            throw e;
        }

        System.out.println("Calling success... " + results.toString());
        return results;
    }
}
