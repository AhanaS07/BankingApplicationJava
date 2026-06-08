package model;

public class Address implements Cloneable {
    private String street;
    private String city;
    private String state;
    private String postalCode;

    public Address(String street, String city, String state, String postalCode) {
        this.street = street;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
    }

    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    @Override
    public Address clone() throws CloneNotSupportedException {
        return (Address) super.clone();
    }

    @Override
    public String toString() {
        return street + ", " + city + ", " + state + " - " + postalCode;
    }
}
