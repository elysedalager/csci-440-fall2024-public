package edu.montana.csci.csci440.model;

import edu.montana.csci.csci440.util.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Customer extends Model {

    private Long customerId;
    private Long supportRepId;
    private String firstName;
    private String lastName;
    private String email;

    public Employee getSupportRep() {
         return Employee.find(supportRepId);
    }

    public List<Invoice> getInvoices(){
        try {
            try (Connection connect = DB.connect();
                 PreparedStatement stmt = connect.prepareStatement(
                         "SELECT * FROM invoices WHERE CustomerId = ?")) {
                stmt.setLong(1, this.getCustomerId());

                ArrayList<Invoice> result = new ArrayList<>();
                ResultSet resultSet = stmt.executeQuery();
                while (resultSet.next()) {
                    result.add(new Invoice(resultSet));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching invoices for customer: " + this.getCustomerId(), e);
        }
    }

    public Customer(){
    }
    public Customer(ResultSet results) throws SQLException {
        firstName = results.getString("FirstName");
        lastName = results.getString("LastName");
        customerId = results.getLong("CustomerId");
        supportRepId = results.getLong("SupportRepId");
        email = results.getString("Email");
    }

    @Override
    public boolean verify() {
        _errors.clear(); // clear any existing errors
        if (firstName == null || "".equals(firstName)){
            addError("FirstName can't be null or blank!");
        }
        if (lastName == null || "".equals(lastName)){
            addError("LastName can't be null or blank!");
        }
        if (getEmail() == null || "".equals(getEmail())){
            addError("Email can't be null or blank!");
        }
        else if (!getEmail().contains("@")){
            addError("Email must contain an @");
        }
        if (getSupportRepId() == null || "".equals(getSupportRepId())){
            addError("Rep can't be null or blank!");
        }
        return !hasErrors();
    }

    @Override
    public void delete() {
        // Step 1: Get all invoices associated with the customer
        List<Invoice> invoices = this.getInvoices();

        // Step 2: Delete all invoice_items for each invoice
        for (Invoice invoice : invoices) {
            try (Connection conn = DB.connect();
                 PreparedStatement stmt = conn.prepareStatement("DELETE FROM invoice_items WHERE InvoiceId=?")) {
                stmt.setLong(1, invoice.getInvoiceId());
                stmt.executeUpdate();
            } catch (SQLException sqlException) {
                throw new RuntimeException("Error deleting invoice items for invoice: " + invoice.getInvoiceId(), sqlException);
            }
        }

        // Step 3: Delete the invoices
        try (Connection conn = DB.connect();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM invoices WHERE CustomerId=?")) {
            stmt.setLong(1, getCustomerId());
            stmt.executeUpdate();
        } catch (SQLException sqlException) {
            throw new RuntimeException("Error deleting invoices", sqlException);
        }

        // Step 4: Delete the customer
        try (Connection conn = DB.connect();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM customers WHERE CustomerId=?")) {
            stmt.setLong(1, getCustomerId());
            stmt.executeUpdate();
        } catch (SQLException sqlException) {
            throw new RuntimeException("Error deleting customer", sqlException);
        }

//        List<Invoice> invoices = this.getInvoices();
//        for (Invoice i : invoices) {
//            List<InvoiceItem> invoiceItems = i.getInvoiceItems();
//            for (InvoiceItem iT : invoiceItems) {
//                try (Connection conn = DB.connect(); PreparedStatement stmt = conn.prepareStatement("DELETE FROM invoice_items WHERE InvoiceId=?")) {
//                    stmt.setLong(1, i.getInvoiceId());
//                    stmt.executeUpdate();
//                } catch (SQLException sqlException) {
//                    throw new RuntimeException(sqlException);
//                }
//            }
//
//            try (Connection conn = DB.connect(); PreparedStatement stmt = conn.prepareStatement("DELETE FROM invoices WHERE CustomerId=?")) {
//                stmt.setLong(1, getCustomerId());
//                stmt.executeUpdate();
//            } catch (SQLException sqlException) {
//                throw new RuntimeException(sqlException);
//            }
//        }
//
//        try (Connection conn = DB.connect(); PreparedStatement stmt = conn.prepareStatement("DELETE FROM customers WHERE CustomerId=?")) {
//            stmt.setLong(1, getCustomerId());
//            stmt.executeUpdate();
//        } catch (SQLException sqlException) {
//            throw new RuntimeException(sqlException);
//        }
    }

    @Override
    public boolean update() {
        if (verify()) {
            try (Connection conn = DB.connect();
                 PreparedStatement stmt = conn.prepareStatement(
                         "UPDATE customers SET FirstName=?, LastName=?, Email=?, SupportRepId=? WHERE CustomerId=?")) {
                stmt.setString(1, this.getFirstName());
                stmt.setString(2, this.getLastName());
                stmt.setString(3, this.getEmail());
                stmt.setLong(4, this.getSupportRepId());
                stmt.executeUpdate();
                return true;
            } catch (SQLException sqlException) {
                throw new RuntimeException(sqlException);
            }
        } else {
            return false;
        }
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) { this.email = email; }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId){
        this.customerId = customerId;
    }

    public Long getSupportRepId() {
        return supportRepId;
    }

    public void setSupportRepId(Long repId){
        this.supportRepId = repId;
    }

    public static List<Customer> all() {
        return all(0, Integer.MAX_VALUE);
    }

    public static List<Customer> all(int page, int count) {
        try {
            try(Connection connect = DB.connect();
                PreparedStatement stmt = connect.prepareStatement("SELECT  * FROM customers LIMIT ? OFFSET ?")) {
                ArrayList<Customer> result = new ArrayList<>();
                int offset = (page == 0) ? (page * count) : (page - 1) * count;
                stmt.setInt(1, count);
                stmt.setInt(2, offset);
                ResultSet resultSet = stmt.executeQuery();
                while (resultSet.next()) {
                    result.add(new Customer(resultSet));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static Customer find(long customerId) {
        try {
            try (Connection conn = DB.connect();
                 PreparedStatement stmt = conn.prepareStatement(
                         "SELECT * FROM customers WHERE CustomerId = ?")) {
                stmt.setLong(1, customerId);
                ResultSet resultSet = stmt.executeQuery();
                if (resultSet.next()) {
                    return new Customer(resultSet);
                } else {
                    return null;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Customer> forEmployee(long employeeId) {
        return Collections.emptyList();
    }

}
