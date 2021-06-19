package postgresJDBC;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Client {
    private int id;
    private String login;
    private int passHash;
    private int passSalt;
    private int sessionKey;
}
