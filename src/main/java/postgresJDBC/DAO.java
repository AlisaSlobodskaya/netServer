package postgresJDBC;

public interface DAO<Entity, Key> {
    boolean create(Entity model);

    boolean update(Entity model);

    boolean delete(Entity model);

    boolean writeMsg(Key key);

    Entity read(Key key);

    String[] getMsg();

    void clearMsgTable();
}
