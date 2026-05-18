package model;
import java.util.HashSet;
import java.util.Set;
import exception.DuplicateCustomerException;
import exception.InvalidEmailIdException;
import exception.InvalidPhoneNoException;

public class Customer implements Cloneable {

    private static final Set<Integer> existingCustomerIDs = new HashSet<>();

    private final int customerID;
    private String name;
    private String phoneNo;
    private String emailId;
    private String address;
    //Constructor
    public Customer(int customerID,String name,String phoneNo,String emailId,String address)throws DuplicateCustomerException,InvalidPhoneNoException,InvalidEmailIdException {

// Customer ID validation
        if (customerID <= 0) {
            throw new IllegalArgumentException("Customer ID must be positive");
        }
// Duplicate ID validation
        if (existingCustomerIDs.contains(customerID)) {
            throw new DuplicateCustomerException("Customer ID already exists");
        }
        existingCustomerIDs.add(customerID);

        this.customerID = customerID;
        this.name = name;

// setters for validation
        setPhoneNo(phoneNo);
        setEmailID(emailId);

        this.address = address;
    }

    //GEtters
    public int getCustomerID() {
        return customerID;
    }

    public String getName() {
        return name;
    }

    public String getPhoneNo() {
        return phoneNo;
    }

    public String getEmailID() {
        return emailId;
    }

    public String getAddress() {
        return address;
    }

    //Setters
    public void setName(String name) {
        this.name = name;
    }

    public void setPhoneNo(String phoneNo)
            throws InvalidPhoneNoException {
        if (phoneNo == null || phoneNo.length() != 10 || !phoneNo.matches("\\d+")) {
            throw new InvalidPhoneNoException("Phone number must contain exactly 10 digits");
        }
        this.phoneNo = phoneNo;
    }

    public void setEmailID(String emailId)
            throws InvalidEmailIdException {
        if (emailId == null || !emailId.contains("@")) {
            throw new InvalidEmailIdException("Email ID must contain '@'");
        }
        this.emailId = emailId;
    }

    public void setAddress(String address) {
        this.address = address;
    }
    
}
