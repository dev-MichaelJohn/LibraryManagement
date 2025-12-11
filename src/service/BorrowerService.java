package service;

import lib.Borrower.InsertBorrowerBuilder;
import lib.Borrower.ReadBorrowerBuilder;
import lib.Borrower.UpdateBorrowerBuilder;
import lib.Borrower.DeleteBorrowerBuilder;

/**
 * Service class that provides factory methods for Borrower builder operations.
 * 
 * @author dev-MichaelJohn
 */
public class BorrowerService {
    
    /**
     * Creates and returns a new InsertBorrowerBuilder instance.
     * 
     * @return A new InsertBorrowerBuilder for inserting borrower records.
     */
    public static InsertBorrowerBuilder InsertBorrower() {
        return new InsertBorrowerBuilder();
    }

    /**
     * Creates and returns a new ReadBorrowerBuilder instance.
     * 
     * @return A new ReadBorrowerBuilder for reading borrower records.
     */
    public static ReadBorrowerBuilder ReadBorrower() {
        return new ReadBorrowerBuilder();
    }

    /**
     * Creates and returns a new UpdateBorrowerBuilder instance.
     * 
     * @return A new UpdateBorrowerBuilder for updating borrower records.
     */
    public static UpdateBorrowerBuilder UpdateBorrower() {
        return new UpdateBorrowerBuilder();
    }

    /**
     * Creates and returns a new DeleteBorrowerBuilder instance.
     * 
     * @return A new DeleteBorrowerBuilder for deleting borrower records.
     */
    public static DeleteBorrowerBuilder DeleteBorrower() {
        return new DeleteBorrowerBuilder();
    }
}
