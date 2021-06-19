package postgresJDBC;

import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ClientDAO implements DAO<Client, String> {
    @NotNull
    private final Connection connection;

    public ClientDAO(final Connection connection) {
        this.connection = connection;
    }

    /**
     * Create Client in database.
     *
     * @return False if Client already exist. If creating success true.
     */
    @Override
    public boolean create(@NotNull final Client client) {
        boolean result = false;

        try (PreparedStatement statement = connection.prepareStatement(SQLClient.INSERT.QUERY)) {
            statement.setString(1, client.getLogin());
            statement.setInt(2, client.getPassHash());
            statement.setInt(3, client.getPassSalt());
            statement.setInt(4, client.getSessionKey());
            result = statement.executeQuery().next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Write Client message in table.
     *
     * @return True if success. False if fail.
     */
    @Override
    public boolean writeMsg(@NotNull final String message) {
        boolean result = false;

        try (PreparedStatement statement = connection.prepareStatement(SQLClient.WRITE_MSG.QUERY)) {
            statement.setString(1, message);
            result = statement.executeQuery().next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Get 20 latest messages from server.
     *
     * @return Array of messages.
     */
    @Override
    public String[] getMsg() {
        String[] array = new String[20];

        clearMsgTable();
        try (PreparedStatement statement = connection.prepareStatement(SQLClient.GET_MSG.QUERY)) {
            final ResultSet rs = statement.executeQuery();
            for (int i = 0; i < 20; i++) {
                if (rs.next()) {
                    array[i] = rs.getString("message");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return array;
    }

    /**
     * Clear early messages. Leave only the last 20.
     */
    @Override
    public void clearMsgTable() {
        try (PreparedStatement statement = connection.prepareStatement(SQLClient.CLEAR_MSG.QUERY)) {
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Select Client by id.
     *
     * @return Valid entity if she exist. If entity does not exist return empty Client with id = -1.
     */
    @Override
    public Client read(@NotNull final String login) {
        final Client result = new Client();
        result.setId(-1);

        try (PreparedStatement statement = connection.prepareStatement(SQLClient.GET.QUERY)) {
            statement.setString(1, login);
            final ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                result.setId(Integer.parseInt(rs.getString("id")));
                result.setLogin(login);
                result.setPassHash(rs.getInt("passHash"));
                result.setPassSalt(rs.getInt("passSalt"));
                result.setSessionKey(rs.getInt("sessionKey"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Update Client's password by id.
     *
     * @return True if success. False if fail.
     */
    @Override
    public boolean update(@NotNull final Client client) {
        boolean result = false;

        try (PreparedStatement statement = connection.prepareStatement(SQLClient.UPDATE.QUERY)) {
            statement.setInt(1, client.getPassHash());
            statement.setInt(2, client.getPassSalt());
            statement.setInt(3, client.getId());
            result = statement.executeQuery().next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Delete Client by id AND login AND password.
     *
     * @return true if Client was deleted. False if Client not exist.
     */
    @Override
    public boolean delete(@NotNull final Client client) {
        boolean result = false;

        try (PreparedStatement statement = connection.prepareStatement(SQLClient.DELETE.QUERY)) {
            statement.setInt(1, client.getId());
            statement.setString(2, client.getLogin());
            statement.setInt(3, client.getPassHash());
            statement.setInt(4, client.getPassSalt());
            result = statement.executeQuery().next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * SQL queries.
     */
    enum SQLClient {
        GET("SELECT id, login, passHash, passSalt, sessionKey FROM clients WHERE login = (?)"),
        INSERT("INSERT INTO Clients (id, login, passHash, passSalt, sessionKey) VALUES (DEFAULT, (?), (?), (?), (?)) RETURNING id"),
        DELETE("DELETE FROM Clients WHERE id = (?) AND login = (?) AND passHash = (?) AND passSalt = (?) RETURNING id"),
        UPDATE("UPDATE Clients SET passHash = (?), passSalt = (?) WHERE id = (?) RETURNING id"),
        WRITE_MSG("INSERT INTO Messages (id, message) VALUES (DEFAULT, (?)) RETURNING id"),
        GET_MSG("SELECT id, message FROM messages LIMIT 20"),
        CLEAR_MSG("DELETE FROM messages WHERE id<((select max(id) from messages)-19)");

        String QUERY;

        SQLClient(String QUERY) {
            this.QUERY = QUERY;
        }
    }
}
