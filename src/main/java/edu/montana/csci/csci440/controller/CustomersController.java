package edu.montana.csci.csci440.controller;

import edu.montana.csci.csci440.model.Customer;
import edu.montana.csci.csci440.model.Employee;
import edu.montana.csci.csci440.util.Web;

import java.util.List;

import static spark.Spark.get;
import static spark.Spark.post;

public class CustomersController extends BaseController {
    public static void init(){
        /* READ */
        get("/customers", (req, resp) -> {
            List<Customer> customers = Customer.all(Web.getCurrentPage(), Web.PAGE_SIZE);
            return renderTemplate("templates/customers/index.vm",
                    "customers", customers);
        });

        get("/customers/:id", (req, resp) -> {
            Customer customer = Customer.find(asInt(req.params(":id")));
            return renderTemplate("templates/customers/show.vm",
                    "customer", customer);
        });

        /* UPDATE */
        get("/customers/:id/edit", (req, resp) -> {
            Customer customer = Customer.find(asInt(req.params(":id")));
            return renderTemplate("templates/customers/edit.vm",
                    "customer", customer);
        });

        post("/customers/:id", (req, resp) -> {
            Customer cust = Customer.find(asInt(req.params(":id")));
            cust.setFirstName(req.queryParams("FirstName"));
            cust.setLastName(req.queryParams("LastName"));
            cust.setEmail(req.queryParams("Email"));
            cust.setSupportRepId(Long.parseLong(req.queryParams("EmployeeId")));
            if (cust.update()) {
                Web.showMessage("Updated Customer!");
                return Web.redirect("/customers/" + cust.getCustomerId());
            } else {
                Web.showErrorMessage("Could Not Update Customer!");
                return renderTemplate("templates/customers/edit.vm", "customer", cust);
            }
        });

        /* DELETE */
        get("/customers/:id/delete", (req, resp) -> {
            Customer customer = Customer.find(asInt(req.params(":id")));
            customer.delete();
            Web.showMessage("Deleted Customer " + customer.getEmail());
            return Web.redirect("/customers");
        });
    }
}
