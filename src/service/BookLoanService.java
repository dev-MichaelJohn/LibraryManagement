package service;

import lib.BookLoan.DeleteBookLoanBuilder;
import lib.BookLoan.InsertBookLoanBuilder;
import lib.BookLoan.ReadBookLoanBuilder;
import lib.BookLoan.UpdateBookLoanBuilder;


/**
 * Service class providing static methods to access book loan CRUD functionalities.
 */
public class BookLoanService {
    /**
     * Method that returns a builder to insert a new book loan into the database.
     * 
     * @return InsertBookLoanBuilder instance
     */
    public InsertBookLoanBuilder InsertBookLoan() {
        return new InsertBookLoanBuilder();
    }

    /**
     * Method that returns a builder to read book loans from the database.
     * 
     * @return ReadBookLoanBuilder instance
     */
    public ReadBookLoanBuilder ReadBookLoan() {
        return new ReadBookLoanBuilder();
    }

    /**
     * Method that returns a builder to update existing book loans in the database.
     * 
     * @return UpdateBookLoanBuilder instance
     */
    public UpdateBookLoanBuilder UpdateBookLoan() {
        return new UpdateBookLoanBuilder();
    }

    /**
     * Method that returns a builder to delete book loans from the database.
     * 
     * @return DeleteBookLoanBuilder instance
     */
    public DeleteBookLoanBuilder DeleteBookLoan() {
        return new DeleteBookLoanBuilder();
    }
}
