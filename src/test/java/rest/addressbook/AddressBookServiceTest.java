package rest.addressbook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;


import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.junit.After;
import org.junit.Test;
import rest.addressbook.config.ApplicationConfig;
import rest.addressbook.domain.AddressBook;
import rest.addressbook.domain.Person;

/**
 * A simple test suite.
 * <ul>
 *   <li>Safe and idempotent: verify that two identical consecutive requests do not modify
 *   the state of the server.</li>
 *   <li>Not safe and idempotent: verify that only the first of two identical consecutive
 *   requests modifies the state of the server.</li>
 *   <li>Not safe nor idempotent: verify that two identical consecutive requests modify twice
 *   the state of the server.</li>
 * </ul>
 */
public class AddressBookServiceTest {

  private HttpServer server;

  @Test
  public void serviceIsAlive() throws IOException {
    // Prepare server
    AddressBook ab = new AddressBook();
    launchServer(ab);
    List<Person> oldContactList = new ArrayList<>(ab.getPersonList());
    // Request the address book
    Client client = ClientBuilder.newClient();
    Response response = client.target("http://localhost:8282/contacts")
      .request().get();
    assertEquals(200, response.getStatus());
    assertEquals(0, response.readEntity(AddressBook.class).getPersonList()
      .size());

    //////////////////////////////////////////////////////////////////////
    // Verify that GET /contacts is well implemented by the service, i.e
    // complete the test to ensure that it is safe and idempotent
    //////////////////////////////////////////////////////////////////////



    //safety verification.
    List<Person> newContactList = ab.getPersonList();
    assertEquals(oldContactList.size(), newContactList.size());


    //idempotency verification.
    Response secondResponse = client.target("http://localhost:8282/contacts").request().get();
    assertEquals(200, secondResponse.getStatus());
    assertEquals(0, secondResponse.readEntity(AddressBook.class).getPersonList().size());
  }

  @Test
  public void createUser() throws IOException {
    // Prepare server
    AddressBook ab = new AddressBook();
    launchServer(ab);
    List<Person> oldContactList = new ArrayList<>(ab.getPersonList());

    // Prepare data
    Person juan = new Person();
    juan.setName("Juan");
    URI juanURI = URI.create("http://localhost:8282/contacts/person/1");

    // Create a new user
    Client client = ClientBuilder.newClient();
    Response response = client.target("http://localhost:8282/contacts")
      .request(MediaType.APPLICATION_JSON)
      .post(Entity.entity(juan, MediaType.APPLICATION_JSON));

    assertEquals(201, response.getStatus());
    assertEquals(juanURI, response.getLocation());
    assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
    Person juanUpdated = response.readEntity(Person.class);
    assertEquals(juan.getName(), juanUpdated.getName());
    assertEquals(1, juanUpdated.getId());
    assertEquals(juanURI, juanUpdated.getHref());

    // Check that the new user exists
    response = client.target("http://localhost:8282/contacts/person/1")
      .request(MediaType.APPLICATION_JSON).get();
    assertEquals(200, response.getStatus());
    assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
    juanUpdated = response.readEntity(Person.class);
    assertEquals(juan.getName(), juanUpdated.getName());
    assertEquals(1, juanUpdated.getId());
    assertEquals(juanURI, juanUpdated.getHref());

    //////////////////////////////////////////////////////////////////////
    // Verify that POST /contacts is well implemented by the service, i.e
    // complete the test to ensure that it is not safe and not idempotent
    //////////////////////////////////////////////////////////////////////

    // Not safe.
    List<Person> newContactList = ab.getPersonList();
    assertEquals(oldContactList.size() + 1, newContactList.size());


    // Not idempotent
    Client secondClient = ClientBuilder.newClient();
    Response secondResponse = secondClient.target("http://localhost:8282/contacts")
            .request(MediaType.APPLICATION_JSON)
            .post(Entity.entity(juan, MediaType.APPLICATION_JSON));

    Person secondJuanUpdated = secondResponse.readEntity(Person.class);

    assertEquals(201, secondResponse.getStatus());
    assertEquals(MediaType.APPLICATION_JSON_TYPE, secondResponse.getMediaType());

    assertNotEquals(juanURI, secondResponse.getLocation());
    assertNotEquals(1, secondJuanUpdated.getId());
    assertNotEquals(juanURI, secondJuanUpdated.getHref());

  }

