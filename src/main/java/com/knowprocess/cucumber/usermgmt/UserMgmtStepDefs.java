package com.knowprocess.cucumber.usermgmt;

import org.springframework.http.HttpStatus;

import com.knowprocess.cucumber.IntegrationTestSupport;

import cucumber.api.PendingException;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class UserMgmtStepDefs extends IntegrationTestSupport {
    
    @Given("^the server is available$")
    public void the_server_is_available() throws Throwable {
         executeGet("/").statusCodeIsAvailable();
    }

    @When("^a valid username and password are presented$")
    public void a_valid_username_and_password_are_presented() throws Throwable {
        // TODO add payload
//        executePost("/auth/login");
        throw new PendingException();
    }
    
    @Then("^success code and JWT access and refresh tokens are returned$")
    public void success_code_and_JWT_access_and_refresh_tokens_are_returned() throws Throwable {
        // Write code here that turns the phrase above into concrete actions
        throw new PendingException();
    }
    
    @Given("^the user is logged in$")
    public void the_user_is_logged_in() throws Throwable {
        // Write code here that turns the phrase above into concrete actions
        // throw new PendingException();
    }
    
    @When("^my profile is requested$")
    public void my_profile_is_requested() throws Throwable {
        executeGet("/users/"+usr).statusCodeIs(HttpStatus.OK);
    }

}
