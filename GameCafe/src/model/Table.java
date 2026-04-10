package model;

public class Table {

    public int x, y;
    private Customer customer;
    public boolean isDirty = false;
    public boolean isLocked = false;

    public Table(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
        this.isDirty = false;
    }

    public void clear() {
        customer = null;
        isDirty = true; // Table becomes dirty after eating
    }
    
    public void clean() {
        isDirty = false;
    }
}