  @Test
  public void createUsers() throws IOException {
    // Prepare server
    AddressBook ab = new AddressBook();
    Person salvador = new Person();
    salvador.setName("Salvador");
    salvador.setId(ab.nextId());
    ab.getPersonList().add(salvador);
    launchServer(ab);

    // Prepare data
    Person juan = new Person();
    juan.setName("Juan");
    URI juanURI = URI.create("http://localhost:8282/contacts/person/2");
    Person maria = new Person();
    maria.setName("Maria");
    URI mariaURI = URI.create("http://localhost:8282/contacts/person/3");

    // Create a user
    Client client = ClientBuilder.newClient();
    Response response = client.target("http://localhost:8282/contacts")
      .request(MediaType.APPLICATION_JSON)
      .post(Entity.entity(juan, MediaType.APPLICATION_JSON));
    assertEquals(201, response.getStatus());
    assertEquals(juanURI, response.getLocation());

    // Create a second user
    response = client.target("http://localhost:8282/contacts")
      .request(MediaType.APPLICATION_JSON)
      .post(Entity.entity(maria, MediaType.APPLICATION_JSON));
    assertEquals(201, response.getStatus());
    assertEquals(mariaURI, response.getLocation());
    assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
    Person mariaUpdated = response.readEntity(Person.class);
    assertEquals(maria.getName(), mariaUpdated.getName());
    assertEquals(3, mariaUpdated.getId());
    assertEquals(mariaURI, mariaUpdated.getHref());

    List<Person> oldContactList = new ArrayList<>(ab.getPersonList());

    // Check that the new user exists
    response = client.target("http://localhost:8282/contacts/person/3")
      .request(MediaType.APPLICATION_JSON).get();
    assertEquals(200, response.getStatus());
    assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
    mariaUpdated = response.readEntity(Person.class);
    assertEquals(maria.getName(), mariaUpdated.getName());
    assertEquals(3, mariaUpdated.getId());
    assertEquals(mariaURI, mariaUpdated.getHref());

    //////////////////////////////////////////////////////////////////////
    // Verify that GET /contacts/person/3 is well implemented by the service, i.e
    // complete the test to ensure that it is safe and idempotent
    //////////////////////////////////////////////////////////////////////

    // Safe
    List<Person> newContactList = ab.getPersonList();
    assertEquals(oldContactList.size(), newContactList.size());


    // Idempotent
    Client secondClient = ClientBuilder.newClient();
    Response secondResponse = secondClient.target("http://localhost:8282/contacts/person/3")
            .request(MediaType.APPLICATION_JSON).get();
    assertEquals(200, response.getStatus());
    assertEquals(MediaType.APPLICATION_JSON_TYPE, secondResponse.getMediaType());
    mariaUpdated = secondResponse.readEntity(Person.class);
    assertEquals(maria.getName(), mariaUpdated.getName());
    assertEquals(3, mariaUpdated.getId());
    assertEquals(mariaURI, mariaUpdated.getHref());

  }

  @Test
  public void listUsers() throws IOException {

    // Prepare server
    AddressBook ab = new AddressBook();
    Person salvador = new Person();
    salvador.setName("Salvador");
    Person juan = new Person();
    juan.setName("Juan");
    ab.getPersonList().add(salvador);
    ab.getPersonList().add(juan);
    launchServer(ab);

    List<Person> oldContactList = new ArrayList<>(ab.getPersonList());

    // Test list of contacts
    Client client = ClientBuilder.newClient();
    Response response = client.target("http://localhost:8282/contacts")
      .request(MediaType.APPLICATION_JSON).get();
    assertEquals(200, response.getStatus());
    assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
    AddressBook addressBookRetrieved = response
      .readEntity(AddressBook.class);
    assertEquals(2, addressBookRetrieved.getPersonList().size());
    assertEquals(juan.getName(), addressBookRetrieved.getPersonList()
      .get(1).getName());

    //////////////////////////////////////////////////////////////////////
    // Verify that GET /contacts is well implemented by the service, i.e
    // complete the test to ensure that it is safe and idempotent
    //////////////////////////////////////////////////////////////////////

    // Safe
    List<Person> newContactList = ab.getPersonList();
    assertEquals(oldContactList.size(), newContactList.size());

    // Idempotent
    Client secondClient = ClientBuilder.newClient();
    Response secondResponse = secondClient.target("http://localhost:8282/contacts")
            .request(MediaType.APPLICATION_JSON).get();
    assertEquals(200, secondResponse.getStatus());
    assertEquals(MediaType.APPLICATION_JSON_TYPE, secondResponse.getMediaType());
    AddressBook secondAddressBookRetrieved = secondResponse
            .readEntity(AddressBook.class);
    assertEquals(2, secondAddressBookRetrieved.getPersonList().size());
    assertEquals(juan.getName(), secondAddressBookRetrieved.getPersonList()
            .get(1).getName());
  }

