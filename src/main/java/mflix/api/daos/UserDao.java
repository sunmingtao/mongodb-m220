package mflix.api.daos;

import static com.mongodb.client.model.Updates.set;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

import java.util.Map;

import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.mongodb.MongoClientSettings;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;

import mflix.api.models.Session;
import mflix.api.models.User;

@Configuration
public class UserDao extends AbstractMFlixDao {

  private final MongoCollection<User> usersCollection;
  private final MongoCollection<Session> sessionsCollection;

  private final Logger log;

  @Autowired
  public UserDao(
      MongoClient mongoClient, @Value("${spring.mongodb.database}") String databaseName) {
    super(mongoClient, databaseName);
    CodecRegistry pojoCodecRegistry =
        fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(),
            fromProviders(PojoCodecProvider.builder().automatic(true).build()));

    usersCollection = db.getCollection("users", User.class).withCodecRegistry(pojoCodecRegistry);
    log = LoggerFactory.getLogger(this.getClass());
    sessionsCollection = db.getCollection("sessions", Session.class).withCodecRegistry(pojoCodecRegistry);
  }

  /**
   * Inserts the `user` object in the `users` collection.
   *
   * @param user - User object to be added
   * @return True if successful, throw IncorrectDaoOperation otherwise
   */
  public boolean addUser(User user) {
    try {
        if (getUser((user.getEmail())) == null ){
            usersCollection.withWriteConcern(WriteConcern.MAJORITY).insertOne(user);
            return true;
        }
    }catch(Exception e) {
        throw new IncorrectDaoOperation("");
    }
    return false;
  }

  /**
   * Creates session using userId and jwt token.
   *
   * @param userId - user string identifier
   * @param jwt - jwt string token
   * @return true if successful
   */
  public boolean createUserSession(String userId, String jwt) {
      Bson updateFilter = new Document("user_id", userId);
      Bson setUpdate = Updates.set("jwt", jwt);
      UpdateOptions options = new UpdateOptions().upsert(true);
      sessionsCollection.updateOne(updateFilter, setUpdate, options);
      return true;
  }

  /**
   * Returns the User object matching the an email string value.
   *
   * @param email - email string to be matched.
   * @return User object or null.
   */
  public User getUser(String email) {
    User user = usersCollection.find(new Document("email", email)).first();
    return user;
  }

  /**
   * Given the userId, returns a Session object.
   *
   * @param userId - user string identifier.
   * @return Session object or null.
   */
  public Session getUserSession(String userId) {
    return sessionsCollection.find(new Document("user_id", userId)).first();
  }

  public boolean deleteUserSessions(String userId) {
    return sessionsCollection.deleteOne(new Document("user_id", userId)).wasAcknowledged();
  }

  /**
   * Removes the user document that match the provided email.
   *
   * @param email - of the user to be deleted.
   * @return true if user successfully removed
   */
  public boolean deleteUser(String email) {
      deleteUserSessions(email);
    return usersCollection.deleteOne(new Document("email", email)).wasAcknowledged();
  }

  /**
   * Updates the preferences of an user identified by `email` parameter.
   *
   * @param email - user to be updated email
   * @param userPreferences - set of preferences that should be stored and replace the existing
   *     ones. Cannot be set to null value
   * @return User object that just been updated.
   */
  public boolean updateUserPreferences(String email, Map<String, ?> userPreferences) {
    //TODO> Ticket: User Preferences - implement the method that allows for user preferences to
    // be updated.
    //TODO > Ticket: Handling Errors - make this method more robust by
    // handling potential exceptions when updating an entry.
      if (userPreferences == null) {
          throw new IncorrectDaoOperation("User preferences cannot be null");
      }
    return usersCollection.updateOne(new Document("email", email), set("preferences", userPreferences)).wasAcknowledged();
  }
}
