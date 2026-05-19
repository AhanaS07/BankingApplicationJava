package model;

public class Customer implements Cloneable {

    private String customerId;
    private String name;
    private String email;
    private String phoneNumber;

    public Customer(String customerId, String name, String email, String phoneNumber) {
        this.customerId = customerId;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    // Deep cloning
    @Override
    public Customer clone() throws CloneNotSupportedException {
        Customer cloned = (Customer) super.clone();
        cloned.customerId  = new String(this.customerId);
        cloned.name        = new String(this.name);
        cloned.email       = new String(this.email);
        cloned.phoneNumber = new String(this.phoneNumber);
        return cloned;
    }

    @Override
    public String toString() {
        return "Customer [ID=" + customerId + ", Name=" + name +
                ", Email=" + email + ", Phone=" + phoneNumber + "]";
    }
}