  @Test
  public void updateUsers() throws IOException {
    // Prepare server
    AddressBook ab = new AddressBook();
    Person salvador = new Person();
    salvador.setName("Salvador");
    salvador.setId(ab.nextId());
    Person juan = new Person();
    juan.setName("Juan");
    juan.setId(ab.getNextId());
    URI juanURI = URI.create("http://localhost:8282/contacts/person/2");
    ab.getPersonList().add(salvador);
    ab.getPersonList().add(juan);
    launchServer(ab);

    List<Person> oldContactList = new ArrayList<>(ab.getPersonList());

    // Update Maria
    Person maria = new Person();
    maria.setName("Maria");
    Client client = ClientBuilder.newClient();
    Response response = client
      .target("http://localhost:8282/contacts/person/2")
      .request(MediaType.APPLICATION_JSON)
      .put(Entity.entity(maria, MediaType.APPLICATION_JSON));
    assertEquals(200, response.getStatus());
    assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
    Person juanUpdated = response.readEntity(Person.class);
    assertEquals(maria.getName(), juanUpdated.getName());
    assertEquals(2, juanUpdated.getId());
    assertEquals(juanURI, juanUpdated.getHref());

    // Verify that the update is real
    response = client.target("http://localhost:8282/contacts/person/2")
      .request(MediaType.APPLICATION_JSON).get();
    assertEquals(200, response.getStatus());
    assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
    Person mariaRetrieved = response.readEntity(Person.class);
    assertEquals(maria.getName(), mariaRetrieved.getName());
    assertEquals(2, mariaRetrieved.getId());
    assertEquals(juanURI, mariaRetrieved.getHref());

    // Verify that only can be updated existing values
    response = client.target("http://localhost:8282/contacts/person/3")
      .request(MediaType.APPLICATION_JSON)
      .put(Entity.entity(maria, MediaType.APPLICATION_JSON));
    assertEquals(400, response.getStatus());

    //////////////////////////////////////////////////////////////////////
    // Verify that PUT /contacts/person/2 is well implemented by the service, i.e
    // complete the test to ensure that it is idempotent but not safe
    //////////////////////////////////////////////////////////////////////

    // Not Safe
    List<Person> newContactList = ab.getPersonList();
    assertNotEquals(oldContactList.get(1).getName(), newContactList.get(1).getName());


    // Idempotent
    response = client
            .target("http://localhost:8282/contacts/person/2")
            .request(MediaType.APPLICATION_JSON)
            .put(Entity.entity(maria, MediaType.APPLICATION_JSON));
    assertEquals(200, response.getStatus());
    assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
    juanUpdated = response.readEntity(Person.class);
    assertEquals(maria.getName(), juanUpdated.getName());
    assertEquals(2, juanUpdated.getId());
    assertEquals(juanURI, juanUpdated.getHref());
  }

  @Test
  public void deleteUsers() throws IOException {
    // Prepare server
    AddressBook ab = new AddressBook();
    Person salvador = new Person();
    salvador.setName("Salvador");
    salvador.setId(1);
    Person juan = new Person();
    juan.setName("Juan");
    juan.setId(2);
    ab.getPersonList().add(salvador);
    ab.getPersonList().add(juan);
    launchServer(ab);

    List<Person> oldContactList = new ArrayList<>(ab.getPersonList());

    // Delete a user
    Client client = ClientBuilder.newClient();
    Response response = client
      .target("http://localhost:8282/contacts/person/2").request()
      .delete();
    assertEquals(204, response.getStatus());

    // Verify that the user has been deleted
    response = client.target("http://localhost:8282/contacts/person/2")
      .request().delete();
    assertEquals(404, response.getStatus());

    //////////////////////////////////////////////////////////////////////
    // Verify that DELETE /contacts/person/2 is well implemented by the service, i.e
    // complete the test to ensure that it is idempotent but not safe
    //////////////////////////////////////////////////////////////////////

    // Not Safe
    List<Person> newContactList = ab.getPersonList();
    assertNotEquals(oldContactList.size(), newContactList.size());

    // Idempotent
    client = ClientBuilder.newClient();
    client.target("http://localhost:8282/contacts/person/2").request().delete();

    response = client.target("http://localhost:8282/contacts/person/2").request().delete();
    assertEquals(404, response.getStatus());

  }

  @Test
  public void findUsers() throws IOException {
    // Prepare server
    AddressBook ab = new AddressBook();
    Person salvador = new Person();
    salvador.setName("Salvador");
    salvador.setId(1);
    Person juan = new Person();
    juan.setName("Juan");
    juan.setId(2);
    ab.getPersonList().add(salvador);
    ab.getPersonList().add(juan);
    launchServer(ab);

    // Test user 1 exists
    Client client = ClientBuilder.newClient();
    Response response = client
      .target("http://localhost:8282/contacts/person/1")
      .request(MediaType.APPLICATION_JSON).get();
    assertEquals(200, response.getStatus());
    assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
    Person person = response.readEntity(Person.class);
    assertEquals(person.getName(), salvador.getName());
    assertEquals(person.getId(), salvador.getId());
    assertEquals(person.getHref(), salvador.getHref());

    // Test user 2 exists
    response = client.target("http://localhost:8282/contacts/person/2")
      .request(MediaType.APPLICATION_JSON).get();
    assertEquals(200, response.getStatus());
    assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
    person = response.readEntity(Person.class);
    assertEquals(person.getName(), juan.getName());
    assertEquals(2, juan.getId());
    assertEquals(person.getHref(), juan.getHref());

    // Test user 3 exists
    response = client.target("http://localhost:8282/contacts/person/3")
      .request(MediaType.APPLICATION_JSON).get();
    assertEquals(404, response.getStatus());
  }

  private void launchServer(AddressBook ab) throws IOException {
    URI uri = UriBuilder.fromUri("http://localhost/").port(8282).build();
    server = GrizzlyHttpServerFactory.createHttpServer(uri,
      new ApplicationConfig(ab));
    server.start();
  }

  @After
  public void shutdown() {
    if (server != null) {
      server.shutdownNow();
    }
    server = null;
  }

}
